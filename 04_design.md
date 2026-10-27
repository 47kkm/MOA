# MOA 설계 문서

- 시작 2026-10-24 · **형식 개정 2026-10-27** (줄글 → 그림·표, 모듈 장 틀 통일)
- **개발자 문서.** 안에서 어떻게 만드는지를 정한다. 화면·문구·사용자 흐름은 기획서(03)의 몫
- 0장 = 모든 모듈 공통. 1장부터 = 모듈 하나가 장 하나
- 구현하면서 바뀐 것은 그 장의 결정 사항에 날짜와 함께 덧붙인다
- 기능 이름 = 요구사항정의서·기획서의 기능 이름 그대로. 테스트 이름도 `"기능 이름 - 상황"`. 번호 없음

## 읽는 법

| 표시 | 뜻 | 누가 보나 |
|---|---|---|
| **[검토]** | 업무 규칙과 흐름. 기획서와 같은지 판단하는 곳 | 사용자 |
| **[참고]** | 구현 상세 (프레임워크, SQL, 파일). 테스트가 증명한다 | Claude. 사용자는 코드를 읽다가 필요할 때 찾아봄 |

**모듈 장 틀** (모든 모듈이 같은 순서)

| 절 | 표시 | 내용 |
|---|---|---|
| N.1 한눈에 | [검토] | 의존 그림, 기능 목록 |
| N.2 기능 흐름 | [검토] | 기능마다 화살표 그림 |
| N.3 규칙 | [검토] | 규칙 · 안/밖 · 어기면 · 근거 |
| N.4 애그리거트 | [검토] | 필드 표, 메서드 표 |
| N.5 테스트 목록 | [검토] | `@DisplayName` 목록 |
| N.6 결정 사항 | [검토] | 결정 · 결론 · 이유 |
| N.7 공개 API·이벤트·HTTP | [참고] | 메서드·이벤트·경로 표 |
| N.8 테이블·파일 | [참고] | SQL, 파일 트리 |

**쓰는 규칙:** 흐름은 화살표 그림 · 규칙과 결정은 표 · 한 줄에 한 문장, 문단은 3줄 이하 · 이유는 "왜" 열이나 `→ 왜:` 한 줄 · 새 오류 코드는 0.6 표에 더함

**흐름 그림 기호**

| 기호 | 뜻 |
|---|---|
| `→` | 다음 단계 |
| `├` `└` | 갈래. 위에서 아래 순서로 검사 |
| `✗ 409 CODE` | 여기서 거부하고 이 응답을 돌려줌 |
| `[안]` `[밖]` | 애그리거트 안 규칙 / 밖 규칙 (0.5) |
| `══ 커밋 ══` | 트랜잭션 끝. 여기서 DB에 확정 |

## 배경

- MOA = 사장님이 하루 마감 때 매출·비용을 입력 → 이번 달 현황과 예산 잔여를 보여 주는 매장 가계부
- Phase 1 (~2026-11-30) = 입력과 집계. 사용자 1명 · 매장 1개 · 로그인 없음
- 백엔드 = Spring Boot 서비스 하나(MOA Core) 안에 모듈 6개 → **모듈러 모놀리스**
- DB = MySQL 서버 하나. 모듈마다 데이터베이스(스키마) 하나
- 모듈 연결은 세 가지뿐: ID 참조 · 공개 API 조회 · 도메인 이벤트 (0.3). 다른 모듈 테이블에 FK·JOIN 금지
- **API는 조회만, 변경은 이벤트로**

```
모듈 의존 방향

store ── StoreRegistered ──┬──▶ category ◀── 조회 ── expense ── 이벤트 ──▶ budget
                           └──▶ sales

report ── 조회 ──▶ store · category · sales · expense · budget   (자기 데이터 없음)
```

| 장 | 모듈 | 상태 |
|---|---|---|
| 0 | 공통 | 초안 |
| 1 | store | 초안 |
| 2 | category | 초안 |
| 3 | sales | 예정 (3주차) |
| 4 | expense | 예정 (3주차) |
| 5 | budget | 예정 (4주차) |
| 6 | report | 예정 (4주차) |

## 용어

**책의 개념이 이 문서 어디에 있는가** (최범균 『도메인 주도 개발 시작하기』 1~3장 기준)

| 책의 개념 | 뜻 (한 줄) | 이 문서에서 |
|---|---|---|
| 엔티티 | ID로 구분되는 객체. 값이 바뀌어도 같은 것 | Store, Category 등 애그리거트 표의 클래스 |
| 값 객체 | ID 없이 값으로만 비교. 바꾸지 않고 새로 만듦 | Money (0.8). Phase 1은 이것 하나 |
| 애그리거트 | 함께 바뀌고 함께 저장되는 객체 묶음. 트랜잭션 하나의 단위 | 각 장의 N.4. 묶음의 범위를 어떻게 잘랐는지와 이유 |
| 애그리거트 루트 | 묶음의 대표. 바깥은 루트의 메서드로만 상태를 바꿈 | N.4의 메서드 표. setter 없음 |
| 리포지토리 | 애그리거트를 통째로 저장·복원하는 창구 | N.8 파일의 `XxxRepository`. Spring Data JPA가 구현을 만들어 줌 |
| 도메인 이벤트 | "무슨 일이 있었다"를 다른 모듈에 알리는 메시지 | N.7 이벤트 표와 0.3 |
| 응용 서비스 | 요청 하나를 받아 리포지토리에서 애그리거트를 꺼내고 메서드를 부르고 저장하는 층. 규칙은 두지 않음 | N.7의 공개 API (`XxxService`) |
| 애그리거트 밖 규칙 | 여러 애그리거트를 봐야 지킬 수 있는 규칙 (예: 형제 이름 중복). 책 3장 끝·7장 도메인 서비스 | N.3 규칙 표의 `[밖]` |
| ID 참조 | 다른 애그리거트를 객체가 아니라 ID로 가리킴 | `Long storeId`, `Long parentId`, `Long categoryId` |

**기술 용어** (Spring·DB. 처음 보면 여기서 한 줄 확인)

| 용어 | 한 줄 |
|---|---|
| 빈 (bean) | Spring이 만들어 들고 있다가 필요한 곳에 넣어 주는 객체. `@Service` 등을 붙인 클래스, Spring Data 리포지토리, `@Bean` 메서드가 돌려준 것 |
| 주입 | 빈을 생성자로 받는 것. MOA는 생성자 주입만 쓴다 (Lombok 없음, D0-7) |
| 어노테이션 | 클래스·메서드에 붙이는 표시. Spring이 보고 일을 한다. 소속은 import로 구분: `org.springframework.` Spring / `org.springframework.modulith.` Spring Modulith / `jakarta.persistence.` JPA |
| 트랜잭션 | 여러 DB 작업을 "전부 되거나 전부 안 되거나"로 묶은 것. `@Transactional` 메서드 하나 = 트랜잭션 하나 |
| 커밋 / 롤백 | 트랜잭션 확정 / 취소 |
| 스레드 | 요청 하나를 처음부터 끝까지 처리하는 일꾼 하나 |
| 동기 / 비동기 | 같은 스레드가 이어서 처리해 끝나야 응답 / 다른 스레드에 넘기고 먼저 응답 |
| JPA · Hibernate · Spring Data JPA | 표준 · 그 구현 · 리포지토리 구현을 자동으로 만들어 주는 층 |
| JDBC | Java ↔ DB 통신의 표준. MyBatis도 JPA도 밑에서는 이걸 쓴다 |
| DataSource | DB 연결을 미리 열어 두고 빌려주는 창구(연결 풀). Spring Boot가 `application.yml`을 보고 빈으로 만든다 |
| Flyway | 테이블 SQL 파일을 번호 순서대로, 아직 안 돌린 것만 실행한다. DB 구조의 git 이력 |
| Spring Modulith | 모듈 경계 검사(0.2)와 이벤트 발행 기록(0.3)을 해 주는 Spring 추가 라이브러리 |
| Testcontainers | 테스트할 때 Docker로 진짜 MySQL을 잠깐 띄우는 도구 |
| 인터셉터 | 컨트롤러 앞에서 먼저 도는 Spring MVC 검사. 프론트의 라우트 미들웨어와 같은 자리 |

---

## 0. 공통 설계

### 0.1 프로젝트 모양과 뜨는 순서 [참고]

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

**뜨는 순서**

