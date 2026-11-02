package moa.category;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import moa.category.CategoryEvents.CategoryAdded;
import moa.category.internal.Category;
import moa.category.internal.CategoryRepository;
import moa.shared.ErrorCode;
import moa.shared.MoaException;

@Service
public class CategoryService {

    // 상위 20개, 상위당 하위 20개. 꺼진 것도 센다 (04 설계 2.3)
    static final int MAX_SIBLINGS = 20;

    // 형제 안은 만든 순서 (04 설계 2.2 "분류 tree 조회")
    private static final Comparator<Category> SIBLING_ORDER = Comparator.comparingInt(Category::getSortOrder)
            .thenComparing(Category::getId);

    private final CategoryRepository categories;
    private final ApplicationEventPublisher events;

    public CategoryService(CategoryRepository categories, ApplicationEventPublisher events) {
        this.categories = categories;
        this.events = events;
    }

    @Transactional
    public CategoryInfo add(Long storeId, Long parentId, String name) {
        Category parent = parentId == null ? null : find(storeId, parentId);
        if (parent != null && !parent.isTop()) {
            throw new MoaException(ErrorCode.CATEGORY_DEPTH, "categories have at most 2 levels");
        }
        int siblings = (int) categories.countByStoreIdAndParentId(storeId, parentId);
        if (siblings >= MAX_SIBLINGS) {
            throw new MoaException(ErrorCode.CATEGORY_LIMIT, "at most " + MAX_SIBLINGS + " categories per level");
        }
        int sortOrder = siblings;
        Category category = parent == null
                ? Category.top(storeId, name, sortOrder)
                : Category.childOf(parent, name, sortOrder);
        rejectDuplicateName(storeId, parentId, category.getName());
        categories.save(category);
        events.publishEvent(new CategoryAdded(storeId, category.getId(), category.getParentId(), category.getName()));
        return toInfo(category, parent);
    }

    // 상위 → 그 하위 순서로 펼친다
    @Transactional(readOnly = true)
    public List<CategoryInfo> tree(Long storeId, boolean includeInactive) {
        List<Category> all = categories.findByStoreId(storeId);
        List<CategoryInfo> result = new ArrayList<>();
        all.stream().filter(Category::isTop).sorted(SIBLING_ORDER).forEach(top -> {
            result.add(toInfo(top, null));
            all.stream().filter(c -> c.isChildOf(top)).sorted(SIBLING_ORDER)
                    .forEach(child -> result.add(toInfo(child, top)));
        });
        return result;
    }

    // 형제 = 같은 상위 아래. 상위끼리는 전체가 형제다. 꺼진 분류도 센다 (04 설계 2.3)
    private void rejectDuplicateName(Long storeId, Long parentId, String name) {
        if (categories.existsByStoreIdAndParentIdAndName(storeId, parentId, name)) {
            throw new MoaException(ErrorCode.CATEGORY_NAME_DUPLICATE, "same name exists among siblings");
        }
    }

    private Category find(Long storeId, Long id) {
        return categories.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new MoaException(ErrorCode.NOT_FOUND, "category not found: " + id));
    }

    private static CategoryInfo toInfo(Category category, Category parent) {
        String path = parent == null ? category.getName() : parent.getName() + " > " + category.getName();
        return new CategoryInfo(category.getId(), category.getParentId(), category.getName(), path,
                category.isActive(), category.getSortOrder(), 0);
    }
}
