# Lab 3 — template engines and server-side rendering with Thymeleaf

Run with Java 21+: `./mvnw spring-boot:run`. Verify with `./mvnw compile test`.
Data is stored in memory. The presentation layer is rendered on the server
using Thymeleaf. The interface features a dark cyber-cinema aesthetic with glassmorphism
and an interactive Antigravity particle/constellation canvas background. Authentication
and role-based authorization are powered by Spring Security with session storage.

## Requirements and classes

| Requirement | Implementation |
|---|---|
| Template engine | Thymeleaf configured via `spring-boot-starter-thymeleaf` |
| Conditional formatting (`th:if`, `th:unless`) | `screenings/list.html`, `screenings/details.html`, `tickets/my.html` for empty states, alert messages, and seat booking forms |
| Elvis operator (`?:`) | `screenings/list.html`, `screenings/details.html`, `tickets/my.html` for fallback descriptions and guest names |
| Selection element (`th:switch`, `th:case`) | `screenings/list.html` for runtime and hall categories; `screenings/details.html` for seat availability status |
| Loop element (`th:each`) | `screenings/list.html`, `screenings/details.html` (15-seat 3 × 5 hall layout), `tickets/my.html`, and admin tables |
| XSS resistance | Strict HTML escaping via `th:text` across all templates; verified with raw `<script>` payloads |
| Shared navigation & layout | `fragments/layout.html` with responsive navbar, authentication state, and Antigravity canvas |
| Interactive Antigravity background | `src/main/resources/static/js/antigravity.js` (particle gravity, cursor tracking, hue shifts, shockwaves) |
| Cinema styling | `src/main/resources/static/css/styles.css` (dark glassmorphism, glowing accents, 3 × 5 seat grid visualizer) |
| Authentication & sessions | Spring Security in `SecurityConfig`, `UserService` with `BCryptPasswordEncoder`, `InMemoryUserRepository` |
| User ticket management | `UserTicketController` (`/tickets/my`), tickets linked to `User` by `userId`, booking cancellation |
| Protected admin panel | `AdminScreeningController` (`/admin/screenings`), `AdminTicketController` (`/admin/tickets`), restricted to `ROLE_ADMIN` |
| Server-side validation | `ScreeningForm`, `RegisterForm`, `TicketUpdateForm` with Jakarta Validation and `th:errors` |

All user and movie texts render strictly through `th:text`. Unescaped rendering (`th:utext`) is forbidden.

## Routes and demonstration

1. Open `http://localhost:8080/`: the home landing page with the Antigravity interactive particle canvas, hero section, and quick links. Moving the mouse influences particle trajectory and color hues; clicking emits a shockwave.
2. Open `http://localhost:8080/screenings`: list of screenings demonstrating `th:each`, duration badges via `th:switch`/`th:case`, hall badges, and fallback descriptions via `?:`.
3. Follow a title to `GET /screenings/{id}`: full details, cinema screen visualizer, and 15 seats in a 3 × 5 layout. Available seats render in emerald (`Available`); booked seats render in red (`Booked`).
4. Enter a name beside an available seat and click “Book seat” (`POST /screenings/{id}/book`). After redirect, flash message confirms booking, seat turns booked, and form disappears (`th:unless`).
5. Open `http://localhost:8080/register`: register a new account (e.g. `test@svinema.ua`). Passwords are encrypted with BCrypt. Duplicate emails are rejected.
6. Open `http://localhost:8080/login`: sign in with demo accounts:
   - User: `user@svinema.ua` / `user123`
   - Admin: `admin@svinema.ua` / `admin123`
7. When authenticated as a user, book a seat: the ticket links to `userId`. Open `http://localhost:8080/tickets/my` to view your booked tickets. Click “Cancel Booking” to release the seat.
8. When authenticated as `admin@svinema.ua`:
   - Open `http://localhost:8080/admin/screenings`: view all screenings, create new screenings (auto-generating 15 seats), edit, or delete screenings.
   - Form validation: submitting a blank movie title or negative price re-renders `/admin/screenings/new` with field validation errors.
   - Open `http://localhost:8080/admin/tickets`: list all booked tickets, rename customer names, or cancel bookings.
9. Unauthenticated or regular user requests to `/admin/**` are blocked (redirected to `/login` or HTTP 403).

## Tests

- `ScreeningMvcTest`: verifies public pages, details, 15 seats, booking, double-booking prevention, 404 handling, and guest booking compatibility from Lab 2.
- `ThymeleafViewTest`: verifies XSS protection by submitting `<script>alert('xss-attack')</script>` and checking that it renders escaped as `&lt;script&gt;`, and verifies `th:switch` and Elvis constructs.
- `AuthenticationTest`: verifies registration, duplicate email handling, BCrypt hashing, login success/failure, logout, and anonymous access.
- `UserTicketBookingTest`: verifies user-linked booking, `/tickets/my` dashboard, and ticket cancellation.
- `AdminPanelTest`: verifies role-based access control (`ROLE_ADMIN` required), screening CRUD, validation error display, and ticket renaming/cancellation.
- `DependencyInjectionTest`, `TicketServiceClockTest`, `InMemoryRepositoriesTest`, `ModelValidationTest`, and `DemoDataTest` continue to pass without regression.

RESTful API endpoints (Lab 4), JDBC database persistence (Lab 5), and Spring Data JPA (Lab 6) are outside this stage.