```
MoaApplication.main()
 → Spring이 moa 아래를 훑어 @Service·@Repository·@RestController 등을 빈으로 만들고 서로 주입
 → application.yml 주소로 MySQL에 연결 (DataSource)
 → Flyway가 db/migration 중 아직 안 돌린 SQL을 순서대로 실행 → 테이블을 맞춤
 → JPA가 @Entity 클래스와 테이블이 맞는지 확인
 → 8080 포트에서 HTTP 요청을 받기 시작
```

어느 단계든 실패하면 앱이 뜨지 않고 로그에 이유가 남는다.

**요청 하나의 길**

```
HTTP 요청
 → 컨트롤러 (internal/)       JSON → 입력값
 → 공개 API XxxService         리포지토리로 애그리거트를 꺼냄 → 메서드 호출 → 저장
 ══ 커밋 ══
 → 이벤트가 있으면 다른 모듈의 리스너. 같은 스레드에서 이어서 (0.3)
 → 컨트롤러                    결과 → JSON 응답
```

### 0.2 패키지 규칙 [참고]

```
moa/category/                      ← 모듈 루트. 여기 있는 것만 다른 모듈이 쓸 수 있다
├─ CategoryService.java            공개 API (응용 서비스)
├─ CategoryInfo.java               공개 API가 돌려주는 읽기 전용 레코드
├─ CategoryEvents.java             이 모듈이 발행하는 이벤트 (레코드들)
├─ CategoryUsage.java              이 모듈이 다른 모듈에 요구하는 인터페이스 (0.3 "역전")
└─ internal/                       ← 모듈 안. 다른 모듈은 import 금지
    ├─ Category.java               애그리거트 (엔티티)
    ├─ CategoryRepository.java     리포지토리 (Spring Data 인터페이스)
    ├─ CategoryController.java     HTTP
    ├─ CategoryTemplate.java       기본 분류 템플릿
    └─ StoreRegisteredListener.java 다른 모듈 이벤트 수신
```

| 규칙 | 지키는 방법 |
|---|---|
| 모듈 루트에는 공개 API · 공개 레코드 · 이벤트 · 요구 인터페이스만. 나머지는 전부 `internal/` | 아래 테스트 |
| 다른 모듈의 `internal/` import 금지 | **Spring Modulith 검증 테스트** `ModularityTest` 하나가 실패한다. 코드 리뷰로 지키지 않는다 |
| 모듈 사이 순환 의존 금지. expense → category가 있으면 category → expense는 불가 | 같은 테스트. 그래서 0.3의 "역전"이 필요하다 |

**흔한 계층별 구조와의 차이**

```
계층별 (흔한 형태)          모듈별 (MOA)
controller/                 store/      ← StoreService (공개)
service/                     └ internal/ ← StoreController, Store, StoreRepository
mapper/                     category/   …
```

→ 왜: Spring Modulith는 `moa` 바로 아래 패키지 하나를 모듈 하나로 보고 경계를 검사한다. 모듈 안(`internal/`)은 계층별 구조의 축소판이다.

### 0.3 모듈 간 연결 [참고]

| 방법 | 구현 | 예 |
|---|---|---|
| ID 참조 | 애그리거트에 `Long xxxId` 필드. JPA 연관관계 어노테이션 없음 | `Expense.categoryId`, 모든 애그리거트의 `storeId` |
| 공개 API 조회 | 상대 모듈 루트의 `XxxService` 빈을 생성자로 받아 메서드 호출. 돌려받는 건 엔티티가 아니라 읽기 전용 레코드 | expense가 `categoryService.isActive(storeId, categoryId)` |
| 이벤트 | 발행하는 쪽: `ApplicationEventPublisher.publishEvent(레코드)`. 수신하는 쪽: `@TransactionalEventListener` + `@Transactional(propagation = REQUIRES_NEW)` 메서드 | store가 `StoreRegistered`를 발행하고 category가 수신해 템플릿 생성 |

- 이벤트는 Java 레코드. 상대 모듈이 알아야 할 **ID와 값만** 담는다. 엔티티는 담지 않는다

**이벤트 동작** (D0-3. 2026-10-25 비동기 → 동기)

```
store: 매장 저장
 ══ 커밋 ══                              ① 커밋된 뒤에만 실행 → 저장 안 된 매장의 분류가 생기는 일 없음
 → category 리스너 (같은 스레드)          ② 동기 → HTTP 응답 전에 끝남
     새 트랜잭션 (REQUIRES_NEW)            ③ 자기 트랜잭션 → 여기서 실패해도 매장 저장은 그대로
     기본 비용 분류 생성
     ══ 커밋 ══
 → 201 응답                               → 프론트가 바로 조회해도 비용 분류가 이미 있음
```

- Spring Modulith가 `moa.event_publication` 테이블에 "누가 아직 처리 안 했나"를 적는다
- → 리스너가 실패하거나 처리 중 앱이 죽어도 다시 뜰 때 재실행할 수 있다
- Phase 1은 재실행 설정을 켜 두는 것까지만. 운영 감시는 하지 않는다

**리스너가 실패하면**

```
리스너에서 예외
 → 호출자(store)로 전파되지 않음. 로그에만 남음
 → 매장 등록 응답은 201 · 비용 분류는 빈 상태
 → 발행 기록 "미완료" → 앱 재시작 때 재실행
```

- Phase 1은 이대로 받아들인다. 사용자에게 알리는 흐름은 없다 (**2026-10-27 확정.** 사용자 1명, DB 장애 수준의 드문 경우)
- 발행 기록은 `@ApplicationModuleListener`만이 아니라 `@TransactionalEventListener`가 붙은 리스너 전부가 대상
- 위 세 가지(① 예외가 호출자로 안 감 ② 미완료로 기록 ③ 재발행하면 처리됨)는 프레임워크 동작이라 믿지 않고 **뼈대 첫 테스트로 확인**한다 (0.7 "프레임워크 확인"). 다르게 동작하면 이 절과 D0-3을 고친다

**`@ApplicationModuleListener`를 쓰지 않는 이유**

| | `@TransactionalEventListener` + `REQUIRES_NEW` (MOA) | `@ApplicationModuleListener` |
|---|---|---|
| 소속 | Spring | Spring Modulith |
| 실행 | 커밋 후 · 같은 스레드 (동기) | 커밋 후 · 다른 스레드 (비동기, `@Async`) |
| 응답 직후 조회 | 데이터가 있음 | 아직 없을 수 있음 |
| 테스트 | 바로 확인 | 기다리는 장치(Awaitility) 필요 |

→ 왜: 비동기의 이득(응답이 빨라짐)이 사용자 1명 규모에서는 없다. Phase 2에서 AI 서비스처럼 오래 걸리는 수신자가 생기면 그 리스너만 비동기로 바꾼다. 어노테이션 하나 차이다.

**의존 역전 (category ↔ expense)**

```
문제  category는 "이 분류로 입력된 비용이 몇 건인가"를 알아야 삭제 여부를 정한다
      그런데 expense → category 의존이 이미 있다 → category → expense를 더하면 순환 ✗

해결  인터페이스는 category가 정하고, 구현은 expense가 한다

      CategoryService ──사용──▶ category/CategoryUsage (인터페이스)
                                        ▲
                                        │ implements
                      expense/internal/CategoryUsageImpl

      코드 의존  expense → category 한 방향뿐
      실행 시    Spring이 CategoryUsageImpl 빈을 CategoryService에 넣어 줌
```

```java
// moa/category/CategoryUsage.java  (category 루트)
public interface CategoryUsage {
    Map<Long, Long> countByCategoryIds(Long storeId, Collection<Long> categoryIds);
}

// moa/expense/internal/CategoryUsageImpl.java  (expense 안)
@Component
class CategoryUsageImpl implements CategoryUsage { ... expense 테이블을 센다 ... }
```

| 시기 | `CategoryUsage` 구현 | 왜 |
|---|---|---|
| 2주차 main | `NoExpenseYet` (항상 0건) | 비용 데이터가 실제로 0건이라 사실과 맞음. 앱이 뜨려면 구현체가 하나 있어야 함 |
| 2주차 테스트 | `FakeCategoryUsage` (테스트 소스, 분류 ID별 건수 지정) | "사용된 분류는 삭제 거부" 테스트가 2주차에 성립해야 함 |
| 3주차 | expense의 `CategoryUsageImpl` | **expense 완료 조건: `NoExpenseYet` 삭제** |

### 0.4 데이터베이스 [참고]

