# Лабораторна робота №4: RESTful Web Services & Documentation

## AbsoluteSvinema — Screening REST API

У межах лабораторної роботи №4 розроблено повнофункціональний RESTful вебсервіс для ресурсу **Screening** (сеанс кінотеатру), що функціонує паралельно з існуючим MVC-інтерфейсом (Labs 2–3), використовує ті самі сервіси та моделі, підтримує CRUD, фільтрацію, пагінацію, два стандарти часткового оновлення (PATCH) та інтегрований із OpenAPI 3 (Swagger UI).

---

## 1. Карта API (API Endpoints Map)

| Метод | URL | Медіатип (`Content-Type` / `Accept`) | Доступ / Роль | Опис |
|---|---|---|---|---|
| `GET` | `/api/csrf` | `application/json` | Публічний | Отримання актуального CSRF-токена та назв заголовків |
| `GET` | `/api/screenings` | `application/json` | Публічний | Отримання списку сеансів із фільтрацією (`title`, `hall`) та пагінацією (`page`, `size`) |
| `GET` | `/api/screenings/{id}` | `application/json` | Публічний | Отримання детальної інформації про сеанс за числовим ID |
| `POST` | `/api/screenings` | `application/json` | `ROLE_ADMIN` + CSRF | Створення нового сеансу з автоматичною ініціалізацією 15 місць (3×5) |
| `PUT` | `/api/screenings/{id}` | `application/json` | `ROLE_ADMIN` + CSRF | Повне оновлення (заміна) усіх редагованих полів сеансу |
| `PATCH` | `/api/screenings/{id}` | `application/json-patch+json` | `ROLE_ADMIN` + CSRF | Часткове оновлення сеансу за стандартом **RFC 6902 JSON Patch** (масив операцій) |
| `PATCH` | `/api/screenings/{id}` | `application/merge-patch+json` | `ROLE_ADMIN` + CSRF | Часткове оновлення сеансу за стандартом **RFC 7396 JSON Merge Patch** (JSON-документ) |
| `DELETE` | `/api/screenings/{id}` | — | `ROLE_ADMIN` + CSRF | Видалення сеансу разом із усіма його місцями та квитками |
| `GET` | `/v3/api-docs` | `application/json` | Публічний | Специфікація OpenAPI 3.0 у форматі JSON |
| `GET` | `/swagger-ui.html` | `text/html` | Публічний | Інтерактивна документація Swagger UI |

---

## 2. Таблиця HTTP статус-кодів

| HTTP Код | Назва | Коли повертається | Формат тіла відповіді |
|---|---|---|---|
| **200 OK** | Успішний запит | `GET` сеансу/списку, `PUT`, `PATCH` (JSON Patch або Merge Patch). | JSON сутності (`ScreeningResponse`, `PageResponse`, `CsrfResponse`) |
| **201 Created** | Створено ресурс | `POST /api/screenings`. Повертає створений сеанс та заголовок `Location: /api/screenings/{id}`. | `ScreeningResponse` |
| **204 No Content** | Успішно без тіла | `DELETE /api/screenings/{id}`. | Порожнє тіло |
| **400 Bad Request** | Невалідний запит | Помилки Bean Validation, некоректні параметри пагінації (`page < 0`, `size < 1` або `> 100`), невалідний синтаксис JSON, заборонений/невідомий шлях операції PATCH, спроба змінити `id`/`seats`, або видалення обов'язкового поля через `null` у Merge Patch. | RFC 7807 `ProblemDetail` (`title`, `status`, `detail`, `errors`) |
| **401 Unauthorized** | Не автентифіковано | Запит на зміну даних (POST/PUT/PATCH/DELETE) без активної сесії адміністратора. | RFC 7807 `ProblemDetail` |
| **403 Forbidden** | Доступ заборонено | Користувач автентифікований без ролі `ROLE_ADMIN` (наприклад, звичайна роль `USER`), або відсутній/недійсний CSRF-токен у заголовку `X-CSRF-TOKEN`. | RFC 7807 `ProblemDetail` |
| **404 Not Found** | Ресурс не знайдено | Звернення до неіснуючого `id` сеансу (`GET`, `PUT`, `PATCH`, `DELETE`). | RFC 7807 `ProblemDetail` |
| **409 Conflict** | Конфлікт передумови | Невдача операції `"test"` у RFC 6902 JSON Patch (фактичне значення поля не збіглося з очікуваним у `value`). | RFC 7807 `ProblemDetail` (`Patch Test Condition Failed`) |
| **415 Unsupported Media Type** | Непідтримуваний тип | Запит `PATCH` надіслано з `Content-Type: application/json` або іншим типом замість `application/json-patch+json` чи `application/merge-patch+json`. | RFC 7807 `ProblemDetail` |

