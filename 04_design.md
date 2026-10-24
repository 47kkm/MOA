# MOA 설계 문서

- 시작일: 2026-10-24
- 역할: **개발자 문서.** 안에서 어떻게 만드는지를 정한다. 모듈별 애그리거트·값 객체·도메인 이벤트·공개 API·DB 스키마·패키지 구조. 화면·문구·사용자 흐름은 쓰지 않는다 (그건 기획서의 몫).
- 구성: 0장은 모든 모듈에 공통인 설계, 1장부터는 **모듈 하나가 장 하나**다. 모듈을 구현할 때마다 그 장을 채우고, 구현하면서 바뀐 것은 그 장의 결정 사항에 날짜와 함께 덧붙인다.
- 각 장의 구성: 개요 → 애그리거트 → 애그리거트 밖 규칙 → 공개 API → 이벤트 → HTTP → 테이블 → 파일 → 테스트 목록 → 결정 사항.
- 기능 이름은 요구사항정의서·기획서의 기능 이름을 그대로 쓴다. 테스트 이름(`@DisplayName`)도 "기능 이름 - 상황"이다. 번호는 두지 않는다.

**배경 (이 문서를 읽는 데 필요한 만큼만)**

- MOA는 사장님이 하루 마감 때 매출과 비용을 입력하면 이번 달 현황과 예산 잔여를 보여 주는 매장 가계부다. Phase 1(~2026-11-30)은 입력과 집계, 사용자 1명·매장 1개, 로그인 없음.
- 백엔드는 Spring Boot 서비스 하나(MOA Core) 안에 모듈 6개(store, category, sales, expense, budget, report)를 두는 모듈러 모놀리스다. MySQL 서버 하나에 모듈별 데이터베이스(스키마)를 나눈다. 모듈끼리는 ID 참조·공개 API 조회·도메인 이벤트로만 연결하고, 다른 모듈의 테이블을 FK·JOIN으로 건드리지 않는다. **API는 조회만, 변경은 이벤트로.**
- 모듈 의존 방향: category와 sales는 store의 이벤트를 받는다. expense는 category를 조회한다. budget은 expense의 이벤트를 받는다. report는 모두를 조회한다 (자기 데이터 없음).

| 장 | 모듈 | 상태 |
|---|---|---|
| 0 | 공통 | 초안 |
| 1 | store | 초안 |
| 2 | category | 초안 |
| 3 | sales | 예정 (3주차) |
| 4 | expense | 예정 (3주차) |
| 5 | budget | 예정 (4주차) |
| 6 | report | 예정 (4주차) |

**책의 개념이 이 문서 어디에 있는가** (최범균 『도메인 주도 개발 시작하기』 1~3장 기준)

| 책의 개념 | 뜻 (한 줄) | 이 문서에서 |
|---|---|---|
| 엔티티 | ID로 구분되는 객체. 값이 바뀌어도 같은 것 | Store, Category 등 애그리거트 표의 클래스 |
| 값 객체 | ID 없이 값으로만 비교. 바꾸지 않고 새로 만듦 | Money (0.8). Phase 1은 이것 하나 |
| 애그리거트 | 함께 바뀌고 함께 저장되는 객체 묶음. 트랜잭션 하나의 단위 | 각 장의 "애그리거트" 절. 묶음의 범위를 어떻게 잘랐는지와 이유 |
| 애그리거트 루트 | 묶음의 대표. 바깥은 루트의 메서드로만 상태를 바꿈 | 애그리거트 표의 "메서드" 열. setter 없음 |
| 리포지토리 | 애그리거트를 통째로 저장·복원하는 창구 | 각 장의 "파일" 절의 `XxxRepository`. Spring Data JPA가 구현을 만들어 줌 |
| 도메인 이벤트 | "무슨 일이 있었다"를 다른 모듈에 알리는 메시지 | 각 장의 "이벤트" 절과 0.3 |
| 응용 서비스 | 요청 하나를 받아 리포지토리에서 애그리거트를 꺼내고 메서드를 부르고 저장하는 층. 규칙은 두지 않음 | 각 장의 "공개 API"가 곧 응용 서비스 (`XxxService`) |
| 애그리거트 밖 규칙 | 여러 애그리거트를 봐야 지킬 수 있는 규칙 (예: 형제 이름 중복). 책 3장 끝·7장 도메인 서비스 | 각 장의 "애그리거트 밖 규칙" 절 |
| ID 참조 | 다른 애그리거트를 객체가 아니라 ID로 가리킴 | `Long storeId`, `Long parentId`, `Long categoryId` |

---

## 0. 공통 설계

### 0.1 프로젝트가 어떻게 생겼고 어떻게 뜨는가

이미 돌아가는 프로젝트의 컨트롤러·서비스·매퍼를 읽어 봤다면, 비어 있기 쉬운 부분이 "프로젝트가 어떻게 조립되어 뜨는가"다. 한 번만 적어 둔다.

```
core/
├─ build.gradle              의존성 목록 (Spring Boot, JPA, Modulith, Flyway, MySQL 드라이버, 테스트 도구)
├─ settings.gradle           프로젝트 이름
├─ gradlew                   Gradle 실행기. 따로 설치 안 함. ./gradlew test, ./gradlew bootRun
├─ docker-compose.yml        로컬 MySQL 8 하나. docker compose up -d
└─ src/
   ├─ main/java/moa/
   │   ├─ MoaApplication.java        main(). 여기서 Spring이 시작됨
   │   ├─ shared/                    공통 값 객체 (Money)
   │   ├─ store/                     모듈. 아래 0.2 규칙
   │   ├─ category/
   │   ├─ sales/  expense/  budget/  report/
   ├─ main/resources/
   │   ├─ application.yml            DB 주소, 타임존, Flyway 설정
   │   └─ db/migration/<모듈>/       테이블 생성 SQL (V1__xxx.sql …)
   └─ test/java/moa/                 테스트. main과 같은 패키지 구조
```

