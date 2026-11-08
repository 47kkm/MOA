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
import moa.expense.ExpenseEvents.ExpenseSnapshot;
import moa.expense.ExpenseEvents.ExpenseUpdated;
import moa.expense.internal.Expense;
import moa.expense.internal.ExpenseRepository;
import moa.shared.ErrorCode;
import moa.shared.MoaException;

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

    // 입력 규칙(400)을 먼저 보고, 그다음 고를 수 있는 비용 분류인가(409)를 본다. 수정과 같은 순서 (04 설계 4.2)
    @Transactional
    public ExpenseInfo register(Long storeId, Long categoryId, Long amount, LocalDate date, String memo) {
        Expense expense = Expense.register(storeId, categoryId, amount, date, memo, LocalDate.now(clock));
        rejectUnselectable(storeId, expense.getCategoryId());
        expenses.save(expense);
        events.publishEvent(new ExpenseRegistered(storeId, expense.getId(), expense.getExpenseDate(),
                expense.getAmount().getAmount(), expense.getCategoryId()));
        return toInfo(expense, categories.infoOf(storeId, List.of(expense.getCategoryId())).get(expense.getCategoryId()));
    }

    // save를 부르지 않는다. 읽어 온 expense를 고치면 커밋 때 변경 감지가 UPDATE를 보낸다
    @Transactional
    public ExpenseInfo update(Long storeId, Long id, Long categoryId, Long amount, LocalDate date, String memo) {
        Expense expense = find(storeId, id);
        ExpenseSnapshot before = expense.snapshot();
        expense.change(categoryId, amount, date, memo, LocalDate.now(clock));
        events.publishEvent(new ExpenseUpdated(storeId, id, before, expense.snapshot()));
        return toInfo(expense, categories.infoOf(storeId, List.of(expense.getCategoryId())).get(expense.getCategoryId()));
    }

    private Expense find(Long storeId, Long id) {
        return expenses.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new MoaException(ErrorCode.NOT_FOUND, "expense not found: " + id));
    }

    // 있음 + 켜짐 + 하위가 있는 상위가 아님. tree를 아는 category가 판단한다 (04 설계 4.3, D4-3)
    private void rejectUnselectable(Long storeId, Long categoryId) {
        if (!categories.isSelectable(storeId, categoryId)) {
            throw new MoaException(ErrorCode.EXPENSE_CATEGORY_UNAVAILABLE, "category is not selectable: " + categoryId);
        }
    }

    private static ExpenseInfo toInfo(Expense expense, CategoryInfo category) {
        return new ExpenseInfo(expense.getId(), expense.getCategoryId(), category.path(), category.active(),
                expense.getAmount().getAmount(), expense.getExpenseDate(), expense.getMemo());
    }
}
