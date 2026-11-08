package moa.expense;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.expense.ExpenseEvents.ExpenseDeleted;
import moa.expense.ExpenseEvents.ExpenseReclassified;
import moa.expense.ExpenseEvents.ExpenseRegistered;
import moa.expense.ExpenseEvents.ExpenseSnapshot;
import moa.expense.ExpenseEvents.ExpenseUpdated;
import moa.expense.internal.Expense;
import moa.expense.internal.ExpenseRepository;
import moa.shared.ErrorCode;
import moa.shared.MoaException;
import moa.shared.Money;

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
        // 바뀐 것이 없으면 change가 false를 돌려준다. 아무 일 없고 이벤트도 없다 (04 설계 4.3)
        if (expense.change(categoryId, amount, date, memo, LocalDate.now(clock))) {
            ExpenseSnapshot after = expense.snapshot();
            // 비용 분류가 바뀌었는지는 서비스가 본다. 화면은 수정과 재분류를 구분하지 않는다 (04 설계 D4-1)
            // 바뀌었을 때만 새 비용 분류를 검사한다. 그대로면 꺼져 있어도 금액 · 날짜 · 메모를 고칠 수 있다 (D4-6)
            // 거부되면 트랜잭션이 되돌아가 change도 저장되지 않는다 (04 설계 4.2)
            if (!before.categoryId().equals(after.categoryId())) {
                rejectUnselectable(storeId, after.categoryId());
                events.publishEvent(new ExpenseReclassified(storeId, id, before, after));
            } else {
                events.publishEvent(new ExpenseUpdated(storeId, id, before, after));
            }
        }
        return toInfo(expense, categories.infoOf(storeId, List.of(expense.getCategoryId())).get(expense.getCategoryId()));
    }

    @Transactional
    public void delete(Long storeId, Long id) {
        Expense expense = find(storeId, id);
        expenses.delete(expense);
        events.publishEvent(new ExpenseDeleted(storeId, id, expense.getExpenseDate(), expense.getAmount().getAmount(),
                expense.getCategoryId()));
    }

    // 비용 분류 경로 · 꺼짐 여부는 category.infoOf로 한 번에 받는다 (04 설계 4.2)
    @Transactional(readOnly = true)
    public ExpenseList list(Long storeId, YearMonth yearMonth, Long categoryId) {
        List<Expense> month = expenses.findByStoreIdAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(storeId,
                yearMonth.atDay(1), yearMonth.atEndOfMonth());
        Map<Long, CategoryInfo> infos = categories.infoOf(storeId,
                month.stream().map(Expense::getCategoryId).distinct().toList());
        List<ExpenseInfo> items = month.stream().map(e -> toInfo(e, infos.get(e.getCategoryId()))).toList();
        Money total = month.stream().map(Expense::getAmount).reduce(Money.create(0), Money::plus);
        return new ExpenseList(yearMonth, total.getAmount(), items.size(), items);
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