**뜨는 순서.** `MoaApplication.main()` → Spring이 `moa` 아래 패키지를 훑어 `@Service`, `@Repository`, `@RestController` 등이 붙은 클래스를 전부 객체(빈)로 만들고 서로 연결(주입) → `application.yml`의 주소로 MySQL에 연결 → Flyway가 `db/migration` 아래 SQL 중 아직 안 돌린 것을 순서대로 실행해 테이블을 맞춤 → JPA가 `@Entity` 클래스와 테이블이 맞는지 확인 → 8080 포트에서 HTTP 요청을 받기 시작. 어느 단계든 실패하면 애플리케이션이 뜨지 않고 로그에 이유가 남는다.

**요청 하나의 길.** HTTP 요청 → 컨트롤러(`internal/`)가 JSON을 받아 공개 API(`XxxService`)를 호출 → 서비스가 리포지토리로 애그리거트를 꺼내고 메서드를 부른 뒤 저장 → 트랜잭션 커밋 → 이벤트가 있으면 커밋 뒤에 다른 모듈의 리스너가 받음 → 컨트롤러가 결과를 JSON으로 돌려줌.

### 0.2 패키지 규칙

```
moa/category/                      ← 모듈 루트. 여기 있는 것만 다른 모듈이 쓸 수 있다
├─ CategoryService.java            공개 API (응용 서비스)
├─ CategoryInfo.java               공개 API가 돌려주는 읽기 전용 레코드
├─ CategoryEvents.java             이 모듈이 내는 이벤트 (레코드들)
├─ CategoryUsage.java              이 모듈이 다른 모듈에 요구하는 인터페이스 (0.3 "역전")
└─ internal/                       ← 모듈 안. 다른 모듈은 import 금지
    ├─ Category.java               애그리거트 (엔티티)
    ├─ CategoryRepository.java     리포지토리 (Spring Data 인터페이스)
    ├─ CategoryController.java     HTTP
    ├─ CategoryTemplate.java       기본 분류 템플릿
    └─ StoreRegisteredListener.java 다른 모듈 이벤트 수신
```

- 모듈 루트에는 공개 API, 공개 레코드, 이벤트, 요구 인터페이스만 둔다. 나머지는 전부 `internal/`.
- 다른 모듈의 `internal/`을 import하면 **Spring Modulith 검증 테스트**가 실패한다. 이 테스트 하나(`ModularityTest`)가 모듈 경계를 지킨다. 코드 리뷰로 지키는 게 아니다.
- 모듈 사이에 순환 의존이 있어도 같은 테스트가 실패한다. expense → category가 있으면 category → expense는 둘 수 없다. 그래서 0.3의 "역전"이 필요하다.

### 0.3 모듈 간 연결 세 가지의 구현

| 방법 | 구현 | 예 |
|---|---|---|
| ID 참조 | 애그리거트에 `Long xxxId` 필드. JPA 연관관계 어노테이션 없음 | `Expense.categoryId`, 모든 애그리거트의 `storeId` |
| 공개 API 조회 | 상대 모듈 루트의 `XxxService` 빈을 주입받아 메서드 호출. 돌려받는 건 엔티티가 아니라 읽기 전용 레코드 | expense가 `categoryService.isActive(storeId, categoryId)` |
| 이벤트 | 내는 쪽: `ApplicationEventPublisher.publishEvent(레코드)`. 받는 쪽: `@ApplicationModuleListener` 메서드 | store가 `StoreRegistered`를 내고 category가 받아 템플릿 생성 |

**이벤트의 동작 방식.** `@ApplicationModuleListener`는 Spring Modulith가 주는 어노테이션으로, 세 가지가 묶여 있다. ① 내는 쪽 트랜잭션이 **커밋된 뒤에** 실행된다 (매장이 저장되지 않았는데 분류가 만들어지는 일이 없음). ② **비동기**로, 별도 스레드에서 실행된다 (매장 등록 응답이 분류 생성을 기다리지 않음). ③ 받는 쪽은 **자기 트랜잭션**을 연다. 그리고 Modulith가 `moa.event_publication` 테이블에 "누가 아직 처리 안 했는지"를 적어 두어, 처리 중 애플리케이션이 죽어도 다시 뜰 때 재실행할 수 있다. Phase 1에서는 재실행 설정을 켜 두는 것까지만 하고 운영 감시는 하지 않는다.

이벤트는 Java 레코드로 만들고, 상대 모듈이 알아야 할 **ID와 값만** 담는다. 엔티티를 담지 않는다.

**의존 역전 (category ↔ expense).** category는 "이 분류로 입력된 비용이 몇 건인가"를 알아야 삭제 가능 여부를 정한다. 그런데 expense → category 의존이 이미 있어서 category → expense를 더하면 순환이다. 해결은 **인터페이스를 category가 정하고, 구현을 expense가 하는 것**이다.

```java
// moa/category/CategoryUsage.java  (category 루트)
public interface CategoryUsage {
    Map<Long, Long> countByCategoryIds(Long storeId, Collection<Long> categoryIds);
}

// moa/expense/internal/CategoryUsageImpl.java  (expense 안)
@Component
class CategoryUsageImpl implements CategoryUsage { ... expense 테이블을 센다 ... }
```

컴파일 의존은 expense → category 한 방향뿐이다. category는 자기 인터페이스만 알고, 실행 시 Spring이 expense의 구현체를 꽂아 준다. expense 모듈이 아직 없는 2주차에는 category 테스트와 임시 설정에서 "항상 0건"을 돌려주는 구현을 쓴다.

