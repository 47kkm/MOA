package moa.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    @DisplayName("금액 생성 - 정상 생성")
    void creates() {
        Money money = Money.create(1000);

        assertThat(money.getAmount()).isEqualTo(1000);
    }
}