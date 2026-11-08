package moa.expense;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.expense.ExpenseEvents.ExpenseRegistered;
import moa.expense.internal.Expense;
import moa.expense.internal.ExpenseRepository;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final CategoryService categories;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public ExpenseService(ExpenseRepository expenses, CategoryService categories, Clock clock,
            ApplicationEventPublisher events) {
        this.expenses = expenses;
        this.categories = categories;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public ExpenseInfo register(Long storeId, Long categoryId, Long amount, LocalDate date, String memo) {
        Expense expense = expenses.save(Expense.register(storeId, categoryId, amount, date, memo, LocalDate.now(clock)));
        events.publishEvent(new ExpenseRegistered(storeId, expense.getId(), expense.getExpenseDate(),
                expense.getAmount().getAmount(), expense.getCategoryId()));
        return toInfo(expense, categories.infoOf(storeId, List.of(expense.getCategoryId())).get(expense.getCategoryId()));
    }

    private static ExpenseInfo toInfo(Expense expense, CategoryInfo category) {
        return new ExpenseInfo(expense.getId(), expense.getCategoryId(), category.path(), category.active(),
                expense.getAmount().getAmount(), expense.getExpenseDate(), expense.getMemo());
    }
}
