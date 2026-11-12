# 모아 MOA

> 매출을 모아, 인사이트를 더하다.  
> **M**anage · **O**bserve · **A**nalyze

작은 매장의 매출 · 비용 · 예산을 한곳에 모아 현황을 보는 매장 관리 서비스입니다.  
Phase 1에서 서비스를 만들고, Phase 2에서 AI(비용 분류 추론, 리포트 서술, Local LLM)를 붙입니다.

## 지금 상태 (2026-11)

Phase 1 진행 중 (목표 11/30). 백엔드 모듈 6개 중 3개 완료.

| 모듈 | 하는 일 | 상태 |
|---|---|---|
| store | 매장 등록 · 정보 수정 | 완료 |
| category | 비용 분류 (상위 · 하위 2단 tree). 추가 · 이름 변경 · 켜기/끄기 · 삭제. 매장을 등록하면 기본 비용 분류를 만들어 줌 | 완료 |
| expense | 비용 등록 · 수정 · 재분류 · 삭제 · 월 목록 | 완료 |
| sales | 결제수단 · 일 매출 | 설계 중 |
| budget · report | 예산 소진, 기간별 집계 | 예정 |
| 프론트엔드 | 대시보드 · 입력 화면 | 예정 |

## 기술

| 영역 | 선택 |
|---|---|
| 백엔드 | Java 17 · Spring Boot 3.5 · Spring Data JPA · Spring Modulith |
| DB | MySQL 8 (모듈마다 스키마 하나) · Flyway |
| 테스트 | JUnit 5 · AssertJ · Mockito · Testcontainers (실제 MySQL로 테스트) |
| 프론트엔드 (예정) | React · Vite · TypeScript · Tailwind CSS · ECharts |

## 구조: 모듈러 모놀리스

앱은 하나지만 안은 모듈 6개로 나뉩니다. 모듈 하나 = 패키지 하나 + DB 스키마 하나.

```
store    ── StoreRegistered 이벤트 ──▶ category   기본 비용 분류 만들기
expense  ── 공개 API 호출 ──────────▶ category   고를 수 있는 비용 분류인지 확인
expense  ── CategoryUsage 구현 ─────▶ category   비용 분류를 지울 때 쓰인 건수 (의존 역전)
expense  ── 비용 이벤트 4종 ─────────▶ budget     예산 소진 갱신 (예정)
```

- 모듈 사이는 **공개 API나 도메인 이벤트로만** 오갑니다. 다른 모듈의 `internal/` 패키지를 쓰면 `ModularityTest`가 실패합니다.
- 다른 모듈의 데이터는 ID로만 가리킵니다. 스키마를 넘는 FK · JOIN은 없습니다.
- 이벤트 발행 기록은 DB에 남습니다. 받는 쪽이 실패해도 앱이 다시 뜰 때 다시 실행됩니다.
- 금액은 값 객체 `Money`(`moa.shared`)로 다룹니다. 원 단위 정수, DB는 `DECIMAL(15,0)`.

## 만드는 순서

요구사항 → 기획(PRD) → 설계(DDD) → 구현(TDD) → 검증. 단계마다 문서가 하나씩 있습니다.

| 문서 | 내용 |
|---|---|
| [00_start](docs/00_start.md) | 방향 · 방법론 · 기술 선택 · 일정 |
| [01_branding](docs/01_branding.md) | 이름과 핵심 가치 |
| [02_requirements](docs/02_requirements.md) | 대표 사용자 · 시나리오 · 용어 · 이벤트 스토밍 · 수용 조건 |
| [03_prd](docs/03_prd.md) | 화면 7개의 동작 · 입력 규칙 · 검증 기준 |
| [04_design](docs/04_design.md) | 모듈별 애그리거트 · 이벤트 · 공개 API · 테이블 · 결정 기록 |
| [05_verification](docs/05_verification.md) | AI가 만든 문서 · 코드의 검증 기록 |

- 테스트 이름(`@DisplayName`)은 기획서의 기능 이름을 그대로 씁니다. 예: `비용 등록 - 미래 날짜 → 거부`
- 커밋 하나가 TDD 한 단계입니다 (빨강 → 초록 → 정리). PR의 Commits 탭을 위에서부터 읽으면 기능이 자란 순서가 보입니다.

## AI와 일하는 방식

AI(Claude Code)를 페어로 두고 개발합니다.

| 누가 | 무엇을 |
|---|---|
| kei | 기능 범위 · 규칙 · 설계 결정 (04 설계의 "확정 (kei)"), PR 머지. 코드 리뷰 결과는 [05_verification](docs/05_verification.md)에 (expense는 리뷰 대기) |
| Claude Code | 문서와 코드의 대부분. 정할 것이 생기면 추천안과 함께 kei에게 넘김 |
| kei 직접 구현 | `Money` 값 객체 (TDD, PR #4), sales 모듈 (설계 요약 작성, 구현 예정) |

- AI 산출물을 검토한 결과와 거기서 잡은 오류는 [05_verification](docs/05_verification.md)에 남깁니다.
- 설계 문서는 다른 AI(Muse)에게도 교차 검토를 받았습니다 ([muse-feedback](docs/muse-feedback.md)).

## 실행

Docker가 필요합니다.

```bash
cd core
docker compose up -d   # 로컬 MySQL 8
./gradlew bootRun      # http://localhost:8080
./gradlew test         # 테스트는 Testcontainers가 MySQL을 따로 띄움
```
