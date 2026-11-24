-- Bookstore schema | H2 (MODE=MySQL) + MySQL 8.0.16+

CREATE TABLE IF NOT EXISTS books (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    isbn        VARCHAR(13)    NOT NULL,
    title       VARCHAR(255)   NOT NULL,
    author      VARCHAR(255)   NOT NULL,
    price       DECIMAL(10,2)  NOT NULL,
    CONSTRAINT pk_books       PRIMARY KEY (id),
    CONSTRAINT uk_books_isbn  UNIQUE (isbn),
    CONSTRAINT ck_books_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS inventory (
    book_id             BIGINT  NOT NULL,
    available_quantity  INT     NOT NULL DEFAULT 0,
    version             BIGINT  NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory      PRIMARY KEY (book_id),
    CONSTRAINT fk_inventory_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT ck_inventory_qty  CHECK (available_quantity >= 0)
);

CREATE TABLE IF NOT EXISTS users (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    email          VARCHAR(255)  NOT NULL,
    password_hash  VARCHAR(100)  NOT NULL,
    full_name      VARCHAR(100)  NOT NULL,
    role           VARCHAR(20)   NOT NULL DEFAULT 'USER',
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users       PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS carts (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_carts       PRIMARY KEY (id),
    CONSTRAINT uk_carts_user  UNIQUE (user_id),
    CONSTRAINT fk_carts_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cart_items (
    id        BIGINT  NOT NULL AUTO_INCREMENT,
    cart_id   BIGINT  NOT NULL,
    book_id   BIGINT  NOT NULL,
    quantity  INT     NOT NULL,
    CONSTRAINT pk_cart_items            PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_cart_book  UNIQUE (cart_id, book_id),
    CONSTRAINT fk_cart_items_cart       FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_book       FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT ck_cart_items_qty        CHECK (quantity BETWEEN 1 AND 99)
);

CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    user_id       BIGINT         NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'PLACED',
    total_amount  DECIMAL(12,2)  NOT NULL,
    created_at    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    idempotency_key VARCHAR(64),             -- optional Idempotency-Key
    CONSTRAINT pk_orders        PRIMARY KEY (id),
    CONSTRAINT uk_orders_user_idempotency_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_orders_user   FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_orders_total  CHECK (total_amount >= 0)
);
CREATE INDEX ix_orders_user_created ON orders (user_id, created_at);

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    order_id    BIGINT         NOT NULL,
    book_id     BIGINT         NOT NULL,   -- intentionally no FK: orders survive catalog changes
    title       VARCHAR(255)   NOT NULL,
    unit_price  DECIMAL(10,2)  NOT NULL,
    quantity    INT            NOT NULL,
    CONSTRAINT pk_order_items        PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order  FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_items_price  CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_qty    CHECK (quantity >= 1)
);
