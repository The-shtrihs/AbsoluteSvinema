# Lab 2 — minimal MVC and Spring DI

Run with Java 21+: `mvn spring-boot:run`. Verify with `mvn compile test`.
Data is stored only in memory. When storage is empty, `DemoData` creates three
screenings through `ScreeningService`: Interstellar (169 minutes), Pulp Fiction
(154 minutes), and The Lord of the Rings: The Return of the King (201 minutes,
theatrical version). Each receives 15 seats, giving 45 seats in total.

## Requirements and classes

| Requirement | Implementation |
|---|---|
| `@Component` | `DemoData`, `BookingDraft`, `SetterClockDemo`, `FieldClockDemo` |
| `@Repository` | Three `InMemory…Repository` classes in `repository/memory/` |
| `@Service` | `ScreeningService`, `TicketService` |
| `@Controller` | `HomeController`; `ScreeningController`, which depends only on services |
| `@Configuration`, `@Bean` | `ApplicationConfig.clock()` returns a `Clock` using the system time zone |
| Constructor injection | Both services and the screening MVC controller; `TicketService` also receives `Clock` |
| Setter injection | `SetterClockDemo.setClock()` with `@Autowired` |
| Field injection | `FieldClockDemo.clock` with `@Autowired` |
| Singleton | The default service scope |
| Prototype | `BookingDraft` with `@Scope(PROTOTYPE)`; each lookup creates a separate draft |

Setter and field injection appear in short educational examples.
Main dependencies use constructor injection. Booking uses `Clock` to create
its timestamp through `LocalDateTime.now(clock)`.

## Routes and demonstration

1. Open `http://localhost:8080/`: the home page links to the screening list.
2. Open `http://localhost:8080/screenings`: title, date and time, hall, price,
   and duration for each screening.
3. Follow a title to `GET /screenings/{id}`: full description, duration,
   15 seats in a 3 × 5 layout, and their availability.
4. Enter a name beside an available seat and click “Book seat”.
   The form sends `POST /screenings/{id}/book` with `seatId` and `customerName`.
   After the redirect, a confirmation appears; the seat is booked and its form disappears.
5. To demonstrate duplicate booking, open the same screening in two tabs before
   booking. Book a seat in the first tab, then submit its old form in the second:
   “This seat is already booked. Please choose another.” appears.
6. A name containing only whitespace is rejected by the server. A missing screening
   or seat, including a seat belonging to another screening, returns an HTML page
   with HTTP 404.
7. Show `DependencyInjectionTest`: the IoC Container, three DI styles,
   repeated singleton lookups, and independent prototype drafts.

Double-booking protection remains atomic in the Fake Repository.
Templates render text with `th:text`.

## Tests

- `DependencyInjectionTest` (`@SpringBootTest`): real beans from
  `ApplicationContext`, constructor/setter/field injection, singleton/prototype.
- `TicketServiceClockTest`: a unit test with a fixed `Clock` verifies the booking timestamp.
- `ScreeningMvcTest`: starts Spring Boot with a real HTTP server on a random port;
  checks HTML, description and duration, 15 seats, booking, duplicate booking,
  invalid names, 404, and rejection of a seat from another screening. Its home-page
  test uses MockMvc with the same application context to check the `index` view.
- Model, repository, and Spring context startup tests also run.
- `DemoDataTest`: checks all three movie titles and durations, 45 generated seats,
  and that running initialization again leaves existing data unchanged.

Administration pages, REST, additional DI infrastructure, and database storage
are outside this stage.