### 0.4 데이터베이스

| 항목 | 결정 |
|---|---|
| 서버 | MySQL 8 하나 (Docker Compose). 테스트는 Testcontainers가 같은 이미지로 띄움 |
| 스키마 | 모듈별 6개 `store, category, sales, expense, budget, report` + 공통 1개 `moa` (Flyway 이력표, Modulith `event_publication`) |
| 연결 | DataSource 하나. JDBC URL에 기본 DB를 두지 않고, 엔티티에 `@Table(name = "categories", schema = "category")`처럼 스키마를 명시 |
| Flyway | 인스턴스 하나가 스키마 7개를 만들고 관리. SQL 파일은 `db/migration/<모듈>/V1__create_xxx.sql`. 파일 안에서 `category.categories`처럼 스키마를 붙여 씀 |
| 이름 규칙 | 테이블은 복수형 snake_case, 컬럼은 snake_case. 모든 테이블에 `id BIGINT AUTO_INCREMENT`, `created_at DATETIME`, `updated_at DATETIME` |
| 금액 | `DECIMAL(15,0)`. 날짜는 `DATE`. 문자열은 `VARCHAR` + `utf8mb4` |
| 모듈 간 참조 | `store_id`, `category_id` 같은 컬럼만. FK 제약 없음. JOIN 없음 |
| 타임존 | 서버·JDBC 모두 `Asia/Seoul`. "오늘"은 `LocalDate.now(ZoneId.of("Asia/Seoul"))` 한 군데(`Clock` 빈)에서만 만든다 |

스키마를 나누면 실수로 JOIN을 써도 코드 리뷰에서 바로 보이고(테이블 이름 앞에 다른 모듈 이름이 붙음), Phase 2에서 모듈 하나를 떼어 낼 때 테이블을 그대로 들고 나갈 수 있다.

### 0.5 계층과 트랜잭션, 규칙의 자리

```
컨트롤러 (internal)  →  응용 서비스 (루트, 공개)  →  애그리거트 · 리포지토리 (internal)
JSON ↔ 레코드            @Transactional               규칙은 여기
```

- **애그리거트 안 규칙**: 그 객체 하나만 보면 판단되는 것. 이름 길이, 상태 전이("꺼진 걸 또 끌 수 없다"), 금액 음수 금지. 애그리거트의 메서드 안에서 검사하고 어기면 예외.
- **애그리거트 밖 규칙**: 다른 객체를 봐야 판단되는 것. 형제 이름 중복, 개수 상한, "마지막 하나는 못 끈다". 응용 서비스가 리포지토리로 세어 보고 검사한다. 책 3장이 말하는 "애그리거트 하나 = 트랜잭션 하나"를 지키되, 이런 규칙까지 애그리거트 안에 넣으려고 애그리거트를 키우지 않는다.
- 트랜잭션은 응용 서비스 메서드 하나. 그 안에서 애그리거트 하나만 바꾼다. 둘 이상을 바꿔야 하면(예: 상위를 끄면 하위도) 같은 모듈 안에서는 같은 트랜잭션에 넣는 것을 허용한다 (사용자 1명, 토이 규모. 책 3장의 원칙에서 의도적으로 벗어나는 지점이고 결정 사항 D0-2에 적는다).
- 동시성: 사용자 1명이라 잠금·버전 관리를 두지 않는다. 이름 중복처럼 DB가 2중 보호해 줄 수 있는 것만 유니크 인덱스를 건다.

### 0.6 오류 처리

도메인·응용 서비스는 예외를 던지고, 공통 핸들러가 HTTP 응답으로 바꾼다. 프론트는 `code`를 보고 기획서의 문구를 띄운다. 서버는 문구를 모른다.

```json
{ "code": "CATEGORY_NAME_DUPLICATE", "message": "same name exists among siblings" }
```

| 상황 | HTTP | 비고 |
|---|---|---|
| 입력 형식 위반 (빈 이름, 길이 초과, 음수) | 400 | `VALIDATION` + 항목명 |
| 규칙 위반 (중복, 상한, 마지막 하나, 사용 중 삭제) | 409 | 모듈별 코드. 각 장의 HTTP 절에 목록 |
| 없는 ID | 404 | `NOT_FOUND` |
| 그 외 | 500 | 로그에 스택, 응답에는 코드만 |

### 0.7 테스트 전략

| 층 | 무엇을 | 도구 | 속도 |
|---|---|---|---|
| 도메인 단위 | 애그리거트 메서드의 규칙 | JUnit 5 + AssertJ. DB 없음, Spring 없음 | ms |
| 모듈 | 응용 서비스 한 기능 = 요청 하나. 저장·조회·이벤트 발행까지 | `@ApplicationModuleTest` + Testcontainers MySQL. 다른 모듈은 뜨지 않고, 필요한 상대 인터페이스는 가짜로 | 초 |
| 경계 | 모듈 간 import·순환 | `ApplicationModules.of(MoaApplication.class).verify()` 하나 | 초 |
| HTTP | 컨트롤러의 JSON 변환과 상태 코드 | `@WebMvcTest`, 서비스는 Mock. 기능당 정상 1개 + 오류 1개만 | ms |

- 테스트 이름은 `@DisplayName("기능 이름 - 상황")`. 기획서 검증 기준의 "기능 | 상황" 두 칸과 같게 쓴다. 검증 기준 중 화면만의 것(버튼 비활성, 토스트, 포커스)은 백엔드 테스트가 없고 프론트 테스트(Vitest)로 간다.
- TDD 순서: 검증 기준 한 줄 → 테스트 이름 쓰고 실패 확인(빨강) → 통과하는 최소 코드(초록) → 정리. 기능 하나가 끝나면 커밋 하나.

