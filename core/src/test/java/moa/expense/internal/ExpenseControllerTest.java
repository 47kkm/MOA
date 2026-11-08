package moa.expense.internal;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import moa.expense.ExpenseInfo;
import moa.expense.ExpenseList;
import moa.expense.ExpenseService;
import moa.shared.ErrorCode;
import moa.shared.MoaException;

/**
 * 컨트롤러만 띄우는 HTTP 테스트 (04 설계 0.7). 서비스는 가짜(Mock)라 규칙은 여기서 검사하지 않고,
 * JSON 변환과 상태 코드만 본다.
 * 매장 접근 검사(store의 인터셉터)는 store 테스트가 확인하므로 여기서는 store 쪽 빈을 띄우지 않는다.
 */
@WebMvcTest(value = ExpenseController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "moa\\.store\\..*"))
class ExpenseControllerTest {

    static final LocalDate DATE = LocalDate.of(2026, 10, 28);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ExpenseService expenseService;

    // 금액은 숫자 하나, 날짜는 "2026-10-28" (04 설계 D4-10)
    @Test
    @DisplayName("비용 등록 - 201과 본문")
    void registerReturns201() throws Exception {
        given(expenseService.register(1L, 11L, 35000L, DATE, "원두 2kg"))
                .willReturn(new ExpenseInfo(7L, 11L, "식자재 > 원두", true, 35000L, DATE, "원두 2kg"));

        mvc.perform(post("/api/stores/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 11, "amount": 35000, "date": "2026-10-28", "memo": "원두 2kg"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.categoryId").value(11))
                .andExpect(jsonPath("$.categoryPath").value("식자재 > 원두"))
                .andExpect(jsonPath("$.categoryActive").value(true))
                .andExpect(jsonPath("$.amount").value(35000))
                .andExpect(jsonPath("$.date").value("2026-10-28"))
                .andExpect(jsonPath("$.memo").value("원두 2kg"));
    }

    // categoryId는 빈 값이면 필터 없음 (04 설계 4.7 "?yearMonth=2026-10&categoryId=")
    @Test
    @DisplayName("비용 목록 조회 - 200과 본문 (yearMonth \"2026-10\", 합계 · 건수)")
    void listReturns200() throws Exception {
        given(expenseService.list(1L, YearMonth.of(2026, 10), null))
                .willReturn(new ExpenseList(YearMonth.of(2026, 10), 35000L, 1,
                        List.of(new ExpenseInfo(7L, 11L, "식자재 > 원두", false, 35000L, DATE, null))));

        mvc.perform(get("/api/stores/1/expenses").param("yearMonth", "2026-10").param("categoryId", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-10"))
                .andExpect(jsonPath("$.total").value(35000))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].categoryActive").value(false));
    }

    // 공통 핸들러가 Spring의 400을 VALIDATION으로 바꾼다 (04 설계 0.6, 4.7)
    @Test
    @DisplayName("비용 목록 조회 - yearMonth 없음 · 형식 오류 → 400 VALIDATION")
    void listRequiresYearMonth() throws Exception {
        mvc.perform(get("/api/stores/1/expenses"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
        mvc.perform(get("/api/stores/1/expenses").param("yearMonth", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    // 수정과 재분류는 같은 경로 (04 설계 D4-1)
    @Test
    @DisplayName("비용 수정 - 200과 본문")
    void updateReturns200() throws Exception {
        given(expenseService.update(1L, 7L, 12L, 12000L, DATE, null))
                .willReturn(new ExpenseInfo(7L, 12L, "식자재 > 유제품", true, 12000L, DATE, null));

        mvc.perform(put("/api/stores/1/expenses/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 12, "amount": 12000, "date": "2026-10-28"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryPath").value("식자재 > 유제품"))
                .andExpect(jsonPath("$.amount").value(12000));
    }

    @Test
    @DisplayName("비용 재분류 - 고를 수 없는 비용 분류 → 409 EXPENSE_CATEGORY_UNAVAILABLE")
    void reclassifyUnavailable() throws Exception {
        given(expenseService.update(1L, 7L, 10L, 12000L, DATE, null))
                .willThrow(new MoaException(ErrorCode.EXPENSE_CATEGORY_UNAVAILABLE, "category is not selectable: 10"));

        mvc.perform(put("/api/stores/1/expenses/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 10, "amount": 12000, "date": "2026-10-28"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXPENSE_CATEGORY_UNAVAILABLE"));
    }

    @Test
    @DisplayName("비용 삭제 - 204")
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/stores/1/expenses/7"))
                .andExpect(status().isNoContent());

        then(expenseService).should().delete(1L, 7L);
    }

    @Test
    @DisplayName("비용 삭제 - 없는 비용 → 404 NOT_FOUND")
    void deleteNotFound() throws Exception {
        willThrow(new MoaException(ErrorCode.NOT_FOUND, "expense not found: 99"))
                .given(expenseService).delete(1L, 99L);

        mvc.perform(delete("/api/stores/1/expenses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("비용 등록 - 날짜 형식 오류 → 400 VALIDATION")
    void registerRejectsBadDate() throws Exception {
        mvc.perform(post("/api/stores/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 11, "amount": 35000, "date": "2026/10/28"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }
}
