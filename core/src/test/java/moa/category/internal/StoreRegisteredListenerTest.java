package moa.category.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.support.TransactionTemplate;

import moa.TestcontainersConfiguration;
import moa.category.CategoryEvents.DefaultCategoriesCreated;
import moa.category.CategoryInfo;
import moa.category.CategoryService;
import moa.store.StoreEvents.StoreRegistered;
import moa.store.StoreType;

/**
 * 매장 등록 이벤트를 받아 기본 비용 분류를 만드는 리스너 (04 설계 2.2 "기본 분류 생성").
 * store 모듈은 띄우지 않고, store가 발행했을 이벤트를 테스트가 직접 발행한다.
 */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class StoreRegisteredListenerTest {

    static final Long STORE_ID = 1L;

    // 04 설계 2.2의 카페 템플릿. 상위 7 + 하위 9
    static final List<String> TEMPLATE_PATHS = List.of(
            "식자재", "식자재 > 원두", "식자재 > 유제품", "식자재 > 베이커리", "식자재 > 기타 식자재",
            "인건비", "인건비 > 급여", "인건비 > 주휴·수당",
            "임대료",
            "공과금", "공과금 > 전기", "공과금 > 수도·가스", "공과금 > 통신",
            "마케팅",
            "소모품",
            "기타");

    @Autowired
    CategoryService categoryService;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM category.categories");
        jdbc.update("DELETE FROM category.template_applications");
    }

    @Test
    @DisplayName("기본 분류 생성 - StoreRegistered 수신 → 상위 7, 하위 9 생성, DefaultCategoriesCreated 발행")
    void createsTemplate(AssertablePublishedEvents events) {
        storeRegistered(StoreType.CAFE);

        assertThat(paths()).containsExactlyElementsOf(TEMPLATE_PATHS);
        assertThat(events)
                .contains(DefaultCategoriesCreated.class)
                .matching(DefaultCategoriesCreated::count, 16);
    }

    @Test
    @DisplayName("기본 분류 생성 - 다른 업종 → 같은 템플릿")
    void sameTemplateForOtherTypes() {
        storeRegistered(StoreType.RETAIL);

        assertThat(paths()).containsExactlyElementsOf(TEMPLATE_PATHS);
    }

    // 첫 번째 뒤에 사장님이 "마케팅"을 지웠다. 두 번째 수신이 그걸 되살리면 안 된다
    @Test
    @DisplayName("기본 분류 생성 - 두 번 수신 → 두 번째는 무시 (적용 기록 있음)")
    void secondReceiptIgnored(AssertablePublishedEvents events) {
        storeRegistered(StoreType.CAFE);
        categoryService.delete(STORE_ID, idOf("마케팅"));

        storeRegistered(StoreType.CAFE);

        assertThat(paths()).doesNotContain("마케팅").hasSize(15);
        assertThat(events.ofType(DefaultCategoriesCreated.class)).hasSize(1);
    }

    // store가 커밋한 뒤에 리스너가 돈다(04 설계 0.3). 테스트도 트랜잭션 안에서 발행하고 커밋한다
    private void storeRegistered(StoreType type) {
        tx.executeWithoutResult(status -> publisher.publishEvent(
                new StoreRegistered(STORE_ID, type, LocalDate.of(2026, 10, 28))));
    }

    private List<String> paths() {
        return categoryService.tree(STORE_ID, true).stream().map(CategoryInfo::path).toList();
    }

    private Long idOf(String path) {
        return categoryService.tree(STORE_ID, true).stream()
                .filter(info -> info.path().equals(path))
                .findFirst().orElseThrow().id();
    }
}
