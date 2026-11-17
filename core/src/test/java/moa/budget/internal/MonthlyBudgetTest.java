package moa.budget.internal;

import static org.assertj.core.api.Assertions.assertThat;

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
}
