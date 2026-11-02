package moa.category.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CategoryTest {

    private static final Long STORE_ID = 1L;

    @Test
    @DisplayName("분류 추가 - 상위 생성, parentId 없음")
    void createsTop() {
        Category top = Category.top(STORE_ID, "식자재", 0);

        assertThat(top.getStoreId()).isEqualTo(STORE_ID);
        assertThat(top.getParentId()).isNull();
        assertThat(top.getName()).isEqualTo("식자재");
        assertThat(top.getSortOrder()).isZero();
        assertThat(top.isActive()).isTrue();
        assertThat(top.isTop()).isTrue();
    }

    @Test
    @DisplayName("분류 추가 - 하위 생성, 상위를 가리킴")
    void createsChild() {
        Category parent = saved(Category.top(STORE_ID, "식자재", 0), 10L);

        Category child = Category.childOf(parent, "원두", 0);

        assertThat(child.getStoreId()).isEqualTo(STORE_ID);
        assertThat(child.getParentId()).isEqualTo(10L);
        assertThat(child.isTop()).isFalse();
        assertThat(child.isChildOf(parent)).isTrue();
    }

    @Test
    @DisplayName("분류 추가 - 하위 아래 추가 → 거부 (2단계 초과)")
    void rejectsGrandchild() {
        Category parent = saved(Category.top(STORE_ID, "식자재", 0), 10L);
        Category child = saved(Category.childOf(parent, "원두", 0), 11L);

        assertThatThrownBy(() -> Category.childOf(child, "에티오피아", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // store에서 배운 것: 공백만, 전각 공백만, 요청에 이름이 빠진 경우(null)도 빈 이름이다
    @Test
    @DisplayName("분류 추가 - 빈 이름 → 거부 (공백만 · 전각 공백만 · null 포함)")
    void rejectsEmptyName() {
        for (String empty : new String[] { "", "   ", "　", null }) {
            assertThatThrownBy(() -> Category.top(STORE_ID, empty, 0))
                    .as("name [%s]", empty)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("분류 추가 - 21자 → 거부 (20자는 허용)")
    void rejectsNameOver20() {
        String twenty = "가".repeat(20);

        assertThat(Category.top(STORE_ID, twenty, 0).getName()).hasSize(20);
        assertThatThrownBy(() -> Category.top(STORE_ID, twenty + "가", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("분류 이름 변경 - 앞뒤 공백 제거")
    void renameStrips() {
        Category category = Category.top(STORE_ID, "식자재", 0);

        category.rename("  재료비  ");

        assertThat(category.getName()).isEqualTo("재료비");
    }

    // id는 DB가 매긴다. 단위 테스트에는 DB가 없으므로 저장된 것처럼 id를 직접 넣는다
    private static Category saved(Category category, Long id) {
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
