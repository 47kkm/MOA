package moa.category;

import java.util.List;

public final class CategoryEvents {

    private CategoryEvents() {
    }

    // Phase 1에는 받는 모듈이 없다. Phase 2 AI 서비스가 tree 변화를 따라가는 데 쓴다 (04 설계 2.7)
    public record CategoryAdded(Long storeId, Long categoryId, Long parentId, String name) {
    }

    // 같은 이름으로 바꾸면 발행하지 않는다
    public record CategoryRenamed(Long storeId, Long categoryId, String oldName, String newName) {
    }

    // 상위를 끄면 함께 꺼진 하위 ID도 들어 있다
    public record CategoryDeactivated(Long storeId, List<Long> categoryIds) {
    }
}
