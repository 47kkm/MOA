package moa.expense;

import java.time.LocalDate;

// 금액은 숫자 하나 (04 설계 D4-10). categoryPath는 "식자재 > 원두", 꺼진 비용 분류면 categoryActive=false (04 설계 4.7)
public record ExpenseInfo(Long id, Long categoryId, String categoryPath, boolean categoryActive, long amount,
        LocalDate date, String memo) {
}
