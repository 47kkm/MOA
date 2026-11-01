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

    @Test
    @DisplayName("매장 등록 - 앞뒤 공백 → 제거되어 저장")
    void stripsName() {
        Store store = Store.register("  카페 모아  ", StoreType.CAFE, TODAY);

        assertThat(store.getName()).isEqualTo("카페 모아");
    }

    @Test
    @DisplayName("매장 등록 - 50자 초과 → 거부 (50자는 허용)")
    void rejectsNameOver50() {
        String fifty = "가".repeat(50);

        assertThat(Store.register(fifty, StoreType.CAFE, TODAY).getName()).hasSize(50);
        assertThatThrownBy(() -> Store.register(fifty + "가", StoreType.CAFE, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("매장 등록 - 이름 없음(null) → 거부")
    void rejectsNullName() {
        assertThatThrownBy(() -> Store.register(null, StoreType.CAFE, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("매장 등록 - 업종 비움 → 거부")
    void rejectsNullType() {
        assertThatThrownBy(() -> Store.register("카페 모아", null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
