# Cocos API

API de portfolio, búsqueda de activos y envío de órdenes. Esta entrega está en **Java 21** y **Spring Boot 4**, con Clean Architecture.

No hay autenticación.

## Cómo levantarla

Hace falta JDK 21 o superior y Docker. La API corre con el perfil `local`, que es el perfil por defecto, así que no hay que indicarlo.

```bash
docker compose up -d
./mvnw spring-boot:run
```

En Windows: `mvnw.cmd spring-boot:run`.

Spring Boot también levanta `compose.yaml` solo al arrancar (`spring.docker.compose.enabled: true`), así que el `docker compose up -d` previo es opcional.

Postgres queda en `localhost:5432`, base `cocos`, usuario `cocos`, clave `cocos`. Flyway crea el esquema (`V1__schema.sql`) y carga los datos de prueba (`V2__seed.sql`) al arrancar.

Si se editan esas migraciones y la base ya tenía una versión anterior aplicada, Flyway falla por checksum. Para empezar de cero hay que borrar el contenedor y su volumen:

```bash
docker compose down -v
```

El perfil `local` publica eventos en SNS contra LocalStack (`aws.sns.enabled: true`). Con `aws.sns.enabled: false`, como en el perfil `test`, se registra un publicador que no hace nada.

LocalStack queda en `localhost:4566`, con SNS y SQS. Al quedar listo crea el topic `order-persisted` y la cola `order-persisted-inspection`, suscrita a ese topic, para mirar el evento en local. La API publica directo en SNS y no consume la cola. SQS no entra en el flujo: en local esa cola solo sirve para inspeccionar el mensaje, y en los tests para afirmar que salió.

- API: `http://localhost:8080`
- OpenAPI: `http://localhost:8080/swagger-ui/index.html`
- Colección Postman: [postman/Cocos API.postman_collection.json](postman/Cocos%20API.postman_collection.json), con la variable `baseUrl`

Tests:

```bash
./mvnw test
```

Los tests de integración levantan Postgres 16 con Testcontainers. No usan el contenedor de `compose.yaml`.

## Datos de prueba

El seed (`V2__seed.sql`) parte de los datos del `database.sql` del enunciado, adaptados al esquema de este proyecto: se agrega la tabla `currencies` con `ARS`, cada instrumento y cada orden llevan `currency_id`, y los `CASH_IN` y `CASH_OUT` quedan con `instrumentid` nulo.

| id | email | cuenta |
| --- | --- | --- |
| 1 | emiliano@test.com | 10001 |
| 2 | jose@test.com | 10002 |
| 3 | francisco@test.com | 10003 |
| 4 | juan@test.com | 10004 |

Hay 65 instrumentos, todos `ACCIONES`. `GGAL` es el id 34. El peso es la fila `ARS` de `currencies`, y cada instrumento cotiza en esa moneda. `marketdata` trae los precios del 2023-07-13 y del 2023-07-14. Las órdenes de ejemplo son las de Emiliano.

## Endpoints

`GET /api/users/{userId}/portfolio`

Devuelve `totalValue`, `availableCash` y las posiciones con cantidad, precio, valor de mercado, rendimiento total y rendimiento diario. La moneda no entra en el listado.

`GET /api/instruments?ticker=&name=&page=&size=`

Busca por ticker y/o por nombre, por coincidencia parcial y sin distinguir mayúsculas. `galicia` encuentra `Grupo Financiero Galicia`. Sin filtros recorre el catálogo, pero la respuesta es una página ordenada por ticker. `page` arranca en 0 y `size` en 20, con máximo 100. Con los 65 instrumentos del seed, la llamada sin filtros devuelve los primeros 20. `page.totalElements` queda en 65 y `page.totalPages` en 4. La respuesta es `{ "instruments": [...], "page": { "number", "size", "totalElements", "totalPages" } }`. Cada resultado trae el último `close` y el rendimiento diario.

`POST /api/orders`

Body:

```json
{
  "userId": 1,
  "instrumentId": 34,
  "side": "BUY",
  "type": "MARKET",
  "quantity": 1
}
```

Hay que mandar `quantity` o `amount`, uno solo. `quantity` es una cantidad entera de acciones. `amount` es un monto en pesos y se convierte con `floor(monto / precio)`. Si no alcanza para una acción, la orden se graba `REJECTED`.

