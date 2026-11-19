package moa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

// Swagger: 컨트롤러를 읽어 API 문서를 만든다. 백엔드만 있을 때의 동작 확인 화면 (00_start ④)
// 프론트는 이 화면을 보며 타입을 손으로 쓴다 (04_design_frontend F0-4)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiDocsTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("API 문서 - /v3/api-docs 200, 매장 · 비용 분류 · 비용 경로가 있다")
    void apiDocs() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/stores']").exists())
                .andExpect(jsonPath("$.paths['/api/stores/{storeId}/categories']").exists())
                .andExpect(jsonPath("$.paths['/api/stores/{storeId}/expenses']").exists());
    }

    // 브라우저로 여는 주소. springdoc이 화면 파일(/swagger-ui/index.html)로 보낸다
    @Test
    @DisplayName("API 문서 - /swagger-ui.html → Swagger 화면")
    void swaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
