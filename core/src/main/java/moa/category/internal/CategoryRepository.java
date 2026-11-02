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

    // 형제 중 순서가 가장 뒤인 것 (새 분류의 순서)
    Optional<Category> findTopByStoreIdAndParentIdOrderBySortOrderDesc(Long storeId, Long parentId);

    boolean existsByStoreIdAndParentIdAndName(Long storeId, Long parentId, String name);

    // 템플릿을 넣을 때 같은 이름의 상위가 있으면 그것을 쓴다
    Optional<Category> findByStoreIdAndParentIdAndName(Long storeId, Long parentId, String name);

    // 켜진 상위 분류 수
    long countByStoreIdAndParentIdIsNullAndActiveTrue(Long storeId);

    List<Category> findByStoreId(Long storeId);

    // 상위 하나의 하위 목록
    List<Category> findByStoreIdAndParentId(Long storeId, Long parentId);
}
