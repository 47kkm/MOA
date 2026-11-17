package moa.expense;

import java.util.Map;

// 기간 비용 합계 (04 설계 5.7, D4-11). 금액은 숫자 하나 (D4-10)
// byTop: 상위 비용 분류 ID → 합 (하위를 상위로 합산). byCategory: 비용이 가리키는 비용 분류 ID → 합
public record ExpenseTotals(long total, Map<Long, Long> byTop, Map<Long, Long> byCategory) {
}