- `MARKET` no usa el precio del body: si viene, se ignora sin error. Se ejecuta al `close` y queda `FILLED`.
- Si el instrumento no tiene un `close` válido en `marketdata`, una `MARKET` se graba `REJECTED`.
- `LIMIT` exige `price` y queda `NEW`. No se simula el mercado: una límite no pasa sola a `FILLED`.
- Si faltan pesos (compra) o acciones (venta), la orden se graba igual, con estado `REJECTED`. La respuesta es **201**.
- Un body inválido responde **400** y no inserta. Son inválidos: un `side` distinto de `BUY` o `SELL`, una límite sin precio o con precio no positivo, mandar cantidad y monto juntos o ninguno de los dos, una cantidad fraccionaria o no positiva y un monto no positivo.
- Usuario o instrumento inexistente: **404**.

`POST /api/orders/{orderId}/cancel`

No forma parte del enunciado como endpoint: el enunciado define el estado `CANCELLED` y la regla de que solo se cancelan órdenes `NEW`, pero no dice por dónde entra. Lo separé de `POST /api/orders` porque cancelar no es el mismo caso de uso que comprar o vender un activo: no crea una orden, cambia el estado de una que ya existe.

Solo una orden `NEW` pasa a `CANCELLED`. Otro estado responde **409** y una orden inexistente, **404**. Al cancelar se libera la reserva. Como no hay autenticación, el endpoint solo recibe el `orderId` y no verifica de qué usuario es la orden.

`GET /api/users/{userId}/orders?page=&size=`

No forma parte del enunciado. Está para verificar que todo funciona bien: después de crear o cancelar una orden, permite ver con qué estado quedó guardada. Sin `page` ni `size` devuelve la primera página: `page` 0 y `size` 20, máximo 100, de la más nueva a la más vieja. Emiliano (usuario 1) tiene 11 órdenes en el seed, así que esa llamada las trae todas y `page.size` queda en 20. `page.totalElements` queda en 11 y `page.totalPages` en 1. La respuesta es `{ "orders": [...], "page": { "number", "size", "totalElements", "totalPages" } }`.

## Evento al guardar una orden

Cada vez que una orden queda persistida, en el alta (`FILLED`, `NEW` o `REJECTED`) o en la cancelación, la API publica en SNS el mensaje `ORDER_PERSISTED`. La publicación corre después del commit: el cliente que recibe el aviso ya puede leer la fila.

```json
{
  "type": "ORDER_PERSISTED",
  "orderId": 1,
  "userId": 1,
  "instrumentId": 34,
  "side": "BUY",
  "status": "FILLED"
}
```

El mensaje no trae el portfolio ni el cálculo de la tenencia. Existe para que el cliente que nos consume, al guardar la orden, vuelva a consultar `GET /api/users/{userId}/portfolio` y recalcule sus activos: efectivo disponible, posiciones y rendimientos. `userId` dice de quién es la cuenta. La cancelación también se publica, porque libera la reserva de una orden `NEW`.

Si SNS no responde, la orden igual queda guardada y la API responde con el alta o la cancelación. El evento es un aviso, no parte de la transacción.

Para verlo en local, después de crear una orden:

```bash
docker compose exec localstack awslocal sqs receive-message --queue-url http://localhost:4566/000000000000/order-persisted-inspection
```

## Cómo se calcula la tenencia

No hay tabla de posiciones. `HoldingCalculator` (en `application/service`) recorre las órdenes del usuario, ordenadas por fecha. Lo usan tanto el portfolio como el alta de órdenes, así que el saldo que se valida al comprar o vender es el mismo que muestra el portfolio.

Cuentan las órdenes `FILLED`:

- `CASH_IN` suma `size` pesos. `CASH_OUT` los resta.
- `BUY` resta `size * price` del efectivo y suma `size` acciones. El costo de la posición sube `size * price`.
- `SELL` suma `size * price` al efectivo y baja la cantidad. El costo baja el precio promedio por las acciones vendidas.

`REJECTED` y `CANCELLED` no mueven nada.

Las `LIMIT` en estado `NEW` reservan: una compra reserva `size * price` pesos y una venta reserva `size` acciones. Los pesos disponibles son el efectivo liquidado menos esas reservas, así no se pueden apilar límites que superen el saldo. El valor total de la cuenta no resta la reserva: es el efectivo liquidado más la suma de `cantidad * close`.

