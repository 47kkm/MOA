package moa.store.internal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import moa.shared.ErrorCode;
import moa.shared.MoaException;
import moa.store.StoreInfo;
import moa.store.StoreService;
import moa.store.StoreType;

/**
 * 컨트롤러만 띄우는 HTTP 테스트 (04 설계 0.7). 서비스는 가짜(Mock)라 규칙은 여기서 검사하지 않고,
 * JSON 변환과 상태 코드만 본다.
 */
@WebMvcTest(StoreController.class)
class StoreControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    StoreService storeService;

    // 매장 접근 검사(인터셉터)도 함께 뜨므로 필요하다. 가짜라 existsById는 늘 false →
    // 아래 "없는 ID" 테스트가 NOT_FOUND면 store 자기 경로가 검사에서 빠졌다는 뜻이다 (04 설계 1.7)
    @MockitoBean
    StoreRepository stores;

    @Test
    @DisplayName("매장 등록 - 201과 본문")
    void registerReturns201() throws Exception {
        given(storeService.register("카페 모아", StoreType.CAFE))
                .willReturn(new StoreInfo(1L, "카페 모아", StoreType.CAFE, LocalDate.of(2026, 10, 28)));

        mvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "카페 모아", "type": "CAFE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("카페 모아"))
                .andExpect(jsonPath("$.type").value("CAFE"))
                .andExpect(jsonPath("$.registeredAt").value("2026-10-28"));
    }

    @Test
    @DisplayName("매장 등록 - 빈 이름 → 400 VALIDATION")
    void registerEmptyName() throws Exception {
        given(storeService.register("", StoreType.CAFE))
                .willThrow(new IllegalArgumentException("name is required"));

        mvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "type": "CAFE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("name is required"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    @DisplayName("매장 등록 - 업종이 목록에 없음 → 400 VALIDATION")
    void registerUnknownType() throws Exception {
        mvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "카페 모아", "type": "BAKERY"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    @DisplayName("매장 조회 - 없는 ID → 404 NOT_FOUND (store 자기 경로는 접근 검사에서 제외)")
    void getMissingStore() throws Exception {
        given(storeService.get(99L)).willThrow(new MoaException(ErrorCode.NOT_FOUND, "store not found: 99"));

        mvc.perform(get("/api/stores/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
