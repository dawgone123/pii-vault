-- PII Vault Database Initialization Script
-- This script creates all necessary tables and indexes for development

-- 1. KMS Key Management Table
CREATE TABLE IF NOT EXISTS pii_vault_kms_keys (
    id BIGSERIAL PRIMARY KEY,
    key_alias VARCHAR(255) NOT NULL UNIQUE,
    key_arn VARCHAR(500) NOT NULL UNIQUE,
    key_id VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    activated_at TIMESTAMP,
    deactivated_at TIMESTAMP,
    reason VARCHAR(500),
    rotations_count INTEGER NOT NULL DEFAULT 0,
    last_rotation_at TIMESTAMP,
    metadata JSONB,
    CONSTRAINT check_only_one_active CHECK (
        (is_active = false) OR (
            SELECT COUNT(*) FROM pii_vault_kms_keys WHERE is_active = true
        ) <= 1
    )
);

-- 2. PII Data Storage Table
CREATE TABLE IF NOT EXISTS pii_data (
    pii_token_id UUID PRIMARY KEY,
    encrypted_data TEXT NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_date TIMESTAMP,
    owner VARCHAR(255),
    created_by VARCHAR(255),
    kms_key_id VARCHAR(255) NOT NULL,
    CONSTRAINT pii_data_immutable CHECK (created_date IS NOT NULL)
);

-- 3. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    event_id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    operation_type VARCHAR(50) NOT NULL,
    pii_token_id UUID,
    owner VARCHAR(255),
    request_id UUID NOT NULL,
    status VARCHAR(50),
    error_message TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSONB
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_pii_data_owner ON pii_data(owner);
CREATE INDEX IF NOT EXISTS idx_pii_data_is_deleted ON pii_data(is_deleted);
CREATE INDEX IF NOT EXISTS idx_pii_data_created_date ON pii_data(created_date);
CREATE INDEX IF NOT EXISTS idx_pii_data_deleted_date ON pii_data(deleted_date);
CREATE INDEX IF NOT EXISTS idx_pii_data_kms_key_id ON pii_data(kms_key_id);

CREATE INDEX IF NOT EXISTS idx_kms_keys_is_active ON pii_vault_kms_keys(is_active);
CREATE INDEX IF NOT EXISTS idx_kms_keys_created_at ON pii_vault_kms_keys(created_at);

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_pii_token_id ON audit_logs(pii_token_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_request_id ON audit_logs(request_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_operation_type ON audit_logs(operation_type);

-- Partitioning setup for operational tier management
-- Note: Actual partitioning can be implemented after initial setup
-- ALTER TABLE pii_data PARTITION BY RANGE (DATE_TRUNC('month', created_date));

-- Grant permissions to vault_user (development only)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO vault_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO vault_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO vault_user;

-- Optional: Create a development KMS key entry
INSERT INTO pii_vault_kms_keys (
    key_alias,
    key_arn,
    key_id,
    is_active,
    created_at,
    activated_at,
    reason
) VALUES (
    'VAULT_20260708',
    'arn:aws:kms:us-east-1:000000000000:key/dev-key',
    'dev-key-id-12345',
    true,
    NOW(),
    NOW(),
    'Development default key'
) ON CONFLICT (key_alias) DO NOTHING;

-- Optional: Grant read-only access for reporting user (development)
-- CREATE USER report_user WITH PASSWORD 'report_password';
-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO report_user;
