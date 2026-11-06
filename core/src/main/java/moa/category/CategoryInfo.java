package moa.category;

// path는 "식자재 > 유제품", 상위는 이름만. expenseCount는 tree에서만 채운다 (04 설계 2.7)
public record CategoryInfo(Long id, Long parentId, String name, String path, boolean active, int sortOrder,
        long expenseCount) {
}
