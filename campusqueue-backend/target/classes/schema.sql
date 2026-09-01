-- ===================================================================
-- CampusQueue - Database Schema Reference (PostgreSQL)
-- Note: Spring Data JPA + Hibernate automatically manages schema with
-- spring.jpa.hibernate.ddl-auto=update. This file is provided for
-- reference and manual database setup if desired.
-- ===================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'STAFF', 'ADMIN')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Counters Table
CREATE TABLE IF NOT EXISTS counters (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tickets Table
CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    token_number VARCHAR(20) NOT NULL,
    counter_id BIGINT NOT NULL REFERENCES counters(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('WAITING', 'SERVING', 'COMPLETED', 'CANCELLED', 'SKIPPED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    called_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    remarks VARCHAR(255)
);

-- Indexes for Queue Processing and Reporting
CREATE INDEX IF NOT EXISTS idx_tickets_counter_status ON tickets(counter_id, status, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_tickets_user_status ON tickets(user_id, status);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at);
