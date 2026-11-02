package moa.category.internal;

import java.util.List;

import org.springframework.data.repository.Repository;

// 쓰는 메서드만 적는다. 구현은 Spring Data가 메서드 이름을 읽고 만든다 (04 설계 2.7)
public interface CategoryRepository extends Repository<Category, Long> {

    Category save(Category category);

    // parentId가 null이면 상위 분류끼리 센다
    long countByStoreIdAndParentId(Long storeId, Long parentId);

    List<Category> findByStoreId(Long storeId);
}