### 0.8 공통 값 객체

| 값 객체 | 내용 | 규칙 |
|---|---|---|
| `Money` | `long amount` (원) | 음수 불가. `plus`, `minus`(결과 음수 허용은 호출자가 결정), `isZero`. JPA에서는 `@Embeddable`, 컬럼 `DECIMAL(15,0)` |

`YearMonth`와 `LocalDate`는 Java 표준을 그대로 쓴다. ID는 `Long`이다 (StoreId 같은 래퍼는 두지 않는다. 토이 규모에서 얻는 것보다 JPA 매핑 부담이 큼).

### 0.9 결정 사항

| # | 결정 | 결론 | 이유 |
|---|---|---|---|
| D0-1 | 공개 API의 형태 | Java 인터페이스가 아니라 **공개 클래스 `XxxService`** 하나. 다른 모듈은 이 빈을 주입받아 호출 | 인터페이스+구현 분리는 구현이 둘 이상일 때 가치가 있음. Phase 1에는 없음. 역전이 필요한 곳(0.3)만 인터페이스 |
| D0-2 | 트랜잭션 하나에 애그리거트 여러 개 | 같은 모듈 안에서는 허용 (상위 끄면 하위도, 템플릿 생성) | 사용자 1명·토이 규모에서 이벤트로 쪼개면 복잡함만 늚. 모듈을 넘는 변경은 예외 없이 이벤트 |
| D0-3 | 이벤트 리스너 방식 | `@ApplicationModuleListener` (커밋 후·비동기·별도 트랜잭션), 재발행 켬 | 모듈러 모놀리스에서 Phase 2 물리 분리와 같은 모양을 미리 갖춤 |
| D0-4 | 공통 스키마 `moa` | Flyway 이력표와 Modulith 발행표를 둠 | 프레임워크 테이블이 특정 모듈 스키마에 섞이지 않게 |
| D0-5 | 오류 응답 | `code` 중심. 문구는 프론트 | 서버가 문구를 가지면 기획서 문구 변경 때 배포가 필요 |
| D0-6 | 현재 시각 | `Clock` 빈 하나 | 테스트에서 "오늘"을 고정하려면 주입 가능해야 함 |

---

## 1. store 모듈

### 1.1 개요

| 항목 | 내용 |
|---|---|
| 기능 | 매장 등록, 매장 정보 수정, 매장 조회 |
| 받는 이벤트 | 없음 |
| 내는 이벤트 | `StoreRegistered` |
| 다른 모듈 호출 | 없음 |
| 다른 모듈이 이 모듈을 | category·sales가 `StoreRegistered`를 받음. Phase 1에서 공개 API를 호출하는 모듈은 없음 |
| 스키마 | `store` |

### 1.2 애그리거트: Store

| 필드 | 타입 | 규칙 |
|---|---|---|
| `id` | Long | DB 생성 |
| `name` | String | 1~50자. 앞뒤 공백 제거 후 검사. 공백만은 비어 있는 것 |
| `type` | `StoreType` (CAFE, RESTAURANT, RETAIL, OTHER) | 필수 |
| `registeredAt` | LocalDate | 등록 시 서버 날짜. 바뀌지 않음 |

| 메서드 | 하는 일 | 규칙 검사 |
|---|---|---|
| `Store.register(name, type, today)` (정적) | 새 매장 생성 | 이름 규칙 |
| `update(name, type)` | 이름·업종 변경 | 이름 규칙. 업종은 바뀌어도 아무 일 없음 |

setter 없음. `registeredAt`을 바꾸는 메서드 없음. 등록 제한(Phase 1 전체 1개)은 이 객체가 모르는 규칙이라 밖(1.3)에 있다.

### 1.3 애그리거트 밖 규칙

| 규칙 | 어디서 | 어떻게 |
|---|---|---|
| 시스템 전체에 매장 1개 (Phase 1) | `StoreService.register` 첫 줄 | `storeRepository.count() >= 1`이면 `STORE_ALREADY_EXISTS`. Phase 2에서는 세는 범위만 계정으로 바뀜. DB 유니크 제약은 걸지 않음 |

### 1.4 공개 API: StoreService

| 메서드 | 입력 | 출력 | 트랜잭션 |
|---|---|---|---|
| `register(name, type)` | String, StoreType | `StoreInfo` | 쓰기. 커밋 후 `StoreRegistered` |
| `update(storeId, name, type)` | Long, String, StoreType | `StoreInfo` | 쓰기 |
| `get(storeId)` | Long | `StoreInfo` | 읽기. 없으면 `NOT_FOUND` |
| `findAll()` | — | `List<StoreInfo>` | 읽기. Phase 1에서 프론트가 "매장이 있는가"를 판단하는 데 씀 (0개 또는 1개) |

`StoreInfo(id, name, type, registeredAt)` 레코드.

### 1.5 이벤트

| 이벤트 | 내용 | 언제 | 받는 모듈 |
|---|---|---|---|
| `StoreRegistered` | `storeId`, `type`, `registeredAt` | 등록 커밋 후 | category(기본 분류 생성), sales(기본 결제수단 생성) |

업종 변경은 이벤트를 내지 않는다 (Phase 1에서 업종에 반응하는 모듈이 없음. 기본 분류는 등록 때 한 번만).

### 1.6 HTTP

| 메서드 | 경로 | 요청 | 응답 | 오류 코드 |
|---|---|---|---|---|
| POST | `/api/stores` | `{name, type}` | 201 + StoreInfo | `VALIDATION`, `STORE_ALREADY_EXISTS` (409) |
| GET | `/api/stores` | — | 200 + `[StoreInfo]` | — |
| GET | `/api/stores/{id}` | — | 200 + StoreInfo | `NOT_FOUND` |
| PUT | `/api/stores/{id}` | `{name, type}` | 200 + StoreInfo | `VALIDATION`, `NOT_FOUND` |

