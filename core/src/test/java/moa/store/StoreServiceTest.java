package moa.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;

import moa.TestcontainersConfiguration;
import moa.shared.ErrorCode;
import moa.shared.MoaException;
import moa.store.StoreEvents.StoreRegistered;

/**
 * store 모듈만 띄우고 진짜 MySQL(Testcontainers)에 저장해 보는 통합 테스트 (04 설계 0.7).
 */
@ApplicationModuleTest
@Import({ TestcontainersConfiguration.class, StoreServiceTest.FixedClock.class })
class StoreServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

    @Autowired
    StoreService storeService;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM store.stores");
    }

    @Test
    @DisplayName("매장 등록 - 저장되고 StoreRegistered 발행")
    void registerSavesAndPublishes(AssertablePublishedEvents events) {
        StoreInfo info = storeService.register("카페 모아", StoreType.CAFE);

        assertThat(info.id()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM store.stores", Integer.class)).isEqualTo(1);
        assertThat(events)
                .contains(StoreRegistered.class)
                .matching(StoreRegistered::storeId, info.id());
    }

    @Test
    @DisplayName("매장 등록 - 이미 매장 있음 → STORE_ALREADY_EXISTS")
    void rejectsSecondStore() {
        storeService.register("카페 모아", StoreType.CAFE);

        assertThatThrownBy(() -> storeService.register("두 번째 매장", StoreType.RETAIL))
                .isInstanceOfSatisfying(MoaException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.STORE_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("매장 조회 - 이름·업종·등록일 반환")
    void getReturnsStore() {
        Long id = storeService.register("카페 모아", StoreType.CAFE).id();

        StoreInfo found = storeService.get(id);

        assertThat(found).isEqualTo(new StoreInfo(id, "카페 모아", StoreType.CAFE, TODAY));
    }

    // "오늘"을 2026-10-28로 고정한다 (04 설계 D0-6)
    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {

        @Bean
        @Primary
        Clock fixedClock() {
            ZoneId seoul = ZoneId.of("Asia/Seoul");
            return Clock.fixed(TODAY.atStartOfDay(seoul).toInstant(), seoul);
        }
    }
}
