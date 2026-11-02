package moa.category;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import moa.category.CategoryEvents.CategoryAdded;
import moa.category.CategoryEvents.CategoryDeactivated;
import moa.category.CategoryEvents.CategoryRenamed;
import moa.category.internal.Category;
import moa.category.internal.CategoryRepository;
import moa.shared.ErrorCode;
import moa.shared.MoaException;

@Service
public class CategoryService {

    // 형제 안은 만든 순서 (04 설계 2.2 "분류 tree 조회")
    private static final Comparator<Category> SIBLING_ORDER = Comparator.comparingInt(Category::getSortOrder)
            .thenComparing(Category::getId);

    private final CategoryRepository categories;
    private final CategoryUsage usage;
    private final ApplicationEventPublisher events;

    // usage는 인터페이스만 안다. 실제로 무엇이 들어오는지(지금은 NoExpenseYet, 3주차부터 expense의 구현)는 Spring이 정한다
    public CategoryService(CategoryRepository categories, CategoryUsage usage, ApplicationEventPublisher events) {
        this.categories = categories;
        this.usage = usage;
        this.events = events;
    }

    @Transactional
    public CategoryInfo add(Long storeId, Long parentId, String name) {
        Category parent = parentId == null ? null : find(storeId, parentId);
        if (parent != null && !parent.isTop()) {
            throw new MoaException(ErrorCode.CATEGORY_DEPTH, "categories have at most 2 levels");
        }
        // 꺼진 상위 아래에 켜진 하위가 생기지 않게 (04 설계 2.3)
        if (parent != null && !parent.isActive()) {
            throw new MoaException(ErrorCode.CATEGORY_PARENT_INACTIVE, "parent category is inactive");
        }
        if (categories.countByStoreIdAndParentId(storeId, parentId) >= Category.MAX_SIBLINGS) {
            throw new MoaException(ErrorCode.CATEGORY_LIMIT,
                    "at most " + Category.MAX_SIBLINGS + " categories per level");
        }
        int sortOrder = categories.nextSortOrder(storeId, parentId);
        Category category = parent == null
                ? Category.top(storeId, name, sortOrder)
                : Category.childOf(parent, name, sortOrder);
        rejectDuplicateName(storeId, parentId, category.getName());
        categories.save(category);
        events.publishEvent(new CategoryAdded(storeId, category.getId(), category.getParentId(), category.getName()));
        return toInfo(category, parent);
    }

    @Transactional
    public CategoryInfo rename(Long storeId, Long id, String name) {
        Category category = find(storeId, id);
        String oldName = category.getName();
        String newName = Category.validName(name);
        // 지금 이름은 형제 중 같은 이름에 걸리므로 중복 검사 전에 거른다. 아무 일 없고 이벤트도 없다 (04 설계 2.2)
        if (newName.equals(oldName)) {
            return toInfo(category, parentOf(category));
        }
        rejectDuplicateName(storeId, category.getParentId(), newName);
        category.rename(newName);
        events.publishEvent(new CategoryRenamed(storeId, id, oldName, newName));
        return toInfo(category, parentOf(category));
    }

    // 상위를 끄면 하위도 같은 트랜잭션에서 끈다 (04 설계 D2-7). 돌려주는 것은 이번에 꺼진 ID 전부
    @Transactional
    public List<Long> deactivate(Long storeId, Long id) {
        Category category = find(storeId, id);
        rejectIfLastActiveTop(storeId, category);
        if (!category.isActive()) {
            return List.of();
        }
        category.deactivate();
        List<Long> deactivated = new ArrayList<>(List.of(id));
        if (category.isTop()) {
            for (Category child : categories.findByStoreIdAndParentId(storeId, id)) {
                if (child.isActive()) {
                    child.deactivate();
                    deactivated.add(child.getId());
                }
            }
        }
        events.publishEvent(new CategoryDeactivated(storeId, deactivated));
        return deactivated;
    }

    // 상위를 켜도 하위는 꺼진 채로 둔다. 하위는 하나씩 켠다. 이벤트 없음 (04 설계 2.2, D2-6)
    @Transactional
    public CategoryInfo activate(Long storeId, Long id) {
        Category category = find(storeId, id);
        category.activate();
        return toInfo(category, parentOf(category));
    }