### 1.7 테이블: store.stores

```sql
CREATE TABLE store.stores (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(50)  NOT NULL,
  store_type    VARCHAR(20)  NOT NULL,          -- CAFE / RESTAURANT / RETAIL / OTHER
  registered_at DATE         NOT NULL,
  created_at    DATETIME     NOT NULL,
  updated_at    DATETIME     NOT NULL
) CHARACTER SET utf8mb4;
```

### 1.8 파일

```
moa/store/
├─ StoreService.java        공개 API
├─ StoreInfo.java           레코드
├─ StoreType.java           enum (공개. category·sales가 이벤트에서 봄)
├─ StoreEvents.java         StoreRegistered 레코드
└─ internal/
    ├─ Store.java           애그리거트
    ├─ StoreRepository.java Spring Data
    └─ StoreController.java
```

### 1.9 테스트 목록

기획서 1장 검증 기준 18건 중 백엔드가 맡는 것. 나머지(버튼 비활성, 글자 수, 토스트, 리다이렉트, 가이드)는 프론트.

| 층 | `@DisplayName` |
|---|---|
| 도메인 | 매장 등록 - 정상 등록 |
| 도메인 | 매장 등록 - 이름 비움 → 거부 |
| 도메인 | 매장 등록 - 공백만 입력 → 거부 |
| 도메인 | 매장 등록 - 앞뒤 공백 → 제거되어 저장 |
| 도메인 | 매장 등록 - 50자 초과 → 거부 (50자는 허용) |
| 도메인 | 매장 정보 수정 - 이름 변경 |
| 도메인 | 매장 정보 수정 - 업종 변경 → 등록일은 그대로 |
| 모듈 | 매장 등록 - 저장되고 StoreRegistered 발행 |
| 모듈 | 매장 등록 - 이미 매장 있음 → STORE_ALREADY_EXISTS |
| 모듈 | 매장 조회 - 이름·업종·등록일 반환 |
| 모듈 | 매장 조회 - 없는 ID → NOT_FOUND |
| 모듈 | 매장 정보 수정 - 변경 후 조회에 반영 |
| HTTP | 매장 등록 - 201과 본문 |
| HTTP | 매장 등록 - 빈 이름 → 400 VALIDATION |

### 1.10 결정 사항

| # | 결정 | 결론 | 이유 |
|---|---|---|---|
| D1-1 | 등록 제한의 자리 | 응용 서비스 한 줄. 모델·DB에 없음 | 요구사항정의서 8.1. Phase 2에서 바뀌는 코드가 이 줄뿐이어야 함 |
| D1-2 | 등록일 | 서버의 Asia/Seoul 날짜 (`Clock`) | 클라이언트 시계를 믿지 않음 |
| D1-3 | 수정 API | 이름·업종을 한 번에 PUT | 항목이 둘뿐. 나눌 이유 없음 |
| D1-4 | 업종 변경 이벤트 | 내지 않음 | 받는 모듈이 없음. 필요해지면 그때 추가 |

---

## 2. category 모듈

### 2.1 개요

| 항목 | 내용 |
|---|---|
| 기능 | 기본 분류 생성, 분류 추가, 분류 이름 변경, 분류 비활성화, 분류 활성화, 분류 삭제, 분류 tree 조회, 분류 존재 확인, 분류 이동(선택), 분류 tree Markdown 내보내기(선택) |
| 받는 이벤트 | `StoreRegistered` → 기본 분류 생성 |
| 내는 이벤트 | `DefaultCategoriesCreated`, `CategoryAdded`, `CategoryRenamed`, `CategoryMoved`, `CategoryDeactivated` (Phase 1 수신자 없음. Phase 2 AI) |
| 다른 모듈 호출 | 없음. 단, 비용 건수는 `CategoryUsage` 인터페이스로 받음 (expense가 구현) |
| 다른 모듈이 이 모듈을 | expense가 `isActive`, `infoOf`로 조회. report가 `infoOf`로 이름·경로 조회 |
| 스키마 | `category` |

### 2.2 애그리거트: Category (노드 하나)

먼저 "무엇을 애그리거트로 볼 것인가"를 정해야 했다. 두 가지가 가능하다.

| 선택지 | 묶음 | 장점 | 단점 |
|---|---|---|---|
| A. 노드 하나 | 분류 하나가 애그리거트. 상위는 `parentId`로 가리킴 | JPA 매핑이 가장 단순 (연관관계 없음). 조회·존재 확인이 가벼움 | 형제 중복·개수·최소 하나 같은 규칙이 애그리거트 밖(응용 서비스)으로 나감 |
| B. tree 전체 | 매장의 분류 tree 전체가 애그리거트 하나 | 모든 규칙이 루트 안에 들어감. 책 3장의 모양 그대로 | `@OneToMany` 두 단계, 삭제에 orphanRemoval(cascade 계열) 필요. JPA 최소 사용 규칙과 충돌. 하위 하나 고칠 때도 420개를 통째로 읽음 |

**A를 채택한다.** 규칙을 밖에 두는 비용보다 JPA 복잡도를 피하는 이득이 크고, 11/02 JPA 체크포인트를 넘기기 쉬운 쪽이다. 규칙이 밖으로 나가는 것은 책 3장 끝에서 말하는 "애그리거트 경계를 넘는 규칙은 응용 서비스나 도메인 서비스에서"에 해당한다. 예산 모듈(4주차)에서 B 모양(월 예산 + 항목 목록)을 보게 되므로 두 모양을 다 겪는다.

