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
        assertThatThrownBy(() -> Money.create(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("금액 생성 - 0 → 정상 생성")
    void createsZero() {
        Money money = Money.create(0);

        assertThat(money.getAmount()).isEqualTo(0);
    }

    @Test
    @DisplayName("금액 합 - 정상 합계")
    void sumsAmount() {
        Money money = Money.create(1000);

        assertThat(money.plus(Money.create(2000))).isEqualTo(Money.create(3000));
    }

    @Test
    @DisplayName("금액 0 체크 - 0원 → true")
    void isZeroWhenZero() {
        Money money = Money.create(0);

        assertThat(money.isZero()).isTrue();
    }

    @Test
    @DisplayName("금액 0 체크 - 0원이 아님 → false")
    void isNotZeroWhenPositive() {
        Money money = Money.create(1);

        assertThat(money.isZero()).isFalse();
    }

    @Test
    @DisplayName("금액 비교 - 같은 숫자의 금액 → 같음")
    void isSameAmount() {
        Money money = Money.create(1000);

        assertThat(money.equals(Money.create(1000))).isTrue();
        assertThat(money.hashCode()).isEqualTo(Money.create(1000).hashCode());
    }

    @Test
    @DisplayName("금액 비교 - 다른 숫자의 금액 → 다름")
    void isDiffAmount() {
        Money money = Money.create(1000);

        assertThat(money.equals(Money.create(2000))).isFalse();
    }
}