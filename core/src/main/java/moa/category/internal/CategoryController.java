package moa.category.internal;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import moa.category.CategoryInfo;
import moa.category.CategoryService;

// JSON ↔ 레코드 변환만 한다. 규칙 검사는 Category와 CategoryService에 있다 (04 설계 0.5)
// storeId는 store의 매장 접근 검사를 지나온 값이라 여기서 다시 확인하지 않는다 (04 설계 1.2)
@RestController
@RequestMapping("/api/stores/{storeId}/categories")
class CategoryController {

    private final CategoryService categoryService;

    CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    List<CategoryInfo> tree(@PathVariable Long storeId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return categoryService.tree(storeId, includeInactive);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryInfo add(@PathVariable Long storeId, @RequestBody AddRequest request) {
        return categoryService.add(storeId, request.parentId(), request.name());
    }

    @PatchMapping("/{id}/name")
    CategoryInfo rename(@PathVariable Long storeId, @PathVariable Long id, @RequestBody RenameRequest request) {
        return categoryService.rename(storeId, id, request.name());
    }

    @PostMapping("/{id}/deactivate")
    DeactivatedResponse deactivate(@PathVariable Long storeId, @PathVariable Long id) {
        return new DeactivatedResponse(categoryService.deactivate(storeId, id));
    }

    @PostMapping("/{id}/activate")
    CategoryInfo activate(@PathVariable Long storeId, @PathVariable Long id) {
        return categoryService.activate(storeId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long storeId, @PathVariable Long id) {
        categoryService.delete(storeId, id);
    }

    // parentId가 없으면 상위 분류
    record AddRequest(Long parentId, String name) {
    }

    record RenameRequest(String name) {
    }

    // 상위를 끄면 함께 꺼진 하위 ID도 들어 있다. 프론트가 화면의 켜짐 표시를 한 번에 바꾼다
    record DeactivatedResponse(List<Long> deactivatedIds) {
    }
}
