# AbsoluteSvinema — лабораторна робота №6: Spring Data JPA

> **Практичний план: 6 невеликих комітів + 6 готових промптів для Codex.**
> База: **`AbsoluteSvinema-main(9).zip`** (Spring Boot 4.1.2 / Java 21 / PostgreSQL 17 / Docker Compose / JDBC з ЛР №5 / MVC, REST і Swagger із ЛР №2–4).
>
> **Правило роботи:** спочатку даємо Codex *лише один* промпт → переглядаємо повний diff та розбираємо змінений код → один адекватний запуск/перевірка → **комітимо вручну** → наступний крок. Codex **не створює коміти**, якщо ти не попросив окремо.

## 0. Що конкретно вимагає методичка

Методичка **Spring-lab-06.docx** (тема «Spring Data»):

1. Реляційна БД з тестовими даними, генерацією ID і зв'язком **один-до-багатьох** між принаймні двома таблицями.
2. `@Entity` для об'єктно-реляційного відображення; DTO — за потреби.
3. Репозиторії на основі **`CrudRepository`**, де продемонстровано **усі три** способи запиту:
   - JPQL через `@Query`;
   - JPQL через `@NamedQuery`;
   - query method, сформований Spring Data **за назвою методу**.
4. Сервісна бізнес-логіка з **хоча б одним `@Transactional` методом**.
5. Доступ до бізнес-логіки через **REST API**.
6. OpenAPI: короткий і повний опис, параметри/моделі, HTTP-коди **кожної** операції.
7. Тестування REST і **транзакцій як при успіху, так і при помилці/rollback**.

У нашому проєкті багато цього **вже готове**: БД і Docker, SQL-таблиці та IDENTITY, зв'язок `screenings (1) → (N) seats`, MVC+REST+Swagger, DTO, `ScreeningManagement`, `ScreeningService.create()` і `delete()` з `@Transactional`.

**Мета ЛР №6:** додати саме JPA/ORM і Spring Data, *не* переписувати готові контролери, DTO, бізнес-правила та дизайн сайту.

---

## 1. Яку архітектуру обираємо

```text
Браузер / Postman
        |
MVC / REST-контролери (без змін)
        |
ScreeningService, TicketService, UserService (без дублювання логіки)
        |
Власні інтерфейси: ScreeningRepository, SeatRepository,
                   TicketRepository, UserRepository
        |
        +-- profile memory --> InMemory*Repository    (ЛР №2–4)
        |
        +-- default/JDBC   --> Jdbc*Repository        (ЛР №5)
        |
        +-- profile jpa    --> Jpa*RepositoryAdapter  (ЛР №6)
                                  |
                         Spring Data CrudRepository
                                  |
                         Hibernate / JPA / PostgreSQL
```

### Чому не перетворювати наявні records на `@Entity`?

- `Screening`, `Seat`, `Ticket`, `User` зараз — **records з перевірками інваріантів**; їх використовує вся програма.
- JPA-сутності зручніше тримати окремо в `repository/jpa/entity`: з `@Id`, `@GeneratedValue`, `@ManyToOne`, `@OneToMany`, конструктором без аргументів і керованим станом.
- Адаптери між JPA-entity і доменними records збережуть **існуючий контракт сервісів**. Це трохи коду, зате мінімум ризику для вже готових лабораторних.
- **Не створюємо нових REST-контролерів і не робимо паралельну бізнес-логіку.** Ті самі `/api/screenings` та HTML-сторінки працюватимуть із JPA, якщо активувати профіль.

### Профілі, щоб не отримати кілька реалізацій одного інтерфейсу

| Режим | Профіль | Які репозиторії обслуговують сервіси |
|---|---|---|
| Старий in-memory | `memory` | `InMemory*Repository` |
| ЛР №5 (за замовчуванням) | без профілю / JDBC | `Jdbc*Repository` |
| **ЛР №6** | `jpa` | `Jpa*RepositoryAdapter` + Spring Data |