    // 상위를 지우면 하위도 같은 트랜잭션에서 지운다. JPA cascade 없이 리포지토리로 하나씩. 이벤트 없음 (04 설계 D2-6)
    @Transactional
    public void delete(Long storeId, Long id) {
        Category category = find(storeId, id);
        List<Category> children = category.isTop() ? categories.findByStoreIdAndParentId(storeId, id) : List.of();
        // 자신과 하위의 누적 비용을 합쳐 1건이라도 있으면 아무것도 지우지 않는다. 끄기만 할 수 있다 (04 설계 2.3)
        List<Long> ids = new ArrayList<>(List.of(id));
        children.forEach(child -> ids.add(child.getId()));
        long expenses = usage.countByCategoryIds(storeId, ids).values().stream().mapToLong(Long::longValue).sum();
        if (expenses > 0) {
            throw new MoaException(ErrorCode.CATEGORY_IN_USE, "category has expenses: " + id);
        }
        rejectIfLastActiveTop(storeId, category);
        children.forEach(categories::delete);
        categories.delete(category);
    }

    // 상위 → 그 하위 순서로 펼친다. 누적 비용 건수는 한 번에 받는다 (04 설계 2.2)
    @Transactional(readOnly = true)
    public List<CategoryInfo> tree(Long storeId, boolean includeInactive) {
        List<Category> all = categories.findByStoreId(storeId);
        Map<Long, Long> counts = usage.countByCategoryIds(storeId, all.stream().map(Category::getId).toList());
        List<CategoryInfo> result = new ArrayList<>();
        all.stream().filter(Category::isTop).sorted(SIBLING_ORDER).forEach(top -> {
            result.add(toInfo(top, null, counts));
            all.stream().filter(c -> c.isChildOf(top)).sorted(SIBLING_ORDER)
                    .forEach(child -> result.add(toInfo(child, top, counts)));
        });
        return includeInactive ? result : result.stream().filter(CategoryInfo::active).toList();
    }

    // expense가 비용 등록·재분류 때 부른다. 없는 분류도 false (04 설계 2.2 "분류 존재 확인")
    @Transactional(readOnly = true)
    public boolean isActive(Long storeId, Long categoryId) {
        return categories.findByIdAndStoreId(categoryId, storeId).map(Category::isActive).orElse(false);
    }

    // expense(비용 목록의 분류 경로)와 report(분류별 집계 이름)가 쓴다. 건수는 채우지 않는다 (04 설계 2.7)
    @Transactional(readOnly = true)
    public Map<Long, CategoryInfo> infoOf(Long storeId, Collection<Long> categoryIds) {
        Map<Long, Category> byId = new HashMap<>();
        categories.findByStoreId(storeId).forEach(c -> byId.put(c.getId(), c));
        Map<Long, CategoryInfo> result = new HashMap<>();
        for (Long id : categoryIds) {
            Category category = byId.get(id);
            if (category != null) {
                result.put(id, toInfo(category, category.isTop() ? null : byId.get(category.getParentId())));
            }
        }
        return result;
    }

    // 형제 = 같은 상위 아래. 상위끼리는 전체가 형제다. 꺼진 분류도 센다 (04 설계 2.3)
    private void rejectDuplicateName(Long storeId, Long parentId, String name) {
        if (categories.existsByStoreIdAndParentIdAndName(storeId, parentId, name)) {
            throw new MoaException(ErrorCode.CATEGORY_NAME_DUPLICATE, "same name exists among siblings");
        }
    }

    // 켜진 상위 분류는 항상 1개 이상 (04 설계 2.3). 끄거나 지우려는 것이 그 하나면 막는다
    private void rejectIfLastActiveTop(Long storeId, Category category) {
        if (category.isTop() && category.isActive()
                && categories.countByStoreIdAndParentIdIsNullAndActiveTrue(storeId) == 1) {
            throw new MoaException(ErrorCode.CATEGORY_LAST_ACTIVE, "at least one active top category is required");
        }
    }

    private Category find(Long storeId, Long id) {
        return categories.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new MoaException(ErrorCode.NOT_FOUND, "category not found: " + id));
    }

    private Category parentOf(Category category) {
        return category.isTop() ? null : find(category.getStoreId(), category.getParentId());
    }

    private static CategoryInfo toInfo(Category category, Category parent) {
        return toInfo(category, parent, Map.of());
    }

    private static CategoryInfo toInfo(Category category, Category parent, Map<Long, Long> expenseCounts) {
        String path = parent == null ? category.getName() : parent.getName() + " > " + category.getName();
        return new CategoryInfo(category.getId(), category.getParentId(), category.getName(), path,
                category.isActive(), category.getSortOrder(), expenseCounts.getOrDefault(category.getId(), 0L));
    }
}
