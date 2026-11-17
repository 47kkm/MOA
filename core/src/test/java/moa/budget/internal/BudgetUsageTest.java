package moa.budget.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
