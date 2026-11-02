package moa.category.internal;

public class Category {

    private Long id;

    private Long storeId;

    private Long parentId;

    private String name;

    private int sortOrder;

    private boolean active;

    private Category(Long storeId, Long parentId, String name, int sortOrder) {
        this.storeId = storeId;
        this.parentId = parentId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    public static Category top(Long storeId, String name, int sortOrder) {
        return new Category(storeId, null, validName(name), sortOrder);
    }

    // 2단계까지 (04 설계 2.3). 응용 서비스가 먼저 isTop()으로 보고 409를 내므로, 여기는 마지막 방어선이다
    public static Category childOf(Category parent, String name, int sortOrder) {
        if (!parent.isTop()) {
            throw new IllegalArgumentException("parent must be a top category");
        }
        return new Category(parent.storeId, parent.id, validName(name), sortOrder);
    }

    // 이름 규칙: 앞뒤 공백을 자른 뒤 1~20자 (04 설계 2.3)
    private static String validName(String name) {
        String stripped = name == null ? "" : name.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (stripped.length() > 20) {
            throw new IllegalArgumentException("name must be at most 20 characters");
        }
        return stripped;
    }

    public boolean isTop() {
        return parentId == null;
    }

    public boolean isChildOf(Category parent) {
        return parent.id.equals(parentId);
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
