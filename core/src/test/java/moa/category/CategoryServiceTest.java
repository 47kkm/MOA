package moa.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import moa.shared.ErrorCode;
import moa.shared.MoaException;

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

    @Test
    @DisplayName("분류 추가 - 하위 추가 → 그 상위 아래 끝에, 경로 \"식자재 > 시럽\"")
    void addChild() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        categoryService.add(STORE_ID, null, "임대료");
        categoryService.add(STORE_ID, food.id(), "원두");

        CategoryInfo syrup = categoryService.add(STORE_ID, food.id(), "시럽");

        assertThat(syrup.parentId()).isEqualTo(food.id());
        assertThat(syrup.path()).isEqualTo("식자재 > 시럽");
        assertThat(categoryService.tree(STORE_ID, false))
                .extracting(CategoryInfo::path)
                .containsExactly("식자재", "식자재 > 원두", "식자재 > 시럽", "임대료");
    }

    @Test
    @DisplayName("분류 추가 - 없는 상위 → NOT_FOUND")
    void addUnderMissingParent() {
        assertFails(ErrorCode.NOT_FOUND, () -> categoryService.add(STORE_ID, 999L, "원두"));
    }

    @Test
    @DisplayName("분류 추가 - 하위 아래 추가 → CATEGORY_DEPTH")
    void addUnderChild() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");

        assertFails(ErrorCode.CATEGORY_DEPTH, () -> categoryService.add(STORE_ID, beans.id(), "에티오피아"));
    }

    // 상위끼리는 DB가 못 막고(04 설계 D2-4) 하위끼리는 DB도 막지만, 둘 다 코드가 먼저 409로 막는다
    @Test
    @DisplayName("분류 추가 - 형제 중복 → CATEGORY_NAME_DUPLICATE")
    void addDuplicateSibling() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        categoryService.add(STORE_ID, food.id(), "원두");

        assertFails(ErrorCode.CATEGORY_NAME_DUPLICATE, () -> categoryService.add(STORE_ID, null, " 식자재 "));
        assertFails(ErrorCode.CATEGORY_NAME_DUPLICATE, () -> categoryService.add(STORE_ID, food.id(), "원두"));
    }

    @Test
    @DisplayName("분류 추가 - 다른 상위와는 중복 허용")
    void sameNameUnderDifferentParents() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo labor = categoryService.add(STORE_ID, null, "인건비");
        categoryService.add(STORE_ID, food.id(), "기타");

        categoryService.add(STORE_ID, labor.id(), "기타");
        categoryService.add(STORE_ID, null, "기타");

        assertThat(categoryService.tree(STORE_ID, false))
                .extracting(CategoryInfo::path)
                .contains("식자재 > 기타", "인건비 > 기타", "기타");
    }

    private static void assertFails(ErrorCode code, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(MoaException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
