package moa.expense;

import java.time.LocalDate;

public final class ExpenseEvents {

    private ExpenseEvents() {
    }

    // 4주차 budget(소진액)과 Phase 2 AI가 받는다. 금액은 숫자 하나 (04 설계 4.7)
    // 상위 합산은 받는 쪽이 category를 보고 한다. 여기에는 비용이 가리키는 비용 분류 ID만

    public record ExpenseRegistered(Long storeId, Long expenseId, LocalDate date, long amount, Long categoryId) {
    }
}
