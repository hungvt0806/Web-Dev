-- ============================================================
--  V1__init_schema.sql
--  Initial database schema for Shopee Ecommerce
--  Managed by Flyway — DO NOT modify after deployment
-- ============================================================

-- ── Extensions ────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pg_trgm";    -- trigram search on product names

-- ── ENUM Types ────────────────────────────────────────────────
CREATE TYPE user_role      AS ENUM ('USER', 'ADMIN', 'SELLER');
CREATE TYPE auth_provider  AS ENUM ('LOCAL', 'GOOGLE', 'FACEBOOK');
CREATE TYPE order_status   AS ENUM (
    'PENDING', 'AWAITING_PAYMENT', 'PAID', 'PROCESSING',
    'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'
);
CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');
CREATE TYPE payment_provider AS ENUM ('PAYPAY', 'STRIPE');

-- ── users ──────────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),                       -- NULL for OAuth2 users
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    avatar_url    TEXT,
    role          user_role    NOT NULL DEFAULT 'USER',
    provider      auth_provider NOT NULL DEFAULT 'LOCAL',
    provider_id   VARCHAR(255),
    is_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email       ON users (email);
CREATE INDEX idx_users_provider_id ON users (provider, provider_id);

-- ── refresh_tokens ─────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    token_id   VARCHAR(36)  NOT NULL UNIQUE,  -- JWT jti claim
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ  NOT NULL,
    is_revoked BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id  ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_id ON refresh_tokens (token_id);

-- ── categories ─────────────────────────────────────────────────
CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    image_url   TEXT,
    parent_id   BIGINT       REFERENCES categories (id) ON DELETE SET NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_slug      ON categories (slug);
CREATE INDEX idx_categories_parent_id ON categories (parent_id);

-- ── products ───────────────────────────────────────────────────
CREATE TABLE products (
    id             BIGSERIAL      PRIMARY KEY,
    name           VARCHAR(500)   NOT NULL,
    slug           VARCHAR(500)   NOT NULL UNIQUE,
    description    TEXT,
    price          NUMERIC(15, 2) NOT NULL CHECK (price >= 0),
    original_price NUMERIC(15, 2) CHECK (original_price >= 0),
    stock_quantity INT            NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    sold_count     INT            NOT NULL DEFAULT 0,
    image_urls     JSONB,                              -- ["url1", "url2", ...]
    category_id    BIGINT         REFERENCES categories (id) ON DELETE SET NULL,
    seller_id      UUID           REFERENCES users (id) ON DELETE SET NULL,
    rating         NUMERIC(3, 2)  NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 5),
    review_count   INT            NOT NULL DEFAULT 0,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_slug        ON products (slug);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_seller_id   ON products (seller_id);
CREATE INDEX idx_products_price       ON products (price);
CREATE INDEX idx_products_rating      ON products (rating DESC);
-- Full-text search index on product names
CREATE INDEX idx_products_name_trgm   ON products USING GIN (name gin_trgm_ops);

-- ── carts ──────────────────────────────────────────────────────
CREATE TABLE carts (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    UUID        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_carts_user_id ON carts (user_id);

-- ── cart_items ─────────────────────────────────────────────────
CREATE TABLE cart_items (
    id            BIGSERIAL      PRIMARY KEY,
    cart_id       BIGINT         NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id    BIGINT         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity      INT            NOT NULL DEFAULT 1 CHECK (quantity > 0),
    price_at_add  NUMERIC(15, 2) NOT NULL,            -- snapshot price at add time
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);

-- ── orders ─────────────────────────────────────────────────────
CREATE TABLE orders (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number     VARCHAR(50)    NOT NULL UNIQUE,
    user_id          UUID           NOT NULL REFERENCES users (id),
    status           order_status   NOT NULL DEFAULT 'PENDING',
    subtotal         NUMERIC(15, 2) NOT NULL CHECK (subtotal >= 0),
    shipping_fee     NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0),
    discount         NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
    total            NUMERIC(15, 2) NOT NULL CHECK (total >= 0),
    shipping_address JSONB          NOT NULL,          -- snapshot of address at order time
    note             TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id      ON orders (user_id);
CREATE INDEX idx_orders_status       ON orders (status);
CREATE INDEX idx_orders_created_at   ON orders (created_at DESC);
CREATE INDEX idx_orders_order_number ON orders (order_number);

-- ── order_items ────────────────────────────────────────────────
CREATE TABLE order_items (
    id            BIGSERIAL      PRIMARY KEY,
    order_id      UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id    BIGINT         REFERENCES products (id) ON DELETE SET NULL,
    product_name  VARCHAR(500)   NOT NULL,             -- snapshot at order time
    product_image TEXT,                                -- snapshot at order time
    quantity      INT            NOT NULL CHECK (quantity > 0),
    unit_price    NUMERIC(15, 2) NOT NULL,
    total_price   NUMERIC(15, 2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- ── payments ───────────────────────────────────────────────────
CREATE TABLE payments (
    id                  BIGSERIAL        PRIMARY KEY,
    order_id            UUID             NOT NULL UNIQUE REFERENCES orders (id),
    provider            payment_provider NOT NULL,
    provider_payment_id VARCHAR(255),                  -- PayPay merchantPaymentId / Stripe PaymentIntent ID
    amount              NUMERIC(15, 2)   NOT NULL,
    currency            VARCHAR(10)      NOT NULL DEFAULT 'JPY',
    status              payment_status   NOT NULL DEFAULT 'PENDING',
    payment_url         TEXT,                          -- PayPay redirect URL
    paid_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id   ON payments (order_id);
CREATE INDEX idx_payments_provider_payment_id ON payments (provider_payment_id);

-- ── Automatic updated_at trigger ──────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables with updated_at
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'users', 'categories', 'products', 'carts', 'cart_items', 'orders', 'payments'
    ] LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%s_updated_at
             BEFORE UPDATE ON %s
             FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()',
            t, t
        );
    END LOOP;
END;
$$;
