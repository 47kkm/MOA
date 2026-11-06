package moa.category.internal;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.shared.ErrorCode;
import moa.shared.MoaException;

/**
 * 컨트롤러만 띄우는 HTTP 테스트 (04 설계 0.7). 서비스는 가짜(Mock)라 규칙은 여기서 검사하지 않고,
 * JSON 변환과 상태 코드만 본다.
 * 매장 접근 검사(store의 인터셉터)는 store 테스트가 확인하므로 여기서는 store 쪽 빈을 띄우지 않는다.
 */
@WebMvcTest(value = CategoryController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "moa\\.store\\..*"))
class CategoryControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CategoryService categoryService;

    @Test
    @DisplayName("분류 추가 - 201과 본문")
    void addReturns201() throws Exception {
        given(categoryService.add(1L, 10L, "시럽"))
                .willReturn(new CategoryInfo(12L, 10L, "시럽", "식자재 > 시럽", true, 4, 0));

        mvc.perform(post("/api/stores/1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId": 10, "name": "시럽"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.parentId").value(10))
                .andExpect(jsonPath("$.name").value("시럽"))
                .andExpect(jsonPath("$.path").value("식자재 > 시럽"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.expenseCount").value(0));
    }

    @Test
    @DisplayName("분류 삭제 - 사용 중 → 409 CATEGORY_IN_USE")
    void deleteInUse() throws Exception {
        willThrow(new MoaException(ErrorCode.CATEGORY_IN_USE, "category has expenses: 5"))
                .given(categoryService).delete(1L, 5L);

        mvc.perform(delete("/api/stores/1/categories/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"));
    }

    @Test
    @DisplayName("분류 삭제 - 204")
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/stores/1/categories/5"))
                .andExpect(status().isNoContent());

        then(categoryService).should().delete(1L, 5L);
    }

    @Test
    @DisplayName("분류 tree 조회 - 200과 목록, includeInactive 기본값은 false")
    void treeDefaultsToActiveOnly() throws Exception {
        given(categoryService.tree(1L, false))
                .willReturn(List.of(new CategoryInfo(10L, null, "식자재", "식자재", true, 0, 3)));

        mvc.perform(get("/api/stores/1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("식자재"))
                .andExpect(jsonPath("$[0].expenseCount").value(3));
    }

    @Test
    @DisplayName("분류 이름 변경 - 200과 본문")
    void renameReturns200() throws Exception {
        given(categoryService.rename(1L, 10L, "재료비"))
                .willReturn(new CategoryInfo(10L, null, "재료비", "재료비", true, 0, 0));

        mvc.perform(patch("/api/stores/1/categories/10/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "재료비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("재료비"));
    }

    @Test
    @DisplayName("분류 비활성화 - 200과 deactivatedIds")
    void deactivateReturnsIds() throws Exception {
        given(categoryService.deactivate(1L, 10L)).willReturn(List.of(10L, 11L));

        mvc.perform(post("/api/stores/1/categories/10/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deactivatedIds[0]").value(10))
                .andExpect(jsonPath("$.deactivatedIds[1]").value(11));
    }

    @Test
    @DisplayName("분류 활성화 - 200과 본문")
    void activateReturns200() throws Exception {
        given(categoryService.activate(1L, 10L))
                .willReturn(new CategoryInfo(10L, null, "식자재", "식자재", true, 0, 0));

        mvc.perform(post("/api/stores/1/categories/10/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }
}
