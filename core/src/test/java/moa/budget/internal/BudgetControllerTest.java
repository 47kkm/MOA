package moa.budget.internal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import moa.budget.BudgetLevel;
import moa.budget.BudgetLineInput;
import moa.budget.BudgetLineStatus;
import moa.budget.BudgetService;
import moa.budget.BudgetStatus;
import moa.shared.ErrorCode;
import moa.shared.MoaException;

/**
 * 컨트롤러만 띄우는 HTTP 테스트 (04 설계 0.7). 서비스는 가짜(Mock)라 규칙은 여기서 검사하지 않고,
 * JSON 변환과 상태 코드만 본다.
 * 매장 접근 검사(store의 인터셉터)는 store 테스트가 확인하므로 여기서는 store 쪽 빈을 띄우지 않는다.
 */
@WebMvcTest(value = BudgetController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "moa\\.store\\..*"))
class BudgetControllerTest {

    static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BudgetService budgetService;

    // 금액은 숫자 하나 (04 설계 D4-10). 항목이 없는 행은 예산 칸이 null (5.7)
    @Test
    @DisplayName("월 예산 현황 조회 - 200과 본문 (yearMonth \"2026-11\", 항목 없는 행은 예산 칸 null)")
    void statusReturns200() throws Exception {
        given(budgetService.status(1L, NOVEMBER)).willReturn(new BudgetStatus(NOVEMBER, 5_000_000L, 2_130_000L,
                2_870_000L, 43L, BudgetLevel.NORMAL, 1_500_000L, List.of(
                        new BudgetLineStatus(10L, "식자재", true, 2_000_000L, 2_130_000L, -130_000L, 107L,
                                BudgetLevel.OVER, 1_900_000L),
                        new BudgetLineStatus(20L, "임대료", false, null, 0L, null, null, null, 0L))));

        mvc.perform(get("/api/stores/1/budgets/2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-11"))
                .andExpect(jsonPath("$.budget").value(5_000_000))
                .andExpect(jsonPath("$.unassigned").value(1_500_000))
                .andExpect(jsonPath("$.lines[0].remaining").value(-130_000))
                .andExpect(jsonPath("$.lines[0].ratePercent").value(107))
                .andExpect(jsonPath("$.lines[0].level").value("OVER"))
                .andExpect(jsonPath("$.lines[0].lastMonthSpent").value(1_900_000))
                .andExpect(jsonPath("$.lines[1].active").value(false))
                .andExpect(jsonPath("$.lines[1].budget").isEmpty());
    }

    // 공통 핸들러가 Spring의 400을 VALIDATION으로 바꾼다 (04 설계 0.6, 5.7)
    @Test
    @DisplayName("월 예산 현황 조회 - yearMonth 형식 오류 → 400 VALIDATION")
    void statusRejectsBadYearMonth() throws Exception {
        mvc.perform(get("/api/stores/1/budgets/2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    @DisplayName("월 예산 설정 - 200과 본문")
    void setReturns200() throws Exception {
        given(budgetService.set(1L, NOVEMBER, 5_000_000L, List.of(new BudgetLineInput(10L, 2_000_000L))))
                .willReturn(new BudgetStatus(NOVEMBER, 5_000_000L, 0L, 5_000_000L, 0L, BudgetLevel.NORMAL, 3_000_000L,
                        List.of(new BudgetLineStatus(10L, "식자재", true, 2_000_000L, 0L, 2_000_000L, 0L,
                                BudgetLevel.NORMAL, 0L))));

        mvc.perform(put("/api/stores/1/budgets/2026-11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"total": 5000000, "lines": [{"categoryId": 10, "amount": 2000000}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budget").value(5_000_000))
                .andExpect(jsonPath("$.unassigned").value(3_000_000))
                .andExpect(jsonPath("$.lines[0].budget").value(2_000_000));
    }

    @Test
    @DisplayName("월 예산 설정 - 쓸 수 없는 비용 분류 → 409 BUDGET_CATEGORY_UNAVAILABLE")
    void setUnavailableCategory() throws Exception {
        given(budgetService.set(1L, NOVEMBER, 5_000_000L, List.of(new BudgetLineInput(11L, 1_000_000L))))
                .willThrow(new MoaException(ErrorCode.BUDGET_CATEGORY_UNAVAILABLE,
                        "category is not available for a budget line: 11"));

        mvc.perform(put("/api/stores/1/budgets/2026-11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"total": 5000000, "lines": [{"categoryId": 11, "amount": 1000000}]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUDGET_CATEGORY_UNAVAILABLE"));
    }

    // 입력 규칙(합 ≤ 총액 등)은 MonthlyBudget이 IllegalArgumentException으로 거부 → 공통 핸들러가 400 (04 설계 0.6, 5.3)
    // 금액은 원 단위 정수. 소수는 잘라 저장하지 않고 거부한다 (04 설계 D4-15)
    @Test
    @DisplayName("월 예산 설정 - 입력 규칙 위반 · 소수 금액 → 400 VALIDATION")
    void setRejectsInvalidInput() throws Exception {
        given(budgetService.set(1L, NOVEMBER, 1_000_000L, List.of(new BudgetLineInput(10L, 2_000_000L))))
                .willThrow(new IllegalArgumentException("sum of lines must not exceed total"));

        mvc.perform(put("/api/stores/1/budgets/2026-11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"total": 1000000, "lines": [{"categoryId": 10, "amount": 2000000}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
        mvc.perform(put("/api/stores/1/budgets/2026-11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"total": 5000000.5, "lines": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }
}
