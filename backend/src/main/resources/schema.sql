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
    user_role VARCHAR(20) NOT NULL DEFAULT 'USER',

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
-- 가계부 테이블 (Ledger Service에서 동기화)
-- ==========================================
DROP TABLE IF EXISTS tbl_ledgers;
CREATE TABLE tbl_ledgers (
    ledger_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ledger_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    currency VARCHAR(10) NOT NULL DEFAULT 'KRW',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,

    -- Auditing Fields (from BaseEntity)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    -- Soft Delete Fields (from BaseEntity)
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME,

    INDEX idx_user_id (user_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Ledger Service에서 동기화된 가계부 정보';

-- ==========================================
-- 거래 테이블
-- ==========================================
DROP TABLE IF EXISTS tbl_transactions;
CREATE TABLE tbl_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ledger_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
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

    -- Codef 연동 필드
    source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    external_approval_no VARCHAR(100),
    linked_card_id BIGINT,

    INDEX idx_ledger_id (ledger_id),
    INDEX idx_user_id (user_id),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_is_deleted (is_deleted),
    INDEX idx_source_type (source_type),
    INDEX idx_external_approval_no (external_approval_no),
    INDEX idx_linked_card_id (linked_card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Codef 연동 카드 테이블
-- ==========================================
DROP TABLE IF EXISTS tbl_linked_cards;
CREATE TABLE tbl_linked_cards (
    linked_card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ledger_id BIGINT NOT NULL,
    connected_id VARCHAR(100) NOT NULL,
    organization VARCHAR(10) NOT NULL,
    card_name VARCHAR(100) NOT NULL,
    card_no_masked VARCHAR(20),
    card_id VARCHAR(100),
    last_synced_date VARCHAR(10),

    -- Auditing Fields (from BaseEntity)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    -- Soft Delete Fields (from BaseEntity)
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME,

    INDEX idx_linked_card_user_id (user_id),
    INDEX idx_linked_card_ledger_id (ledger_id),
    INDEX idx_linked_card_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Codef 연동 카드 정보';

-- ==========================================
-- 가계부 공유 테이블 (Ledger Service에서 동기화)
-- ==========================================
DROP TABLE IF EXISTS tbl_ledger_shares;
CREATE TABLE tbl_ledger_shares (
    ledger_share_id BIGINT PRIMARY KEY,
    ledger_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    shared_user_id BIGINT NOT NULL,
    share_status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    permission VARCHAR(20) NOT NULL DEFAULT 'READ_ONLY',
    shared_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at DATETIME,
    rejection_reason VARCHAR(500),

    -- Auditing Fields (from BaseEntity)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    -- Soft Delete Fields (from BaseEntity)
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME,

    INDEX idx_ledger_id (ledger_id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_shared_user_id (shared_user_id),
    INDEX idx_share_status (share_status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Ledger Service에서 동기화된 가계부 공유 정보';

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
    event_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PUBLISHED, FAILED
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
    INDEX idx_event_status_created (event_status, created_at),
    INDEX idx_event_id (event_id),
    INDEX idx_topic (topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Transactional Outbox 이벤트 테이블 - DB 트랜잭션과 이벤트 발행의 원자성 보장';

-- ==========================================
-- 카드 테이블 (계정 소유)
-- ==========================================
CREATE TABLE IF NOT EXISTS tbl_cards (
    card_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id       BIGINT       NOT NULL,
    connected_id     VARCHAR(255) NOT NULL,
    organization     VARCHAR(10)  NOT NULL,
    card_identifier  VARCHAR(50),
    card_name        VARCHAR(100) NOT NULL,
    card_no_masked   VARCHAR(20),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME
);

CREATE INDEX idx_card_account_id ON tbl_cards(account_id);
CREATE INDEX idx_card_connected_id ON tbl_cards(connected_id);

-- ==========================================
-- 가계부-카드 연결 테이블 (M:N)
-- ==========================================
CREATE TABLE IF NOT EXISTS tbl_ledger_cards (
    ledger_card_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    ledger_id        BIGINT       NOT NULL,
    card_id          BIGINT       NOT NULL,
    linked_by        BIGINT       NOT NULL,
    last_synced_date VARCHAR(20),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME,
    CONSTRAINT fk_ledger_card_card FOREIGN KEY (card_id) REFERENCES tbl_cards(card_id) ON DELETE CASCADE,
    CONSTRAINT uk_ledger_card UNIQUE (ledger_id, card_id)
);

CREATE INDEX idx_ledger_card_ledger_id ON tbl_ledger_cards(ledger_id);
CREATE INDEX idx_ledger_card_card_id ON tbl_ledger_cards(card_id);

-- ==========================================
-- 통장 테이블 (계정 소유)
-- ==========================================
CREATE TABLE IF NOT EXISTS tbl_bank_accounts (
    bank_account_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id       BIGINT       NOT NULL,
    connected_id     VARCHAR(255) NOT NULL,
    bank_code        VARCHAR(10)  NOT NULL,
    account_number   VARCHAR(30),
    account_name     VARCHAR(100) NOT NULL,
    account_no_masked VARCHAR(20),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME
);

CREATE INDEX idx_bank_account_account_id ON tbl_bank_accounts(account_id);
CREATE INDEX idx_bank_account_connected_id ON tbl_bank_accounts(connected_id);

-- ==========================================
-- 가계부-통장 연결 테이블 (M:N)
-- ==========================================
CREATE TABLE IF NOT EXISTS tbl_ledger_bank_accounts (
    ledger_bank_account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ledger_id              BIGINT       NOT NULL,
    bank_account_id        BIGINT       NOT NULL,
    linked_by              BIGINT       NOT NULL,
    last_synced_date       VARCHAR(20),
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(50),
    updated_by             VARCHAR(50),
    is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at             DATETIME,
    CONSTRAINT fk_ledger_bank_account FOREIGN KEY (bank_account_id) REFERENCES tbl_bank_accounts(bank_account_id) ON DELETE CASCADE,
    CONSTRAINT uk_ledger_bank_account UNIQUE (ledger_id, bank_account_id)
);

CREATE INDEX idx_ledger_ba_ledger_id ON tbl_ledger_bank_accounts(ledger_id);
CREATE INDEX idx_ledger_ba_ba_id ON tbl_ledger_bank_accounts(bank_account_id);

-- ==========================================
-- 거래내역 테이블에 새 컬럼 추가
-- ==========================================
ALTER TABLE tbl_transactions ADD COLUMN IF NOT EXISTS account_id BIGINT AFTER user_id;
ALTER TABLE tbl_transactions ADD COLUMN IF NOT EXISTS card_id BIGINT AFTER linked_card_id;
ALTER TABLE tbl_transactions ADD COLUMN IF NOT EXISTS bank_account_id BIGINT AFTER card_id;

CREATE INDEX IF NOT EXISTS idx_transaction_card_id ON tbl_transactions(card_id);
CREATE INDEX IF NOT EXISTS idx_transaction_bank_account_id ON tbl_transactions(bank_account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_account_id ON tbl_transactions(account_id);
