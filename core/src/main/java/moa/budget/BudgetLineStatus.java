package moa.budget;

// 월 예산 현황의 행 하나 = 상위 비용 분류 하나 (04 설계 5.7). 금액은 숫자 하나 (D4-10)
// 그 상위에 예산 항목이 없으면 budget · remaining · ratePercent · level이 null. 소진 · 지난 달 실제는 항목과 상관없이 있음
public record BudgetLineStatus(Long categoryId, String name, boolean active, Long budget, long spent, Long remaining,
        Integer ratePercent, BudgetLevel level, long lastMonthSpent) {
}