```
storeRepository.save(store)     ← MOA 코드는 여기까지만 안다
 → JPA (Hibernate)              INSERT SQL을 만든다
 → DataSource                   미리 열어 둔 DB 연결을 빌려준다
 → JDBC 드라이버                 Java ↔ MySQL 실제 통신
 → MySQL                        테이블은 앱이 뜰 때 Flyway가 만들어 둔다
```

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

→ 왜 스키마를 나누나: 실수로 JOIN을 써도 테이블 이름 앞에 다른 모듈 이름이 붙어 리뷰에서 바로 보인다. Phase 2에서 모듈 하나를 떼어 낼 때 테이블을 그대로 들고 나갈 수 있다.

### 0.5 계층과 트랜잭션, 규칙의 자리 [검토]

```
컨트롤러 (internal)  →  응용 서비스 (루트, 공개)  →  애그리거트 · 리포지토리 (internal)
JSON ↔ 레코드            @Transactional               규칙은 여기
```

**규칙은 두 군데에 산다**

| | 판단 기준 | 예 | 어디서 검사 |
|---|---|---|---|
| **[안]** 애그리거트 안 | 그 객체 하나만 보면 판단됨 | 이름 길이, 하위 아래에 하위 금지, 금액 음수 금지 | 애그리거트 메서드 안. 어기면 예외 |
| **[밖]** 애그리거트 밖 | 다른 객체를 세어 봐야 판단됨 | 형제 이름 중복, 개수 상한, "마지막 하나는 못 끈다" | 응용 서비스가 리포지토리로 세어 보고 검사. 어기면 예외 |

→ 왜 밖에 두나: 밖 규칙까지 안에 넣으려고 애그리거트를 키우지 않는다 (책 3장 "애그리거트 하나 = 트랜잭션 하나"는 지킨다).

**트랜잭션**

| 규칙 | 내용 |
|---|---|
| 범위 | 응용 서비스 메서드 하나 = 트랜잭션 하나 |
| 기본 | 그 안에서 애그리거트 하나만 바꾼다 (책 3장) |
| 예외 (D0-2) | 같은 모듈 안에서 여럿을 바꿔야 하면 한 트랜잭션에 넣는 것을 허용. 예: 상위를 끄면 하위도 → 왜: 사용자 1명·토이 규모. 책의 원칙에서 의도적으로 벗어나는 지점 |
| 모듈을 넘는 변경 | 예외 없이 이벤트 |
| 동시성 | 잠금·버전 관리 없음 (사용자 1명). 이름 중복처럼 DB가 2중으로 막아 줄 수 있는 것만 유니크 인덱스 |

### 0.6 오류 처리 [검토]

```
도메인 · 응용 서비스   예외를 던짐
 → 공통 핸들러          HTTP 응답으로 바꿈 { code, message }
 → 프론트               code를 보고 기획서의 문구를 띄움. 서버는 문구를 모름 (D0-5)
```

```json
{ "code": "CATEGORY_NAME_DUPLICATE", "message": "same name exists among siblings" }
```

- 상태 코드는 HTTP 응답에 실리고, 본문은 `{code, message}`다. 본문에 `status`는 넣지 않는다 (2026-10-27)
- 프론트는 공통 처리를 상태 코드로, 화면 문구를 `code`로 한다
- `message`는 개발자용이라 화면에 띄우지 않는다. 화면 문구는 `code`와 동작을 보고 프론트가 정한다 (예: 같은 `CATEGORY_NAME_DUPLICATE`라도 추가와 이동의 문구가 다름, 기획서 2.4·2.5)

**HTTP 상태 코드**

| HTTP | 뜻 | 예 |
|---|---|---|
| 400 | 요청 형식·입력 규칙 위반 | 빈 이름, 길이 초과, 음수, 숫자 자리에 글자 |
| 404 | 주소가 가리키는 대상이 없음 (형식은 맞음). "페이지 없음"만이 아니라 "자원 없음" | `/api/stores/99/...`의 99번 매장, 없는 분류 ID |
| 409 | 형식은 맞지만 지금 상태와 충돌 (업무 규칙 위반) | 이름 중복, 개수 상한, 사용 중 삭제 |
| 500 | 서버 내부 오류 | 로그에 스택, 응답에는 코드만 |

**오류 코드 전체 목록** (모듈 장을 새로 쓰면 그 모듈의 코드를 여기에 더한다)

| 코드 | HTTP | 모듈 | 언제 | 나오는 곳 |
|---|---|---|---|---|
| `VALIDATION` | 400 | 공통 | 입력 형식 위반. 응답에 항목명 포함 | 모든 입력 |
| `NOT_FOUND` | 404 | 공통 | 없는 ID | 조회·수정·삭제 |
| `INTERNAL_ERROR` | 500 | 공통 | 그 외 서버 오류 (2026-10-27 이름 정함) | — |
| `STORE_NOT_FOUND` | 404 | store | 매장 접근 검사에서 매장 없음. `NOT_FOUND`와 일부러 구분 → 프론트는 매장 등록 화면으로 보냄 (기획서 1장 "매장이 없으면 어느 주소로 들어와도 등록 화면") | 1.2 매장 접근 검사 |
| `STORE_ALREADY_EXISTS` | 409 | store | 매장이 이미 있는데 또 등록 (Phase 1은 1개) | 1.2 매장 등록 |
| `CATEGORY_NAME_DUPLICATE` | 409 | category | 형제 중에 같은 이름 (꺼진 것 포함) | 2.2 분류 추가 · 이름 변경 · 이동 |
| `CATEGORY_LIMIT` | 409 | category | 상위 20개, 또는 상위당 하위 20개를 넘김 | 2.2 분류 추가 · 이동 |
| `CATEGORY_DEPTH` | 409 | category | 하위 아래에 추가 (2단계 초과) | 2.2 분류 추가 |
| `CATEGORY_PARENT_INACTIVE` | 409 | category | 상위가 꺼진 하위를 켬 | 2.2 분류 활성화 |
| `CATEGORY_LAST_ACTIVE` | 409 | category | 마지막 켜진 상위를 끄거나 삭제 | 2.2 분류 비활성화 · 삭제 |
| `CATEGORY_HAS_CHILDREN` | 409 | category | 하위가 있는 상위를 삭제하거나 이동 | 2.2 분류 삭제 · 이동 |
| `CATEGORY_IN_USE` | 409 | category | 누적 비용 1건 이상인 분류를 삭제 | 2.2 분류 삭제 |

- 분류 이동(선택)의 "상위를 옮기려 함" 거부 코드는 구현 때 정해 이 표에 더한다
- 각 장의 흐름 그림과 N.7 HTTP 표에도 코드를 그대로 적는다. 이 표는 "코드의 뜻", 각 장은 "어디서 나오나"

### 0.7 테스트 전략 [참고]

| 층 | 무엇을 | 도구 | 속도 |
|---|---|---|---|
| 단위 | 애그리거트 메서드의 규칙 | JUnit 5 + AssertJ. DB 없음, Spring 없음 | ms |
| 통합 (모듈 하나) | 응용 서비스 한 기능 = 요청 하나. 저장·조회·이벤트 발행·리스너 처리까지 | `@ApplicationModuleTest` + Testcontainers MySQL. 다른 모듈은 뜨지 않고, 필요한 상대 인터페이스는 테스트 소스의 가짜로 (예: `FakeCategoryUsage`는 분류 ID별 건수를 지정). 리스너가 동기라 대기 장치(Awaitility) 불필요 | 초 |
| 경계 | 모듈 간 import·순환 | `ApplicationModules.of(MoaApplication.class).verify()` 하나 | 초 |
| HTTP | 컨트롤러의 JSON 변환과 상태 코드 | `@WebMvcTest`, 서비스는 Mock. 기능당 정상 1개 + 오류 1개만. 매장 접근 검사는 테스트 소스의 가짜 컨트롤러(`/api/stores/{storeId}/ping`)와 인터셉터 등록 설정을 `@Import`한 슬라이스에서 | ms |
| 프레임워크 확인 | 0.3의 리스너 동작 세 가지 (예외 비전파, 미완료 기록, 재발행) | 뼈대 작업 첫 테스트. 일부러 실패하는 리스너를 테스트 소스에 두고 확인. 기능 테스트가 아니라 설계 전제의 확인 | 초 |

**TDD 순서**

```
검증 기준 한 줄 → 테스트 이름 쓰기 → 실패 확인 (빨강) → 통과하는 최소 코드 (초록) → 정리 → 커밋
                                                                          기능 하나 = 커밋 하나
```

