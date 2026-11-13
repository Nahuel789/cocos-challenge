DELETE FROM orders;
DELETE FROM marketdata;
DELETE FROM instruments;
DELETE FROM currencies;
DELETE FROM users;

INSERT INTO users (id, email, accountnumber)
VALUES (10, 'order@cocos.test', 'ACC-10');

INSERT INTO currencies (id, code, name)
VALUES (1, 'ARS', 'Peso Argentino');

INSERT INTO instruments (id, ticker, name, type, currency_id)
VALUES
    (200, 'GGAL', 'Grupo Financiero Galicia', 'ACCIONES', 1),
    (201, 'YPFD', 'YPF', 'ACCIONES', 1),
    (202, 'BMA', 'Banco Macro', 'ACCIONES', 1);

INSERT INTO marketdata (instrumentid, high, low, open, close, previousclose, date)
VALUES
    (200, 110, 90, 95, 100, 90, DATE '2026-11-08'),
    (201, 55, 45, 48, 50, 40, DATE '2026-11-08');

INSERT INTO orders (instrumentid, currency_id, userid, side, size, price, type, status, datetime)
VALUES
    (NULL, 1, 10, 'CASH_IN', 10000, 1, NULL, 'FILLED', TIMESTAMP '2026-11-07 12:00:00'),
    (200, 1, 10, 'BUY', 5, 100, 'MARKET', 'FILLED', TIMESTAMP '2026-11-08 15:00:00');

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('currencies', 'id'), (SELECT MAX(id) FROM currencies));
SELECT setval(pg_get_serial_sequence('instruments', 'id'), (SELECT MAX(id) FROM instruments));
SELECT setval(pg_get_serial_sequence('orders', 'id'), (SELECT MAX(id) FROM orders));
SELECT setval(pg_get_serial_sequence('marketdata', 'id'), (SELECT MAX(id) FROM marketdata));
