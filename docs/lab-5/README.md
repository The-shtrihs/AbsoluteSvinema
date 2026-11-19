# Лабораторна робота №5: Docker + PostgreSQL + JdbcTemplate / JdbcClient

## AbsoluteSvinema — постійне сховище на PostgreSQL

У ЛР5 in-memory репозиторії (Labs 2–4) замінено на PostgreSQL. Чотири DAO реалізовано двома способами — **`JdbcTemplate`** (сеанси, місця) і **`JdbcClient`** (квитки, користувачі). Контролери, Thymeleaf-сторінки та REST API не змінювались: змінився лише шар репозиторіїв. Багатозаписні операції сервісу виконуються в **транзакції** з перевіреним **rollback**. Застосунок і БД запускаються однією командою через Docker Compose.

---

## 1. Запуск

### 1.1. Передумови

- Docker з Compose v2 (`docker compose version`). Перевірено на Docker 29 / Compose 5 (OrbStack, macOS).
- Для режиму B (див. нижче) — JDK 21. Maven окремо не потрібен: є `./mvnw`.

### 1.2. Локальний `.env`

```bash
cp .env.example .env
# відредагуй .env: DB_PASSWORD і PASSWORD_SALT — свої локальні значення
```

| Ключ | Призначення |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Назва БД і обліковий запис PostgreSQL (контейнер `db` створює їх при першому старті) |
| `DB_PORT` | Порт PostgreSQL **на хості** (за замовчуванням 5432) |
| `APP_PORT` | Порт застосунку на хості (за замовчуванням 8080) |
| `PASSWORD_SALT` | Додатковий секрет для `PasswordEncoder` (див. розділ 5) |

`.env` ігнорується Git (`.gitignore`) і не потрапляє в Docker-образ (`.dockerignore`). У Git лежить лише `.env.example` з вигаданими значеннями.

> `DB_PASSWORD` PostgreSQL зчитує лише під час **першої** ініціалізації volume. Щоб змінити його пізніше, потрібно видалити volume (`docker compose down -v`, див. попередження нижче).

### 1.3. Режим A — застосунок і БД у Docker Compose

```bash
docker compose up --build -d     # зібрати образ app і запустити db + app
docker compose ps                # db має бути (healthy), app — Up
docker compose logs -f app       # логи Spring Boot
```

- Сайт: <http://localhost:8080/>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Зупинка та перезапуск:

```bash
docker compose restart app       # перезапуск лише застосунку, дані БД зберігаються
docker compose down              # прибрати контейнери; volume postgres_data ЗАЛИШАЄТЬСЯ
docker compose up -d             # дані на місці
```

> ⚠️ **`docker compose down -v` ВИДАЛЯЄ volume `postgres_data`, тобто ВСІ дані PostgreSQL** (сеанси, квитки, зареєстрованих користувачів). Використовуй лише свідомо, коли потрібна чиста БД.

### 1.4. Режим B — лише PostgreSQL у Docker, Spring Boot локально

```bash
docker compose stop app          # якщо app був запущений — звільнити порт 8080
docker compose up -d db
./mvnw spring-boot:run           # або Run у IDE з робочою директорією = корінь проєкту
```

Spring локально читає `./.env` через `spring.config.import=optional:file:./.env[.properties]` і підключається до `localhost:${DB_PORT}`.

