CREATE TABLE currencies (
    id SERIAL PRIMARY KEY,
    code VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255),
    accountnumber VARCHAR(20)
);

CREATE TABLE instruments (
    id SERIAL PRIMARY KEY,
    ticker VARCHAR(10),
    name VARCHAR(255),
    type VARCHAR(10),
    currency_id INT NOT NULL,
    FOREIGN KEY (currency_id) REFERENCES currencies (id)
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    instrumentid INT,
    currency_id INT NOT NULL,
    userid INT,
    size INT,
    price NUMERIC(10, 2),
    type VARCHAR(10),
    side VARCHAR(10),
    status VARCHAR(20),
    datetime TIMESTAMP,
    FOREIGN KEY (instrumentid) REFERENCES instruments (id),
    FOREIGN KEY (currency_id) REFERENCES currencies (id),
    FOREIGN KEY (userid) REFERENCES users (id),
    CONSTRAINT orders_side_target CHECK (
        (side IN ('BUY', 'SELL') AND instrumentid IS NOT NULL)
        OR (side IN ('CASH_IN', 'CASH_OUT') AND instrumentid IS NULL)
    )
);

CREATE TABLE marketdata (
    id SERIAL PRIMARY KEY,
    instrumentid INT,
    high NUMERIC(10, 2),
    low NUMERIC(10, 2),
    open NUMERIC(10, 2),
    close NUMERIC(10, 2),
    previousclose NUMERIC(10, 2),
    date DATE,
    FOREIGN KEY (instrumentid) REFERENCES instruments (id)
);

CREATE INDEX idx_orders_user_status ON orders (userid, status);
CREATE INDEX idx_orders_user_instrument_status ON orders (userid, instrumentid, status);
CREATE INDEX idx_marketdata_instrument_date ON marketdata (instrumentid, date DESC);
