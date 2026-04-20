-- ============================================================
-- V1__create_initial_schema.sql
-- Production Order Monitoring System - Initial Schema
-- ============================================================

-- ============================================================
-- ENUM: order_status
-- Represents the lifecycle of a production order
-- ============================================================
CREATE TYPE order_status AS ENUM ('PENDING', 'RUNNING', 'FINISHED', 'CANCELLED');

-- ============================================================
-- TABLE: production_orders
-- Core entity representing an industrial production order
-- ============================================================
CREATE TABLE production_orders (
    id              BIGSERIAL       PRIMARY KEY,
    order_code      VARCHAR(20)     NOT NULL UNIQUE,
    product_name    VARCHAR(150)    NOT NULL,
    quantity        INTEGER         NOT NULL CHECK (quantity > 0),
    status          order_status    NOT NULL DEFAULT 'PENDING',
    priority        SMALLINT        NOT NULL DEFAULT 1 CHECK (priority BETWEEN 1 AND 5),
    machine_id      VARCHAR(50),
    operator_name   VARCHAR(100),
    notes           TEXT,
    scheduled_at    TIMESTAMPTZ     NOT NULL,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABLE: execution_logs
-- Stores detailed log entries for each order lifecycle event
-- ============================================================
CREATE TABLE execution_logs (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES production_orders(id) ON DELETE CASCADE,
    level           VARCHAR(10)     NOT NULL CHECK (level IN ('INFO', 'WARN', 'ERROR')),
    message         TEXT            NOT NULL,
    metadata        JSONB,
    logged_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES
-- Optimized for the most common query patterns
-- ============================================================

-- Fast lookup by status (dashboard queries)
CREATE INDEX idx_orders_status ON production_orders(status);

-- Fast lookup by date range (period queries)
CREATE INDEX idx_orders_scheduled_at ON production_orders(scheduled_at DESC);

-- Composite: status + date (combined filter queries)
CREATE INDEX idx_orders_status_scheduled ON production_orders(status, scheduled_at DESC);

-- Machine performance queries
CREATE INDEX idx_orders_machine_id ON production_orders(machine_id);

-- Log queries by order (most common join)
CREATE INDEX idx_logs_order_id ON execution_logs(order_id);

-- Log queries by time range
CREATE INDEX idx_logs_logged_at ON execution_logs(logged_at DESC);

-- JSONB metadata search support
CREATE INDEX idx_logs_metadata ON execution_logs USING GIN(metadata);

-- ============================================================
-- TRIGGER: auto-update updated_at on production_orders
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON production_orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- SEED DATA: sample orders for development/demo
-- ============================================================
INSERT INTO production_orders (order_code, product_name, quantity, status, priority, machine_id, operator_name, scheduled_at)
VALUES
    ('OP-2024-0001', 'Steel Shaft 40mm',     500, 'FINISHED', 3, 'CNC-01', 'John Smith',   NOW() - INTERVAL '3 days'),
    ('OP-2024-0002', 'Aluminum Bracket Type A', 200, 'RUNNING',  4, 'CNC-02', 'Maria Garcia',  NOW() - INTERVAL '1 day'),
    ('OP-2024-0003', 'Hydraulic Cylinder Rod', 100, 'PENDING',   5, 'CNC-03', NULL,            NOW() + INTERVAL '2 days'),
    ('OP-2024-0004', 'Gear Housing Cover',    350, 'PENDING',   2, NULL,     NULL,            NOW() + INTERVAL '4 days'),
    ('OP-2024-0005', 'Drive Shaft Assembly',  80,  'CANCELLED', 1, 'CNC-01', 'John Smith',   NOW() - INTERVAL '5 days');

-- Seed logs
INSERT INTO execution_logs (order_id, level, message, metadata)
VALUES
    (1, 'INFO',  'Order created and queued for processing.',           '{"source": "scheduler"}'),
    (1, 'INFO',  'Machine CNC-01 assigned. Production started.',       '{"machine": "CNC-01", "operator": "John Smith"}'),
    (1, 'INFO',  'Production completed. All 500 units manufactured.',  '{"units_produced": 500, "defects": 0}'),
    (2, 'INFO',  'Order created and queued for processing.',           '{"source": "manual"}'),
    (2, 'INFO',  'Machine CNC-02 assigned. Production started.',       '{"machine": "CNC-02", "operator": "Maria Garcia"}'),
    (2, 'WARN',  'Minor deviation detected on unit batch #3.',         '{"batch": 3, "deviation_mm": 0.02}'),
    (3, 'INFO',  'Order created and scheduled.',                       '{"source": "erp_integration"}'),
    (5, 'INFO',  'Order created.',                                     NULL),
    (5, 'ERROR', 'Order cancelled due to raw material shortage.',      '{"reason": "material_shortage", "material": "steel_rod_40mm"}');
