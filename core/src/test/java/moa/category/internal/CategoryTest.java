package moa.category.internal;

import static org.assertj.core.api.Assertions.assertThat;

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

    // id는 DB가 매긴다. 단위 테스트에는 DB가 없으므로 저장된 것처럼 id를 직접 넣는다
    private static Category saved(Category category, Long id) {
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
