package moa.category.internal;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import moa.category.CategoryEvents.DefaultCategoriesCreated;
import moa.store.StoreEvents.StoreRegistered;

// 매장 등록이 커밋된 뒤 같은 스레드·새 트랜잭션에서 기본 비용 분류를 만든다 (04 설계 0.3, 2.2)
// 여기서 실패해도 매장 등록은 그대로이고, 발행 기록이 미완료로 남아 다시 뜰 때 재실행된다
@Component
class StoreRegisteredListener {

    private final CategoryRepository categories;
    private final TemplateApplicationRepository applications;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    StoreRegisteredListener(CategoryRepository categories, TemplateApplicationRepository applications, Clock clock,
            ApplicationEventPublisher events) {
        this.categories = categories;
        this.applications = applications;
        this.clock = clock;
        this.events = events;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(StoreRegistered event) {
        Long storeId = event.storeId();
        // 재발행이나 중복 수신: 이미 적용했으면 아무것도 안 한다
        if (applications.existsById(storeId)) {
            return;
        }
        // 사장님이 먼저 만든 분류와 이름이 겹칠 수 있다. 2.3 규칙을 우회하지 않게 형제 이름을 먼저 본다 (04 설계 D2-10)
        int created = 0;
        for (CategoryTemplate.Node node : CategoryTemplate.NODES) {
            Category top = categories.findByStoreIdAndParentIdAndName(storeId, null, node.name()).orElse(null);
            if (top == null) {
                // 상위가 20개면 이 상위와 그 하위는 건너뛴다
                if (isFull(storeId, null)) {
                    continue;
                }
                top = categories.save(Category.top(storeId, node.name(), nextSortOrder(storeId, null)));
                created++;
            }
            for (String childName : node.children()) {
                if (categories.existsByStoreIdAndParentIdAndName(storeId, top.getId(), childName)
                        || isFull(storeId, top.getId())) {
                    continue;
                }
                Category child = Category.childOf(top, childName, nextSortOrder(storeId, top.getId()));
                // 꺼진 상위에 합치면 넣는 하위도 꺼진 상태로
                if (!top.isActive()) {
                    child.deactivate();
                }
                categories.save(child);
                created++;
            }
        }
        applications.save(new TemplateApplication(storeId, LocalDateTime.now(clock)));
        events.publishEvent(new DefaultCategoriesCreated(storeId, created));
    }

    private boolean isFull(Long storeId, Long parentId) {
        return categories.countByStoreIdAndParentId(storeId, parentId) >= Category.MAX_SIBLINGS;
    }

    private int nextSortOrder(Long storeId, Long parentId) {
        return categories.findTopByStoreIdAndParentIdOrderBySortOrderDesc(storeId, parentId)
                .map(last -> last.getSortOrder() + 1)
                .orElse(0);
    }
}
