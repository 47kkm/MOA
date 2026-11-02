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
        int created = 0;
        int topOrder = 0;
        for (CategoryTemplate.Node node : CategoryTemplate.NODES) {
            Category top = categories.save(Category.top(storeId, node.name(), topOrder++));
            created++;
            int childOrder = 0;
            for (String childName : node.children()) {
                categories.save(Category.childOf(top, childName, childOrder++));
                created++;
            }
        }
        applications.save(new TemplateApplication(storeId, LocalDateTime.now(clock)));
        events.publishEvent(new DefaultCategoriesCreated(storeId, created));
    }
}
