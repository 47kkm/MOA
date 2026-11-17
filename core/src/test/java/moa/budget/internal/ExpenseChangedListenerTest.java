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
import moa.budget.BudgetEvents.BudgetExceeded;
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

    // 그 달 예산이 없으면 아무것도 내지 않는다 (요구사항정의서 8.5 "무시", 04 설계 5.2). 소진은 현황 조회 때 계산된다
    @Test
    @DisplayName("소진액 갱신 - 예산 없는 달의 비용 등록 → 이벤트 없음")
    void noBudgetNoEvent(AssertablePublishedEvents events) {
        budgetService.set(STORE_ID, THIS_MONTH.plusMonths(1), 5_000_000L, List.of());

        expenseService.register(STORE_ID, beans.id(), 180_000L, TODAY, null);

        assertThat(events.ofType(BudgetSpentUpdated.class)).isEmpty();
    }

    // 재분류는 이전 상위에서 빠지고 이후 상위에 더해진다 (04 설계 5.2, D4-2의 이전 · 이후 값)
    @Test
    @DisplayName("소진액 갱신 - 비용 재분류 원두 → 임대료 → 식자재 · 임대료 둘 다 BudgetSpentUpdated")
    void reclassifyPublishesBothTops(AssertablePublishedEvents events) {
        budgetService.set(STORE_ID, THIS_MONTH, 5_000_000L, List.of());
        Long id = expenseService.register(STORE_ID, beans.id(), 180_000L, TODAY, null).id();

        expenseService.update(STORE_ID, id, rent.id(), 180_000L, TODAY, null);

        assertThat(events.ofType(BudgetSpentUpdated.class))
                .contains(new BudgetSpentUpdated(STORE_ID, THIS_MONTH, food.id(), 0L),
                        new BudgetSpentUpdated(STORE_ID, THIS_MONTH, rent.id(), 180_000L));
    }

    // 날짜를 지난 달로 옮기면 두 달이 바뀐다. 예산이 있는 이번 달만 알린다. 삭제는 빠진 소진을 알린다
    @Test
    @DisplayName("소진액 갱신 - 비용 날짜를 지난 달로 · 삭제 → 예산 있는 달의 BudgetSpentUpdated")
    void updateAndDeletePublish(AssertablePublishedEvents events) {
        budgetService.set(STORE_ID, THIS_MONTH, 5_000_000L, List.of());
        Long moved = expenseService.register(STORE_ID, beans.id(), 180_000L, TODAY, null).id();
        Long deleted = expenseService.register(STORE_ID, rent.id(), 1_000_000L, TODAY, null).id();

        expenseService.update(STORE_ID, moved, beans.id(), 180_000L, LocalDate.of(2026, 10, 31), null);
        expenseService.delete(STORE_ID, deleted);

        assertThat(events.ofType(BudgetSpentUpdated.class))
                .containsExactly(new BudgetSpentUpdated(STORE_ID, THIS_MONTH, food.id(), 180_000L),
                        new BudgetSpentUpdated(STORE_ID, THIS_MONTH, rent.id(), 1_000_000L),
                        new BudgetSpentUpdated(STORE_ID, THIS_MONTH, food.id(), 0L),
                        new BudgetSpentUpdated(STORE_ID, THIS_MONTH, rent.id(), 0L));
    }

    // "바뀐 상위마다" (04 설계 5.2): 같은 상위 안에서 옮기거나 메모만 고치면 그 상위의 소진은 그대로라 알리지 않는다
    @Test
    @DisplayName("소진액 갱신 - 같은 상위 안의 재분류 원두 → 유제품 · 메모만 수정 → 이벤트 없음")
    void sameTopNoEvent(AssertablePublishedEvents events) {
        budgetService.set(STORE_ID, THIS_MONTH, 5_000_000L, List.of());
        Long id = expenseService.register(STORE_ID, beans.id(), 180_000L, TODAY, null).id();

        expenseService.update(STORE_ID, id, milk.id(), 180_000L, TODAY, null);
        expenseService.update(STORE_ID, id, milk.id(), 180_000L, TODAY, "우유 6팩");

        assertThat(events.ofType(BudgetSpentUpdated.class))
                .containsExactly(new BudgetSpentUpdated(STORE_ID, THIS_MONTH, food.id(), 180_000L));
    }

    // 예산 초과 = 100% 선을 아래 → 위로 넘는 순간 한 번 (04 설계 5.3, D5-9 "소진 ≥ 예산")
    @Test
    @DisplayName("예산 초과 감지 - 1,990,000에 20,000 → BudgetExceeded, 이어서 5,000 → 없음")
    void exceededOnceWhenCrossing(AssertablePublishedEvents events) {
        budgetService.set(STORE_ID, THIS_MONTH, 5_000_000L, List.of(new BudgetLineInput(food.id(), 2_000_000L)));
        expenseService.register(STORE_ID, beans.id(), 1_990_000L, TODAY, null);

        expenseService.register(STORE_ID, beans.id(), 20_000L, TODAY, null);
        expenseService.register(STORE_ID, milk.id(), 5_000L, TODAY, null);

        assertThat(events.ofType(BudgetExceeded.class))
                .containsExactly(new BudgetExceeded(STORE_ID, THIS_MONTH, food.id(), 2_010_000L, 2_000_000L));
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