- 단위 = 단위 테스트 (객체만). 통합 = 모듈 하나만 띄운 통합 테스트 (Spring + 진짜 MySQL). HTTP = 컨트롤러만 띄운 슬라이스 테스트
- 테스트 이름은 `@DisplayName("기능 이름 - 상황")`. 기획서 검증 기준의 "기능 | 상황" 두 칸과 같게 쓴다
- 검증 기준 중 화면만의 것(버튼 비활성, 토스트, 포커스)은 백엔드 테스트가 없고 프론트 테스트(Vitest)로 간다

### 0.8 공통 값 객체 [참고]

| 값 객체 | 내용 | 규칙 |
|---|---|---|
| `Money` | `long amount` (원) | 음수 불가. `plus`, `minus`(결과 음수 허용은 호출자가 결정), `isZero`. JPA에서는 `@Embeddable`, 컬럼 `DECIMAL(15,0)` |

- `YearMonth`와 `LocalDate`는 Java 표준을 그대로 쓴다
- ID는 `Long`. StoreId 같은 래퍼는 두지 않는다 → 왜: 토이 규모에서 얻는 것보다 JPA 매핑 부담이 큼

### 0.9 결정 사항 [참고]

D0-3은 2026-10-29에 사용자가 리스너 확인 테스트 결과와 함께 확인한다 (muse-feedback 결정 3).

| # | 결정 | 결론 | 이유 |
|---|---|---|---|
| D0-1 | 공개 API의 형태 | Java 인터페이스가 아니라 **공개 클래스 `XxxService`** 하나. 다른 모듈은 이 빈을 주입받아 호출 | 인터페이스+구현 분리는 구현이 둘 이상일 때 가치가 있음. Phase 1에는 없음. 역전이 필요한 곳(0.3)만 인터페이스 |
| D0-2 | 트랜잭션 하나에 애그리거트 여러 개 | 같은 모듈 안에서는 허용 (상위 끄면 하위도, 템플릿 생성) | 사용자 1명·토이 규모에서 이벤트로 쪼개면 복잡함만 늚. 모듈을 넘는 변경은 예외 없이 이벤트 |
| D0-3 | 이벤트 리스너 방식 | **변경 (2026-10-25)** `@TransactionalEventListener` + `REQUIRES_NEW` (커밋 후·**동기**·별도 트랜잭션), 재발행 켬. 이전: `@ApplicationModuleListener`(비동기) | Muse 리뷰. 비동기는 등록 직후 조회 경합과 테스트 대기 장치를 만들고 사용자 1명 규모에서 이득이 없음. 커밋 후·별도 트랜잭션이라 모듈 독립성은 그대로. Phase 2에서 오래 걸리는 수신자만 비동기로 |
| D0-4 | 공통 스키마 `moa` | Flyway 이력표와 Modulith 발행표를 둠 | 프레임워크 테이블이 특정 모듈 스키마에 섞이지 않게 |
| D0-5 | 오류 응답 | `code` 중심. 문구는 프론트. 본문에 `status` 없음 (2026-10-27) | 서버가 문구를 가지면 기획서 문구 변경 때 배포가 필요. 상태 코드는 HTTP 응답에서 이미 꺼낼 수 있음 |
| D0-6 | 현재 시각 | `Clock` 빈 하나 | 테스트에서 "오늘"을 고정하려면 주입 가능해야 함 |
| D0-7 | Lombok | 쓰지 않음. 엔티티는 생성자·변경 메서드·getter를 손으로 쓰고, DTO·이벤트·밸류는 Java record | `@Setter`·`@Data`는 1.4·2.4의 "setter 없음"을 한 줄로 무너뜨림. 필드가 적어 손으로 써도 부담 없고, 규칙 검사가 어디 있는지 보이는 것이 학습에 중요. record가 DTO 용도를 대신함 (2026-10-24) |

---

## 1. store 모듈

### 1.1 한눈에 [검토]

```
                  StoreRegistered (커밋 후)
store ────────┬──────────────────────────▶ category   기본 비용 분류 생성
              └──────────────────────────▶ sales      기본 결제수단 생성

/api/stores/{storeId}/** 요청 ──▶ store의 매장 접근 검사 ──▶ 각 모듈의 컨트롤러
```

| 항목 | 내용 |
|---|---|
| 기능 | 매장 등록 · 매장 정보 수정 · 매장 조회 · (화면 없음) 매장 접근 검사 |
| 수신 이벤트 | 없음 |
| 발행 이벤트 | `StoreRegistered` |
| 다른 모듈 호출 | 없음 |
| 다른 모듈이 store를 | 호출하지 않음. 이벤트만 수신함. 단, 다른 모듈의 HTTP 요청(`/api/stores/{storeId}/...`)은 컨트롤러에 닿기 전에 store의 인터셉터가 그 매장이 있는지만 먼저 확인함 (1.2 매장 접근 검사) |
| 스키마 | `store` |

### 1.2 기능 흐름 [검토]

**매장 등록** `POST /api/stores {name, type}`

```
StoreService.register
 ├ 매장이 이미 1개 이상?                    ✗ 409 STORE_ALREADY_EXISTS   [밖]
 └ Store.register(name, type, 오늘)
     ├ 앞뒤 공백 제거 → 비었거나 50자 초과?   ✗ 400 VALIDATION            [안]
     └ 등록일 = 서버의 오늘 (Asia/Seoul, Clock 빈)
 → 저장
 ══ 커밋 ══
 → StoreRegistered(storeId, type, registeredAt)    같은 스레드에서 이어서 (0.3)
     ├ category: 기본 비용 분류 16개 (상위 7 + 하위 9)
     └ sales: 기본 결제수단 4개
 → 201 StoreInfo
```

**매장 정보 수정** `PUT /api/stores/{id} {name, type}`

```
StoreService.update
 ├ 매장 없음?                               ✗ 404 NOT_FOUND
 └ store.update(name, type)
     └ 이름 규칙 (등록과 같음)                ✗ 400 VALIDATION            [안]
 ══ 커밋 ══  (바뀐 필드가 저장됨)
 → 200 StoreInfo
```

- 업종이 바뀌어도 이벤트 없음 · 기본 비용 분류 그대로 · 등록일 그대로 (D1-4)

**매장 조회**

```
GET /api/stores        → findAll()  → 200 [StoreInfo]   0개 또는 1개. 프론트가 "매장이 있나"를 판단
GET /api/stores/{id}   → get(id)    → 200 StoreInfo     없으면 ✗ 404 NOT_FOUND
```

**매장 접근 검사** (2026-10-25 추가. 화면 없음)

```
/api/stores/{storeId}/... 요청 (category · sales 등 다른 모듈의 경로 전부)
 → StoreAccessInterceptor (store 모듈의 Spring MVC 인터셉터)
     ├ storeId의 매장 없음?   ✗ 404 STORE_NOT_FOUND → 프론트가 매장 등록 화면으로
     └ 있음                  ✓ 통과
 → 그 모듈의 컨트롤러          storeId를 다시 검증하지 않음 ("검사를 지나온 값")
```

- 인터셉터 = 컨트롤러 앞에서 먼저 도는 검사. 프론트의 라우트 미들웨어(Nuxt `middleware`)와 같은 자리
- 애그리거트 루트와는 상관없다. Store 객체를 꺼내지 않고 "있나"만 본다

| 왜 이렇게 | |
|---|---|
| 한 곳에서 검사 | 모듈마다 store를 호출하면 모든 모듈이 store에 의존하게 됨 |
| 막는 것 | 없는 매장으로 데이터가 생기는 일. "매장 없음"과 "분류 없음"이 섞이는 일 |
| Phase 2 | "이 매장이 로그인한 계정 것인가" 검사가 들어갈 자리 |
| 이벤트로 온 storeId | 검사하지 않음. store가 직접 발행한 값 |

### 1.3 규칙 [검토]

| 규칙 | 안/밖 | 어기면 | 근거 |
|---|---|---|---|
| 매장 이름 필수, 1~50자. 앞뒤 공백 제거 후 검사. 공백만 = 빈 값 | 안 (`Store`) | 400 `VALIDATION` | 기획서 1.4 |
| 업종은 카페·음식점·소매·기타 중 하나. 필수 | 안 (enum `StoreType`) | 400 `VALIDATION` | 기획서 1.4 |
| 등록일 = 등록할 때 서버의 오늘. 바뀌지 않음 | 안 (바꾸는 메서드 없음) | — | D1-2 |
| 시스템 전체에 매장 1개 (Phase 1) | 밖 (`StoreService.register` 첫 줄 `count() >= 1`) | 409 `STORE_ALREADY_EXISTS` | 요구사항정의서 8.1, D1-1 |
| 업종을 바꿔도 다른 곳에 영향 없음 | — | — | D1-4 |

