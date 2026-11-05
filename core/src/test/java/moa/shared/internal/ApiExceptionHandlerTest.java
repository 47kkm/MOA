package moa.shared.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모듈 컨트롤러가 직접 던지지 않는 예외의 응답 (04 설계 0.6). Spring 없이 가짜 컨트롤러 하나와 핸들러만 붙인다.
 */
class ApiExceptionHandlerTest {

    MockMvc mvc = MockMvcBuilders.standaloneSetup(new FakeController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    @DisplayName("오류 응답 - 처리하지 못한 예외 → 500 INTERNAL_ERROR, 내부 메시지는 숨김")
    void unexpectedException() throws Exception {
        mvc.perform(get("/test/broken"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("internal error"));
    }

    @Test
    @DisplayName("오류 응답 - 없는 주소 → 404 NOT_FOUND")
    void unknownPath() throws Exception {
        mvc.perform(get("/test/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("오류 응답 - 숫자 자리에 글자 → 400 VALIDATION")
    void letterForNumber() throws Exception {
        mvc.perform(get("/test/numbers/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @RestController
    static class FakeController {

        @GetMapping("/test/broken")
        String broken() {
            throw new IllegalStateException("secret internal detail");
        }

        @GetMapping("/test/numbers/{n}")
        Long number(@PathVariable Long n) {
            return n;
        }
    }
}
