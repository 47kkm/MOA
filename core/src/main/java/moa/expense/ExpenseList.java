package moa.expense;

import java.time.YearMonth;
import java.util.List;

// 합계 · 건수는 비용 분류 필터를 건 뒤의 목록 기준 (04 설계 4.2 "비용 목록 조회")
public record ExpenseList(YearMonth yearMonth, long total, int count, List<ExpenseInfo> items) {
}
