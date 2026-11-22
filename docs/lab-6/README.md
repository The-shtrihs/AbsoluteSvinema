# Лабораторна робота №6: Spring Data JPA

## Мета і межі реалізації

Застосунок використовує Spring Data JPA та Hibernate як альтернативне сховище для наявних таблиць PostgreSQL. Зовнішній контракт не змінювався: сервіси, MVC/REST контролери, доменні записи й DTO працюють із тими самими власними `ScreeningRepository`, `SeatRepository`, `TicketRepository`, `UserRepository` та незмінними записами `Screening`, `Seat`, `Ticket`, `User`. JPA адаптери перетворюють записи на сутності та назад.

Схема лишається у [`src/main/resources/schema.sql`](../../src/main/resources/schema.sql): `screenings`, `seats`, `tickets`, `users`, identity PK, FK та обмеження унікальності. JPA профіль має `spring.jpa.hibernate.ddl-auto=validate` і `spring.sql.init.mode=never`: Hibernate звіряє сутності з наявною схемою, а SQL-ініціалізатор не створює, не видаляє та не перестворює таблиці. Міграцій схеми немає.

## Сутності та зв’язки

Сутності розміщені в `ua.kpi.absolutesvinema.repository.jpa.entity`:

| Сутність | Таблиця | Відображення |
|---|---|---|
| `ScreeningEntity` | `screenings` | назва, опис, тривалість, час початку, зал, ціна; identity `id` |
| `SeatEntity` | `seats` | ряд і місце; обов’язковий lazy `@ManyToOne` до сеансу через `screening_id` |
| `TicketEntity` | `tickets` | lazy `@OneToOne` до місця (`seat_id` унікальний); необов’язковий lazy `@ManyToOne` до користувача через nullable `user_id`, тому гостьові квитки підтримуються |
| `UserEntity` | `users` | email, хеш пароля, ім’я, роль; email унікальний |

Зв’язок **Screening → Seats (1:N)** — `ScreeningEntity.seats` має `@OneToMany(mappedBy = "screening")`; власник FK — `SeatEntity.screening`. Автоматичного cascade delete немає. Видалення пов’язаних рядків виконує сервіс у транзакції у порядку квитки → місця → сеанс.

## CrudRepository та запити

Чотири інтерфейси у `repository.jpa.store` розширюють `CrudRepository<Entity, Long>`: `ScreeningJpaDataRepository`, `SeatJpaDataRepository`, `TicketJpaDataRepository`, `UserJpaDataRepository`. Їхні імена не конфліктують із доменними контрактами.

- **Derived query:** `SeatJpaDataRepository.findByScreening_IdOrderByRowNumberAscSeatNumberAsc` знаходить місця сеансу й сортує їх за рядом та номером. У `TicketJpaDataRepository` методи `findBySeat_Id` і `findByUser_IdOrderByIdAsc` шукають квитки за місцем і власником.
- **JPQL `@Query`:** `ScreeningJpaDataRepository.findByTitleAndHall` використовує `ScreeningEntity`, її властивості та `LOCATE`: назва є необов’язковим регістронезалежним підрядком, зал — необов’язковою регістронезалежною точною назвою після `trim`, результат стабільно сортується за ID. `LOCATE` трактує `%` та `_` як звичайні символи, а не шаблони `LIKE`.
- **JPQL `@NamedQuery`:** `UserEntity.findByNormalizedEmail` оголошений на сутності та викликається методом `UserJpaDataRepository.findByEmailNormalized`; запит шукає email без урахування регістру та крайніх пробілів. Це використовується під час пошуку користувача для входу.

## Профілі й запуск

Підготуйте локальний `.env` за інструкцією ЛР5: скопіюйте `.env.example` і задайте власні `DB_PASSWORD` та `PASSWORD_SALT`. Не додавайте `.env` до Git. Compose передає змінні контейнеру застосунку через `env_file`.

```sh
cp .env.example .env
# відредагуйте локальний .env: задайте власні DB_PASSWORD і PASSWORD_SALT
```

У `.env` додайте `SPRING_PROFILES_ACTIVE=jpa`, щоб Compose запустив JPA-режим:

```sh
docker compose up --build -d
docker compose logs -f app
```

Для локального процесу спочатку запустіть PostgreSQL у Compose, а потім активуйте профіль лише для застосунку:

```sh
docker compose up -d db
SPRING_PROFILES_ACTIVE=jpa ./mvnw spring-boot:run
```

Перемикання сховища:

| Режим | Активація | Реалізації доменних репозиторіїв |
|---|---|---|
| JDBC (типовий) | активний профіль не заданий | чотири JDBC репозиторії з `!memory & !jpa`; `schema.sql` запускається як раніше |
| Memory | `SPRING_PROFILES_ACTIVE=memory` | чотири наявні in-memory репозиторії |
| JPA | `SPRING_PROFILES_ACTIVE=jpa` | чотири JPA адаптери й чотири Spring Data інтерфейси; лише перевірка схеми Hibernate |

У пам’яті залишається той самий PostgreSQL datasource з локального `.env`; перемикається шар репозиторіїв. Не використовуйте `docker compose down -v` для звичайного перемикання профілю: команда видаляє volume бази даних.

## Транзакції та перевірені результати

`ScreeningService.create` має `@Transactional`: зберігає сеанс і 15 місць (3 × 5) однією транзакцією. `ScreeningService.delete` в одній транзакції знаходить квитки за ID місць, видаляє квитки, місця, потім сеанс. Унікальне обмеження `uk_tickets_seat` разом із перевіркою сервісу не допускає повторного бронювання; порушення обмеження перетворюється на наявний `IllegalStateException` конфлікту.

Докази у тестах JPA профілю:

- `JpaRepositoryIntegrationTest.serviceCreateCommitsOneScreeningAndExactlyFifteenSeats` після повернення сервісу перевіряє через `JdbcTemplate`, що в БД з’явився один сеанс і рівно 15 місць; тест не має `@Transactional`, яка могла б приховати результат коміту.
- `ScreeningCreateRollbackIntegrationTest.createRollsBackScreeningAndEarlierSeatsWhenSeatInsertFails` підміняє `SeatRepository` лише в тестовій конфігурації. Після семи успішних вставок тестовий адаптер кидає помилку на місці 2:3; SQL-перевірки після завершення виклику підтверджують нуль сеансів і нуль місць із ID невдалої операції.
- Інтеграційні тести також виконують JPQL/NamedQuery/derived запити, перевіряють гостьовий квиток, унікальність місця, видалення пов’язаних рядків і взаємовиключність профілів. Тестова БД — наявна H2 у режимі PostgreSQL; це не є запуском проти Docker PostgreSQL.

## Swagger і Postman: сценарії

Після запуску відкрийте Swagger UI: <http://localhost:8080/swagger-ui.html>. OpenAPI JSON: <http://localhost:8080/v3/api-docs>. Це наявні маршрути, не додаткові контролери.

1. **Перегляд і фільтрація (публічні запити).** У Swagger виконайте `GET /api/screenings?title=Interstellar&hall=Hall%201&page=0&size=10`. Очікується `200`; перевірте `content`, `totalElements`, `totalPages`. Від’ємна сторінка або `size` поза 1–100 дає `400`. `GET /api/screenings/{id}` повертає `200`, для відсутнього ID — `404`.
2. **Підготовка Postman до змін стану.** Увімкніть збереження cookie. Виконайте `GET /api/csrf` (`200`), потім `POST /login` як `application/x-www-form-urlencoded` з `email`, `password` і CSRF токеном. Використовуйте локальні змінні Postman, наприклад `{{adminEmail}}` і `{{adminPassword}}`, не записуйте пароль у колекцію. Успішний form login повертає redirect `302`; cookie jar має зберегти `JSESSIONID`. Повторно виконайте `GET /api/csrf` і використовуйте оновлений токен у заголовку `X-CSRF-TOKEN`.
3. **Створення.** Надішліть `POST /api/screenings` із cookie, CSRF заголовком і JSON тілом нижче. Очікується `201 Created`, `Location: /api/screenings/{id}`, тіло містить згенерований ID. Збережіть ID у `{{screeningId}}`.

   ```json
   {
     "movieTitle": "Лабораторний сеанс",
     "movieDescription": "Перевірка JPA та транзакцій",
     "movieDurationMinutes": 95,
     "startsAt": "2027-02-04T18:00:00",
     "hall": "Hall 9",
     "ticketPrice": 120.00
   }
   ```

   Очікується 15 місць у таблиці `seats`. Перевірте сеанс через `GET /api/screenings/{{screeningId}}` (`200`), а сітку місць — на HTML сторінці `/screenings/{{screeningId}}`.
