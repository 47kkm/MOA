package moa.expense.internal;

// 비용 분류를 누가 골랐나. Phase 1은 USER만. Phase 2 AI 분류가 AI를 쓴다 (04 설계 4.4, D4-7)
public enum ClassifiedBy {
    USER,
    AI
}
