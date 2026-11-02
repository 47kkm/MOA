package moa.category.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
