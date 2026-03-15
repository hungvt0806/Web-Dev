-- ============================================================
--  V5__add_created_at_to_order_items.sql
--  Add missing created_at audit column to order_items.
--  Hibernate update mode cannot add NOT NULL columns automatically.
--  Managed by Flyway — DO NOT modify after deployment
-- ============================================================

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();