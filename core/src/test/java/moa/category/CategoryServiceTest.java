package moa.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;

import moa.TestcontainersConfiguration;
import moa.category.CategoryEvents.CategoryAdded;
import moa.category.CategoryEvents.CategoryDeactivated;
import moa.category.CategoryEvents.CategoryRenamed;
import moa.shared.ErrorCode;
import moa.shared.MoaException;

/**
 * category 모듈만 띄우고 진짜 MySQL(Testcontainers)에 저장해 보는 통합 테스트 (04 설계 0.7).
 */
@ApplicationModuleTest
@Import({ TestcontainersConfiguration.class, CategoryServiceTest.Fakes.class })
class CategoryServiceTest {

    static final Long STORE_ID = 1L;

    @Autowired
    CategoryService categoryService;

    @Autowired
    FakeCategoryUsage usage;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM category.categories");
        usage.clear();
    }

    @Test
    @DisplayName("분류 추가 - 상위 추가 → tree 끝에, CategoryAdded 발행")
    void addTop(AssertablePublishedEvents events) {
        categoryService.add(STORE_ID, null, "식자재");

        CategoryInfo added = categoryService.add(STORE_ID, null, "임대료");

        assertThat(added.parentId()).isNull();
        assertThat(added.path()).isEqualTo("임대료");
        assertThat(categoryService.tree(STORE_ID, false))
                .extracting(CategoryInfo::name)
                .containsExactly("식자재", "임대료");
        assertThat(events)
                .contains(CategoryAdded.class)
                .matching(CategoryAdded::categoryId, added.id());
    }

    @Test
    @DisplayName("분류 추가 - 하위 추가 → 그 상위 아래 끝에, 경로 \"식자재 > 시럽\"")
    void addChild() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        categoryService.add(STORE_ID, null, "임대료");
        categoryService.add(STORE_ID, food.id(), "원두");

        CategoryInfo syrup = categoryService.add(STORE_ID, food.id(), "시럽");

        assertThat(syrup.parentId()).isEqualTo(food.id());
        assertThat(syrup.path()).isEqualTo("식자재 > 시럽");
        assertThat(categoryService.tree(STORE_ID, false))
                .extracting(CategoryInfo::path)
                .containsExactly("식자재", "식자재 > 원두", "식자재 > 시럽", "임대료");
    }

    // 형제 수를 새 순서로 쓰면, 앞쪽 형제를 지운 뒤 순서 번호가 겹쳐 중간에 끼어든다
    @Test
    @DisplayName("분류 추가 - 앞쪽 형제를 지운 뒤 추가해도 끝에")
    void addAfterDeletingEarlierSiblings() {
        CategoryInfo a = categoryService.add(STORE_ID, null, "가");
        CategoryInfo b = categoryService.add(STORE_ID, null, "나");
        categoryService.add(STORE_ID, null, "다");
        categoryService.add(STORE_ID, null, "라");
        categoryService.delete(STORE_ID, a.id());
        categoryService.delete(STORE_ID, b.id());

        categoryService.add(STORE_ID, null, "마");

        assertThat(categoryService.tree(STORE_ID, false))
                .extracting(CategoryInfo::name)
                .containsExactly("다", "라", "마");
    }

    @Test
    @DisplayName("분류 추가 - 없는 상위 → NOT_FOUND")
    void addUnderMissingParent() {
        assertFails(ErrorCode.NOT_FOUND, () -> categoryService.add(STORE_ID, 999L, "원두"));
    }

    @Test
    @DisplayName("분류 추가 - 하위 아래 추가 → CATEGORY_DEPTH")
    void addUnderChild() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");

        assertFails(ErrorCode.CATEGORY_DEPTH, () -> categoryService.add(STORE_ID, beans.id(), "에티오피아"));
    }

    // 상위끼리는 DB가 못 막고(04 설계 D2-4) 하위끼리는 DB도 막지만, 둘 다 코드가 먼저 409로 막는다
    @Test
    @DisplayName("분류 추가 - 형제 중복 → CATEGORY_NAME_DUPLICATE")
    void addDuplicateSibling() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        categoryService.add(STORE_ID, food.id(), "원두");

        assertFails(ErrorCode.CATEGORY_NAME_DUPLICATE, () -> categoryService.add(STORE_ID, null, " 식자재 "));
        assertFails(ErrorCode.CATEGORY_NAME_DUPLICATE, () -> categoryService.add(STORE_ID, food.id(), "원두"));
    }

    @Test
    @DisplayName("분류 추가 - 다른 상위와는 중복 허용")
    void sameNameUnderDifferentParents() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo labor = categoryService.add(STORE_ID, null, "인건비");
        categoryService.add(STORE_ID, food.id(), "기타");

        categoryService.add(STORE_ID, labor.id(), "기타");
        categoryService.add(STORE_ID, null, "기타");

        assertThat(categoryService.tree(STORE_ID, false))
                .extracting(CategoryInfo::path)
                .contains("식자재 > 기타", "인건비 > 기타", "기타");
    }

    @Test
    @DisplayName("분류 추가 - 꺼진 분류와 중복 → CATEGORY_NAME_DUPLICATE")
    void addDuplicateOfInactive() {
        categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo rent = categoryService.add(STORE_ID, null, "임대료");
        categoryService.deactivate(STORE_ID, rent.id());

        assertFails(ErrorCode.CATEGORY_NAME_DUPLICATE, () -> categoryService.add(STORE_ID, null, "임대료"));
    }

    @Test
    @DisplayName("분류 추가 - 상위 20개 → CATEGORY_LIMIT")
    void topLimit() {
        for (int i = 1; i <= 20; i++) {
            categoryService.add(STORE_ID, null, "상위" + i);
        }

        assertFails(ErrorCode.CATEGORY_LIMIT, () -> categoryService.add(STORE_ID, null, "상위21"));
    }

    @Test
    @DisplayName("분류 추가 - 하위 20개 → CATEGORY_LIMIT")
    void childLimit() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        for (int i = 1; i <= 20; i++) {
            categoryService.add(STORE_ID, food.id(), "하위" + i);
        }

        assertFails(ErrorCode.CATEGORY_LIMIT, () -> categoryService.add(STORE_ID, food.id(), "하위21"));
    }

    @Test
    @DisplayName("분류 이름 변경 - 정상, CategoryRenamed 발행")
    void rename(AssertablePublishedEvents events) {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");

        CategoryInfo renamed = categoryService.rename(STORE_ID, beans.id(), " 커피콩 ");

        assertThat(renamed.path()).isEqualTo("식자재 > 커피콩");
        assertThat(categoryService.tree(STORE_ID, false))
                .extracting(CategoryInfo::path)
                .containsExactly("식자재", "식자재 > 커피콩");
        assertThat(events)
                .contains(CategoryRenamed.class)
                .matching(e -> e.oldName().equals("원두") && e.newName().equals("커피콩"));
    }

    @Test
    @DisplayName("분류 이름 변경 - 중복 → CATEGORY_NAME_DUPLICATE")
    void renameToSiblingName() {
        categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo rent = categoryService.add(STORE_ID, null, "임대료");

        assertFails(ErrorCode.CATEGORY_NAME_DUPLICATE, () -> categoryService.rename(STORE_ID, rent.id(), "식자재"));
    }

    // 지금 이름도 "형제 중 같은 이름"에 걸리므로, 중복 검사보다 먼저 걸러야 한다
    @Test
    @DisplayName("분류 이름 변경 - 같은 이름 → 200, 이벤트 없음")
    void renameToSameName(AssertablePublishedEvents events) {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");

        CategoryInfo same = categoryService.rename(STORE_ID, food.id(), " 식자재 ");

        assertThat(same.name()).isEqualTo("식자재");
        assertThat(events.ofType(CategoryRenamed.class)).isEmpty();
    }

    @Test
    @DisplayName("분류 비활성화 - 하위 없는 상위 → 꺼짐, CategoryDeactivated 발행")
    void deactivateTop(AssertablePublishedEvents events) {
        categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo rent = categoryService.add(STORE_ID, null, "임대료");

        List<Long> deactivated = categoryService.deactivate(STORE_ID, rent.id());

        assertThat(deactivated).containsExactly(rent.id());
        assertThat(activeByPath()).containsExactly(entry("식자재", true), entry("임대료", false));
        assertThat(events)
                .contains(CategoryDeactivated.class)
                .matching(CategoryDeactivated::categoryIds, List.of(rent.id()));
    }

    @Test
    @DisplayName("분류 비활성화 - 하위 있는 상위 → 하위도 함께 꺼짐")
    void deactivateTopWithChildren() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");
        CategoryInfo milk = categoryService.add(STORE_ID, food.id(), "유제품");
        categoryService.add(STORE_ID, null, "임대료");

        List<Long> deactivated = categoryService.deactivate(STORE_ID, food.id());

        assertThat(deactivated).containsExactlyInAnyOrder(food.id(), beans.id(), milk.id());
        assertThat(activeByPath()).containsExactly(
                entry("식자재", false), entry("식자재 > 원두", false), entry("식자재 > 유제품", false),
                entry("임대료", true));
    }

    @Test
    @DisplayName("분류 비활성화 - 하위만 → 상위·형제는 그대로")
    void deactivateChildOnly() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");
        categoryService.add(STORE_ID, food.id(), "유제품");

        List<Long> deactivated = categoryService.deactivate(STORE_ID, beans.id());

        assertThat(deactivated).containsExactly(beans.id());
        assertThat(activeByPath()).containsExactly(
                entry("식자재", true), entry("식자재 > 원두", false), entry("식자재 > 유제품", true));
    }

    @Test
    @DisplayName("분류 비활성화 - 마지막 상위 → CATEGORY_LAST_ACTIVE")
    void deactivateLastActiveTop() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo rent = categoryService.add(STORE_ID, null, "임대료");
        categoryService.deactivate(STORE_ID, rent.id());

        assertFails(ErrorCode.CATEGORY_LAST_ACTIVE, () -> categoryService.deactivate(STORE_ID, food.id()));
    }

    // 켜진 상위가 하나뿐이어도, 끄려는 것이 이미 꺼진 다른 상위면 막지 않는다
    @Test
    @DisplayName("분류 비활성화 - 이미 꺼짐 → 아무 일 없음, 이벤트 없음")
    void deactivateAlreadyInactive(AssertablePublishedEvents events) {
        categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo rent = categoryService.add(STORE_ID, null, "임대료");
        categoryService.deactivate(STORE_ID, rent.id());

        List<Long> deactivated = categoryService.deactivate(STORE_ID, rent.id());

        assertThat(deactivated).isEmpty();
        assertThat(events.ofType(CategoryDeactivated.class)).hasSize(1);
    }

    @Test
    @DisplayName("분류 활성화 - 상위 켜기 → 하위는 여전히 꺼짐")
    void activateTopLeavesChildrenOff() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        categoryService.add(STORE_ID, food.id(), "원두");
        categoryService.add(STORE_ID, null, "임대료");
        categoryService.deactivate(STORE_ID, food.id());

        CategoryInfo activated = categoryService.activate(STORE_ID, food.id());

        assertThat(activated.active()).isTrue();
        assertThat(activeByPath()).containsExactly(
                entry("식자재", true), entry("식자재 > 원두", false), entry("임대료", true));
    }

    @Test
    @DisplayName("분류 활성화 - 상위 켜진 하위 → 켜짐")
    void activateChildUnderActiveTop() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");
        categoryService.deactivate(STORE_ID, beans.id());

        CategoryInfo activated = categoryService.activate(STORE_ID, beans.id());

        assertThat(activated.path()).isEqualTo("식자재 > 원두");
        assertThat(activeByPath()).containsEntry("식자재 > 원두", true);
    }

    @Test
    @DisplayName("분류 삭제 - 0건 하위 → 삭제")
    void deleteChild() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");

        categoryService.delete(STORE_ID, beans.id());

        assertThat(activeByPath()).containsOnlyKeys("식자재");
    }

    @Test
    @DisplayName("분류 삭제 - 0건 상위, 하위 없음 → 삭제")
    void deleteTop() {
        categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo rent = categoryService.add(STORE_ID, null, "임대료");

        categoryService.delete(STORE_ID, rent.id());

        assertThat(activeByPath()).containsOnlyKeys("식자재");
    }

    // 상위를 지우면 하위도 같은 트랜잭션에서 지운다 (기획서 P2-7, 2026-11-03 변경)
    @Test
    @DisplayName("분류 삭제 - 0건 상위와 하위 → 함께 삭제")
    void deleteTopWithChildren() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        categoryService.add(STORE_ID, food.id(), "원두");
        categoryService.add(STORE_ID, food.id(), "유제품");
        categoryService.add(STORE_ID, null, "임대료");

        categoryService.delete(STORE_ID, food.id());

        assertThat(activeByPath()).containsOnlyKeys("임대료");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category.categories", Integer.class)).isEqualTo(1);
    }

    // 상위와 하위의 건수를 합쳐 1건이라도 있으면 아무것도 지우지 않는다
    @Test
    @DisplayName("분류 삭제 - 하위 중 하나라도 사용 → CATEGORY_IN_USE, 상위·하위 모두 남음")
    void deleteTopWithUsedChild() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        categoryService.add(STORE_ID, food.id(), "원두");
        CategoryInfo milk = categoryService.add(STORE_ID, food.id(), "유제품");
        categoryService.add(STORE_ID, null, "임대료");
        usage.set(milk.id(), 1);

        assertFails(ErrorCode.CATEGORY_IN_USE, () -> categoryService.delete(STORE_ID, food.id()));
        assertThat(activeByPath()).containsOnlyKeys("식자재", "식자재 > 원두", "식자재 > 유제품", "임대료");
    }

    @Test
    @DisplayName("분류 삭제 - 마지막 상위 → CATEGORY_LAST_ACTIVE")
    void deleteLastActiveTop() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");

        assertFails(ErrorCode.CATEGORY_LAST_ACTIVE, () -> categoryService.delete(STORE_ID, food.id()));
    }

    @Test
    @DisplayName("분류 삭제 - 사용된 분류 → CATEGORY_IN_USE")
    void deleteInUse() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");
        usage.set(beans.id(), 3);

        assertFails(ErrorCode.CATEGORY_IN_USE, () -> categoryService.delete(STORE_ID, beans.id()));
    }

    @Test
    @DisplayName("분류 tree 조회 - 상위 → 하위 순서, 경로·활성 여부·건수")
    void treeWithCounts() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");
        CategoryInfo milk = categoryService.add(STORE_ID, food.id(), "유제품");
        CategoryInfo rent = categoryService.add(STORE_ID, null, "임대료");
        categoryService.deactivate(STORE_ID, milk.id());
        usage.set(beans.id(), 5);
        usage.set(rent.id(), 2);

        assertThat(categoryService.tree(STORE_ID, true))
                .extracting(CategoryInfo::path, CategoryInfo::active, CategoryInfo::expenseCount)
                .containsExactly(
                        tuple("식자재", true, 0L),
                        tuple("식자재 > 원두", true, 5L),
                        tuple("식자재 > 유제품", false, 0L),
                        tuple("임대료", true, 2L));
    }

    @Test
    @DisplayName("분류 tree 조회 - 활성만 → 꺼진 것 제외")
    void treeActiveOnly() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        categoryService.add(STORE_ID, food.id(), "원두");
        CategoryInfo milk = categoryService.add(STORE_ID, food.id(), "유제품");
        CategoryInfo rent = categoryService.add(STORE_ID, null, "임대료");
        categoryService.add(STORE_ID, rent.id(), "월세");
        categoryService.deactivate(STORE_ID, milk.id());
        categoryService.deactivate(STORE_ID, rent.id());

        assertThat(categoryService.tree(STORE_ID, false))
                .extracting(CategoryInfo::path)
                .containsExactly("식자재", "식자재 > 원두");
    }

    @Test
    @DisplayName("분류 존재 확인 - 켜진 분류 → true, 꺼진·없는 분류 → false")
    void isActive() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");
        CategoryInfo milk = categoryService.add(STORE_ID, food.id(), "유제품");
        categoryService.deactivate(STORE_ID, milk.id());

        assertThat(categoryService.isActive(STORE_ID, beans.id())).isTrue();
        assertThat(categoryService.isActive(STORE_ID, milk.id())).isFalse();
        assertThat(categoryService.isActive(STORE_ID, 999L)).isFalse();
        assertThat(categoryService.isActive(2L, beans.id())).as("다른 매장").isFalse();
    }

    // expense(비용 목록의 분류 경로)와 report(분류별 집계 이름)가 쓴다 (04 설계 2.7)
    @Test
    @DisplayName("분류 정보 - 요청한 ID의 경로, 건수는 0, 없는 ID는 빠짐")
    void infoOf() {
        CategoryInfo food = categoryService.add(STORE_ID, null, "식자재");
        CategoryInfo beans = categoryService.add(STORE_ID, food.id(), "원두");
        CategoryInfo rent = categoryService.add(STORE_ID, null, "임대료");
        usage.set(beans.id(), 5);

        Map<Long, CategoryInfo> infos = categoryService.infoOf(STORE_ID, List.of(beans.id(), rent.id(), 999L));

        assertThat(infos).containsOnlyKeys(beans.id(), rent.id());
        assertThat(infos.get(beans.id()).path()).isEqualTo("식자재 > 원두");
        assertThat(infos.get(beans.id()).expenseCount()).isZero();
    }

    // 꺼진 것까지 tree 순서대로 "경로 → 켜짐 여부"
    private Map<String, Boolean> activeByPath() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        categoryService.tree(STORE_ID, true).forEach(info -> result.put(info.path(), info.active()));
        return result;
    }

    private static void assertFails(ErrorCode code, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(MoaException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }

    // 비용 건수는 가짜가 대신 답한다. main의 NoExpenseYet보다 먼저 쓰이게 @Primary
    @TestConfiguration(proxyBeanMethods = false)
    static class Fakes {

        @Bean
        @Primary
        FakeCategoryUsage fakeCategoryUsage() {
            return new FakeCategoryUsage();
        }
    }
}
