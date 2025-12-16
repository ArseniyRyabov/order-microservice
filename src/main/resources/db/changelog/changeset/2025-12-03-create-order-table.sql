--liquibase formatted sql


--changeset arseniyryabov:create-orders-user-id-sequence
CREATE SEQUENCE IF NOT EXISTS user_id_sequence
START WITH 1
INCREMENT BY 1;

--changeset arseniyryabov:create-orders
CREATE TABLE orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id INT DEFAULT NEXTVAL('user_id_sequence'),
    address VARCHAR(255) NOT NULL,
    delivery_method VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'в обработке',
    total_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--changeset arseniyryabov:create-order-items
CREATE TABLE order_items (
    order_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL
);