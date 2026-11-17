package moa.budget;

// 예산 대비 소진 구간 (04 설계 5.4). 기획서 P5-4의 기본 · 주황 · 빨강
// 현황 응답(BudgetStatus)에 담겨 나가서 모듈 루트에 둔다. 판단은 internal의 BudgetUsage가 한다
public enum BudgetLevel {
    NORMAL,
    WARNING,
    OVER
}
