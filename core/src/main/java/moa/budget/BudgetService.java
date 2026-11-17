package moa.budget;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import moa.budget.BudgetEvents.MonthlyBudgetSet;
import moa.budget.internal.BudgetLine;
import moa.budget.internal.BudgetUsage;
import moa.budget.internal.MonthlyBudget;
import moa.budget.internal.MonthlyBudgetRepository;
import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.expense.ExpenseService;
import moa.expense.ExpenseTotals;
import moa.shared.ErrorCode;
import moa.shared.MoaException;
import moa.shared.Money;

@Service
public class BudgetService {

    private final MonthlyBudgetRepository budgets;
    private final ExpenseService expenses;
    private final CategoryService categories;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public BudgetService(MonthlyBudgetRepository budgets, ExpenseService expenses, CategoryService categories,
            Clock clock, ApplicationEventPublisher events) {
        this.budgets = budgets;
        this.expenses = expenses;
        this.categories = categories;
        this.clock = clock;
        this.events = events;
    }

    // 입력 규칙(400)을 먼저 보고, 그다음 예산 항목에 쓸 수 있는 비용 분류인가(409)를 본다 (04 설계 5.2)
    // 응답은 현황 조회와 같은 모양
    @Transactional
    // 그 달 예산이 있으면 덮어쓰기(change), 없으면 새로 잡기(set). 월 예산은 연월당 하나 (04 설계 5.3)
    // 덮어쓰기는 save를 부르지 않는다. 읽어 온 월 예산을 고치면 커밋 때 변경 감지가 UPDATE를 보낸다
    // 거부되면 트랜잭션이 되돌아가 change도 저장되지 않는다
    public BudgetStatus set(Long storeId, YearMonth yearMonth, Long total, List<BudgetLineInput> lines) {
        YearMonth thisMonth = YearMonth.now(clock);
        Optional<MonthlyBudget> existing = budgets.findByStoreIdAndMonth(storeId, yearMonth.atDay(1));
        boolean changed;
        if (existing.isPresent()) {
            Set<Long> before = categoryIds(existing.get());
            // 바뀐 것이 없으면 change가 false를 돌려준다. 아무 일 없고 이벤트도 없다 (04 설계 5.2)
            changed = existing.get().change(total, lines, thisMonth);
            rejectUnavailable(storeId, existing.get(), before);
        } else {
            MonthlyBudget budget = MonthlyBudget.set(storeId, yearMonth, total, lines, thisMonth);
            rejectUnavailable(storeId, budget, Set.of());
            budgets.save(budget);
            changed = true;
        }
        if (changed) {
            events.publishEvent(new MonthlyBudgetSet(storeId, yearMonth));
        }
        return status(storeId, yearMonth);
    }

    // 행은 상위 비용 분류 순서 (category.tops). 소진은 저장하지 않고 매번 계산한다 (04 설계 5.2, D5-1)
    @Transactional(readOnly = true)
    public BudgetStatus status(Long storeId, YearMonth yearMonth) {
        Optional<MonthlyBudget> budget = budgets.findByStoreIdAndMonth(storeId, yearMonth.atDay(1));
        // 그 달 비용 합. 상위별은 하위를 합산한 값 (expense.totals)
        ExpenseTotals spent = expenses.totals(storeId, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        List<BudgetLineStatus> lines = categories.tops(storeId).stream()
                .map(top -> lineStatus(top, budget.flatMap(b -> b.lineOf(top.id())),
                        spent.byTop().getOrDefault(top.id(), 0L)))
                .toList();
        if (budget.isEmpty()) {
            return new BudgetStatus(yearMonth, null, spent.total(), null, null, null, null, lines);
        }
        Money total = budget.get().getTotal();
        BudgetUsage usage = BudgetUsage.of(total, Money.create(spent.total()));
        return new BudgetStatus(yearMonth, total.getAmount(), spent.total(), usage.remaining(), usage.ratePercent(),
                usage.level(), budget.get().unassigned().getAmount(), lines);
    }

    // 항목은 상위 비용 분류만, 새 항목은 켜진 상위만 (04 설계 5.3, 기획서 P5-7)
    // 하위 · 없는 것 · 다른 매장의 것은 tops에 없다. 그 달에 이미 있던 항목(before)은 꺼진 상위여도 유지 · 수정 가능
    private void rejectUnavailable(Long storeId, MonthlyBudget budget, Set<Long> before) {
        Map<Long, CategoryInfo> tops = categories.tops(storeId).stream()
                .collect(Collectors.toMap(CategoryInfo::id, Function.identity()));
        for (BudgetLine line : budget.getLines()) {
            CategoryInfo top = tops.get(line.getCategoryId());
            if (top == null || !top.active() && !before.contains(line.getCategoryId())) {
                throw new MoaException(ErrorCode.BUDGET_CATEGORY_UNAVAILABLE,
                        "category is not available for a budget line: " + line.getCategoryId());
            }
        }
    }

    private static Set<Long> categoryIds(MonthlyBudget budget) {
        return budget.getLines().stream().map(BudgetLine::getCategoryId).collect(Collectors.toSet());
    }

    private static BudgetLineStatus lineStatus(CategoryInfo top, Optional<BudgetLine> line, long spent) {
        if (line.isEmpty()) {
            return new BudgetLineStatus(top.id(), top.name(), top.active(), null, spent, null, null, null, 0);
        }
        Money amount = line.get().getAmount();
        BudgetUsage usage = BudgetUsage.of(amount, Money.create(spent));
        return new BudgetLineStatus(top.id(), top.name(), top.active(), amount.getAmount(), spent, usage.remaining(),
                usage.ratePercent(), usage.level(), 0);
    }
}
