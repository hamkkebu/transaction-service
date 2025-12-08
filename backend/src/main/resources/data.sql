-- Transaction Service Initial Data

-- 기본 카테고리 데이터
INSERT INTO tbl_category (name, description, parent_id) VALUES
    ('수입', '수입 관련 카테고리', NULL),
    ('지출', '지출 관련 카테고리', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name);
