package moa.category;

import java.util.Collection;
import java.util.Map;

/**
 * category가 다른 모듈에 요구하는 것: "이 분류로 입력된 비용이 몇 건인가" (04 설계 0.3 의존 역전, D2-2).
 * 인터페이스는 category가 정하고 구현은 expense가 한다. 그래서 코드 의존은 expense → category 한 방향뿐이다.
 */
public interface CategoryUsage {

    // 건수가 0인 분류는 결과에 없어도 된다
    Map<Long, Long> countByCategoryIds(Long storeId, Collection<Long> categoryIds);
}
