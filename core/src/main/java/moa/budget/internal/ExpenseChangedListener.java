package moa.budget.internal;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import moa.budget.BudgetEvents.BudgetExceeded;
import moa.budget.BudgetEvents.BudgetSpentUpdated;
import moa.budget.BudgetLevel;
import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.expense.ExpenseEvents.ExpenseDeleted;
import moa.expense.ExpenseEvents.ExpenseReclassified;
import moa.expense.ExpenseEvents.ExpenseRegistered;
import moa.expense.ExpenseEvents.ExpenseSnapshot;
import moa.expense.ExpenseEvents.ExpenseUpdated;
import moa.expense.ExpenseService;
import moa.expense.ExpenseTotals;
import moa.shared.Money;

// 비용이 커밋된 뒤 같은 스레드 · 새 트랜잭션에서 소진액 갱신 · 예산 초과 감지 이벤트를 낸다 (04 설계 0.3, 5.2)
// budget이 저장하는 것은 없다. 지금 소진은 expense.totals로 다시 계산한다 (D5-1)
@Component
class ExpenseChangedListener {

    private final MonthlyBudgetRepository budgets;
    private final ExpenseService expenses;
    private final CategoryService categories;
    private final ApplicationEventPublisher events;

    ExpenseChangedListener(MonthlyBudgetRepository budgets, ExpenseService expenses, CategoryService categories,
            ApplicationEventPublisher events) {
        this.budgets = budgets;
        this.expenses = expenses;
        this.categories = categories;
        this.events = events;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ExpenseRegistered event) {
        handle(event.storeId(), List.of(new Change(event.date(), event.categoryId(), event.amount())));
    }

    // 수정 · 재분류는 이전 값을 빼고 이후 값을 더한다 (04 설계 5.2, D4-2의 이전 · 이후 값)
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ExpenseUpdated event) {
        handle(event.storeId(), beforeAndAfter(event.before(), event.after()));
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ExpenseReclassified event) {
        handle(event.storeId(), beforeAndAfter(event.before(), event.after()));
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ExpenseDeleted event) {
        handle(event.storeId(), List.of(new Change(event.date(), event.categoryId(), -event.amount())));
    }

    private static List<Change> beforeAndAfter(ExpenseSnapshot before, ExpenseSnapshot after) {
        return List.of(new Change(before.date(), before.categoryId(), -before.amount()),
                new Change(after.date(), after.categoryId(), after.amount()));
    }

    // 비용 하나가 바꾼 것: 그 날짜의 달, 그 비용 분류, ±금액
    private record Change(LocalDate date, Long categoryId, long amount) {
    }

    // 달마다 따로 본다. 날짜를 다른 달로 옮기면 두 달이 바뀐다
    private void handle(Long storeId, List<Change> changes) {
        Map<YearMonth, List<Change>> byMonth = new LinkedHashMap<>();
        changes.forEach(c -> byMonth.computeIfAbsent(YearMonth.from(c.date()), m -> new ArrayList<>()).add(c));
        byMonth.forEach((month, monthChanges) -> handleMonth(storeId, month, monthChanges));
    }

    // 그 달 예산이 없으면 끝 (요구사항정의서 8.5 "무시")
    // 바뀐 비용 분류를 상위로 올려 상위마다 ±를 더한다. 합이 0인 상위(같은 상위 안의 이동, 메모만 수정)는 바뀌지 않은 것
    private void handleMonth(Long storeId, YearMonth month, List<Change> changes) {
        MonthlyBudget budget = budgets.findByStoreIdAndMonth(storeId, month.atDay(1)).orElse(null);
        if (budget == null) {
            return;
        }
        Map<Long, CategoryInfo> infos = categories.infoOf(storeId, changes.stream().map(Change::categoryId).toList());
        Map<Long, Long> deltaByTop = new LinkedHashMap<>();
        changes.forEach(c -> deltaByTop.merge(topOf(c.categoryId(), infos.get(c.categoryId())), c.amount(), Long::sum));
        ExpenseTotals now = expenses.totals(storeId, month.atDay(1), month.atEndOfMonth());
        deltaByTop.forEach((top, delta) -> {
            if (delta == 0) {
                return;
            }
            long after = now.byTop().getOrDefault(top, 0L);
            events.publishEvent(new BudgetSpentUpdated(storeId, month, top, after));
            budget.lineOf(top).ifPresent(line -> {
                if (crossedOver(line.getAmount(), after - delta, after)) {
                    events.publishEvent(
                            new BudgetExceeded(storeId, month, top, after, line.getAmount().getAmount()));
                }
            });
        });
        // 총액도 같은 규칙. 항목이 없는 상위의 비용도 총액에는 들어간다. 총액이면 categoryId null (D5-8)
        long totalDelta = deltaByTop.values().stream().mapToLong(Long::longValue).sum();
        if (totalDelta != 0 && crossedOver(budget.getTotal(), now.total() - totalDelta, now.total())) {
            events.publishEvent(
                    new BudgetExceeded(storeId, month, null, now.total(), budget.getTotal().getAmount()));
        }
    }

    // 이전 = 지금 − 이번 변화. 이전엔 초과가 아니었고 지금 초과면 선을 넘은 것 (04 설계 5.3)
    // 선 규칙은 BudgetUsage.level 한 곳에 있다. 다시 실행되면 이전이 음수로 계산될 수 있어 0으로 둔다
    private static boolean crossedOver(Money budget, long before, long after) {
        return BudgetUsage.of(budget, Money.create(Math.max(before, 0))).level() != BudgetLevel.OVER
                && BudgetUsage.of(budget, Money.create(after)).level() == BudgetLevel.OVER;
    }

    // 하위면 그 상위, 상위면 자신
    private static Long topOf(Long categoryId, CategoryInfo info) {
        return info == null || info.parentId() == null ? categoryId : info.parentId();
    }
}
