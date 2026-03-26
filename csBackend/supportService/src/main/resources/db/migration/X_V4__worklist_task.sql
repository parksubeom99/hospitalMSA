-- v6: Support Worklist 자동 생성(Clinical Order 생성 시 upsert)

CREATE TABLE IF NOT EXISTS support_worklist_task (
  worklist_task_id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  visit_id BIGINT NOT NULL,
  category VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (worklist_task_id),
  UNIQUE KEY uk_support_worklist_task_order (order_id),
  KEY idx_support_worklist_task_category_status (category, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
