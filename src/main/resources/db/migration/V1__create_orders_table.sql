CREATE TABLE orders (
    id           UUID         NOT NULL PRIMARY KEY,
    customer_id  VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    created_at   TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ
);