---

## 3. Зразки JSON-запитів та відповідей

### 3.1. Створення сеансу (`POST /api/screenings`)

**Запит:**
```http
POST /api/screenings HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-CSRF-TOKEN: a1b2c3d4-e5f6-7890-abcd-ef1234567890

{
  "movieTitle": "Dune: Part Two",
  "movieDescription": "Paul Atreides unites with Chani and the Fremen to seek revenge.",
  "movieDurationMinutes": 166,
  "startsAt": "2028-11-20T20:00:00",
  "hall": "Hall IMAX",
  "ticketPrice": 290.00
}
```

**Відповідь (`201 Created`):**
```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/screenings/4
Content-Type: application/json

{
  "id": 4,
  "movieTitle": "Dune: Part Two",
  "movieDescription": "Paul Atreides unites with Chani and the Fremen to seek revenge.",
  "movieDurationMinutes": 166,
  "startsAt": "2028-11-20T20:00:00",
  "hall": "Hall IMAX",
  "ticketPrice": 290.00
}
```

---

### 3.2. Список із пагінацією та фільтрацією (`GET /api/screenings?title=Interstellar&page=0&size=2`)

**Відповідь (`200 OK`):**
```json
{
  "content": [
    {
      "id": 1,
      "movieTitle": "Interstellar",
      "movieDescription": "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
      "movieDurationMinutes": 169,
      "startsAt": "2028-01-10T18:00:00",
      "hall": "Hall 1",
      "ticketPrice": 180.00
    }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 3.3. RFC 6902 JSON Patch (`PATCH /api/screenings/1`)

**Заголовок:** `Content-Type: application/json-patch+json`

**Тіло запиту:**
```json
[
  { "op": "replace", "path": "/movieTitle", "value": "Interstellar (IMAX Remastered)" },
  { "op": "replace", "path": "/ticketPrice", "value": 250.00 },
  { "op": "test", "path": "/hall", "value": "Hall 1" },
  { "op": "copy", "from": "/movieTitle", "path": "/movieDescription" }
]
```

**Особливості реалізації:**
- Підтримуються всі стандартні операції: `add`, `remove`, `replace`, `move`, `copy`, `test`.
- Редагувати дозволено **тільки** поля `movieTitle`, `movieDescription`, `movieDurationMinutes`, `startsAt`, `hall`, `ticketPrice`.
- Спроби змінити `/id`, `/seats` або невідомі шляхи негайно відхиляються з кодом **400 Bad Request**.
- Якщо операція `test` не пройшла — повертається **409 Conflict**.
- Атомарність: жодні зміни не зберігаються у пам'яті, доки всі операції не завершаться успішно й результуючий об'єкт не пройде валідацію Bean Validation.

---

### 3.4. RFC 7396 JSON Merge Patch (`PATCH /api/screenings/1`)

**Заголовок:** `Content-Type: application/merge-patch+json`

**Тіло запиту:**
```json
{
  "movieTitle": "Interstellar (Director's Cut)",
  "movieDurationMinutes": 175
}
```

**Поведінка `null` за RFC 7396:**
Передача `"movieTitle": null` видаляє властивість із цільового документа. Оскільки всі поля `ScreeningRequest` позначені `@NotBlank` або `@NotNull`, таке видалення викликає відхилення запиту валідатором із кодом **400 Bad Request** (`Validation Failed`), а не приховане ігнорування `null`.

---

### 3.5. Формат помилок (RFC 7807 ProblemDetail)

**Приклад відповіді 400 при помилці валідації:**
```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/screenings",
  "errors": {
    "movieTitle": "Movie title must not be blank",
    "ticketPrice": "Ticket price must be non-negative"
  }
}
```

---

## 4. Інструкція із запуску та документація OpenAPI

### 4.1. Збірка та запуск

Вимоги: **JDK 21+**, Maven (або `./mvnw`).

```bash
# Компіляція та запуск усіх тестів
./mvnw clean test

