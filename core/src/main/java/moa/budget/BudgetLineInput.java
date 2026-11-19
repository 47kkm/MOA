package moa.budget;

// 월 예산 설정의 예산 항목 하나 (04 설계 5.7). 금액은 숫자 하나 (D4-10). 빠진 칸은 null로 들어와 MonthlyBudget이 400으로 거부한다
public record BudgetLineInput(Long categoryId, Long amount) {
}