- 매장 1개 규칙은 Phase 2에서 세는 범위만 "계정별"로 바뀐다. DB 유니크 제약은 걸지 않는다
- → 왜 밖: "매장이 몇 개인가"는 Store 하나만 봐서는 알 수 없다 (0.5)

### 1.4 애그리거트: Store [검토]

| 필드 | 타입 | 규칙 |
|---|---|---|
| `id` | Long | DB 생성 |
| `name` | String | 필수. 1~50자. 앞뒤 공백 제거 후 검사. 공백만은 비어 있는 것 |
| `type` | `StoreType` (CAFE, RESTAURANT, RETAIL, OTHER) | 필수 |
| `registeredAt` | LocalDate | 등록 시 서버 날짜. 바뀌지 않음 |

| 메서드 | 하는 일 | 규칙 검사 |
|---|---|---|
| `Store.register(name, type, today)` (정적) | 새 매장 생성 | 이름 규칙 |
| `update(name, type)` | 이름·업종 변경 | 이름 규칙. 업종은 바뀌어도 아무 일 없음 |

- setter 없음. `registeredAt`을 바꾸는 메서드 없음
- 등록 제한(Phase 1 전체 1개)은 이 객체가 모르는 규칙 → 밖 (1.3)

### 1.5 테스트 목록 [검토]

기획서 1장 검증 기준 18건 중 백엔드가 맡는 것. 나머지(버튼 비활성, 글자 수, 토스트, 리다이렉트, 가이드)는 프론트.

| 층 | `@DisplayName` |
|---|---|
| 단위 | 매장 등록 - 정상 등록 |
| 단위 | 매장 등록 - 이름 비움 → 거부 |
| 단위 | 매장 등록 - 공백만 입력 → 거부 |
| 단위 | 매장 등록 - 앞뒤 공백 → 제거되어 저장 |
| 단위 | 매장 등록 - 50자 초과 → 거부 (50자는 허용) |
| 단위 | 매장 정보 수정 - 이름 변경 |
| 단위 | 매장 정보 수정 - 업종 변경 → 등록일은 그대로 |
| 통합 | 매장 등록 - 저장되고 StoreRegistered 발행 |
| 통합 | 매장 등록 - 이미 매장 있음 → STORE_ALREADY_EXISTS |
| 통합 | 매장 조회 - 이름·업종·등록일 반환 |
| 통합 | 매장 조회 - 없는 ID → NOT_FOUND |
| 통합 | 매장 정보 수정 - 변경 후 조회에 반영 |
| HTTP | 매장 등록 - 201과 본문 |
| HTTP | 매장 등록 - 빈 이름 → 400 VALIDATION |
| HTTP | 매장 조회 - 없는 ID → 404 NOT_FOUND (store 자기 경로는 접근 검사에서 제외, 1.7) |
| HTTP | 매장 접근 검사 - 없는 storeId 하위 경로(가짜 컨트롤러) → 404 STORE_NOT_FOUND |
| HTTP | 매장 접근 검사 - 있는 storeId → 통과 |

### 1.6 결정 사항 [검토]

| # | 결정 | 결론 | 이유 |
|---|---|---|---|
| D1-1 | 등록 제한의 자리 | 응용 서비스 한 줄. 모델·DB에 없음 | 요구사항정의서 8.1. Phase 2에서 바뀌는 코드가 이 줄뿐이어야 함 |
| D1-2 | 등록일 | 서버의 Asia/Seoul 날짜 (`Clock`) | 클라이언트 시계를 믿지 않음 |
| D1-3 | 수정 API | 이름·업종을 한 번에 PUT | 항목이 둘뿐. 나눌 이유 없음 |
| D1-4 | 업종 변경 이벤트 | 발행하지 않음 | 수신하는 모듈이 없음 (Phase 1에서 업종에 반응하는 모듈 없음. 기본 비용 분류는 등록 때 한 번만). 필요해지면 그때 추가 |
| D1-5 | storeId 검증 | **2026-10-25 추가** store 모듈의 웹 인터셉터 한 곳에서 (1.2 매장 접근 검사). 다른 모듈은 검증하지 않음 | Muse 리뷰("없는 storeId로 고아 데이터"). 모듈마다 `exists()`를 부르면 모든 모듈이 store에 의존하게 됨. 접근 검사는 Phase 2 로그인 검사와 같은 자리라 한 곳이 맞음 |

### 1.7 공개 API·이벤트·HTTP [참고]

**공개 API: StoreService**

| 메서드 | 입력 | 출력 | 트랜잭션 |
|---|---|---|---|
| `register(name, type)` | String, StoreType | `StoreInfo` | 쓰기. 커밋 후 `StoreRegistered` |
| `update(storeId, name, type)` | Long, String, StoreType | `StoreInfo` | 쓰기 |
| `get(storeId)` | Long | `StoreInfo` | 읽기. 없으면 `NOT_FOUND` |
| `findAll()` | — | `List<StoreInfo>` | 읽기. Phase 1에서 프론트가 "매장이 있는가"를 판단하는 데 씀 (0개 또는 1개) |

`StoreInfo(id, name, type, registeredAt)` 레코드.

**이벤트**

| 이벤트 | 내용 | 언제 | 수신 모듈 |
|---|---|---|---|
| `StoreRegistered` | `storeId`, `type`, `registeredAt` | 등록 커밋 후 | category(기본 분류 생성), sales(기본 결제수단 생성) |

**HTTP**

| 메서드 | 경로 | 요청 | 응답 | 오류 코드 |
|---|---|---|---|---|
| POST | `/api/stores` | `{name, type}` | 201 + StoreInfo | `VALIDATION`, `STORE_ALREADY_EXISTS` (409) |
| GET | `/api/stores` | — | 200 + `[StoreInfo]` | — |
| GET | `/api/stores/{id}` | — | 200 + StoreInfo | `NOT_FOUND` |
| PUT | `/api/stores/{id}` | `{name, type}` | 200 + StoreInfo | `VALIDATION`, `NOT_FOUND` |

- 인터셉터 패턴 `/api/stores/{storeId}/**`의 `**`는 경로 0개도 포함하므로 `/api/stores/{id}` 자체에도 걸린다. store 자기 경로는 인터셉터에서 제외해 위 표의 `NOT_FOUND`를 유지한다 (2026-10-27 추가. HTTP 테스트로 확인)

### 1.8 테이블·파일 [참고]

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

```
moa/store/
├─ StoreService.java        공개 API
├─ StoreInfo.java           레코드
├─ StoreType.java           enum (공개. category·sales가 이벤트에서 봄)
├─ StoreEvents.java         StoreRegistered 레코드
└─ internal/
    ├─ Store.java           애그리거트
    ├─ StoreRepository.java Spring Data
    ├─ StoreController.java
    └─ StoreAccessInterceptor.java  /api/stores/{storeId}/** 매장 존재 검사 (Phase 2 로그인 검사 자리)
```

---

## 2. category 모듈

### 2.1 한눈에 [검토]

```
store ── StoreRegistered ──▶ category        기본 비용 분류 생성
expense ── isActive · infoOf ──▶ category    비용 등록 때 "켜진 분류인가", 비용 목록의 분류 경로
report ── infoOf ──▶ category                분류별 집계의 이름·경로

category ── CategoryUsage (인터페이스) ◀── expense가 구현    삭제 때 "누적 비용 몇 건" (0.3 의존 역전)
```

| 기능 | 우선순위 | 화면 |
|---|---|---|
| 기본 분류 생성 | 필수 | 없음 (매장 등록 때 자동) |
| 분류 추가 · 분류 이름 변경 · 분류 비활성화 · 분류 활성화 · 분류 삭제 · 분류 tree 조회 | 필수 | 비용 분류 설정 |
| 분류 존재 확인 | 필수 | 없음 (expense가 호출) |
| 분류 이동 · 분류 tree Markdown 내보내기 | 선택 | 비용 분류 설정 |

