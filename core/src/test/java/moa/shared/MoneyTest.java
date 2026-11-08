package moa.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("금액 생성 - 정상 생성")
    void creates() {
        Money money = Money.create(1000);

        assertThat(money.getAmount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("금액 생성 - 음수 금액 → 거부")
    void rejectsNegative() {
        assertThatThrownBy(() -> Money.create(-1000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("금액 생성 - 0 → 정상 생성")
    void createsZero() {
        Money money = Money.create(0);

        assertThat(money.getAmount()).isEqualTo(0);
    }
}