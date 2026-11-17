package moa.budget.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

import moa.shared.Money;

// 예산 대비 소진 (04 설계 5.4). 저장하지 않는 값. 잔여 · 소진율 · 구간 규칙이 사는 곳
// 예산은 월 예산의 총액이나 예산 항목 금액이라 늘 1 이상이다
public final class BudgetUsage {

    private final Money budget;
    private final Money spent;

    private BudgetUsage(Money budget, Money spent) {
        this.budget = budget;
        this.spent = spent;
    }

    public static BudgetUsage of(Money budget, Money spent) {
        return new BudgetUsage(budget, spent);
    }

    // 잔여 = 예산 − 소진. 음수를 0으로 깎지 않는다 → Money가 아니라 long (04 설계 5.3, 0.8)
    public long remaining() {
        return budget.getAmount() - spent.getAmount();
    }

    // 소진율 = 소진 ÷ 예산, 정수 %로 반올림 (51%, 109%) (04 설계 5.3)
    public int ratePercent() {
        return BigDecimal.valueOf(spent.getAmount()).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(budget.getAmount()), 0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}
