package moa.category;

import java.util.Comparator;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import moa.category.CategoryEvents.CategoryAdded;
import moa.category.internal.Category;
import moa.category.internal.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final ApplicationEventPublisher events;

    public CategoryService(CategoryRepository categories, ApplicationEventPublisher events) {
        this.categories = categories;
        this.events = events;
    }

    @Transactional
    public CategoryInfo add(Long storeId, Long parentId, String name) {
        int sortOrder = (int) categories.countByStoreIdAndParentId(storeId, null);
        Category category = categories.save(Category.top(storeId, name, sortOrder));
        events.publishEvent(new CategoryAdded(storeId, category.getId(), category.getParentId(), category.getName()));
        return toInfo(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryInfo> tree(Long storeId, boolean includeInactive) {
        return categories.findByStoreId(storeId).stream()
                .sorted(Comparator.comparingInt(Category::getSortOrder))
                .map(CategoryService::toInfo)
                .toList();
    }

    private static CategoryInfo toInfo(Category category) {
        return new CategoryInfo(category.getId(), category.getParentId(), category.getName(), category.getName(),
                category.isActive(), category.getSortOrder(), 0);
    }
}
