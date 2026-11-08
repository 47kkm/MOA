package moa.expense.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import moa.shared.Money;

class ExpenseTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    static final Long STORE_ID = 1L;
    static final Long BEANS = 11L;

    @Test
    @DisplayName("비용 등록 - 정상 등록 (분류 주체 USER, 승인 true)")
    void registers() {
        Expense expense = Expense.register(STORE_ID, BEANS, 35000L, TODAY, "원두 2kg", TODAY);

        assertThat(expense.getStoreId()).isEqualTo(STORE_ID);
        assertThat(expense.getCategoryId()).isEqualTo(BEANS);
        assertThat(expense.getAmount()).isEqualTo(Money.create(35000));
        assertThat(expense.getExpenseDate()).isEqualTo(TODAY);
        assertThat(expense.getMemo()).isEqualTo("원두 2kg");
        assertThat(expense.getClassifiedBy()).isEqualTo(ClassifiedBy.USER);
        assertThat(expense.isApproved()).isTrue();
    }
}
