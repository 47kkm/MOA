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
        return new Category(storeId, null, name, sortOrder);
    }

    public boolean isTop() {
        return parentId == null;
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
