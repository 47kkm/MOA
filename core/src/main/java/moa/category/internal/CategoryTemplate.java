package moa.category.internal;

import java.util.List;

// 기본 비용 분류 템플릿. Phase 1은 업종과 상관없이 카페 템플릿 하나 (04 설계 2.2, D2-3)
// 매장이 등록되면 이 목록이 그 매장의 비용 분류로 DB에 복사된다. 그 뒤로는 사장님이 고친다
final class CategoryTemplate {

    record Node(String name, List<String> children) {
    }

    static final List<Node> NODES = List.of(
            new Node("식자재", List.of("원두", "유제품", "베이커리", "기타 식자재")),
            new Node("인건비", List.of("급여", "주휴·수당")),
            new Node("임대료", List.of()),
            new Node("공과금", List.of("전기", "수도·가스", "통신")),
            new Node("마케팅", List.of()),
            new Node("소모품", List.of()),
            new Node("기타", List.of()));

    private CategoryTemplate() {
    }
}
