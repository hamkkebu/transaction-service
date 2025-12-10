-- Transaction Service Schema

-- ==========================================
-- 사용자 테이블 (Auth Service에서 동기화)
-- ==========================================
DROP TABLE IF EXISTS tbl_users;
CREATE TABLE tbl_users (
    user_id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    nickname VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',

    -- Auditing Fields (from BaseEntity)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    -- Soft Delete Fields (from BaseEntity)
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME,

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Auth Service에서 동기화된 사용자 정보';

-- ==========================================
-- 거래 테이블
-- ==========================================
DROP TABLE IF EXISTS tbl_transactions;
CREATE TABLE tbl_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ledger_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(100),
    transaction_date DATE NOT NULL,
    memo VARCHAR(1000),

    -- Auditing Fields (from BaseEntity)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    -- Soft Delete Fields (from BaseEntity)
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME,

    INDEX idx_ledger_id (ledger_id),
    INDEX idx_user_id (user_id),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Transactional Outbox 테이블
-- ==========================================
DROP TABLE IF EXISTS tbl_outbox_event;
CREATE TABLE tbl_outbox_event (
    -- Primary Key
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- Event Information
    event_id VARCHAR(36) NOT NULL UNIQUE,      -- UUID 형태의 이벤트 고유 ID
    event_type VARCHAR(100) NOT NULL,          -- 이벤트 타입 (예: USER_CREATED)
    topic VARCHAR(100) NOT NULL,               -- Kafka 토픽명
    resource_id VARCHAR(100) NOT NULL,         -- 리소스 ID (Kafka 파티션 키)

    -- Event Payload (JSON)
    payload JSON NOT NULL,                     -- 전체 이벤트 객체를 JSON으로 직렬화

    -- Status Management
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PUBLISHED, FAILED
    retry_count INT NOT NULL DEFAULT 0,        -- 재시도 횟수
    max_retry INT NOT NULL DEFAULT 3,          -- 최대 재시도 횟수
    version BIGINT NOT NULL DEFAULT 0,         -- 낙관적 락을 위한 버전 필드

    -- Error Tracking
    error_message TEXT,                        -- 실패 시 에러 메시지

    -- Timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME,                     -- 발행 완료 시각
    last_retry_at DATETIME,                    -- 마지막 재시도 시각

    -- Indexes
    INDEX idx_status_created (status, created_at),
    INDEX idx_event_id (event_id),
    INDEX idx_topic (topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Transactional Outbox 이벤트 테이블 - DB 트랜잭션과 이벤트 발행의 원자성 보장';
