# ЛР №2 — мінімальний MVC та Spring DI

Запуск: Java 21+, `mvn spring-boot:run`. Перевірки: `mvn compile test`.
Дані зберігаються лише в пам'яті. `DemoData` створює два сеанси через
`ScreeningService`, якщо сховище порожнє; кожен отримує 15 місць.

## Вимоги та класи

| Вимога | Реалізація |
|---|---|
| `@Component` | `DemoData`, `BookingDraft`, `SetterClockDemo`, `FieldClockDemo` |
| `@Repository` | Три `InMemory…Repository` у `repository/memory/` |
| `@Service` | `ScreeningService`, `TicketService` |
| `@Controller` | `ScreeningController`, залежить тільки від сервісів |
| `@Configuration`, `@Bean` | `ApplicationConfig.clock()` повертає `Clock` із системною часовою зоною |
| Constructor injection | Обидва сервіси та MVC-контролер; `TicketService` отримує також `Clock` |
| Setter injection | `SetterClockDemo.setClock()` із `@Autowired` |
| Field injection | Поле `FieldClockDemo.clock` із `@Autowired` |
| Singleton | Стандартний scope сервісів |
| Prototype | `BookingDraft` із `@Scope(PROTOTYPE)`; кожне отримання створює окрему чернетку |

Setter та field injection винесені в короткі навчальні приклади.
Основні залежності передаються через конструктори. `Clock` використовується
під час бронювання; дата квитка створюється через `LocalDateTime.now(clock)`.

## Маршрути та демонстрація

1. Відкрити `http://localhost:8080/screenings`: назва, дата й час, зал, ціна,
   тривалість кожного сеансу.
2. Перейти за назвою на `GET /screenings/{id}`: повний опис, тривалість,
   15 місць за схемою 3 × 5 та їхня зайнятість.
3. Ввести ім'я біля вільного місця й натиснути «Забронювати».
   Форма надсилає `POST /screenings/{id}/book` із `seatId` та `customerName`.
   Після перенаправлення є підтвердження, місце зайняте, його форма зникає.
4. Для повторної броні відкрити той самий сеанс у двох вкладках до бронювання.
   Забронювати місце в першій, потім надіслати стару форму в другій:
   з'явиться повідомлення «Це місце вже зайняте. Оберіть інше.».
5. Ім'я лише з пробілів відхиляється сервером. Неіснуючий сеанс або місце,
   зокрема місце іншого сеансу, повертає HTML-сторінку з HTTP 404.
6. Показати `DependencyInjectionTest`: робота IoC Container, три способи DI,
   повторне отримання singleton та незалежні prototype-чернетки.

Захист від подвійного бронювання залишається атомарним у Fake Repository.
Виведення тексту у шаблонах використовує `th:text`.

## Тести

- `DependencyInjectionTest` (`@SpringBootTest`): реальні beans із
  `ApplicationContext`, constructor/setter/field injection, singleton/prototype.
- `TicketServiceClockTest`: unit-тест із фіксованим `Clock`, перевіряє дату бронювання.
- `ScreeningMvcTest`: запускає Spring Boot із реальним HTTP-сервером на випадковому
  порту; перевіряє HTML, опис і тривалість, 15 місць, бронювання, повторну бронь,
  некоректне ім'я, 404 та заборону бронювання місця іншого сеансу.
- Наявні тести моделей, репозиторіїв і завантаження Spring-контексту також запускаються.

Адміністративні сторінки, REST, DI-інфраструктура поза цими прикладами та БД
до цього етапу не входять.