El rendimiento total de una posición es `(close - precioPromedio) / precioPromedio * 100`. El precio promedio sale del costo acumulado de las compras `FILLED`, reducido en las ventas. El rendimiento diario es `(close - previousClose) / previousClose * 100`. Si `previousClose` es nulo o cero, el diario es nulo.

El último precio es el `close` de la fila de `marketdata` con mayor `date`. Los precios están en pesos. Las acciones no tienen fracción.

El efectivo es la moneda `ARS`. Las transferencias `CASH_IN` y `CASH_OUT` siguen siendo órdenes del seed: `instrumentid` queda vacío y `currency_id` apunta a `ARS`. `size` es el importe y `price` queda en 1. El alta pública solo acepta `BUY` y `SELL`, y copia en la orden la moneda del instrumento. El body no pide la moneda.

## Decisiones de diseño

La dependencia apunta hacia adentro.

- `domain` tiene el modelo, los enums, las excepciones, el comando `PlaceOrderCommand` y los puertos: los de entrada en `domain/usecases` y los de salida en `domain/repositories` (incluido `TransactionRunner`) y `domain/events` (`OrderEventPublisher`). No conoce Spring ni JPA.
- `application` tiene las implementaciones de los casos de uso (`application/usecases`) y los servicios `HoldingCalculator` y `PlaceOrderValidator` (`application/service`). Las clases no llevan `@Service`.
- `infrastructure` tiene las entidades JPA, los mappers, los adapters, la implementación de `TransactionRunner`, la mensajería y la configuración. `UseCaseConfig` registra cada caso de uso como bean.
- `infrastructure/interfaces/rest` tiene los controllers, los DTOs y el manejo de errores. Un controller solo habla con un puerto de entrada.

El modelo de dominio no es la entidad de la base. El mapper está en infraestructura. `ArchitectureTest` (ArchUnit) falla el build si el dominio depende de `application`, de `infrastructure`, de Spring, de JPA o de Hibernate, y si `application` depende de `infrastructure` o de Spring, o declara `@Service`. Que los controllers no usen adapters es una convención y ningún test la controla.

La transacción del alta y de la cancelación entra por `TransactionRunner`, implementado con `TransactionTemplate`. Al empezar se toma un lock pesimista del usuario para que dos compras concurrentes no usen el mismo saldo. En la cancelación, la orden se vuelve a leer después de tomar el lock.

`OrderEventPublisher` es un puerto del dominio. El adapter de SNS vive en infraestructura y habla con LocalStack. `OrderRepositoryAdapter` lo llama en `afterCommit`, así el aviso sale cuando la orden ya está confirmada.

El esquema parte de `database.sql`. Postgres guarda los identificadores sin comillas en minúscula (`accountnumber`, `instrumentid`, `userid`, `previousclose`, `date`).

- `orders.size` es `INT`. En compra y venta es una cantidad de acciones. En `CASH_IN` y `CASH_OUT` es un importe en pesos. Puede ser 0 solo cuando un monto no alcanza para una acción y la orden queda rechazada.
- `orders.price` y los precios de `marketdata` son `NUMERIC(10, 2)`. `open`, `high` y `low` aceptan nulo, igual que el dump.
- `marketdata.date` es `DATE`. El último precio es el `close` de la fecha más reciente.
- `currencies` guarda el código y el nombre. El seed carga `ARS`.
- `instruments.currency_id` apunta a la moneda de cotización. En este conjunto, todas las acciones usan `ARS`.
- `instruments.type` usa `ACCIONES`. El enunciado modela el peso como un instrumento de tipo `MONEDA`. Acá el peso está en `currencies` y no es un instrumento, así que no aparece en la búsqueda de activos ni en el listado de posiciones.
- `orders.currency_id` es obligatorio. Una compra o una venta exige `instrumentid`. Un `CASH_IN` o un `CASH_OUT` lo deja nulo. El check `orders_side_target` fija esa regla.
- Claves foráneas a `users`, `instruments` y `currencies`.
- Índices `orders(userid, status)`, `orders(userid, instrumentid, status)` y `marketdata(instrumentid, date desc)`.

Flyway es el dueño del esquema. Hibernate arranca con `ddl-auto: validate` y `open-in-view: false`.

Los montos de la respuesta usan dos decimales. Los precios y los porcentajes, cuatro. El redondeo es half up.

## Qué no hace

No autentica, no matchea puntas y no ejecuta sola una orden límite. No hay websocket de precios ni fracciones de acciones.
