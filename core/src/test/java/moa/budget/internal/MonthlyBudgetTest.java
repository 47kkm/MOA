package moa.budget.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import moa.budget.BudgetLineInput;
import moa.shared.Money;

class MonthlyBudgetTest {

    static final YearMonth THIS_MONTH = YearMonth.of(2026, 11);
    static final Long STORE_ID = 1L;
    static final Long FOOD = 10L;
    static final Long RENT = 20L;

    // 예산 항목은 월 예산 안의 값. 항목이 없는 상위는 lineOf가 빈 값 (04 설계 5.4)
    @Test
    @DisplayName("월 예산 설정 - 정상 설정 (총액 · 항목, 미배정 = 총액 − 항목 합)")
    void sets() {
        MonthlyBudget budget = MonthlyBudget.set(STORE_ID, THIS_MONTH, 5_000_000L,
                List.of(new BudgetLineInput(FOOD, 2_000_000L), new BudgetLineInput(RENT, 1_500_000L)), THIS_MONTH);

        assertThat(budget.getStoreId()).isEqualTo(STORE_ID);
        assertThat(budget.yearMonth()).isEqualTo(THIS_MONTH);
        assertThat(budget.getTotal()).isEqualTo(Money.create(5_000_000));
        assertThat(budget.getLines()).containsExactly(BudgetLine.of(FOOD, 2_000_000L), BudgetLine.of(RENT, 1_500_000L));
        assertThat(budget.lineOf(FOOD)).contains(BudgetLine.of(FOOD, 2_000_000L));
        assertThat(budget.lineOf(99L)).isEmpty();
        assertThat(budget.unassigned()).isEqualTo(Money.create(1_500_000));
    }

