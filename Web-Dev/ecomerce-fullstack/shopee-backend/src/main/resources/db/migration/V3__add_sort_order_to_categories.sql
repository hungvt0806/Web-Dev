-- ============================================================
--  V3__add_sort_order_to_categories.sql
--  Add sort_order column to categories table.
--  Managed by Flyway — DO NOT modify after deployment
-- ============================================================

ALTER TABLE categories
    ADD COLUMN sort_order SMALLINT NOT NULL DEFAULT 0;