| 항목 | 내용 |
|---|---|
| 수신 이벤트 | `StoreRegistered` → 기본 분류 생성 |
| 발행 이벤트 | `DefaultCategoriesCreated`, `CategoryAdded`, `CategoryRenamed`, `CategoryMoved`, `CategoryDeactivated` (Phase 1 수신자 없음. Phase 2 AI) |
| 다른 모듈 호출 | 없음. 비용 건수만 `CategoryUsage` 인터페이스로 받음 |
| 스키마 | `category` |

### 2.2 기능 흐름 [검토]

**기본 분류 생성** (`StoreRegistered` 수신. 화면 없음)

템플릿 = `CategoryTemplate` 코드 상수. Phase 1은 업종과 무관하게 카페 템플릿 하나 (기획서 P1-3). 업종별은 Phase 2.

```
식자재 > 원두, 유제품, 베이커리, 기타 식자재
인건비 > 급여, 주휴·수당
임대료
공과금 > 전기, 수도·가스, 통신
마케팅
소모품
기타
```

```
StoreRegisteredListener (커밋 후 · 같은 스레드 · 새 트랜잭션, 0.3)
 ├ 적용 기록에 이 storeId 있음?             → 아무것도 안 함 (두 번째 수신)
 └ 템플릿 노드마다 형제 이름을 먼저 확인
     ├ 같은 이름의 상위 있음 (꺼진 것 포함)   → 새로 안 만들고 그것을 상위로 씀
     │                                      그 상위가 꺼져 있으면 넣는 하위도 꺼진 상태
     ├ 같은 이름의 하위 있음                 → 건너뜀
     ├ 개수 상한(20)에 걸림                  → 그 노드부터 건너뜀
     └ 없음                                → 생성
 → 적용 기록 한 줄 (같은 트랜잭션)
 ══ 커밋 ══
 → DefaultCategoriesCreated(storeId, 개수)
```

| 장치 | 막는 것 |
|---|---|
| 적용 기록 (`template_applications`, `store_id` PK) | 이벤트가 재발행될 때(0.3) 두 번 생성. 동시에 두 번 들어와도 두 번째는 PK 충돌로 통째로 되돌아감 |
| 이름 합치기 (2026-10-25 추가) | 템플릿 실패 → 사용자가 비용 분류를 먼저 만듦 → 재실행 때 형제 이름 중복. 상위 이름 중복은 DB도 못 막음 (2.8) |

- 이전 안 "비용 분류가 하나라도 있으면 무시"는 폐기 (2026-10-25). 실패 뒤 사용자가 하나만 추가해도 템플릿이 영영 안 생김
- 실패하면 매장 등록은 201, 비용 분류는 빈 채로 → 재시작 때 재실행 (0.3, 2026-10-27 확정)
- 적용 기록은 애그리거트가 아니라 리스너의 처리 기록 → 엔티티 하나로 간단히

**분류 추가** `POST .../categories {parentId?, name}`

```
CategoryService.add
 ├ parentId 없음 (상위 추가)
 │   └ 상위가 이미 20개?                     ✗ 409 CATEGORY_LIMIT           [밖]
 ├ parentId 있음 (하위 추가)
 │   ├ 그 상위가 없음?                       ✗ 404 NOT_FOUND
 │   ├ 그 상위가 하위 분류?                   ✗ 409 CATEGORY_DEPTH           [밖]
 │   └ 그 상위의 하위가 이미 20개?             ✗ 409 CATEGORY_LIMIT           [밖]
 ├ Category.top(...) / Category.childOf(parent, ...)
 │   └ 앞뒤 공백 제거 → 비었거나 20자 초과?    ✗ 400 VALIDATION              [안]
 ├ 형제 중에 같은 이름 (꺼진 것 포함)?         ✗ 409 CATEGORY_NAME_DUPLICATE  [밖]
 └ 형제 끝 순서로 저장
 ══ 커밋 ══
 → CategoryAdded → 201 CategoryInfo
```

- 형제 이름 비교는 공백을 제거한 이름으로 한다
- 상위와 하위는 다른 집합이라 같은 이름이어도 된다 ("식자재 > 기타"와 상위 "기타")

**분류 이름 변경** `PATCH .../{id}/name {name}`

```
CategoryService.rename
 ├ 분류 없음?                               ✗ 404 NOT_FOUND
 ├ 앞뒤 공백 제거 → 비었거나 20자 초과?        ✗ 400 VALIDATION              [안]
 ├ 지금 이름과 같음?                          → 아무 일 없음. 이벤트 없음 → 200  [안]
 ├ 형제 중에 같은 이름 (꺼진 것 포함)?         ✗ 409 CATEGORY_NAME_DUPLICATE  [밖]
 └ category.rename(name)
 ══ 커밋 ══
 → CategoryRenamed(oldName, newName) → 200 CategoryInfo
```

- 비용은 분류 ID를 가리키므로, 이름을 바꾸면 비용 쪽 분류 표시도 함께 바뀐다 (기획서 2.5)

**분류 비활성화** `POST .../{id}/deactivate`

```
CategoryService.deactivate
 ├ 분류 없음?                               ✗ 404 NOT_FOUND
 ├ 켜진 상위가 이것 하나뿐?                    ✗ 409 CATEGORY_LAST_ACTIVE     [밖]
 ├ 이미 꺼짐?                                → 아무 일 없음                  [안]
 └ category.deactivate()
     └ 상위면 하위도 전부 deactivate()  (같은 트랜잭션, D0-2)                  [밖]
 ══ 커밋 ══
 → CategoryDeactivated(꺼진 ID 전부) → 200 {deactivatedIds}
```

- 꺼도 이미 입력된 비용은 그대로. 새 비용 입력 선택지에서만 빠진다 (기획서 2.5)

**분류 활성화** `POST .../{id}/activate`

```
CategoryService.activate
 ├ 분류 없음?                               ✗ 404 NOT_FOUND
 ├ 하위인데 상위가 꺼짐?                       ✗ 409 CATEGORY_PARENT_INACTIVE [밖]
 └ category.activate()   (이미 켜져 있으면 아무 일 없음)                        [안]
 ══ 커밋 ══
 → 200 CategoryInfo      이벤트 없음 (D2-6)
```

- 상위를 켜도 하위는 꺼진 채로 남는다. 하위는 하나씩 켠다

**분류 삭제** `DELETE .../{id}`

```
CategoryService.delete
 ├ 분류 없음?                               ✗ 404 NOT_FOUND
 ├ 상위인데 하위가 있음?                       ✗ 409 CATEGORY_HAS_CHILDREN    [밖]
 ├ 누적 비용 1건 이상? (CategoryUsage)         ✗ 409 CATEGORY_IN_USE          [밖]
 ├ 켜진 상위가 이것 하나뿐?                    ✗ 409 CATEGORY_LAST_ACTIVE     [밖]
 └ 삭제
 ══ 커밋 ══
 → 204                  이벤트 없음 (D2-6)
```

- 기본 템플릿으로 만든 분류도 같은 규칙. 0건이면 삭제할 수 있다

**분류 tree 조회** `GET .../categories?includeInactive=false`

```
CategoryService.tree(storeId, includeInactive)
 → 비용 분류 전부 조회 (includeInactive=false면 꺼진 것 제외)
 → 누적 비용 건수를 한 번에 (CategoryUsage.countByCategoryIds)
 → 상위 → 그 하위 순서. 형제 안은 만든 순서
 → 200 [CategoryInfo]   항목마다 경로 "식자재 > 유제품", 켜짐 여부, expenseCount
```

- 끄기·삭제 확인창의 "누적 비용 N건"은 여기서 받은 `expenseCount`를 쓴다. 따로 호출하지 않는다

**분류 존재 확인** (화면 없음. expense가 비용 등록·재분류 때 호출)

```
categoryService.isActive(storeId, categoryId)
 ├ 켜진 분류          → true
 └ 꺼진 분류 · 없는 분류 → false
```

**분류 이동** (선택) `PATCH .../{id}/parent {parentId}`

```
CategoryService.move
 ├ 분류 없음 · 새 상위 없음?                  ✗ 404 NOT_FOUND
 ├ 옮기려는 게 하위 있는 상위?                 ✗ 409 CATEGORY_HAS_CHILDREN    [밖]
 ├ category.moveTo(newParent)
 │   ├ 자신이 상위?                          ✗ 거부 (코드는 구현 때)          [안]
 │   └ 새 상위가 하위 분류?                   ✗ 거부 (코드는 구현 때)          [안]
 ├ 새 상위의 하위가 이미 20개?                 ✗ 409 CATEGORY_LIMIT           [밖]
 └ 새 상위 아래에 같은 이름?                   ✗ 409 CATEGORY_NAME_DUPLICATE  [밖]
 → 새 상위 아래 끝 순서
 ══ 커밋 ══
 → CategoryMoved → 200 CategoryInfo
```