Зараз JDBC-класи мають `@Profile("!memory")`. **Після додавання JPA цей вираз обов'язково треба звузити** до `@Profile("!memory & !jpa")` (або іншого еквівалентного, перевіреного способу), інакше при `jpa` одночасно створяться JDBC- і JPA-адаптери для того самого інтерфейсу.

Для автоматичних Spring Data репозиторіїв можна застосувати `spring.data.jpa.repositories.enabled=false` у базових властивостях та `true` у `application-jpa.properties`. Таким чином нові Spring Data repository beans створюються саме в режимі JPA. Не вводь ручного складного `EntityManagerFactory`, якщо Boot справляється сам.

### Docker залишається

- `compose.yaml`, `Dockerfile` і PostgreSQL 17 **вже є**.
- Та сама БД і ті самі чотири таблиці: `screenings`, `seats`, `tickets`, `users`.
- Та сама `.env` з `DB_*` і `PASSWORD_SALT`. **Секрет не змінюємо**, інакше вже збережені паролі можуть стати непридатними.
- У режимі JPA бажано **`spring.jpa.hibernate.ddl-auto=validate`**, а не `create` або `create-drop`: не знищуємо таблиці/дані ЛР №5. Схема `schema.sql` залишається джерелом SQL-таблиць.

---

## 2. Коротка карта комітів

| № | Назва коміту | Результат | Коли перевіряємо |
|---|---|---|---|
| **1** | `build(jpa): configure Spring Data JPA profile` | JPA-залежність, профіль і запуск без конфліктів | Запуск звичайного JDBC і `memory` |
| **2** | `feat(jpa): map cinema entities and relationships` | 4 ORM-сутності, `Screening 1:N Seat` | Схема відповідає SQL |
| **3** | `feat(jpa): add CrudRepository queries` | 4 Spring Data репозиторії, `@Query`, `@NamedQuery`, derived method | Репозиторії компілюються, JPQL валідний |
| **4** | `feat(jpa): integrate JPA repository adapters` | 4 адаптери, збережені існуючі сервіси/REST/MVC | Запуск `jpa`, CRUD, бронювання, login |
| **5** | `test(jpa): verify transactions and repository behavior` | Перевірки CRUD, зв'язків, пошуку й rollback | Помилка не залишає половину даних |
| **6** | `docs(lab6): verify and document Spring Data API` | README ЛР №6, Postman/Swagger-перевірка, сценарій захисту | Підсумкове проходження вимог |

> **Невелике застереження:** після комітів №1–3 JPA-режим може бути *ще не завершений*, оскільки адаптерів немає. Це нормально: на кожному етапі повинен працювати попередній JDBC-режим; **повний запуск `jpa` перевіряємо після коміту №4**.

---

## КОМІТ 1 — JPA-залежність і конфігурація профілів

**Commit:** `build(jpa): configure Spring Data JPA profile`

**Що повинно змінитися:**
- `pom.xml`: додати `spring-boot-starter-data-jpa` (версію управляє Spring Boot parent); зберегти starter JDBC, PostgreSQL, інші залежності.
- `src/main/resources/application.properties`: базові налаштування, без небезпечного автоматичного створення таблиць; за потреби вимкнути автоматичну реєстрацію Spring Data репозиторіїв поза `jpa`.
- `src/main/resources/application-jpa.properties`: ввімкнути Spring Data JPA, `ddl-auto=validate`, за бажанням `open-in-view=false` за умови коректного керування транзакціями.
- `src/main/java/.../repository/jdbc/Jdbc*Repository.java`: змінити профіль з `!memory` на `!memory & !jpa`, щоб в режимі JPA ці beans не створювалися.
- Для зручності дописати до `.env.example` **необов'язковий** рядок `SPRING_PROFILES_ACTIVE=jpa` з поясненням, як ним користуватися; не змінювати реальний `.env`.

**Не робити:** новий Dockerfile, нову БД, переписування `schema.sql`, нові сутності/DAO, зміну `PASSWORD_SALT`.

