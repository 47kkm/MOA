package moa.expense.internal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
import moa.expense.ExpenseService;

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
}