- 옮긴 분류로 입력된 비용은 분류 ID가 그대로라 영향 없다

**분류 tree Markdown 내보내기** (선택) `GET .../categories/export.md`

```
켜진 비용 분류만 → Markdown 문자열 → 200 text/markdown (파일 내려받기)
```

형식은 요구사항정의서 5.2와 같은 모양. Phase 2 AI 기학습 자료가 된다.

```
식자재
├─ 원두
├─ 유제품
└─ 기타 식자재
임대료
```

- **확인 필요 (2026-10-27):** 기획서 2.5는 목록 형식(`- 식자재` / `  - 원두`)으로 적혀 있어 위와 다르다. 구현 전에 하나로 정한다

### 2.3 규칙 [검토]

| 규칙 | 안/밖 | 어기면 | 근거 |
|---|---|---|---|
| 이름 필수, 1~20자. 앞뒤 공백 제거. 공백만 불가 | 안 | 400 `VALIDATION` | 기획서 2.4, P2-1 |
| 2단계까지. 하위 아래에는 추가 불가 | 안 (`childOf`) + 밖 (상위 조회 후 `isTop()`) | 409 `CATEGORY_DEPTH` | 기획서 2.4 |
| 같은 이름으로 이름 변경 → 아무 일 없음 (이벤트도 없음) | 안 | — | 기획서 2.5 |
| 꺼진 걸 끄기 · 켜진 걸 켜기 → 아무 일 없음 | 안 | — | — |
| 상위는 옮길 수 없음 (선택) | 안 (`moveTo`) | 거부 | 기획서 2.5 옮기기 |
| 형제 간 이름 중복 불가. 꺼진 분류 포함. 상위끼리는 전체가 형제 | 밖 | 409 `CATEGORY_NAME_DUPLICATE` | 기획서 2.4, P2-5, P2-6 |
| 상위 20개, 상위당 하위 20개. 꺼진 것 포함 | 밖 | 409 `CATEGORY_LIMIT` | 기획서 2.4, P2-8 |
| 상위를 끄면 하위도 함께 꺼짐 (같은 트랜잭션) | 밖 | — | 기획서 2.5 끄기, D2-7 |
| 하위를 켜려면 상위가 켜져 있어야 함 | 밖 | 409 `CATEGORY_PARENT_INACTIVE` | 기획서 2.5 켜기 |
| 상위를 켜도 하위는 그대로 | 밖 | — | 기획서 2.5 켜기 |
| 켜진 상위 분류는 항상 1개 이상 | 밖 | 409 `CATEGORY_LAST_ACTIVE` | 기획서 2.4, P2-9 |
| 하위가 있는 상위는 삭제 불가 | 밖 | 409 `CATEGORY_HAS_CHILDREN` | 기획서 2.5 삭제, P2-7 |
| 누적 비용 1건 이상이면 삭제 불가 (끄기만) | 밖 (`CategoryUsage`) | 409 `CATEGORY_IN_USE` | 기획서 2.1, P2-7 |
| 하위가 있는 상위는 이동 불가 (선택) | 밖 | 409 `CATEGORY_HAS_CHILDREN` | 기획서 2.5 옮기기 |
| 기본 템플릿 분류도 위 규칙 그대로 | — | — | P2-7 |

- 리포지토리로 어떻게 세는지는 2.7 "리포지토리 메서드"

### 2.4 애그리거트: Category (노드 하나) [검토]

먼저 "무엇을 애그리거트로 볼 것인가"를 정해야 했다.

| 선택지 | 묶음 | 장점 | 단점 |
|---|---|---|---|
| **A. 노드 하나** | 분류 하나가 애그리거트. 상위는 `parentId`로 가리킴 | JPA 매핑이 가장 단순 (연관관계 없음). 조회·존재 확인이 가벼움 | 형제 중복·개수·최소 하나 같은 규칙이 애그리거트 밖(응용 서비스)으로 나감 |
| B. tree 전체 | 매장의 분류 tree 전체가 애그리거트 하나 | 모든 규칙이 루트 안에 들어감. 책 3장의 모양 그대로 | `@OneToMany` 두 단계, 삭제에 orphanRemoval(cascade 계열) 필요 → JPA 최소 사용 규칙과 충돌. 하위 하나 고칠 때도 420개를 통째로 읽음 |

**A를 채택한다.**
- 규칙을 밖에 두는 비용 < JPA 복잡도를 피하는 이득. 11/02 JPA 체크포인트를 넘기기 쉬운 쪽
- 규칙이 밖으로 나가는 것 = 책 3장 끝의 "애그리거트 경계를 넘는 규칙은 응용 서비스나 도메인 서비스에서"
- 예산 모듈(4주차)에서 B 모양(월 예산 + 항목 목록)을 보게 되므로 두 모양을 다 겪는다

| 필드 | 타입 | 규칙 |
|---|---|---|
| `id` | Long | DB 생성. expense가 이 값을 `categoryId`로 보관 |
| `storeId` | Long | ID 참조 |
| `parentId` | Long, null 가능 | null이면 상위 분류, 아니면 그 상위의 하위 |
| `name` | String | 필수. 1~20자. 앞뒤 공백 제거. 공백만 불가 |
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

- setter 없음. 상태는 위 메서드로만 바뀐다

### 2.5 테스트 목록 [검토]

기획서 2장 검증 기준 36건 중 백엔드가 맡는 것. 접기 상태, 입력란, 버튼 비활성, 확인창은 프론트.

| 층 | `@DisplayName` |
|---|---|
| 단위 | 분류 추가 - 상위 생성, parentId 없음 |
| 단위 | 분류 추가 - 하위 생성, 상위를 가리킴 |
| 단위 | 분류 추가 - 하위 아래 추가 → 거부 (2단계 초과) |
| 단위 | 분류 추가 - 빈 이름 → 거부 |
| 단위 | 분류 추가 - 21자 → 거부 (20자는 허용) |
| 단위 | 분류 이름 변경 - 앞뒤 공백 제거 |
| 단위 | 분류 이름 변경 - 같은 이름 → 변경 없음 |
| 단위 | 분류 비활성화 - 켜짐에서 꺼짐 |
| 단위 | 분류 활성화 - 꺼짐에서 켜짐 |
| 단위 | 분류 이동 - 상위를 옮기려 함 → 거부 |
| 통합 | 기본 분류 생성 - StoreRegistered 수신 → 상위 7, 하위 9 생성, DefaultCategoriesCreated 발행 |
| 통합 | 기본 분류 생성 - 다른 업종 → 같은 템플릿 |
| 통합 | 기본 분류 생성 - 두 번 수신 → 두 번째는 무시 (적용 기록 있음) |
| 통합 | 기본 분류 생성 - 사용자가 분류를 먼저 추가했어도 적용 기록이 없으면 템플릿 생성 |
| 통합 | 기본 분류 생성 - 사용자가 "식자재"를 먼저 만들었으면 새로 만들지 않고 그 아래에 템플릿 하위를 넣음 (중복 없음) |
| 통합 | 기본 분류 생성 - 합칠 "식자재"가 꺼져 있으면 넣는 하위도 꺼진 상태 |
| 통합 | 분류 추가 - 상위 추가 → tree 끝에, CategoryAdded 발행 |
| 통합 | 분류 추가 - 하위 추가 → 그 상위 아래 끝에, 경로 "식자재 > 시럽" |
| 통합 | 분류 추가 - 형제 중복 → CATEGORY_NAME_DUPLICATE |
| 통합 | 분류 추가 - 다른 상위와는 중복 허용 |
| 통합 | 분류 추가 - 꺼진 분류와 중복 → CATEGORY_NAME_DUPLICATE |
| 통합 | 분류 추가 - 상위 20개 → CATEGORY_LIMIT |
| 통합 | 분류 추가 - 하위 20개 → CATEGORY_LIMIT |
| 통합 | 분류 이름 변경 - 정상, CategoryRenamed 발행 |
| 통합 | 분류 이름 변경 - 중복 → CATEGORY_NAME_DUPLICATE |
| 통합 | 분류 비활성화 - 하위 없는 상위 → 꺼짐, CategoryDeactivated 발행 |
| 통합 | 분류 비활성화 - 하위 있는 상위 → 하위도 함께 꺼짐 |
| 통합 | 분류 비활성화 - 하위만 → 상위·형제는 그대로 |
| 통합 | 분류 비활성화 - 마지막 상위 → CATEGORY_LAST_ACTIVE |
| 통합 | 분류 활성화 - 상위 켜기 → 하위는 여전히 꺼짐 |
| 통합 | 분류 활성화 - 상위 꺼진 하위 → CATEGORY_PARENT_INACTIVE |
| 통합 | 분류 활성화 - 상위 켜진 하위 → 켜짐 |
| 통합 | 분류 삭제 - 0건 하위 → 삭제 |
| 통합 | 분류 삭제 - 0건 상위, 하위 없음 → 삭제 |
| 통합 | 분류 삭제 - 사용된 분류 → CATEGORY_IN_USE |
| 통합 | 분류 삭제 - 하위 있는 상위 → CATEGORY_HAS_CHILDREN |
| 통합 | 분류 삭제 - 기본 템플릿 분류 0건 → 삭제 |
| 통합 | 분류 삭제 - 마지막 상위 → CATEGORY_LAST_ACTIVE |
| 통합 | 분류 tree 조회 - 상위 → 하위 순서, 경로·활성 여부·건수 |
| 통합 | 분류 tree 조회 - 활성만 → 꺼진 것 제외 |
| 통합 | 분류 존재 확인 - 켜진 분류 → true, 꺼진·없는 분류 → false |
| 통합 | 분류 이동 (선택) - 정상 → 새 상위 아래 끝 |
| 통합 | 분류 이동 (선택) - 이름 충돌 → CATEGORY_NAME_DUPLICATE |
| 통합 | Markdown 내보내기 (선택) - 켜진 것만, 형식 |
| 경계 | 모듈 경계 - category가 expense internal을 모름, 순환 없음 |
| HTTP | 분류 추가 - 201과 본문 |
| HTTP | 분류 삭제 - 사용 중 → 409 CATEGORY_IN_USE |