**Промпт Codex №1 (скопіюй цілком):**

```text
We are implementing Spring lab #6 (Spring Data JPA) in the existing AbsoluteSvinema project.
Read the current project before editing. Work ONLY on commit #1: dependency and profile setup.

Current architecture: Spring Boot 4.1.2, Java 21, PostgreSQL via Docker Compose,
existing four custom repository interfaces, JdbcTemplate/JdbcClient repositories
annotated @Profile("!memory"), and memory repositories annotated @Profile("memory").

Tasks:
1. Add spring-boot-starter-data-jpa to pom.xml using Boot dependency management.
2. Introduce a minimal opt-in "jpa" profile (application-jpa.properties). Keep the
   current default JDBC behavior and the existing "memory" profile unchanged.
3. Ensure existing JDBC repository beans are NOT active with the "jpa" profile
   (update their @Profile condition accordingly).
4. Arrange Spring Data JPA repository activation so Spring Data repository interfaces
   that will be added later are only active in the "jpa" profile. Prefer simple
   Boot properties over custom infrastructure.
5. Configure Hibernate to validate the existing schema in JPA mode; NEVER drop or
   recreate existing PostgreSQL tables. Preserve schema.sql, Docker Compose,
   datasource credentials, and PASSWORD_SALT.
6. Briefly document how to select the jpa profile for both a Docker Compose app
   and a locally run Spring Boot process, without hardcoding secrets.

Do NOT add JPA entities/repositories/adapters yet. Do NOT change services, controllers,
security, Swagger, tests unrelated to the setup, or UI.
Do NOT commit. Show changed files and explain the configuration.
Verify current default JDBC profile still starts if possible. The jpa profile
may not be runnable until later commits, which is expected.
```

**Одна перевірка:** проект збирається; default/JDBC запускається; profile `memory` не підхоплює JDBC-beans. `jpa` тут ще не тестуємо повністю.

---

## КОМІТ 2 — @Entity та зв'язок один-до-багатьох

**Commit:** `feat(jpa): map cinema entities and relationships`

**Що створюємо:** пакет на кшталт `ua.kpi.absolutesvinema.repository.jpa.entity`:

