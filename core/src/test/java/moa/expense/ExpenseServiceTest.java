package moa.expense;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.modulith.test.AssertablePublishedEvents;

import moa.TestcontainersConfiguration;
import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.expense.ExpenseEvents.ExpenseRegistered;

/**
 * expense 모듈과 그 의존(category)까지 띄우고 진짜 MySQL(Testcontainers)에 저장해 보는 통합 테스트 (04 설계 0.7, 4.7).
 * category 쪽 가짜는 만들지 않는다. 비용 분류는 진짜 CategoryService로 만든다.
 */
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import({ TestcontainersConfiguration.class, ExpenseServiceTest.FixedClock.class })
class ExpenseServiceTest {

    static final Long STORE_ID = 1L;
    static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

    @Autowired
    ExpenseService expenseService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    JdbcTemplate jdbc;

    // 식자재 > 원두 · 유제품, 임대료(하위 없음)
    CategoryInfo food;
    CategoryInfo beans;
    CategoryInfo milk;
    CategoryInfo rent;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM expense.expenses");
        jdbc.update("DELETE FROM category.categories");
        food = categoryService.add(STORE_ID, null, "식자재");
        beans = categoryService.add(STORE_ID, food.id(), "원두");
        milk = categoryService.add(STORE_ID, food.id(), "유제품");
        rent = categoryService.add(STORE_ID, null, "임대료");
    }

    @Test
    @DisplayName("비용 등록 - 저장되고 ExpenseRegistered 발행")
    void registerSavesAndPublishes(AssertablePublishedEvents events) {
        ExpenseInfo info = expenseService.register(STORE_ID, beans.id(), 35000L, TODAY, "원두 2kg");

        assertThat(info).isEqualTo(new ExpenseInfo(info.id(), beans.id(), "식자재 > 원두", true, 35000L, TODAY, "원두 2kg"));
        assertThat(jdbc.queryForObject("SELECT amount FROM expense.expenses", Long.class)).isEqualTo(35000L);
        assertThat(events)
                .contains(ExpenseRegistered.class)
                .matching(ExpenseRegistered::expenseId, info.id());
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
