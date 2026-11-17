package moa.budget;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

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

    // 응답은 현황 조회와 같은 모양 (04 설계 5.2)
    @Transactional
    public BudgetStatus set(Long storeId, YearMonth yearMonth, Long total, List<BudgetLineInput> lines) {
        MonthlyBudget budget = MonthlyBudget.set(storeId, yearMonth, total, lines, YearMonth.now(clock));
        budgets.save(budget);
        events.publishEvent(new MonthlyBudgetSet(storeId, yearMonth));
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
