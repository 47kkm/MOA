package moa.expense.internal;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import moa.category.CategoryUsage;

/**
 * category가 정한 인터페이스를 expense가 구현한다 (04 설계 0.3 의존 역전, 4.7).
 * 코드 의존은 expense → category 한 방향이고, category는 이 클래스를 모른다.
 */
@Component
class CategoryUsageImpl implements CategoryUsage {

    private final ExpenseRepository expenses;

    CategoryUsageImpl(ExpenseRepository expenses) {
        this.expenses = expenses;
    }

    // 비용을 읽어 Java에서 센다. GROUP BY는 JPQL이 필요해서 쓰지 않는다 (04 설계 D4-8)
    @Override
    public Map<Long, Long> countByCategoryIds(Long storeId, Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return expenses.findByStoreIdAndCategoryIdIn(storeId, categoryIds).stream()
                .collect(Collectors.groupingBy(Expense::getCategoryId, Collectors.counting()));
    }
}
