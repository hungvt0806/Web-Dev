-- ============================================================
--  V6__add_missing_columns_to_orders.sql
--  Add estimated_delivery_date and internal_note to orders.
--  Managed by Flyway — DO NOT modify after deployment
-- ============================================================

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS estimated_delivery_date TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS internal_note           TEXT;