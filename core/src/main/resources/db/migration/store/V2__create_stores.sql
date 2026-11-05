-- 매장 (04 설계 1.8)
-- created_at·updated_at은 DB가 채운다. 엔티티는 이 두 칸을 모른다 (04 설계 0.4)
CREATE TABLE store.stores
(
  id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(50) NOT NULL,
  store_type    VARCHAR(20) NOT NULL,
  registered_at DATE        NOT NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4;
