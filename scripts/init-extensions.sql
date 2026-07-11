-- PII Vault PostgreSQL Extensions Initialization
-- This script enables required extensions for partitioning and UUID support

-- Enable UUID extension for native UUID type support
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable pgcrypto for cryptographic functions (used for key derivation)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Enable pg_stat_statements for query performance monitoring
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Enable pg_partman for automatic partition management
CREATE EXTENSION IF NOT EXISTS pg_partman;

-- Enable pgsodium for additional cryptographic support (optional)
-- CREATE EXTENSION IF NOT EXISTS pgsodium;

-- Verify extensions are installed
SELECT * FROM pg_extension;
