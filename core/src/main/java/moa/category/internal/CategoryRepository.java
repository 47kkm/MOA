package moa.category.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

// 쓰는 메서드만 적는다. 구현은 Spring Data가 메서드 이름을 읽고 만든다 (04 설계 2.7)
public interface CategoryRepository extends Repository<Category, Long> {

    Category save(Category category);

    void delete(Category category);

    // 다른 매장의 분류는 없는 것과 같다
    Optional<Category> findByIdAndStoreId(Long id, Long storeId);

    // parentId가 null이면 상위 분류끼리 센다
    long countByStoreIdAndParentId(Long storeId, Long parentId);

    // 형제 중 순서가 가장 뒤인 것
    Optional<Category> findTopByStoreIdAndParentIdOrderBySortOrderDesc(Long storeId, Long parentId);

    // 새 분류의 순서 = 형제 끝. 형제 수로 정하면 앞쪽 형제를 지운 뒤 번호가 겹친다.
    // 인터페이스의 default 메서드는 Spring Data가 구현을 만들지 않고 이 코드를 그대로 쓴다
    default int nextSortOrder(Long storeId, Long parentId) {
        return findTopByStoreIdAndParentIdOrderBySortOrderDesc(storeId, parentId)
                .map(last -> last.getSortOrder() + 1)
                .orElse(0);
    }

    boolean existsByStoreIdAndParentIdAndName(Long storeId, Long parentId, String name);

    // 템플릿을 넣을 때 같은 이름의 상위가 있으면 그것을 쓴다
    Optional<Category> findByStoreIdAndParentIdAndName(Long storeId, Long parentId, String name);

    // 켜진 상위 분류 수
    long countByStoreIdAndParentIdIsNullAndActiveTrue(Long storeId);

    List<Category> findByStoreId(Long storeId);

    // 상위 하나의 하위 목록
    List<Category> findByStoreIdAndParentId(Long storeId, Long parentId);
}
