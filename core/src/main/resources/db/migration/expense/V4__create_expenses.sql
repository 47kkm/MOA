-- 비용 (04 설계 4.8)
-- created_at·updated_at은 DB가 채운다. 엔티티는 이 두 칸을 모른다 (04 설계 0.4)
-- V4: expense가 sales보다 먼저 머지되어 V4, sales는 V5 (04 설계 D4-13, 2026-11-10 맞바꿈)
CREATE TABLE expense.expenses
(
  id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
  store_id      BIGINT        NOT NULL,
  category_id   BIGINT        NOT NULL,     -- category.categories.id. FK 없음 (04 설계 0.4)
  amount        DECIMAL(15,0) NOT NULL,
  expense_date  DATE          NOT NULL,
  memo          VARCHAR(200)  NULL,         -- utf8mb4에서 글자 수 기준
  classified_by VARCHAR(10)   NOT NULL,     -- USER (Phase 2: AI)
  approved      TINYINT(1)    NOT NULL,
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_expenses_store_date (store_id, expense_date),       -- 월 목록
  INDEX idx_expenses_store_category (store_id, category_id)     -- 누적 건수
) CHARACTER SET utf8mb4;
