package moa.category.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// 애그리거트는 분류 하나 (04 설계 D2-1). 상위는 parentId로만 가리키고 JPA 연관관계는 쓰지 않는다
@Entity
@Table(name = "categories", catalog = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long storeId;

    private Long parentId;

    private String name;

    private int sortOrder;

    private boolean active;

    // JPA가 DB에서 읽어 온 값을 채울 때 쓰는 빈 생성자. 우리 코드는 top()·childOf()로만 만든다
    protected Category() {
    }

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

    // 바뀌었으면 true. 공백을 자르고 보니 지금 이름과 같으면 아무 일 없이 false
    public boolean rename(String name) {
        String newName = validName(name);
        if (newName.equals(this.name)) {
            return false;
        }
        this.name = newName;
        return true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    // 이름 규칙: 앞뒤 공백을 자른 뒤 1~20자 (04 설계 2.3).
    // 응용 서비스도 쓴다: 이름 변경 때 형제 이름과 비교하기 전에 같은 규칙으로 자른다
    public static String validName(String name) {
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