| 필드 | 타입 | 규칙 |
|---|---|---|
| `id` | Long | DB 생성. expense가 이 값을 `categoryId`로 보관 |
| `storeId` | Long | ID 참조 |
| `parentId` | Long, null 가능 | null이면 상위 분류, 아니면 그 상위의 하위 |
| `name` | String | 1~20자. 앞뒤 공백 제거. 공백만 불가 |
| `sortOrder` | int | 형제 안의 순서. 만든 순서 (Phase 1은 끌어서 정렬 없음) |
| `active` | boolean | 켜짐/꺼짐 |

| 메서드 | 하는 일 | 규칙 검사 (애그리거트 안) |
|---|---|---|
| `Category.top(storeId, name, sortOrder)` (정적) | 상위 분류 생성 | 이름 |
| `Category.childOf(parent, name, sortOrder)` (정적) | 하위 분류 생성 | 이름. `parent.isTop()`이 아니면 거부 (2단계 초과) |
| `rename(name)` | 이름 변경 | 이름. 같은 이름이면 아무 일 없음 (이벤트도 없음) |
| `deactivate()` | 끄기 | 이미 꺼져 있으면 아무 일 없음 |
| `activate()` | 켜기 | 이미 켜져 있으면 아무 일 없음 |
| `moveTo(newParent)` (선택) | 다른 상위로 | 자신이 하위여야 하고, `newParent.isTop()`이어야 함 |
| `isTop()`, `isChildOf(parent)` | 판별 | — |

### 2.3 애그리거트 밖 규칙 (CategoryService)

| 규칙 | 기능 | 어떻게 (리포지토리 조회) | 오류 코드 |
|---|---|---|---|
| 형제 간 이름 중복 불가 (꺼진 것 포함) | 추가, 이름 변경, 이동 | `existsByStoreIdAndParentIdAndName` (상위끼리는 parentId null) | `CATEGORY_NAME_DUPLICATE` |
| 상위 20개, 상위당 하위 20개 (꺼진 것 포함) | 추가, 이동 | `countByStoreIdAndParentId` | `CATEGORY_LIMIT` |
| 하위 아래에는 추가 불가 | 추가 | 상위 조회 후 `isTop()` | `CATEGORY_DEPTH` |
| 상위를 끄면 하위도 함께 | 비활성화 | 하위 목록 조회 후 각각 `deactivate()`. 같은 트랜잭션 (D0-2) | — |
| 하위를 켜려면 상위가 켜져 있어야 | 활성화 | 상위 조회 후 `active` 확인 | `CATEGORY_PARENT_INACTIVE` |
| 상위를 켜도 하위는 그대로 | 활성화 | 아무것도 안 함 | — |
| 켜진 상위 분류는 항상 1개 이상 | 비활성화, 삭제 | `countByStoreIdAndParentIdIsNullAndActiveTrue`가 1이고 대상이 그것이면 거부 | `CATEGORY_LAST_ACTIVE` |
| 하위가 있는 상위는 삭제 불가 | 삭제 | `countByStoreIdAndParentId > 0` | `CATEGORY_HAS_CHILDREN` |
| 누적 비용 1건 이상이면 삭제 불가 | 삭제 | `categoryUsage.countByCategoryIds` | `CATEGORY_IN_USE` |
| 하위 있는 상위는 이동 불가 (선택) | 이동 | `countByStoreIdAndParentId > 0` | `CATEGORY_HAS_CHILDREN` |

### 2.4 기본 분류 템플릿

`CategoryTemplate`에 카페 tree를 상수로 둔다. Phase 1은 업종과 무관하게 이 템플릿 하나를 쓴다 (기획서 P1-3). 업종별 템플릿은 Phase 2.

```
식자재 > 원두, 유제품, 베이커리, 기타 식자재
인건비 > 급여, 주휴·수당
임대료
공과금 > 전기, 수도·가스, 통신
마케팅
소모품
기타
```

`StoreRegisteredListener`가 받아서 상위 7개와 하위 9개를 한 트랜잭션에 저장하고 `DefaultCategoriesCreated(storeId, 개수)`를 낸다. 같은 `storeId`로 이미 분류가 있으면 아무것도 하지 않는다 (이벤트 재발행에 대비한 멱등 처리).

### 2.5 공개 API: CategoryService

| 메서드 | 입력 | 출력 | 누가 쓰나 |
|---|---|---|---|
| `tree(storeId, includeInactive)` | Long, boolean | `List<CategoryInfo>` 상위 → 그 하위 순서. 각 항목에 `expenseCount` 포함 | 컨트롤러 |
| `add(storeId, parentId, name)` | Long, Long(null 가능), String | `CategoryInfo` | 컨트롤러 |
| `rename(storeId, id, name)` | | `CategoryInfo` | 컨트롤러 |
| `deactivate(storeId, id)` | | 꺼진 ID 목록 (하위 포함) | 컨트롤러 |
| `activate(storeId, id)` | | `CategoryInfo` | 컨트롤러 |
| `delete(storeId, id)` | | — | 컨트롤러 |
| `move(storeId, id, newParentId)` (선택) | | `CategoryInfo` | 컨트롤러 |
| `isActive(storeId, categoryId)` | | boolean (없으면 false) | **expense** (비용 등록·재분류 때 "분류 존재 확인") |
| `infoOf(storeId, categoryIds)` | Long, Collection<Long> | `Map<Long, CategoryInfo>` | **expense**(목록의 분류 경로), **report**(분류별 집계 이름) |
| `exportMarkdown(storeId)` (선택) | | String | 컨트롤러 |

