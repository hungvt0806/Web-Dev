-- ============================================================
--  V2__sync_schema_with_entities.sql
--  Sync DB schema to match current JPA entity definitions.
--  Managed by Flyway — DO NOT modify after deployment
-- ============================================================

-- ══════════════════════════════════════════════════════════════
--  products — add missing columns used by Product entity
-- ══════════════════════════════════════════════════════════════

-- Entity uses: base_price, original_price, short_description,
--              description, thumbnail_url, image_urls (JSONB),
--              tags (JSONB), rating_avg, rating_count, sold_count,
--              total_stock, weight_grams, status, featured,
--              slug, name, seller_id, category_id

-- Rename price → base_price
ALTER TABLE products RENAME COLUMN price TO base_price;

-- Rename stock_quantity → total_stock
ALTER TABLE products RENAME COLUMN stock_quantity TO total_stock;

-- Rename rating → rating_avg
ALTER TABLE products RENAME COLUMN rating TO rating_avg;

-- Rename review_count → rating_count
ALTER TABLE products RENAME COLUMN review_count TO rating_count;

-- Rename is_active → status (drop bool, add enum)
ALTER TABLE products DROP COLUMN is_active;
CREATE TYPE product_status AS ENUM ('DRAFT', 'ACTIVE', 'PAUSED', 'DELETED');
ALTER TABLE products ADD COLUMN status product_status NOT NULL DEFAULT 'DRAFT';
ALTER TABLE products ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;

-- Add short_description
ALTER TABLE products ADD COLUMN short_description TEXT;

-- Add thumbnail_url
ALTER TABLE products ADD COLUMN thumbnail_url TEXT;

-- Add tags JSONB array
ALTER TABLE products ADD COLUMN tags JSONB;

-- Add weight_grams
ALTER TABLE products ADD COLUMN weight_grams INT;

