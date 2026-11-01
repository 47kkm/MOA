package moa.store.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import moa.store.StoreType;

class StoreTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

    @Test
    @DisplayName("매장 등록 - 정상 등록")
    void registers() {
        Store store = Store.register("카페 모아", StoreType.CAFE, TODAY);

        assertThat(store.getName()).isEqualTo("카페 모아");
        assertThat(store.getType()).isEqualTo(StoreType.CAFE);
        assertThat(store.getRegisteredAt()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("매장 등록 - 이름 비움 → 거부")
    void rejectsEmptyName() {
        assertThatThrownBy(() -> Store.register("", StoreType.CAFE, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("매장 등록 - 공백만 입력 → 거부")
    void rejectsBlankName() {
        assertThatThrownBy(() -> Store.register("   ", StoreType.CAFE, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("매장 등록 - 전각 공백만 입력 → 거부")
    void rejectsFullWidthBlankName() {
        assertThatThrownBy(() -> Store.register("\u3000", StoreType.CAFE, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