`CategoryInfo(id, parentId, name, path, active, sortOrder, expenseCount)` 레코드. `path`는 "식자재 > 유제품", 상위는 이름만. `expenseCount`는 `tree`에서만 채우고 `infoOf`에서는 0.

모든 메서드가 `storeId`를 받는다. Phase 1은 매장이 하나지만 Phase 2 로그인 때 "내 매장만"으로 바뀌는 자리를 미리 잡아 둔다 (요구사항정의서 9장 규칙).

### 2.6 이벤트

| 이벤트 | 내용 | 언제 |
|---|---|---|
| `DefaultCategoriesCreated` | `storeId`, `count` | 템플릿 생성 커밋 후 |
| `CategoryAdded` | `storeId`, `categoryId`, `parentId`, `name` | 추가 커밋 후 |
| `CategoryRenamed` | `storeId`, `categoryId`, `oldName`, `newName` | 이름 변경 커밋 후 (같은 이름이면 안 냄) |
| `CategoryMoved` | `storeId`, `categoryId`, `oldParentId`, `newParentId` | 이동 커밋 후 |
| `CategoryDeactivated` | `storeId`, `categoryIds` (하위 포함) | 비활성화 커밋 후 |

Phase 1에서 받는 모듈은 없다. 내는 비용이 작고 Phase 2 AI 서비스가 tree 변화를 따라가는 데 쓰므로 요구사항정의서 목록대로 낸다. 활성화·삭제 이벤트는 요구사항정의서에 없어 내지 않는다 (필요해지면 추가).

### 2.7 HTTP

모든 경로는 `/api/stores/{storeId}/categories` 아래.

| 메서드 | 경로 | 요청 | 응답 | 오류 코드 |
|---|---|---|---|---|
| GET | `/` `?includeInactive=false` | — | 200 + `[CategoryInfo]` | — |
| POST | `/` | `{parentId?, name}` | 201 + CategoryInfo | `VALIDATION`, `CATEGORY_NAME_DUPLICATE`, `CATEGORY_LIMIT`, `CATEGORY_DEPTH`, `NOT_FOUND`(상위) |
| PATCH | `/{id}/name` | `{name}` | 200 + CategoryInfo | `VALIDATION`, `CATEGORY_NAME_DUPLICATE`, `NOT_FOUND` |
| POST | `/{id}/deactivate` | — | 200 + `{deactivatedIds}` | `CATEGORY_LAST_ACTIVE`, `NOT_FOUND` |
| POST | `/{id}/activate` | — | 200 + CategoryInfo | `CATEGORY_PARENT_INACTIVE`, `NOT_FOUND` |
| DELETE | `/{id}` | — | 204 | `CATEGORY_HAS_CHILDREN`, `CATEGORY_IN_USE`, `CATEGORY_LAST_ACTIVE`, `NOT_FOUND` |
| PATCH | `/{id}/parent` (선택) | `{parentId}` | 200 + CategoryInfo | `CATEGORY_HAS_CHILDREN`, `CATEGORY_NAME_DUPLICATE`, `CATEGORY_LIMIT`, `NOT_FOUND` |
| GET | `/export.md` (선택) | — | 200 `text/markdown` | — |

끄기·삭제 확인창에 보이는 "누적 비용 N건"은 `GET /`의 `expenseCount`로 이미 받아 둔 값을 쓴다. 별도 호출 없음.

### 2.8 테이블: category.categories

```sql
CREATE TABLE category.categories (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  store_id    BIGINT       NOT NULL,
  parent_id   BIGINT       NULL,                -- NULL = 상위 분류. FK 없음 (같은 테이블이어도 규칙은 코드에)
  name        VARCHAR(20)  NOT NULL,
  sort_order  INT          NOT NULL,
  active      TINYINT(1)   NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL,
  updated_at  DATETIME     NOT NULL,
  INDEX idx_categories_store_parent (store_id, parent_id),
  UNIQUE KEY uk_categories_sibling_name (store_id, parent_id, name)
) CHARACTER SET utf8mb4;
```

유니크 키는 하위 분류의 중복만 DB가 막아 준다. MySQL은 `parent_id`가 NULL인 행끼리는 유니크를 비교하지 않으므로 상위 분류의 중복은 코드(2.3)만 막는다. 사용자 1명이라 충분하다.

### 2.9 Markdown 내보내기 형식 (선택)

켜진 분류만, 요구사항정의서 5.2와 같은 모양. Phase 2 AI 기학습 자료가 된다.

```
식자재
├─ 원두
├─ 유제품
└─ 기타 식자재
임대료
```

### 2.10 파일

```
moa/category/
├─ CategoryService.java
├─ CategoryInfo.java
├─ CategoryEvents.java          레코드 5개
├─ CategoryUsage.java           인터페이스 (expense가 구현)
└─ internal/
    ├─ Category.java
    ├─ CategoryRepository.java
    ├─ CategoryController.java
    ├─ CategoryTemplate.java
    ├─ StoreRegisteredListener.java
    └─ NoExpenseYet.java        CategoryUsage 임시 구현 (항상 0). 3주차 expense 구현 때 삭제
```

### 2.11 테스트 목록

기획서 2장 검증 기준 36건 중 백엔드가 맡는 것. 접기 상태, 입력란, 버튼 비활성, 확인창은 프론트.