**Чому `DB_HOST` різний:** у `application.properties` стоїть `${DB_HOST:localhost}`. Локальний процес бачить PostgreSQL на `localhost` (порт проброшено `127.0.0.1:${DB_PORT}:5432`). Усередині Docker-мережі `localhost` — це сам контейнер `app`, тому `compose.yaml` задає для нього `DB_HOST=db` (ім'я сервісу) і `DB_PORT=5432` (внутрішній порт).

### 1.5. Як влаштовано Docker

| Файл | Що робить |
|---|---|
| `Dockerfile` | Multi-stage: `maven:3.9-eclipse-temurin-21` збирає jar (`-DskipTests`), `eclipse-temurin:21-jre` лише запускає `app.jar` на порту 8080 |
| `compose.yaml` | Сервіси `db` (`postgres:17-alpine`, volume `postgres_data`, healthcheck `pg_isready`) і `app` (`env_file: .env`, `depends_on: db: condition: service_healthy`). Порти опубліковані лише на `127.0.0.1` |
| `.dockerignore` | Не пускає в build context `.git`, `target`, `.env*` |

`condition: service_healthy` означає, що `app` стартує тільки коли PostgreSQL реально приймає з'єднання, а не просто коли контейнер створено.

### 1.6. Перегляд БД

```bash
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\dt"'
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'   # інтерактивно
```

```sql
SELECT id, movie_title, hall, ticket_price FROM screenings ORDER BY id;
SELECT screening_id, COUNT(*) FROM seats GROUP BY screening_id ORDER BY screening_id;
SELECT id, seat_id, user_id, customer_name FROM tickets ORDER BY id;
SELECT id, email, role FROM users ORDER BY id;
```

---

## 2. Схема БД

Схема — `src/main/resources/schema.sql`, виконується при кожному старті (`spring.sql.init.mode=always`), ідемпотентна (`CREATE TABLE IF NOT EXISTS`). JPA, Flyway, Liquibase не використовуються.

```text
screenings (id IDENTITY)
  └── seats (id IDENTITY, screening_id FK, row_number, seat_number)
        └── tickets (id IDENTITY, seat_id FK UNIQUE, user_id FK NULL)
users (id IDENTITY) ───────────────────────────────┘
```

| Таблиця | PK | Зв'язки та обмеження |
|---|---|---|
| `screenings` | `BIGINT GENERATED ALWAYS AS IDENTITY` | `ticket_price NUMERIC(10,2) ≥ 0`, `starts_at TIMESTAMP`, `movie_duration_minutes > 0` |
| `seats` | identity | FK → `screenings`, `UNIQUE(screening_id, row_number, seat_number)` |
| `users` | identity | `UNIQUE(email)`; email зберігається нормалізованим (trim + lower case) |
| `tickets` | identity | FK → `seats`, FK → `users` (**nullable** — гостьове бронювання), `UNIQUE(seat_id)` |

**Усі чотири PK генерує БД.** INSERT не передає `id`; згенероване значення повертається через `KeyHolder`.

`ON DELETE CASCADE` навмисно не використовується: `ScreeningService.delete` видаляє записи явно в порядку tickets → seats → screening в одній транзакції (розділ 4).

Java ↔ SQL: `BigDecimal` ↔ `NUMERIC`, `LocalDateTime` ↔ `TIMESTAMP`, `Long userId = null` ↔ `NULL`.

---

## 3. DAO: JdbcTemplate та JdbcClient

Інтерфейси репозиторіїв (`repository/*Repository.java`) залишились тими самими (додано лише `ScreeningRepository.findByTitleAndHall`). Реалізації — у `repository/jdbc/`. Старі in-memory класи активні лише з профілем `memory` (`@Profile("memory")`), JDBC — з `@Profile("!memory")`, тож для кожного інтерфейсу є рівно один bean.

| Репозиторій | API | CRUD | Пошук за критеріями (не за ID) |
|---|---|---|---|
| `JdbcScreeningRepository` | **JdbcTemplate** | `save` (INSERT + `GeneratedKeyHolder` / UPDATE), `findById`, `findAll`, `deleteById` | `findByTitleAndHall(title, hall)` — підрядок назви без урахування регістру (`LIKE ... ESCAPE`) + точна назва залу без урахування регістру, `ORDER BY id` |
| `JdbcSeatRepository` | **JdbcTemplate** | те саме | `findByScreeningId` — `ORDER BY row_number, seat_number` |
| `JdbcTicketRepository` | **JdbcClient** | `save` (`.update(keyHolder, "id")` / UPDATE), `findById`, `findAll`, `deleteById` | `findBySeatId`, `findBySeatIdIn(Collection)` (порожня колекція → `List.of()` без `IN ()`), `findByUserId` |
| `JdbcUserRepository` | **JdbcClient** | те саме | `findByEmail` (нормалізація як у `UserService`) |

Спільні правила:

- **Тільки параметризований SQL** (`?` у JdbcTemplate, `:name` у JdbcClient); значення користувача ніколи не конкатенуються в SQL. У пошуку за назвою символи `%` і `_` екрануються.
- `save(record з id = null)` — INSERT; `save(record з id)` — UPDATE існуючого. Неіснуючий ID → `IllegalArgumentException`, «тихого» INSERT немає. Квиток не можна перенести на інше місце.
- **Гонка за місце/email:** перевірка `findBySeatId` у `TicketService` лишилась, але справжній захист — `UNIQUE(seat_id)` / `UNIQUE(email)`. Порушення (`DuplicateKeyException`) перетворюється на той самий `IllegalStateException`, який контролери вже показують як «місце зайняте» / «email вже існує» (REST — 409). Інші помилки БД не приховуються.

**JdbcTemplate** (приклад з `JdbcScreeningRepository`):

```java
KeyHolder keyHolder = new GeneratedKeyHolder();
jdbcTemplate.update(connection -> {
    PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
    ps.setString(1, screening.movieTitle());
    // ...
    return ps;
}, keyHolder);
return keyHolder.getKey().longValue();
```

**JdbcClient** (приклад з `JdbcTicketRepository`):

```java
jdbcClient.sql("SELECT ... FROM tickets WHERE seat_id IN (:seatIds) ORDER BY id")
        .param("seatIds", seatIds)
        .query(ROW_MAPPER)
        .list();
```

---

## 4. Транзакції

| Метод | Що всередині транзакції |
|---|---|
| `ScreeningService.create` — `@Transactional` | 1 INSERT у `screenings` + 15 INSERT у `seats` (3 ряди × 5 місць) |
| `ScreeningService.delete` — `@Transactional` | DELETE квитків → DELETE місць → DELETE сеансу |

- `JdbcTemplate` і `JdbcClient` працюють через один `DataSource`, тому автоконфігурований `DataSourceTransactionManager` об'єднує їх в одну транзакцію (у `delete` беруть участь обидва). Ручної роботи з `Connection` немає.
- Будь-який `RuntimeException` (зокрема `DataIntegrityViolationException` від БД) → rollback усього.
- Інші методи виконують одну зміну, тому окремий `@Transactional` їм не потрібен.

**Інтерфейс сервісу:** `ScreeningManagement` (create / update / delete / findById / findFiltered) реалізує `ScreeningService`; `ScreeningRestController` залежить від інтерфейсу, а не від класу. `UserService` і далі реалізує `UserDetailsService`.

### 4.1. Перевірка успішної транзакції

`POST /api/screenings` (адмін) → `201 Created`, у БД:

```sql
SELECT COUNT(*) FROM seats WHERE screening_id = <id>;   -- 15
```

### 4.2. Перевірка rollback після часткового запису

Ідея: змусити впасти **третій** INSERT місця, коли сеанс і місця 1–2 вже вставлені в цій самій транзакції. Для цього **тимчасово** додається обмеження (лише на локальній тестовій БД, і обов'язково прибирається):

```bash
# 1. тимчасове обмеження; NOT VALID — не перевіряти вже наявні рядки
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "ALTER TABLE seats ADD CONSTRAINT tmp_no_seat_3 CHECK (seat_number <> 3) NOT VALID"'

# 2. валідний POST /api/screenings (Swagger UI або Postman, як адмін) з movieTitle "Rollback Film"
#    → 500: INSERT screenings OK, seat 1 OK, seat 2 OK, seat 3 порушує CHECK → rollback

# 3. прибрати обмеження
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "ALTER TABLE seats DROP CONSTRAINT tmp_no_seat_3"'
```

```sql
SELECT COUNT(*) FROM screenings WHERE movie_title = 'Rollback Film';   -- 0
SELECT last_value FROM pg_sequences WHERE sequencename LIKE 'screenings_id%';
-- лічильник identity зріс на 1: INSERT сеансу справді відбувся і був відкочений
```

Те саме автоматизовано в тесті `ScreeningTransactionTest` (H2, синтаксис `NOCHECK` замість `NOT VALID`): успішний `create` додає 1 сеанс + 15 місць, а при помилці на 3-му місці кількість сеансів і місць не змінюється.

---

## 5. PasswordEncoder і `PASSWORD_SALT`

- BCrypt **і далі сам генерує випадкову сіль для кожного пароля** (тому однаковий пароль дає різні хеші).
- `PASSWORD_SALT` з `.env` — технічно не сіль, а **pepper**: спільний секрет, що зберігається поза БД. `PepperedPasswordEncoder` спочатку рахує `HMAC-SHA256(PASSWORD_SALT, password)`, кодує в Base64 (44 символи — менше за ліміт BCrypt у 72 байти) і передає це в `BCryptPasswordEncoder`. `encode()` і `matches()` роблять те саме перетворення.
- Злив лише БД не дає перебирати паролі без pepper.
- Без `PASSWORD_SALT` застосунок не стартує; значення секрету не виводиться ні в логи, ні в повідомлення про помилки.
- ⚠️ Зміна або втрата `PASSWORD_SALT` робить **усі збережені хеші непридатними** — користувачі не зможуть увійти.

---

## 6. REST API: перевірка у Swagger UI / Postman

API та його OpenAPI-документація (summary, description, параметри, моделі, коди відповідей) не змінювались — див. [ЛР4](../lab-4/README.md). Колекція Postman `docs/lab-4/postman/AbsoluteSvinema-Lab4.postman_collection.json` працює без змін, тепер — на PostgreSQL.

Короткий сценарій:

1. `GET /api/screenings` — публічно, `200`.
2. Увійти як `admin@svinema.ua` / `admin123` (форма `/login` у браузері або запит Login у Postman) — з'являється сесійна кука `JSESSIONID`.
3. `GET /api/csrf` → взяти `token`, надсилати його в заголовку `X-CSRF-TOKEN` з усіма POST/PUT/PATCH/DELETE.
4. `POST /api/screenings` → `201` + `Location`; у БД — сеанс і 15 місць.
5. `GET /api/screenings?title=...&hall=...` (фільтри), `?page=0&size=2` (пагінація).
6. `PUT /api/screenings/{id}` → `200`.
7. `PATCH` з `Content-Type: application/json-patch+json`, тіло `[{"op":"replace","path":"/ticketPrice","value":199.99}]` → `200`.
8. `PATCH` з `Content-Type: application/merge-patch+json`, тіло `{"hall":"Hall 6"}` → `200`.
9. `DELETE /api/screenings/{id}` → `204`; квитки, місця і сеанс зникли з БД.

| Код | Коли |
|---|---|
| 200 / 201 / 204 | успішні GET, PUT, PATCH / POST / DELETE |
| 400 | невалідне тіло, `page < 0`, `size` поза 1..100 |
| 401 | зміна даних без входу |
| 403 | роль USER або відсутній/хибний CSRF-токен |
| 404 | неіснуючий `id` |
| 409 | невдалий `test` у JSON Patch; конфлікт унікальності |
| 415 | PATCH з `application/json` |
| 500 | неочікувана помилка БД (напр., демонстрація rollback) |

---

## 7. Результати фінальної перевірки

Прогін виконано на Docker Compose (PostgreSQL 17) після всіх комітів ЛР5. Дані до/після — реальні значення з БД.

| Перевірка | Результат |
|---|---|
| `./mvnw test` (H2 у режимі PostgreSQL, окрема БД на кожен контекст) | 145 тестів, 0 помилок |
| `docker compose down` → `up --build -d` | `db` healthy, `app` Up; користувачі 3 → 3, сеанси 3 → 3 (дані збережені) |
| POST без входу / як USER / як ADMIN без CSRF | 401 / 403 / 403 |
| GET `/api/screenings/999999`, `?page=-1`, POST з порожньою назвою | 404 / 400 / 400 |
| POST як ADMIN | 201, `Location: /api/screenings/7`, у БД 15 місць |
| Фільтр `title=acceptance&hall=HALL 5` | `totalElements: 1` |
| Пагінація `page=0&size=2` | `size 2, totalElements 4, totalPages 2` |
| PUT / JSON Patch / JSON Patch з хибним `test` / Merge Patch / PATCH як `application/json` | 200 / 200 / 409 / 200 / 415 |
| Рядок у БД після змін | `Acceptance Film PUT \| Hall 6 \| 199.99` |
| Бронювання гостем, потім DELETE сеансу | квиток був (1), після `204`: сеанс 0, місця 0, квитки 0 |
| Rollback (тимчасовий `CHECK seat_number <> 3`) | 500; identity 7 → 8, рядків `Rollback Film` — 0; обмеження прибрано |
| Подвійне бронювання одного місця (MVC; перевірено при коміті 4) | у БД рівно 1 квиток на місце; гостьовий `user_id = NULL`; квиток зареєстрованого користувача має його `user_id` |
| Адмін: перейменування / скасування квитка (перевірено при коміті 4) | `customer_name` змінено в БД / рядок видалено |
| `docker compose restart app` | вхід `user@svinema.ua` працює, «Мої квитки» на місці, демо-дані не дублюються |
| Секрет у БД | 0 хешів містять значення `PASSWORD_SALT` |

---

## 8. Змінені / нові файли ЛР5

| Коміт | Файли |
|---|---|
| `build(docker): add PostgreSQL and app compose setup` | `Dockerfile`, `.dockerignore`, `compose.yaml`, `.env.example`, `pom.xml`, `application.properties`, `schema.sql`, `src/test/resources/config/application.properties` |
| `feat(security): load password pepper from env` | `PepperedPasswordEncoder`, `SecurityConfig`, тест |
| `feat(db): implement screening and seat JdbcTemplate DAOs` | `JdbcScreeningRepository`, `JdbcSeatRepository`, `ScreeningRepository`, `ScreeningService.findFiltered`, тест |
| `feat(db): implement ticket and user JdbcClient DAOs` | `JdbcTicketRepository`, `JdbcUserRepository`, `@Profile("memory")` для in-memory, тест |
| `feat(service): make multi-record operations transactional` | `ScreeningManagement`, `@Transactional` у `ScreeningService`, `ScreeningRestController`, `ScreeningTransactionTest` |
| `docs(lab5): document and verify JDBC REST workflows` | цей файл, `README.md` |
