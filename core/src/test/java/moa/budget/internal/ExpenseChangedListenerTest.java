package moa.budget.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

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
import moa.budget.BudgetEvents.BudgetSpentUpdated;
import moa.budget.BudgetLineInput;
import moa.budget.BudgetService;
import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.expense.ExpenseService;

/**
 * 비용 이벤트를 받아 소진액 갱신 · 예산 초과 감지 이벤트를 내는 리스너 (04 설계 5.2).
 * 진짜 expense로 비용을 저장하면 커밋 뒤 같은 스레드에서 리스너가 돈다 (0.3). budget이 저장하는 것은 없다 (D5-1)
 */
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import({ TestcontainersConfiguration.class, ExpenseChangedListenerTest.FixedClock.class })
class ExpenseChangedListenerTest {

    static final Long STORE_ID = 1L;
    static final LocalDate TODAY = LocalDate.of(2026, 11, 17);
    static final YearMonth THIS_MONTH = YearMonth.of(2026, 11);

    @Autowired
    BudgetService budgetService;

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
        jdbc.update("DELETE FROM budget.budget_lines");
        jdbc.update("DELETE FROM budget.monthly_budgets");
        jdbc.update("DELETE FROM expense.expenses");
        jdbc.update("DELETE FROM category.categories");
        food = categoryService.add(STORE_ID, null, "식자재");
        beans = categoryService.add(STORE_ID, food.id(), "원두");
        milk = categoryService.add(STORE_ID, food.id(), "유제품");
        rent = categoryService.add(STORE_ID, null, "임대료");
    }

    // 하위 비용은 상위로 올려 그 상위의 지금 소진(그 달 합)을 알린다 (04 설계 5.2, 5.7)
    @Test
    @DisplayName("소진액 갱신 - 예산 있는 달의 비용 등록 → 그 상위의 BudgetSpentUpdated (그 달 소진)")
    void registerPublishesSpentUpdated(AssertablePublishedEvents events) {
        budgetService.set(STORE_ID, THIS_MONTH, 5_000_000L, List.of(new BudgetLineInput(food.id(), 2_000_000L)));
        expenseService.register(STORE_ID, milk.id(), 35_000L, LocalDate.of(2026, 11, 3), null);

        expenseService.register(STORE_ID, beans.id(), 180_000L, LocalDate.of(2026, 11, 10), null);

        assertThat(events.ofType(BudgetSpentUpdated.class))
                .containsExactly(new BudgetSpentUpdated(STORE_ID, THIS_MONTH, food.id(), 35_000L),
                        new BudgetSpentUpdated(STORE_ID, THIS_MONTH, food.id(), 215_000L));
    }

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
