package moa.budget;

import java.time.YearMonth;

public final class BudgetEvents {

    private BudgetEvents() {
    }

    // Phase 1에는 받는 모듈이 없다 (요구사항정의서 7.3, 04 설계 5.7). 금액은 숫자 하나 (D4-10)

    // 월 예산 설정 커밋 후. 바뀐 것이 없으면 발행하지 않는다
    public record MonthlyBudgetSet(Long storeId, YearMonth yearMonth) {
    }
}
