-- 월 예산 (04 설계 5.8)
-- created_at·updated_at은 DB가 채운다. 엔티티는 이 두 칸을 모른다 (04 설계 0.4)
-- V6: sales(V5)보다 먼저 머지되면 V5와 맞바꾼다 (04 설계 D5-5)
CREATE TABLE budget.monthly_budgets
(
  id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
  store_id      BIGINT        NOT NULL,
  budget_month  DATE          NOT NULL,     -- 그 달 1일 (04 설계 D5-10)
  amount        DECIMAL(15,0) NOT NULL,     -- 총액
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_monthly_budgets_store_month (store_id, budget_month)   -- 연월당 하나
) CHARACTER SET utf8mb4;

-- 예산 항목 = 값 컬렉션 (04 설계 D5-2). id · created_at 없음 (0.4 이름 규칙의 예외)
CREATE TABLE budget.budget_lines
(
  monthly_budget_id  BIGINT        NOT NULL,  -- 같은 모듈이어도 FK 없음 (04 설계 2.8과 같음)
  category_id        BIGINT        NOT NULL,  -- category.categories.id (상위). FK 없음 (04 설계 0.4)
  amount             DECIMAL(15,0) NOT NULL,
  PRIMARY KEY (monthly_budget_id, category_id)
) CHARACTER SET utf8mb4;
