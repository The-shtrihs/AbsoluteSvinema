package ua.kpi.absolutesvinema.repository.memory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;
import ua.kpi.absolutesvinema.repository.SeatRepository;
import ua.kpi.absolutesvinema.repository.TicketRepository;
import ua.kpi.absolutesvinema.repository.UserRepository;
import ua.kpi.absolutesvinema.repository.jdbc.JdbcScreeningRepository;
import ua.kpi.absolutesvinema.repository.jpa.ScreeningJpaAdapter;
import ua.kpi.absolutesvinema.repository.jpa.store.ScreeningJpaDataRepository;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("memory")
class MemoryProfileIntegrationTest {
    @Autowired private ApplicationContext context;
    @Autowired private ScreeningRepository screenings;
    @Autowired private SeatRepository seats;
    @Autowired private TicketRepository tickets;
    @Autowired private UserRepository users;
    @Autowired private ScreeningService screeningService;

    @Test
    void memoryProfileUsesOnlyMemoryRepositoriesAndStillCreatesSeats() {
        assertThat(screenings).isInstanceOf(InMemoryScreeningRepository.class);
        assertThat(seats).isInstanceOf(InMemorySeatRepository.class);
        assertThat(tickets).isInstanceOf(InMemoryTicketRepository.class);
        assertThat(users).isInstanceOf(InMemoryUserRepository.class);
        assertThat(context.getBeansOfType(JdbcScreeningRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(ScreeningJpaAdapter.class)).isEmpty();
        assertThat(context.getBeansOfType(ScreeningJpaDataRepository.class)).isEmpty();

        var screening = screeningService.create("Memory profile " + System.nanoTime(), "Description", 90,
                LocalDateTime.of(2027, 2, 4, 18, 0), "Hall 13", BigDecimal.TEN);
        assertThat(screeningService.findSeats(screening.id())).hasSize(15);
    }
}
