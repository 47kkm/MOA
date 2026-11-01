package moa.store.internal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 다른 모듈의 경로(/api/stores/{storeId}/...) 앞에서 도는 매장 접근 검사 (04 설계 1.2).
 * 아직 다른 모듈이 없으므로 가짜 컨트롤러(/ping)로 확인한다.
 */
@WebMvcTest(StoreAccessInterceptorTest.PingController.class)
@Import({ StoreWebConfig.class, StoreAccessInterceptor.class, StoreAccessInterceptorTest.PingController.class })
class StoreAccessInterceptorTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    StoreRepository stores;

    @Test
    @DisplayName("매장 접근 검사 - 없는 storeId 하위 경로(가짜 컨트롤러) → 404 STORE_NOT_FOUND")
    void missingStore() throws Exception {
        given(stores.existsById(99L)).willReturn(false);

        mvc.perform(get("/api/stores/99/ping"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"));
    }

    @Test
    @DisplayName("매장 접근 검사 - 있는 storeId → 통과")
    void existingStore() throws Exception {
        given(stores.existsById(1L)).willReturn(true);

        mvc.perform(get("/api/stores/1/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @RestController
    static class PingController {

        @GetMapping("/api/stores/{storeId}/ping")
        String ping(@PathVariable Long storeId) {
            return "pong";
        }
    }
}