- `ScreeningEntity` → `screenings`: `id`, `movieTitle`, `movieDescription`, `movieDurationMinutes`, `startsAt`, `hall`, `ticketPrice`.
- `SeatEntity` → `seats`: `id`, `screening` (`@ManyToOne`), `rowNumber`, `seatNumber`.
- `TicketEntity` → `tickets`: `id`, `seat` (зв'язок), nullable `user` (гостьове бронювання), `customerName`, `bookedAt`.
- `UserEntity` → `users`: `id`, `email`, `password`, `fullName`, `role`.

**Зв'язок ЛР:** у `ScreeningEntity` зробити `@OneToMany(mappedBy = "screening")` зі списком `SeatEntity`; у `SeatEntity` — власника `@ManyToOne` + `@JoinColumn(name="screening_id")`. Це демонстрація **1:N** без додаткових таблиць.

**Ключові нюанси:**
- `@Id + @GeneratedValue(strategy = GenerationType.IDENTITY)` для реальних IDENTITY-полів у SQL.
- Явно зіставити SQL-імена `movie_title`, `screening_id`, `row_number`, `seat_number`, `seat_id`, `user_id`, `booked_at`, `full_name` тощо.
- Врахувати `movie_description TEXT`, `ticket_price NUMERIC(10,2)`, `starts_at TIMESTAMP`, nullable `tickets.user_id`, `UNIQUE` для квитка на одне місце (обмеження вже є в SQL).
- Ніяких `CascadeType.ALL` / `orphanRemoval` лише заради «красивого ORM»: сервіс уже керує видаленням квитків, місць і сеансу в певному порядку.
- Не віддавати `@Entity` безпосередньо через JSON; залишити наявні DTO і records.
- Для JPA передбачити конструктор без аргументів (може бути `protected`), доступні поля/аксесори та правильні типи.

**Промпт Codex №2:**

```text
Implement ONLY commit #2 for Spring lab #6: JPA entity mappings.
Inspect the actual src/main/resources/schema.sql and domain records before editing.

Create four separate, minimal JPA entity classes (ScreeningEntity, SeatEntity,
TicketEntity, UserEntity), preferably in repository.jpa.entity, mapped to the
EXISTING PostgreSQL screenings, seats, tickets, users tables. Do not replace
existing domain records or change their invariants.

Use @Entity, @Table, @Id, @GeneratedValue(strategy = GenerationType.IDENTITY),
proper column names/types/nullability, and JPA-compliant constructors.
Demonstrate the required one-to-many relationship: ScreeningEntity has a
@OneToMany(mappedBy="screening") collection of SeatEntity, and SeatEntity
has @ManyToOne with @JoinColumn(name="screening_id").
Map TicketEntity to SeatEntity and nullable UserEntity using appropriate
relationships; preserve guest tickets. Keep associations lazy where practical.

Do NOT add automatic cascade deletes, new join tables, schema migrations,
new controllers, new DTOs, or business logic. No Lombok required.
Do NOT change schema.sql unless a genuine mapping incompatibility is found;
explain any such issue before changing the schema.
Do NOT commit. Show all new files and explain every entity relationship.
Verify compilation. Hibernate schema validation will be checked after wiring.
```

**Одна перевірка:** назви таблиць/колонок відповідають `schema.sql`; `Screening → Seats` реально є 1:N; `Ticket.user` може бути `null`.

---

## КОМІТ 3 — CrudRepository + три способи пошуку

**Commit:** `feat(jpa): add CrudRepository queries`

**Що створюємо:** чотири Spring Data інтерфейси у `repository/jpa/data` (імена на зразок `ScreeningJpaDataRepository`, щоб не сплутати з існуючим `ScreeningRepository`):

```java
interface ScreeningJpaDataRepository extends CrudRepository<ScreeningEntity, Long> { /* ... */ }
```

**Обов'язково продемонструвати три способи:**

1. **Метод за назвою**, наприклад `findByScreening_IdOrderByRowNumberAscSeatNumberAsc(Long screeningId)` в `SeatJpaDataRepository`; для квитків — `findByUser_Id(...)`, `findBySeat_Id(...)`.
2. **JPQL `@Query`**, наприклад пошук кіносеансів за частиною назви з ігноруванням регістру. Запит працює з **`ScreeningEntity` та її властивостями**, а не з SQL-таблицею `screenings` і полем `movie_title`.
3. **JPQL `@NamedQuery`** на сутності, наприклад іменований пошук сеансів за залом; викликати його через відповідний Spring Data метод. Він має реально використовуватися, а не просто бути декларацією для галочки.

**Не забути:** загальний пошук `findByTitleAndHall(title, hall)` має підтримувати **обидва optional-фільтри** і ту саму поведінку, що у ЛР №5: title — підрядок case-insensitive, hall — точне порівняння case-insensitive, порядок за ID. Якщо змінювати пріоритет або обробку `%`, `_`, `\` — це вже зміна поведінки, яку треба тестувати.

**Промпт Codex №3:**

```text
Implement ONLY commit #3: Spring Data JPA CrudRepository interfaces and searches.
Review the current custom repository contracts and JDBC search behavior first.

Create a Spring Data CrudRepository<Entity, Long> interface for each of the four
new JPA entities. Use distinct names (e.g. ScreeningJpaDataRepository) to avoid
conflicts with the existing domain repository interfaces.

The lab requires three demonstrable query mechanisms:
1. A DERIVED query method parsed from its name (e.g. seats by screening id ordered
   by row and seat number, and tickets by user/seat).
2. A genuine JPQL @Query with entity/property names, not native SQL.
3. A genuine JPQL @NamedQuery declared on an entity and invoked through a
   Spring Data repository method.

Make each query useful for the current application, not dead demonstration code.
Preserve existing search semantics: optional title substring filter,
case-insensitive exact hall, and stable ordering. Carefully handle literal LIKE
wildcards (% and _) so existing filtering behavior doesn't regress.

Do not yet implement adapters or modify services/controllers. No extra tables,
complex Specifications, query DSL libraries, or redundant REST endpoints.
Do NOT commit. Explain exactly which method demonstrates @Query, @NamedQuery,
and a derived query, and why their JPQL is valid.
```

**Одна перевірка:** `@NamedQuery` не плутається з derived method, `@Query` не містить назв SQL-колонок, немає зайвої дубльованої бізнес-логіки.

---

## КОМІТ 4 — Адаптери JPA до наявних сервісів

**Commit:** `feat(jpa): integrate JPA repository adapters`

**Що створюємо:**
- `JpaScreeningRepositoryAdapter implements ScreeningRepository`;
- `JpaSeatRepositoryAdapter implements SeatRepository`;
- `JpaTicketRepositoryAdapter implements TicketRepository`;
- `JpaUserRepositoryAdapter implements UserRepository`.

Кожен адаптер з `@Repository @Profile("jpa")` викликає свій Spring Data `CrudRepository` і робить просте перетворення **Entity ↔ domain record**.

**Обов'язково зберегти контракт**:

| Старий метод сервісів | Що робить JPA-адаптер |
|---|---|
| `save(record)` | insert з IDENTITY або update чинного запису; повернути record із ID |
| `findById(id)` | `Optional<Record>` |
| `findAll()` | `List<Record>` у стабільному порядку |
| `deleteById(id)` | видалити за ID |
| `ScreeningRepository.findByTitleAndHall` | використати JPQL-репозиторії, як у коміті 3 |
| `SeatRepository.findByScreeningId` | усі місця сеансу у порядку ряд/місце |
| `TicketRepository.findBySeatId`, `findBySeatIdIn`, `findByUserId` | усі потрібні запити для бронювання/видалення |
| `UserRepository.findByEmail` | коректний пошук із нормалізацією email |

**Критичні деталі:**
- `Ticket.user_id` nullable: гостьові бронювання не мають зламатися.
- Не допустити N+1/lazy exceptions під час перетворення зв'язків; якщо треба — `@Transactional(readOnly=true)` на методах, доступ через ідентифікатор асоціації або один цільовий fetch, без зайвої складності.
- Під час оновлення **не створювати другий рядок**; зберігати той самий ID та поточні незмінні зв'язки.
- Повторне бронювання одного `seat_id` не повинно створити два квитки (є SQL `UNIQUE`), і на рівні MVC очікується зрозуміла помилка, а не випадковий 500.
- `ScreeningService` і `TicketService` лишаються власниками бізнес-правил. Не дублювати їх у адаптерах.

**Промпт Codex №4:**

```text
Implement ONLY commit #4: connect the new Spring Data JPA repositories to
existing AbsoluteSvinema services via four minimal repository adapters.

Current services depend on our existing custom ScreeningRepository,
SeatRepository, TicketRepository, UserRepository interfaces. DO NOT replace
those service interfaces or rewrite controllers, records, DTOs, or business rules.

Create @Repository @Profile("jpa") adapter classes implementing each custom
repository interface. Each delegates CRUD/search to the new CrudRepository
interfaces and maps between JPA entities and existing immutable domain records.
Preserve all method contracts, sorting, title/hall filtering, generated IDs,
nullable ticket userId for guests, email normalization, ticket ownership, and
existing duplicate-seat error behavior.

Pay attention to JPA lazy associations, transaction boundaries, and unique
constraint errors. No unexpected 500 for double-booking. No unnecessary
cascading or new data access frameworks. No new REST endpoints.

Verify the three modes do not register competing beans:
- no profile -> original JDBC repositories;
- memory -> original memory repositories;
- jpa -> new JPA adapters.

With the existing PostgreSQL Docker database, run the jpa profile and validate
that startup succeeds, demo data is not duplicated, login works, screening CRUD
and seat booking/cancellation behave correctly.
Do NOT commit. Show all changed files, explain the mappings and any failures.
```

**Одна перевірка:** `SPRING_PROFILES_ACTIVE=jpa docker compose up --build` після застосування профілю; GET, POST screening, 15 seats, login, booking, admin ticket, deletion; після перезапуску дані залишилися.

> **Docker:** запуск через `SPRING_PROFILES_ACTIVE=jpa docker compose up --build` спрацює для app-контейнера, **лише якщо Compose справді передає цю змінну всередину app**. У вас `env_file: .env`, тож практичний варіант — додати `SPRING_PROFILES_ACTIVE=jpa` до локального `.env` (не комітити його) і `docker compose up --build`. Перевірте `docker compose config`, щоб профіль потрапив саме в контейнер застосунку. Для локального Spring можна експортувати змінну в shell.

---

## КОМІТ 5 — Перевірки CRUD, асоціацій і rollback

**Commit:** `test(jpa): verify transactions and repository behavior`

**Мета:** довести методичну вимогу про **успішні та неуспішні транзакційні операції**. `@Transactional` уже є в `ScreeningService.create()` і `delete()`, тому не створюємо нові штучні бізнес-операції.

**Мінімальні сценарії:**

1. Створення `Screening` → БД генерує ID, створено **15 Seat**, усе в одній транзакції.
2. Видалення `Screening` → зникають квитки, місця й сеанс; інші сеанси не чіпаються.
3. **Rollback:** штучно провокована помилка на вставленні одного з місць або SQL-обмеження. Після невдалого `create()` у БД **немає ні створеного сеансу, ні залишених місць**. Ін'єкцію помилки робити *лише в тесті*, не через production REST-ендпойнт.
4. Перевірка `@Query`, `@NamedQuery`, derived query на реальних даних.
5. Запит на зайняте місце → другий квиток не створюється.
6. У `jpa` профілі перевірити `POST/PUT/PATCH/DELETE /api/screenings` та HTTP-коди, уже описані в OpenAPI. Повторний повний прогін усієї старої тестової бази достатньо провести один раз.

**Технічний нюанс:** можна використовувати наявні H2-test залежності, якщо їх схема/налаштування справді сумісні, або PostgreSQL у Docker для інтеграційної перевірки. **Не вводити Testcontainers, нову СУБД чи спеціальні REST-ендпойнти лише заради демонстрації**, якщо без цього можна обійтися.

**Промпт Codex №5:**

```text
Implement ONLY commit #5: focused integration tests for Spring Data JPA lab #6.

Inspect current tests and the actual Spring Boot 4.1 / JPA configuration first.
Keep test setup as simple as possible; reuse existing test dependencies and
PostgreSQL Docker infrastructure, or compatible H2 test configuration if it
works. Do not introduce Testcontainers or a new database without clear need.

Tests must prove:
1. JPA CRUD, generated database IDs, OneToMany Screening -> Seats relation;
2. @Query, @NamedQuery, and derived query methods actually execute;
3. Successful transactional creation of one screening and exactly 15 seats;
4. Transaction rollback on an injected failure inside create(): no screening
   or partial seats survive. Inject failure in test-only code/mocks, NOT in
   production endpoints or business logic;
5. Transactional deletion of linked tickets/seats/screening;
6. No duplicate ticket for the same seat and correct behavior of guest tickets;
7. Existing JDBC/default and memory modes still work, as far as practical.

When appropriate, verify that real commits and rollbacks happen in the database,
not only in mocks; avoid test-level @Transactional hiding the expected result.
Do not rewrite old tests unless required for new JPA startup behavior.
Do NOT commit. Report commands and actual outcomes, including skipped checks.
```

**Одна перевірка:** тести проходять; rollback доведено реальним станом БД; немає «тесту транзакції», який перевіряє тільки те, що стоїть анотація.

---

## КОМІТ 6 — OpenAPI, демонстрація REST і документація

**Commit:** `docs(lab6): verify and document Spring Data API`

**Не потрібно створювати ще один CRUD-контролер!** У ЛР №4 вже є `ScreeningRestController` з CRUD, фільтрацією, пагінацією та OpenAPI; з профілем `jpa` він через сервіси працює з іншою реалізацією repository. Це задовольняє вимогу REST-доступу без дублювання API.

**Що додати:**
- `docs/lab-6/README.md`: коротко архітектура, чотири `@Entity`, 1:N, `CrudRepository`, порівняння `@Query`/`@NamedQuery`/derived, `@Transactional`, запуск у Docker, тестування/rollback.
- Зрозумілий список конкретних файлів і методів, де показано три способи пошуку.
- Команди запуску JPA-режиму; де змінити `.env` (секрети не показувати), що робити зі старим volume.
- Інструкція для DBeaver або `psql` — як подивитися `screenings`, `seats`, `tickets`, `users` та зв'язки.
- Сценарії Swagger UI/Postman: `GET list` із фільтрацією, `POST create` (201 + Location), `PUT`, `PATCH` двох типів, `DELETE` (204), 400/404/403/409 за потреби, зв'язки й rollback.
- Тільки якщо в наявній OpenAPI-документації реально не вистачає обов'язкового опису, зробити **мінімальну** документаційну правку.

**Промпт Codex №6:**

```text
Implement ONLY commit #6: final documentation and verification for Spring lab #6.

Review the requirement document's scope: Spring Data JPA ORM, OneToMany,
CrudRepository, JPQL @Query, JPQL @NamedQuery, derived queries, transactional
service operations, REST API, OpenAPI, and successful/failed transaction tests.

Create docs/lab-6/README.md in Ukrainian, concise and technical, explaining:
- existing PostgreSQL schema and Docker startup;
- each JPA entity and the Screening -> Seats 1:N relationship;
- four CrudRepository interfaces and three concrete query examples;
- why existing domain records and MVC/REST controllers remain unchanged;
- profile switching among jdbc/default, memory, and jpa;
- transactional create/delete and an evidence-based rollback demonstration;
- step-by-step Swagger/Postman scenarios and expected HTTP status codes;
- precise SQL queries to inspect results in PostgreSQL.

Reuse the existing REST API and OpenAPI annotations. Only fix missing or
incorrect documentation, do not add duplicate controllers or features.
Preserve README files and docs from labs #2-5; never modify .env secrets.
Run one focused final verification, report what actually ran, and list any
screenshots that a human should capture for their academic report.
Do NOT commit.
```

**Одна перевірка:** документація точно описує реальний JPA-режим, а не вигадані сценарії; старі README і API залишилися; готовий план демонстрації викладачу.

---

## 3. Як запускати після коміту №4

### Варіант А. Усе в Docker — рекомендую для демонстрації

У локальному `.env` залишаємо актуальні `DB_NAME`, `DB_USER`, `DB_PASSWORD`, **той самий `PASSWORD_SALT`**, і додаємо:

```dotenv
SPRING_PROFILES_ACTIVE=jpa
```

У наявному `compose.yaml` вже є `app.env_file: .env`, тож профіль потрапляє в Spring-контейнер. Далі:

```bash
docker compose up --build -d
docker compose logs -f app
```

Сайт: `http://localhost:8080`; Swagger: `http://localhost:8080/swagger-ui.html`.

Перевірити БД:

```bash
docker compose exec db psql -U "$DB_USER" -d "$DB_NAME"
```

Увага: всередині `docker compose exec db ...` змінні оболонки `$DB_USER`, `$DB_NAME` підставляє **локальний shell**, а не контейнер; якщо локально вони не експортовані, найпростіше використовувати конкретні значення зі свого `.env` без їх публікації або команду `docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'`.

Для перевірки таблиць у psql:

```sql
SELECT id, movie_title FROM screenings ORDER BY id;
SELECT screening_id, COUNT(*) FROM seats GROUP BY screening_id ORDER BY screening_id;
SELECT id, seat_id, user_id FROM tickets ORDER BY id;
```

**Не запускай** `docker compose down -v` без наміру стерти дані PostgreSQL: `-v` видаляє volume.

### Варіант Б. Лише БД у Docker + Spring локально

```bash
docker compose up -d db
SPRING_PROFILES_ACTIVE=jpa ./mvnw spring-boot:run
```

Spring використає `DB_HOST=localhost` з поточної конфігурації; при потребі переконайся, що `.env` читається з кореня проєкту.

### Переключення назад

- Прибери `SPRING_PROFILES_ACTIVE=jpa` — у звичайному запуску повернеться **JDBC (ЛР №5)**.
- Для старого in-memory режиму встанови `SPRING_PROFILES_ACTIVE=memory`.
- Якщо активовано одночасно `memory,jpa`, це неоднозначний режим; **не роби цього** без окремої продуманої конфігурації.

---

## 4. Чеклист готовності ЛР №6

- [ ] `@Entity` є для Screening, Seat, Ticket, User; SQL-схема залишена з ЛР №5.
- [ ] Зв'язок `ScreeningEntity @OneToMany SeatEntity` і `SeatEntity @ManyToOne ScreeningEntity` працює.
- [ ] ID генерується PostgreSQL (`GenerationType.IDENTITY`), повертається після `save`.
- [ ] Є **чотири** Spring Data `CrudRepository` інтерфейси з повноцінним CRUD.
- [ ] **`@Query` JPQL** працює на реальних даних.
- [ ] **`@NamedQuery` JPQL** працює на реальних даних.
- [ ] **Derived Query Method** працює на реальних даних.
- [ ] Наявні інтерфейси репозиторіїв збережено; `jpa` вмикає лише JPA-адаптери.
- [ ] Старі режими `memory` та default/JDBC не отримують конфлікти beans.
- [ ] `ScreeningService.create()` створює screening + рівно 15 seats у транзакції.
- [ ] Помилка при створенні дає **rollback усіх записів**.
- [ ] Видалення сеансу правильно прибирає квитки та місця.
- [ ] Гостьове бронювання (`user_id IS NULL`) і зареєстровані користувачі працюють.
- [ ] Немає двох квитків на одне місце; некоректні запити не породжують 500.
- [ ] REST CRUD, фільтрація, пагінація, JSON Patch/Merge Patch працюють у JPA-режимі.
- [ ] OpenAPI описує параметри, тіла, короткий/повний опис, HTTP-коди.
- [ ] Відповідність REST документації перевірено через Postman/Swagger; rollback перевірено окремо.
- [ ] `.env` і `PASSWORD_SALT` не опубліковані; попередні лабораторні залишилися неушкодженими.

---

## 5. П'ять речей, які треба буде вміти пояснити на захисті

1. **ORM/JPA/Hibernate:** `@Entity` зіставляє Java-об'єкт із SQL-рядком; JPA — специфікація, Hibernate — реалізація.
2. **`CrudRepository`:** Spring Data генерує реалізацію базових `save/findById/findAll/deleteById` без ручного SQL.
3. **Три типи запиту:** derived — із назви методу; `@Query` — JPQL безпосередньо над методом; `@NamedQuery` — JPQL, названий на сутності та викликаний репозиторієм.
4. **1:N:** один screening містить багато seats, зовнішній ключ `seats.screening_id` указує на screening; власник зв'язку на стороні `@ManyToOne`.
5. **`@Transactional`:** або повністю створюються screening + 15 seats, або при помилці відбувається rollback; частково створений сеанс не залишається.

**Критерій завершення:** як тільки шість комітів відповідають чеклисту й один повний прогін успішний — припиняємо повторні перевірки. Наступний крок — розбір коду для захисту, не нескінченне полірування.
