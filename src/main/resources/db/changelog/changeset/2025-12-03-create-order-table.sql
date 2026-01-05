--liquibase formatted sql


--changeset arseniyryabov:create-extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--changeset arseniyryabov:create-orders
CREATE TABLE orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    address VARCHAR(255) NOT NULL,
    delivery_method VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    total_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--changeset arseniyryabov:create-order-items
CREATE TABLE order_items (
    order_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(order_id),
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL
);

--changeset arseniyryabov:create-order-indexes
CREATE INDEX idx_orders_created_at ON orders(created_at);