4. **Оновлення.** `PUT /api/screenings/{{screeningId}}` із повним набором полів повертає `200`. Для `PATCH /api/screenings/{{screeningId}}` виберіть у Swagger приклад або надішліть один із форматів:

   ```http
   Content-Type: application/json-patch+json
   ```

   ```json
   [{"op":"replace","path":"/ticketPrice","value":130.00}]
   ```

   ```http
   Content-Type: application/merge-patch+json
   ```

   ```json
   {"ticketPrice":130.00}
   ```

   Обидва PATCH варіанти повертають `200`. Неіснуючий ID — `404`; невалідні дані чи заборонений шлях — `400`; невдала JSON Patch операція `test` — `409`; неправильний тип вмісту — `415`.
5. **Перевірка авторизації й валідації.** Захищений `POST` без сесії повертає `401`; без ролі адміністратора або з відсутнім/неправильним CSRF токеном — `403`; некоректне тіло створення — `400`. У Swagger для захищених операцій задайте `JSESSIONID` і `X-CSRF-TOKEN` через `Authorize`; у Postman залиште cookie jar активним.
6. **Видалення.** `DELETE /api/screenings/{{screeningId}}` з адміністративною сесією та CSRF токеном повертає `204 No Content`. Перевірте SQL-запитами нижче, що сеанс і пов’язані місця/квитки видалені.
7. **Бронювання квитка.** Окремого ticket REST endpoint у Swagger немає. У браузері відкрийте `/screenings/{{screeningId}}` і надішліть форму місця: успіх — redirect `302` назад із повідомленням; повторне бронювання — також `302`, сторінка показує конфліктне повідомлення, у БД лишається один квиток на місце. Власник після входу скасовує квиток із `/tickets/my`; POST скасування повертає `302`, місце знову доступне. Форми використовують сесію та CSRF.

## Перевірка PostgreSQL через `psql`

Підключіться через `docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'`. Нижче — команди для інтерактивного `psql`; замініть `1` на ID створеного сеансу.

```sql
\dt

\set screening_id 1

SELECT id, movie_title, movie_description, movie_duration_minutes,
       starts_at, hall, ticket_price
FROM screenings
WHERE id = :screening_id;

SELECT id, screening_id, row_number, seat_number
FROM seats
WHERE screening_id = :screening_id
ORDER BY row_number, seat_number;

SELECT s.id, s.movie_title, COUNT(se.id) AS seat_count
FROM screenings s
LEFT JOIN seats se ON se.screening_id = s.id
WHERE s.id = :screening_id
GROUP BY s.id, s.movie_title;

SELECT t.id AS ticket_id, t.seat_id, t.user_id,
       t.customer_name, t.booked_at,
       se.screening_id, se.row_number, se.seat_number,
       u.email AS user_email
FROM tickets t
JOIN seats se ON se.id = t.seat_id
LEFT JOIN users u ON u.id = t.user_id
WHERE se.screening_id = :screening_id
ORDER BY se.row_number, se.seat_number;

SELECT seat_id, COUNT(*) AS ticket_count
FROM tickets
GROUP BY seat_id
HAVING COUNT(*) > 1;
```

Після успішного створення очікується `seat_count = 15`; гостьовий квиток має `user_id IS NULL`. Запит пошуку дублікатів має повернути нуль рядків. Після видалення перевірте, що `SELECT COUNT(*) FROM screenings WHERE id = :screening_id;` та `SELECT COUNT(*) FROM seats WHERE screening_id = :screening_id;` повертають `0`. Користувачів перевіряйте без читання паролів: `SELECT id, email, full_name, role FROM users ORDER BY id;`.

## Матеріали для звіту

Зробіть власні скриншоти: Swagger UI з групою `Screenings API`; Postman відповідей `201` із `Location` та `204`; сторінки сеансу із 15 місцями й результату бронювання/повторного бронювання; виводу JPA integration test із успішним commit та rollback; SQL результатів для 15 місць і очищення пов’язаних рядків після видалення. Не показуйте паролі, `.env`, CSRF токен або активні session cookies.
