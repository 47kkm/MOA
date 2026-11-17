package moa.budget.internal;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import moa.budget.BudgetEvents.BudgetSpentUpdated;
import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.expense.ExpenseEvents.ExpenseReclassified;
import moa.expense.ExpenseEvents.ExpenseRegistered;
import moa.expense.ExpenseService;
import moa.expense.ExpenseTotals;

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

    // 이전 값은 빼고 이후 값은 더한다 (04 설계 5.2, D4-2의 이전 · 이후 값)
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ExpenseReclassified event) {
        handle(event.storeId(), List.of(
                new Change(event.before().date(), event.before().categoryId(), -event.before().amount()),
                new Change(event.after().date(), event.after().categoryId(), event.after().amount())));
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

    // 그 달 예산이 없으면 끝 (요구사항정의서 8.5 "무시"). 바뀐 비용 분류를 상위로 올리고, 그 상위마다 지금 소진을 알린다
    private void handleMonth(Long storeId, YearMonth month, List<Change> changes) {
        if (budgets.findByStoreIdAndMonth(storeId, month.atDay(1)).isEmpty()) {
            return;
        }
        Map<Long, CategoryInfo> infos = categories.infoOf(storeId, changes.stream().map(Change::categoryId).toList());
        ExpenseTotals now = expenses.totals(storeId, month.atDay(1), month.atEndOfMonth());
        Set<Long> tops = new LinkedHashSet<>();
        changes.forEach(c -> tops.add(topOf(c.categoryId(), infos.get(c.categoryId()))));
        tops.forEach(top -> events.publishEvent(
                new BudgetSpentUpdated(storeId, month, top, now.byTop().getOrDefault(top, 0L))));
    }

    // 하위면 그 상위, 상위면 자신
    private static Long topOf(Long categoryId, CategoryInfo info) {
        return info == null || info.parentId() == null ? categoryId : info.parentId();
    }
}