-- ══════════════════════════════════════════════════════════════
--  product_variants — new table (not in V1)
-- ══════════════════════════════════════════════════════════════
CREATE TABLE product_variants (
                                  id         BIGSERIAL      PRIMARY KEY,
                                  product_id BIGINT         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
                                  sku        VARCHAR(255)   NOT NULL UNIQUE,
                                  attributes JSONB,                              -- {"color": "red", "size": "M"}
                                  price      NUMERIC(15, 2),
                                  stock      INT            NOT NULL DEFAULT 0 CHECK (stock >= 0),
                                  image_url  TEXT,
                                  is_active  BOOLEAN        NOT NULL DEFAULT TRUE,
                                  created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_variants_product_id ON product_variants (product_id);
CREATE INDEX idx_product_variants_sku        ON product_variants (sku);

CREATE TRIGGER trg_product_variants_updated_at
    BEFORE UPDATE ON product_variants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ══════════════════════════════════════════════════════════════
--  product_images — new table (not in V1)
-- ══════════════════════════════════════════════════════════════
CREATE TABLE product_images (
                                id         BIGSERIAL    PRIMARY KEY,
                                product_id BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
                                url        TEXT         NOT NULL,
                                alt_text   VARCHAR(255),
                                sort_order SMALLINT     NOT NULL DEFAULT 0,
                                created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);

-- ══════════════════════════════════════════════════════════════
--  cart_items — rename price_at_add → unit_price, add variant_id
-- ══════════════════════════════════════════════════════════════
ALTER TABLE cart_items RENAME COLUMN price_at_add TO unit_price;

ALTER TABLE cart_items
    ADD COLUMN variant_id BIGINT REFERENCES product_variants (id) ON DELETE SET NULL;

-- Drop old unique constraint (cart_id, product_id) — now must allow multiple variants
ALTER TABLE cart_items DROP CONSTRAINT cart_items_cart_id_product_id_key;
ALTER TABLE cart_items ADD CONSTRAINT uq_cart_items_cart_variant
    UNIQUE (cart_id, product_id, variant_id);

-- Remove created_at/updated_at from cart_items (not in entity)
-- (Keep them — harmless extra columns are fine)

-- ══════════════════════════════════════════════════════════════
--  orders — align column names with Order entity
-- ══════════════════════════════════════════════════════════════

-- Rename user_id → buyer_id
ALTER TABLE orders RENAME COLUMN user_id TO buyer_id;

-- Rename discount → discount_amount
ALTER TABLE orders RENAME COLUMN discount TO discount_amount;

-- Rename total → total_amount
ALTER TABLE orders RENAME COLUMN total TO total_amount;

-- Rename note → buyer_note
ALTER TABLE orders RENAME COLUMN note TO buyer_note;

-- Add missing order columns
ALTER TABLE orders ADD COLUMN currency         VARCHAR(10)  NOT NULL DEFAULT 'JPY';
ALTER TABLE orders ADD COLUMN coupon_code      VARCHAR(100);
ALTER TABLE orders ADD COLUMN tracking_number  VARCHAR(255);
ALTER TABLE orders ADD COLUMN shipping_carrier VARCHAR(100);
ALTER TABLE orders ADD COLUMN cancellation_reason TEXT;
ALTER TABLE orders ADD COLUMN paid_at          TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN shipped_at       TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN delivered_at     TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN cancelled_at     TIMESTAMPTZ;

-- Drop old order_status enum and recreate to match OrderStatus entity
-- (already correct values — no change needed)

-- ══════════════════════════════════════════════════════════════
--  order_status_history — new table (not in V1)
-- ══════════════════════════════════════════════════════════════
CREATE TYPE actor_type AS ENUM ('SYSTEM', 'BUYER', 'SELLER', 'ADMIN');

CREATE TABLE order_status_history (
                                      id          BIGSERIAL    PRIMARY KEY,
                                      order_id    UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
                                      from_status order_status,
                                      to_status   order_status NOT NULL,
                                      actor_id    UUID,
                                      actor_type  actor_type   NOT NULL DEFAULT 'SYSTEM',
                                      note        TEXT,
                                      created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);

-- ══════════════════════════════════════════════════════════════
--  order_items — add missing snapshot columns
-- ══════════════════════════════════════════════════════════════
ALTER TABLE order_items ADD COLUMN variant_id        BIGINT REFERENCES product_variants (id) ON DELETE SET NULL;
ALTER TABLE order_items ADD COLUMN sku               VARCHAR(255);
ALTER TABLE order_items ADD COLUMN variant_attributes JSONB;
ALTER TABLE order_items ADD COLUMN display_name      VARCHAR(600);
ALTER TABLE order_items ADD COLUMN is_reviewed       BOOLEAN NOT NULL DEFAULT FALSE;

-- Rename total_price → line_total
ALTER TABLE order_items RENAME COLUMN total_price TO line_total;

-- ══════════════════════════════════════════════════════════════
--  payments — align with Payment entity
-- ══════════════════════════════════════════════════════════════

-- Add new payment_status values and columns
ALTER TABLE payments ADD COLUMN merchant_payment_id  VARCHAR(255);
ALTER TABLE payments ADD COLUMN paypay_payment_id    VARCHAR(255);
ALTER TABLE payments ADD COLUMN payment_method       VARCHAR(100);
ALTER TABLE payments ADD COLUMN failure_reason       TEXT;
ALTER TABLE payments ADD COLUMN expires_at           TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN authorized_at        TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN captured_at          TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN failed_at            TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN refunded_amount      NUMERIC(15,2);
ALTER TABLE payments ADD COLUMN refunded_at          TIMESTAMPTZ;

-- Rename provider_payment_id → keep as is (used for PayPay redirect URL tracking)

-- ══════════════════════════════════════════════════════════════
--  carts — add session_id for guest carts
-- ══════════════════════════════════════════════════════════════
ALTER TABLE carts ADD COLUMN session_id VARCHAR(128) UNIQUE;
-- Remove NOT NULL from user_id to support guest (session-based) carts
ALTER TABLE carts ALTER COLUMN user_id DROP NOT NULL;