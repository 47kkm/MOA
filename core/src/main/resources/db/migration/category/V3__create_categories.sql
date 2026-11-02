-- 비용 분류 (04 설계 2.8)
-- created_at·updated_at은 DB가 채운다. 엔티티는 이 두 칸을 모른다 (04 설계 0.4)
-- parent_id가 NULL이면 상위 분류. 같은 테이블이어도 FK는 걸지 않고 규칙은 코드에 둔다
CREATE TABLE category.categories
(
  id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
  store_id   BIGINT      NOT NULL,
  parent_id  BIGINT      NULL,
  name       VARCHAR(20) NOT NULL,
  sort_order INT         NOT NULL,
  active     TINYINT(1)  NOT NULL DEFAULT 1,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_categories_store_parent (store_id, parent_id),
  -- 하위 분류의 형제 이름 중복만 DB가 막는다. MySQL은 NULL끼리 비교하지 않아 상위 분류 중복은 코드만 막는다 (04 설계 D2-4)
  UNIQUE KEY uk_categories_sibling_name (store_id, parent_id, name)
) CHARACTER SET utf8mb4;

-- 기본 분류 템플릿 적용 기록. 매장마다 한 줄 (04 설계 2.2 멱등 처리, D2-8)
CREATE TABLE category.template_applications
(
  store_id   BIGINT   PRIMARY KEY,
  applied_at DATETIME NOT NULL
);
