package moa.store.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import moa.store.StoreType;

class StoreTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

    @Test
    @DisplayName("매장 등록 - 이름 비움 → 거부")
    void rejectsEmptyName() {
        assertThatThrownBy(() -> Store.register("", StoreType.CAFE, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
