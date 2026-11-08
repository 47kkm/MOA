package moa.expense;

import java.time.LocalDate;

public final class ExpenseEvents {

    private ExpenseEvents() {
    }

    // 4주차 budget(소진액)과 Phase 2 AI가 받는다. 금액은 숫자 하나 (04 설계 4.7)
    // 상위 합산은 받는 쪽이 category를 보고 한다. 여기에는 비용이 가리키는 비용 분류 ID만

    public record ExpenseRegistered(Long storeId, Long expenseId, LocalDate date, long amount, Long categoryId) {
    }

    // 수정 이벤트의 이전 · 이후 값 (04 설계 D4-2)
    public record ExpenseSnapshot(LocalDate date, long amount, Long categoryId) {
    }

    // 비용 분류가 그대로인 수정. 바뀐 것이 없으면 발행하지 않는다 (04 설계 D4-2)
    public record ExpenseUpdated(Long storeId, Long expenseId, ExpenseSnapshot before, ExpenseSnapshot after) {
    }

    // 비용 분류가 바뀐 수정. 금액 · 날짜를 함께 바꿔도 이것 하나만 나간다 (04 설계 D4-2)
    public record ExpenseReclassified(Long storeId, Long expenseId, ExpenseSnapshot before, ExpenseSnapshot after) {
    }
}
