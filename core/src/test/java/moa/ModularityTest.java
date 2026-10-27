package moa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    @DisplayName("모듈 경계 - 다른 모듈의 internal을 import하지 않고, 모듈 사이 순환이 없다")
    void verify() {
        ApplicationModules.of(MoaApplication.class).verify();
    }
}