### 2.6 결정 사항 [검토]

| # | 결정 | 결론 | 이유 |
|---|---|---|---|
| D2-1 | 애그리거트 범위 | 노드 하나 (2.4의 A) | JPA 최소 사용 규칙 안에서 끝남. tree 규칙은 응용 서비스에서 조회로 검사 |
| D2-2 | 비용 건수를 아는 방법 | `CategoryUsage` 인터페이스를 category가 정의, expense가 구현 (의존 역전). 이벤트로 건수를 따로 세지 않음 | 순환 의존 없이 삭제 시점에 정확한 건수. 이벤트 집계는 지연이 있어 "방금 등록한 비용이 있는데 삭제됨"이 생길 수 있음 |
| D2-3 | 템플릿 저장 위치 | 코드 상수. 매장마다 DB로 복사해 그 매장의 비용 분류가 됨 | Phase 1 템플릿 하나. 사장님이 이름 바꾸기·끄기·삭제를 하려면 매장별 복사본이 DB에 있어야 함 (2026-10-27 확인). 업종별·DB 관리로 가는 건 Phase 2 |
| D2-4 | 상위 분류 이름 중복의 DB 보호 | 없음 (NULL 유니크 한계). 코드만 | 사용자 1명. 대안(`parent_id`를 0으로 두기)은 ID 참조 의미를 흐림 |
| D2-5 | `storeId`를 모든 API에 | 받음 | Phase 2 로그인에서 범위가 바뀌는 자리를 미리 둠 |
| D2-6 | 활성화·삭제 이벤트 | 발행하지 않음 | 요구사항정의서 목록에 없고 수신자도 없음 |
| D2-7 | 하위 끄기의 트랜잭션 | 상위와 하위를 한 트랜잭션에서 | D0-2 |
| D2-8 | 템플릿 멱등 처리 | **변경 (2026-10-25)** 적용 기록 테이블(`store_id` PK). 이전: "분류가 하나라도 있으면 무시" | Muse 리뷰. 이전 조건은 실패 후 수동 추가 시 템플릿 누락, 동시 재실행 중복을 못 막음 |
| D2-9 | 비용 건수 임시 구현 | main에 `NoExpenseYet`(2주차엔 사실과 같음), 테스트는 `FakeCategoryUsage`. 3주차 완료 조건에 삭제 | Muse 리뷰. 테스트 가짜가 없으면 "사용 중 삭제 거부" 테스트가 2주차에 성립하지 않음 |
| D2-10 | 템플릿과 사용자 분류의 이름 충돌 | **2026-10-25 추가** 리스너가 형제 이름을 확인해 같은 상위는 합치고 같은 하위는 건너뜀. 꺼진 상위에 합치면 하위도 꺼진 상태로 | Muse 리뷰. 리스너가 2.3 규칙을 우회하면 상위 중복이 생기고 DB가 못 막음 |

### 2.7 공개 API·이벤트·HTTP [참고]

**공개 API: CategoryService**

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

- `CategoryInfo(id, parentId, name, path, active, sortOrder, expenseCount)` 레코드
- `path`는 "식자재 > 유제품", 상위는 이름만. `expenseCount`는 `tree`에서만 채우고 `infoOf`에서는 0
- 모든 메서드가 `storeId`를 받는다 → 왜: Phase 1은 매장이 하나지만 Phase 2 로그인 때 "내 매장만"으로 바뀌는 자리 (요구사항정의서 9장 규칙)
- `storeId`의 매장이 실제로 있는지는 확인하지 않는다. HTTP로 온 값은 매장 접근 검사(1.2)를 지났고, 이벤트로 온 값은 store가 발행한 값

**리포지토리 메서드** (Spring Data 메서드 이름 쿼리)

| 메서드 | 쓰는 규칙 |
|---|---|
| `existsByStoreIdAndParentIdAndName` | 형제 이름 중복 (상위끼리는 parentId null) |
| `countByStoreIdAndParentId` | 개수 상한, 하위 있는 상위의 삭제·이동 불가, 새 분류의 순서 |
| `countByStoreIdAndParentIdIsNullAndActiveTrue` | 켜진 상위 분류 최소 1개 |
| 하위 목록 조회 (`findByStoreIdAndParentId`) | 상위를 끄면 하위도 |

**이벤트**

| 이벤트 | 내용 | 언제 |
|---|---|---|
| `DefaultCategoriesCreated` | `storeId`, `count` | 템플릿 생성 커밋 후 |
| `CategoryAdded` | `storeId`, `categoryId`, `parentId`, `name` | 추가 커밋 후 |
| `CategoryRenamed` | `storeId`, `categoryId`, `oldName`, `newName` | 이름 변경 커밋 후 (같은 이름이면 발행 안 함) |
| `CategoryMoved` | `storeId`, `categoryId`, `oldParentId`, `newParentId` | 이동 커밋 후 |
| `CategoryDeactivated` | `storeId`, `categoryIds` (하위 포함) | 비활성화 커밋 후 |

- Phase 1에서 수신하는 모듈은 없다. 발행 비용이 작고 Phase 2 AI 서비스가 tree 변화를 따라가는 데 쓰므로 요구사항정의서 목록대로 발행한다
- 활성화·삭제 이벤트는 요구사항정의서에 없어 발행하지 않는다 (필요해지면 추가)

**HTTP** (모든 경로는 `/api/stores/{storeId}/categories` 아래)

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

### 2.8 테이블·파일 [참고]

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

CREATE TABLE category.template_applications (   -- 기본 분류 템플릿 적용 기록 (멱등 처리, 2.2)
  store_id    BIGINT       PRIMARY KEY,
  applied_at  DATETIME     NOT NULL
);
```

- 유니크 키는 하위 분류의 중복만 DB가 막아 준다
- MySQL은 `parent_id`가 NULL인 행끼리는 유니크를 비교하지 않는다 → 상위 분류의 중복은 코드(2.3)만 막는다. 사용자 1명이라 충분하다 (D2-4)

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
    ├─ TemplateApplication.java 템플릿 적용 기록 (멱등)
    └─ NoExpenseYet.java        CategoryUsage 임시 구현 (항상 0, 2주차엔 사실). **3주차 expense 완료 조건: 삭제**

test/.../category/
    └─ FakeCategoryUsage.java   분류 ID별 건수를 지정하는 가짜 ("사용된 분류 삭제 거부" 테스트용)
```

---

## 3. sales 모듈 (예정, 3주차)

## 4. expense 모듈 (예정, 3주차)

## 5. budget 모듈 (예정, 4주차)

## 6. report 모듈 (예정, 4주차)