# Запуск додатку
./mvnw spring-boot:run
```

### 4.2. Доступ до документації

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Документація містить повний опис кожної операції, прийоми автентифікації, схеми моделей DTO, приклади запитів для обох медіатипів PATCH та коди помилок (`400`, `401`, `403`, `404`, `409`, `415`).

---

## 5. Робота з колекцією Postman

Файл колекції знаходиться у репозиторії за шляхом:
`docs/lab-4/postman/AbsoluteSvinema-Lab4.postman_collection.json`

### 5.1. Покроковий сценарій автентифікації (Login Flow)

Для виклику захищених операцій (`POST`, `PUT`, `PATCH`, `DELETE`) використовується реальний механізм Spring Security (сесії та CSRF):

1. **Крок 1 — Отримання початкового CSRF-токена:**
   Виконайте `GET {{baseUrl}}/api/csrf`. Тестовий скрипт автоматично збереже отриманий токен у змінну колекції `csrfToken`.
2. **Крок 2 — Вхід (POST /login):**
   Виконайте `POST {{baseUrl}}/login` із `Content-Type: application/x-www-form-urlencoded` та полями:
   - `email`: `admin@svinema.ua`
   - `password`: `admin123`
   - `_csrf`: `{{csrfToken}}`
   Postman автоматично збереже сесійну куку `JSESSIONID` у внутрішньому Cookie Jar.
3. **Крок 3 — Оновлення CSRF після входу:**
   Spring Security оновлює токен CSRF під час зміни сесії. Виконайте повторний `GET {{baseUrl}}/api/csrf`, щоб отримати актуальний токен для аутентифікованої сесії у змінну `csrfToken`.
4. **Крок 4 — Захищені виклики:**
   Усі запити `POST`, `PUT`, `PATCH`, `DELETE` автоматично надсилають збережену куку `JSESSIONID` та заголовок `X-CSRF-TOKEN: {{csrfToken}}`.

### 5.2. Склад запитів у колекції (33 запити)

1. **00 - Authentication & CSRF Setup:** Отримання токена, вхід адміна, оновлення токена.
2. **01 - Public Screenings Read API:** Отримання списку, фільтри по `title` та `hall`, пагінація (`page=0&size=2`), сеанс за ID, 404 для неіснуючого ID, 400 для невалідних `page` та `size`.
3. **02 - Screening CRUD (ADMIN):** Створення сеансу (201 + заголовок `Location` + збереження `screeningId`), повне оновлення PUT (200), видалення DELETE (204), перевірка видалення (404).
4. **03 - RFC 6902 JSON Patch:** Оновлення декількох полів, операція `test` (успіх), операція `test` (409 Conflict), операція `copy`, спроба змінити `/id` (400), невідоме поле (400).
5. **04 - RFC 7396 JSON Merge Patch:** Часткове оновлення (200), видалення поля через `null` (400 Validation Failed), заборонене поле `id` (400), невідоме поле (400).
6. **05 - Error Cases & Security:** Невалідний DTO при POST (400), неправильний Content-Type для PATCH (415), запит без токена CSRF (403), вхід як звичайний користувач `USER` і спроба створити сеанс (403 Forbidden).
7. **06 - Documentation Endpoints:** Перевірка `/v3/api-docs` та `/swagger-ui.html`.

---

## 6. Обмеження поточного сховища (In-Memory)

- **In-Memory Fake Repositories:** Дані зберігаються у колекціях `ConcurrentHashMap` у пам'яті процесу та скидаються при перезапуску додатку до початкового стану (`DemoData`).
- **Фіксована розмітка залу:** Для кожного сеансу створюється рівно 15 місць (3 ряди × 5 місць).
- **Майбутнє перенесення:** У Лабораторній роботі №5 це сховище буде замінено на PostgreSQL через Spring JDBC, а в Лабораторній роботі №6 — на Spring Data JPA, без зміни контролерів та контрактів REST API.
