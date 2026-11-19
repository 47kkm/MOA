package moa.budget.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import moa.budget.BudgetLevel;
import moa.shared.Money;

class BudgetUsageTest {

    static final Money BUDGET = Money.create(2_000_000);

    // 잔여는 음수를 0으로 깎지 않는다 → Money가 아니라 long (04 설계 5.3, 5.4)
    // 소진율은 정수 %로 반올림: 106.5% → 107%
    @Test
    @DisplayName("월 예산 현황 조회 - 소진 2,130,000 / 2,000,000 → 잔여 −130,000, 소진율 107%")
    void remainingCanBeNegative() {
        BudgetUsage usage = BudgetUsage.of(BUDGET, Money.create(2_130_000));

        assertThat(usage.remaining()).isEqualTo(-130_000L);
        assertThat(usage.ratePercent()).isEqualTo(107);
    }

    // 구간: 80% 미만 기본 · 80% 이상 주의 · 100% 이상 초과. 반올림 전 값으로 판단 (04 설계 5.3, D5-9, 기획서 P5-4)
    // 1,999,000 = 99.95% → 소진율은 100%로 보이지만 선에 닿지 않았으니 주의
    @Test
    @DisplayName("월 예산 현황 조회 - 소진 1,599,999 · 1,600,000 · 2,000,000 / 예산 2,000,000 → 기본 · 주의 · 초과. 1,999,000은 100%로 보여도 주의")
    void levels() {
        assertThat(spent(1_599_999).level()).isEqualTo(BudgetLevel.NORMAL);
        assertThat(spent(1_600_000).level()).isEqualTo(BudgetLevel.WARNING);
        assertThat(spent(2_000_000).level()).isEqualTo(BudgetLevel.OVER);
        assertThat(spent(1_999_000).ratePercent()).isEqualTo(100);
        assertThat(spent(1_999_000).level()).isEqualTo(BudgetLevel.WARNING);
    }

    // 소진율은 상한이 없다 (기획서 0.3 "100% 초과는 그대로"). 예산이 작고 소진이 크면 int 범위를 넘는다 → long
    @Test
    @DisplayName("월 예산 현황 조회 - 예산 1 · 소진 999,999,999 → 소진율 99,999,999,900% (int를 넘어도 그대로)")
    void rateBeyondInt() {
        BudgetUsage usage = BudgetUsage.of(Money.create(1), Money.create(999_999_999));

        assertThat(usage.ratePercent()).isEqualTo(99_999_999_900L);
    }

    private static BudgetUsage spent(long spent) {
        return BudgetUsage.of(BUDGET, Money.create(spent));
    }
}
