package moa.expense.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import moa.shared.Money;

class ExpenseTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    static final Long STORE_ID = 1L;
    static final Long BEANS = 11L;
    static final Long MILK = 12L;

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

    // 상한은 입력 규칙이라 Money가 아닌 Expense에 있다. Money는 음수만 막는다 (04 설계 4.3, 0.8)
    @Test
    @DisplayName("비용 등록 - 금액 0 · 10억 → 거부 (1, 999,999,999는 허용)")
    void rejectsAmountOutOfRange() {
        assertThat(register(1L).getAmount()).isEqualTo(Money.create(1));
        assertThat(register(999_999_999L).getAmount()).isEqualTo(Money.create(999_999_999));
        assertThatThrownBy(() -> register(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> register(1_000_000_000L)).isInstanceOf(IllegalArgumentException.class);
    }

    // 오늘은 밖(응용 서비스)이 Clock으로 구해 넘긴다. 애그리거트는 Clock을 모른다 (04 설계 4.4)
    @Test
    @DisplayName("비용 등록 - 미래 날짜 → 거부 (오늘은 허용)")
    void rejectsFutureDate() {
        assertThat(Expense.register(STORE_ID, BEANS, 1000L, TODAY, null, TODAY).getExpenseDate()).isEqualTo(TODAY);
        assertThatThrownBy(() -> Expense.register(STORE_ID, BEANS, 1000L, TODAY.plusDays(1), null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("비용 등록 - 메모 201자 · 줄바꿈 → 거부, 공백뿐이면 없음")
    void memoRules() {
        assertThat(withMemo("가".repeat(200)).getMemo()).hasSize(200);
        assertThat(withMemo("  원두 2kg  ").getMemo()).isEqualTo("원두 2kg");
        assertThat(withMemo("   ").getMemo()).isNull();
        assertThat(withMemo(null).getMemo()).isNull();
        assertThatThrownBy(() -> withMemo("가".repeat(201))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withMemo("원두\n2kg")).isInstanceOf(IllegalArgumentException.class);
    }

    // 요청 JSON에서 칸이 빠지면 null로 들어온다. 전부 400 VALIDATION (04 설계 4.3)
    @Test
    @DisplayName("비용 등록 - 비용 분류 · 금액 · 날짜 없음 → 거부")
    void rejectsMissingFields() {
        assertThatThrownBy(() -> Expense.register(STORE_ID, null, 1000L, TODAY, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Expense.register(STORE_ID, BEANS, null, TODAY, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Expense.register(STORE_ID, BEANS, 1000L, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // 수정과 재분류는 한 메서드. 비용 분류가 바뀌었는지는 응용 서비스가 본다 (04 설계 4.4, D4-1)
    @Test
    @DisplayName("비용 수정 - 비용 분류 · 금액 · 날짜 · 메모 변경 → 바뀜")
    void changes() {
        Expense expense = Expense.register(STORE_ID, BEANS, 35000L, TODAY, "원두 2kg", TODAY);

        boolean changed = expense.change(MILK, 12000L, TODAY.minusDays(3), "우유 6팩", TODAY);

        assertThat(changed).isTrue();
        assertThat(expense.getCategoryId()).isEqualTo(MILK);
        assertThat(expense.getAmount()).isEqualTo(Money.create(12000));
        assertThat(expense.getExpenseDate()).isEqualTo(TODAY.minusDays(3));
        assertThat(expense.getMemo()).isEqualTo("우유 6팩");
    }

    private static Expense register(Long amount) {
        return Expense.register(STORE_ID, BEANS, amount, TODAY, null, TODAY);
    }

    private static Expense withMemo(String memo) {
        return Expense.register(STORE_ID, BEANS, 1000L, TODAY, memo, TODAY);
    }
}