| 층 | `@DisplayName` |
|---|---|
| 도메인 | 분류 추가 - 상위 생성, parentId 없음 |
| 도메인 | 분류 추가 - 하위 생성, 상위를 가리킴 |
| 도메인 | 분류 추가 - 하위 아래 추가 → 거부 (2단계 초과) |
| 도메인 | 분류 추가 - 빈 이름 → 거부 |
| 도메인 | 분류 추가 - 21자 → 거부 (20자는 허용) |
| 도메인 | 분류 이름 변경 - 앞뒤 공백 제거 |
| 도메인 | 분류 이름 변경 - 같은 이름 → 변경 없음 |
| 도메인 | 분류 비활성화 - 켜짐에서 꺼짐 |
| 도메인 | 분류 활성화 - 꺼짐에서 켜짐 |
| 도메인 | 분류 이동 - 상위를 옮기려 함 → 거부 |
| 모듈 | 기본 분류 생성 - StoreRegistered 수신 → 상위 7, 하위 9 생성, DefaultCategoriesCreated 발행 |
| 모듈 | 기본 분류 생성 - 다른 업종 → 같은 템플릿 |
| 모듈 | 기본 분류 생성 - 두 번 수신 → 두 번째는 무시 |
| 모듈 | 분류 추가 - 상위 추가 → tree 끝에, CategoryAdded 발행 |
| 모듈 | 분류 추가 - 하위 추가 → 그 상위 아래 끝에, 경로 "식자재 > 시럽" |
| 모듈 | 분류 추가 - 형제 중복 → CATEGORY_NAME_DUPLICATE |
| 모듈 | 분류 추가 - 다른 상위와는 중복 허용 |
| 모듈 | 분류 추가 - 꺼진 분류와 중복 → CATEGORY_NAME_DUPLICATE |
| 모듈 | 분류 추가 - 상위 20개 → CATEGORY_LIMIT |
| 모듈 | 분류 추가 - 하위 20개 → CATEGORY_LIMIT |
| 모듈 | 분류 이름 변경 - 정상, CategoryRenamed 발행 |
| 모듈 | 분류 이름 변경 - 중복 → CATEGORY_NAME_DUPLICATE |
| 모듈 | 분류 비활성화 - 하위 없는 상위 → 꺼짐, CategoryDeactivated 발행 |
| 모듈 | 분류 비활성화 - 하위 있는 상위 → 하위도 함께 꺼짐 |
| 모듈 | 분류 비활성화 - 하위만 → 상위·형제는 그대로 |
| 모듈 | 분류 비활성화 - 마지막 상위 → CATEGORY_LAST_ACTIVE |
| 모듈 | 분류 활성화 - 상위 켜기 → 하위는 여전히 꺼짐 |
| 모듈 | 분류 활성화 - 상위 꺼진 하위 → CATEGORY_PARENT_INACTIVE |
| 모듈 | 분류 활성화 - 상위 켜진 하위 → 켜짐 |
| 모듈 | 분류 삭제 - 0건 하위 → 삭제 |
| 모듈 | 분류 삭제 - 0건 상위, 하위 없음 → 삭제 |
| 모듈 | 분류 삭제 - 사용된 분류 → CATEGORY_IN_USE |
| 모듈 | 분류 삭제 - 하위 있는 상위 → CATEGORY_HAS_CHILDREN |
| 모듈 | 분류 삭제 - 기본 템플릿 분류 0건 → 삭제 |
| 모듈 | 분류 삭제 - 마지막 상위 → CATEGORY_LAST_ACTIVE |
| 모듈 | 분류 tree 조회 - 상위 → 하위 순서, 경로·활성 여부·건수 |
| 모듈 | 분류 tree 조회 - 활성만 → 꺼진 것 제외 |
| 모듈 | 분류 존재 확인 - 켜진 분류 → true, 꺼진·없는 분류 → false |
| 모듈 | 분류 이동 (선택) - 정상 → 새 상위 아래 끝 |
| 모듈 | 분류 이동 (선택) - 이름 충돌 → CATEGORY_NAME_DUPLICATE |
| 모듈 | Markdown 내보내기 (선택) - 켜진 것만, 형식 |
| 경계 | 모듈 경계 - category가 expense internal을 모름, 순환 없음 |
| HTTP | 분류 추가 - 201과 본문 |
| HTTP | 분류 삭제 - 사용 중 → 409 CATEGORY_IN_USE |

### 2.12 결정 사항

| # | 결정 | 결론 | 이유 |
|---|---|---|---|
| D2-1 | 애그리거트 범위 | 노드 하나 (2.2의 A) | JPA 최소 사용 규칙 안에서 끝남. tree 규칙은 응용 서비스에서 조회로 검사 |
| D2-2 | 비용 건수를 아는 방법 | `CategoryUsage` 인터페이스를 category가 정의, expense가 구현 (의존 역전). 이벤트로 건수를 따로 세지 않음 | 순환 의존 없이 삭제 시점에 정확한 건수. 이벤트 집계는 지연이 있어 "방금 등록한 비용이 있는데 삭제됨"이 생길 수 있음 |
| D2-3 | 템플릿 저장 위치 | 코드 상수 | Phase 1 템플릿 하나. 업종별·DB 관리로 가는 건 Phase 2 |
| D2-4 | 상위 분류 이름 중복의 DB 보호 | 없음 (NULL 유니크 한계). 코드만 | 사용자 1명. 대안(`parent_id`를 0으로 두기)은 ID 참조 의미를 흐림 |
| D2-5 | `storeId`를 모든 API에 | 받음 | Phase 2 로그인에서 범위가 바뀌는 자리를 미리 둠 |
| D2-6 | 활성화·삭제 이벤트 | 내지 않음 | 요구사항정의서 목록에 없고 수신자도 없음 |
| D2-7 | 하위 끄기의 트랜잭션 | 상위와 하위를 한 트랜잭션에서 | D0-2 |

---

## 3. sales 모듈 (예정, 3주차)

## 4. expense 모듈 (예정, 3주차)

## 5. budget 모듈 (예정, 4주차)

## 6. report 모듈 (예정, 4주차)
