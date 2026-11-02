package moa.category;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;

import moa.TestcontainersConfiguration;
import moa.category.CategoryEvents.CategoryAdded;

/**
 * category 모듈만 띄우고 진짜 MySQL(Testcontainers)에 저장해 보는 통합 테스트 (04 설계 0.7).
 */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class CategoryServiceTest {

    static final Long STORE_ID = 1L;

    @Autowired
    CategoryService categoryService;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM category.categories");
    }

    @Test
    @DisplayName("분류 추가 - 상위 추가 → tree 끝에, CategoryAdded 발행")
    void addTop(AssertablePublishedEvents events) {
        categoryService.add(STORE_ID, null, "식자재");

        CategoryInfo added = categoryService.add(STORE_ID, null, "임대료");

        assertThat(added.parentId()).isNull();
        assertThat(added.path()).isEqualTo("임대료");
        assertThat(categoryService.tree(STORE_ID, false))
                .extracting(CategoryInfo::name)
                .containsExactly("식자재", "임대료");
        assertThat(events)
                .contains(CategoryAdded.class)
                .matching(CategoryAdded::categoryId, added.id());
    }
}