    // 총액 필수, 항목 금액 선택. 둘 다 1 이상 999,999,999 이하 (04 설계 5.3). 상한은 입력 규칙이라 Money가 아닌 여기서 본다 (0.8)
    @Test
    @DisplayName("월 예산 설정 - 총액 0 · 10억, 항목 금액 0 → 거부 (1, 999,999,999는 허용)")
    void rejectsAmountOutOfRange() {
        assertThat(withTotal(1L).getTotal()).isEqualTo(Money.create(1));
        assertThat(withTotal(999_999_999L).getTotal()).isEqualTo(Money.create(999_999_999));
        assertThat(withLine(1L).lineOf(FOOD)).contains(BudgetLine.of(FOOD, 1L));
        assertThatThrownBy(() -> withTotal(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withTotal(1_000_000_000L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withLine(0L)).isInstanceOf(IllegalArgumentException.class);
    }

    // 같으면 허용, 차이는 미배정 (04 설계 5.3, 요구사항정의서 8.5)
    @Test
    @DisplayName("월 예산 설정 - 항목 합 5,000,001 > 총액 5,000,000 → 거부 (5,000,000은 허용)")
    void rejectsLinesOverTotal() {
        MonthlyBudget full = MonthlyBudget.set(STORE_ID, THIS_MONTH, 5_000_000L,
                List.of(new BudgetLineInput(FOOD, 3_000_000L), new BudgetLineInput(RENT, 2_000_000L)), THIS_MONTH);

        assertThat(full.unassigned()).isEqualTo(Money.create(0));
        assertThatThrownBy(() -> MonthlyBudget.set(STORE_ID, THIS_MONTH, 5_000_000L,
                List.of(new BudgetLineInput(FOOD, 3_000_000L), new BudgetLineInput(RENT, 2_000_001L)), THIS_MONTH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // 화면에서는 나올 수 없다 (상위 비용 분류마다 한 행). 들어오면 400 (04 설계 5.3, D2-15와 같은 처리)
    @Test
    @DisplayName("월 예산 설정 - 같은 비용 분류 항목 두 번 → 거부")
    void rejectsDuplicateCategory() {
        assertThatThrownBy(() -> MonthlyBudget.set(STORE_ID, THIS_MONTH, 5_000_000L,
                List.of(new BudgetLineInput(FOOD, 1_000_000L), new BudgetLineInput(FOOD, 2_000_000L)), THIS_MONTH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // 고칠 수 있는 달은 이번 달 · 다음 달 (04 설계 5.3, 기획서 P5-1). 이번 달은 응용 서비스가 Clock으로 구해 넘긴다
    @Test
    @DisplayName("월 예산 설정 - 지난 달 · 두 달 뒤 → 거부 (이번 달 · 다음 달은 허용)")
    void rejectsMonthOutOfRange() {
        assertThat(inMonth(THIS_MONTH).yearMonth()).isEqualTo(THIS_MONTH);
        assertThat(inMonth(THIS_MONTH.plusMonths(1)).yearMonth()).isEqualTo(YearMonth.of(2026, 12));
        assertThatThrownBy(() -> inMonth(THIS_MONTH.minusMonths(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inMonth(THIS_MONTH.plusMonths(2))).isInstanceOf(IllegalArgumentException.class);
    }

    // 요청 JSON에서 칸이 빠지면 null로 들어온다. 전부 400 VALIDATION. 항목은 선택이라 목록이 없으면 항목 0개 (04 설계 5.3)
    @Test
    @DisplayName("월 예산 설정 - 총액 · 항목의 비용 분류 · 항목 금액 없음 → 거부 (항목 목록이 없으면 항목 0개)")
    void rejectsMissingFields() {
        assertThat(MonthlyBudget.set(STORE_ID, THIS_MONTH, 1_000_000L, null, THIS_MONTH).getLines()).isEmpty();
        assertThatThrownBy(() -> MonthlyBudget.set(STORE_ID, THIS_MONTH, null, List.of(), THIS_MONTH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MonthlyBudget.set(STORE_ID, THIS_MONTH, 1_000_000L,
                List.of(new BudgetLineInput(null, 1000L)), THIS_MONTH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MonthlyBudget.set(STORE_ID, THIS_MONTH, 1_000_000L,
                List.of(new BudgetLineInput(FOOD, null)), THIS_MONTH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // 다시 설정 = 한 장을 통째로 덮어쓰기. 항목은 줄을 전부 갈아 끼운다 (04 설계 5.4, D5-2)
    @Test
    @DisplayName("월 예산 설정 - 다시 설정 → 총액 · 항목을 통째로 바꿈")
    void changes() {
        MonthlyBudget budget = MonthlyBudget.set(STORE_ID, THIS_MONTH, 5_000_000L,
                List.of(new BudgetLineInput(FOOD, 2_000_000L), new BudgetLineInput(RENT, 1_500_000L)), THIS_MONTH);

        boolean changed = budget.change(4_000_000L, List.of(new BudgetLineInput(FOOD, 2_500_000L)), THIS_MONTH);

        assertThat(changed).isTrue();
        assertThat(budget.getTotal()).isEqualTo(Money.create(4_000_000));
        assertThat(budget.getLines()).containsExactly(BudgetLine.of(FOOD, 2_500_000L));
        assertThat(budget.unassigned()).isEqualTo(Money.create(1_500_000));
    }

    // 비용 수정(4.3)과 같은 모양. 바뀐 것이 없으면 응용 서비스가 MonthlyBudgetSet을 내지 않는다 (04 설계 5.2, 5.7)
    // 항목은 순서와 상관없이 비교한다. 화면의 행 순서는 상위 비용 분류 순서라 항목 순서에 뜻이 없다
    @Test
    @DisplayName("월 예산 설정 - 바뀐 것 없음 → 변경 없음")
    void noChange() {
        MonthlyBudget budget = MonthlyBudget.set(STORE_ID, THIS_MONTH, 5_000_000L,
                List.of(new BudgetLineInput(FOOD, 2_000_000L), new BudgetLineInput(RENT, 1_500_000L)), THIS_MONTH);

        assertThat(budget.change(5_000_000L,
                List.of(new BudgetLineInput(RENT, 1_500_000L), new BudgetLineInput(FOOD, 2_000_000L)), THIS_MONTH))
                .isFalse();
        assertThat(budget.getLines()).containsExactly(BudgetLine.of(FOOD, 2_000_000L), BudgetLine.of(RENT, 1_500_000L));
    }

    // 지난 달이 된 예산은 고칠 수 없다 (기획서 P5-1). 11월 예산을 12월에 다시 설정하면 거부
    @Test
    @DisplayName("월 예산 설정 - 다시 설정도 같은 입력 규칙 → 거부")
    void changeRules() {
        MonthlyBudget budget = MonthlyBudget.set(STORE_ID, THIS_MONTH, 5_000_000L,
                List.of(new BudgetLineInput(FOOD, 2_000_000L)), THIS_MONTH);

        assertThatThrownBy(() -> budget.change(0L, List.of(), THIS_MONTH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> budget.change(1_000_000L, List.of(new BudgetLineInput(FOOD, 1_000_001L)), THIS_MONTH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> budget.change(5_000_000L, List.of(), THIS_MONTH.plusMonths(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(budget.getTotal()).isEqualTo(Money.create(5_000_000));
        assertThat(budget.getLines()).containsExactly(BudgetLine.of(FOOD, 2_000_000L));
    }

    private static MonthlyBudget inMonth(YearMonth yearMonth) {
        return MonthlyBudget.set(STORE_ID, yearMonth, 1_000_000L, List.of(), THIS_MONTH);
    }

    private static MonthlyBudget withTotal(Long total) {
        return MonthlyBudget.set(STORE_ID, THIS_MONTH, total, List.of(), THIS_MONTH);
    }

    private static MonthlyBudget withLine(Long amount) {
        return MonthlyBudget.set(STORE_ID, THIS_MONTH, 999_999_999L, List.of(new BudgetLineInput(FOOD, amount)),
                THIS_MONTH);
    }
}
