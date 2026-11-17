package moa.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import moa.budget.BudgetEvents.MonthlyBudgetSet;
import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.expense.ExpenseService;
import moa.shared.ErrorCode;
import moa.shared.MoaException;

/**
 * budget 모듈과 그 의존(expense · category)까지 띄우고 진짜 MySQL(Testcontainers)에 저장해 보는 통합 테스트 (04 설계 0.7, 5.7).
 * 가짜는 만들지 않는다. 비용 분류와 비용은 진짜 CategoryService · ExpenseService로 만든다.
 */
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import({ TestcontainersConfiguration.class, BudgetServiceTest.FixedClock.class })
class BudgetServiceTest {

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

    // 응답은 현황 조회와 같은 모양 (04 설계 5.2). 행은 상위 비용 분류 순서, 항목이 없는 행은 예산 칸이 null
    @Test
    @DisplayName("월 예산 설정 - 저장되고 MonthlyBudgetSet 발행, 현황 (총액 · 미배정 · 행마다 예산)")
    void setSavesAndPublishes(AssertablePublishedEvents events) {
        BudgetStatus status = budgetService.set(STORE_ID, THIS_MONTH, 5_000_000L,
                List.of(new BudgetLineInput(food.id(), 2_000_000L)));

        assertThat(status.yearMonth()).isEqualTo(THIS_MONTH);
        assertThat(status.budget()).isEqualTo(5_000_000L);
        assertThat(status.unassigned()).isEqualTo(3_000_000L);
        assertThat(status.lines())
                .extracting(BudgetLineStatus::categoryId, BudgetLineStatus::name, BudgetLineStatus::active,
                        BudgetLineStatus::budget)
                .containsExactly(tuple(food.id(), "식자재", true, 2_000_000L), tuple(rent.id(), "임대료", true, null));
        assertThat(jdbc.queryForObject("SELECT amount FROM budget.budget_lines", Long.class)).isEqualTo(2_000_000L);
        assertThat(events)
                .contains(MonthlyBudgetSet.class)
                .matching(MonthlyBudgetSet::yearMonth, THIS_MONTH);
    }

    // 항목은 상위 비용 분류만, 새 항목은 켜진 상위만 (04 설계 5.3, 기획서 P5-7). 다른 매장의 비용 분류도 tops에 없어서 409
    @Test
    @DisplayName("월 예산 설정 - 하위 · 없는 · 꺼진 상위 새 항목 → BUDGET_CATEGORY_UNAVAILABLE")
    void setRejectsUnavailableCategory() {
        CategoryInfo labor = categoryService.add(STORE_ID, null, "인건비");
        categoryService.deactivate(STORE_ID, labor.id());

        assertFails(ErrorCode.BUDGET_CATEGORY_UNAVAILABLE, () -> setWithLine(beans.id()));
        assertFails(ErrorCode.BUDGET_CATEGORY_UNAVAILABLE, () -> setWithLine(999L));
        assertFails(ErrorCode.BUDGET_CATEGORY_UNAVAILABLE, () -> setWithLine(labor.id()));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM budget.monthly_budgets", Integer.class)).isZero();
    }

    private BudgetStatus setWithLine(Long categoryId) {
        return budgetService.set(STORE_ID, THIS_MONTH, 5_000_000L, List.of(new BudgetLineInput(categoryId, 1_000_000L)));
    }

    private static void assertFails(ErrorCode code, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(MoaException.class, e -> assertThat(e.getCode()).isEqualTo(code));
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
