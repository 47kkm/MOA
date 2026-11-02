package moa.category.internal;

import java.util.Collection;
import java.util.Map;

import org.springframework.stereotype.Component;

import moa.category.CategoryUsage;

/**
 * CategoryUsage 임시 구현. expense 모듈이 없는 2주차에는 비용이 실제로 0건이라 사실과 같다 (04 설계 0.3, D2-9).
 * 앱이 뜨려면 구현이 하나 있어야 한다. 3주차 expense 완료 조건: 이 파일 삭제.
 */
@Component
class NoExpenseYet implements CategoryUsage {

    @Override
    public Map<Long, Long> countByCategoryIds(Long storeId, Collection<Long> categoryIds) {
        return Map.of();
    }
}
