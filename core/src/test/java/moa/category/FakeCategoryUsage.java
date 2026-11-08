package moa.category;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 테스트용 CategoryUsage. 분류 ID별 누적 비용 건수를 테스트가 정한다 (04 설계 0.3, D2-9).
 * category만 띄우는 테스트에는 expense의 구현이 없으므로 이것을 넣는다. internal 쪽 테스트도 쓰도록 public
 */
public class FakeCategoryUsage implements CategoryUsage {

    private final Map<Long, Long> counts = new ConcurrentHashMap<>();

    void set(Long categoryId, long count) {
        counts.put(categoryId, count);
    }

    void clear() {
        counts.clear();
    }

    @Override
    public Map<Long, Long> countByCategoryIds(Long storeId, Collection<Long> categoryIds) {
        return categoryIds.stream()
                .filter(counts::containsKey)
                .collect(Collectors.toMap(Function.identity(), counts::get));
    }
}
