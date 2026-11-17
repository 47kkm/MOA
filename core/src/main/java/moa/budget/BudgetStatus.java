package moa.budget;

import java.time.YearMonth;
import java.util.List;

// 월 예산 현황 (04 설계 5.7). 월 예산 설정의 응답도 같은 모양
// 예산이 없는 달은 budget · remaining · ratePercent · level · unassigned가 null. 소진은 예산과 상관없이 있음 (D5-1)
public record BudgetStatus(YearMonth yearMonth, Long budget, long spent, Long remaining, Integer ratePercent,
        BudgetLevel level, Long unassigned, List<BudgetLineStatus> lines) {
}
