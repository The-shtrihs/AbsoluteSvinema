package ua.kpi.absolutesvinema.repository.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.repository.SeatRepository;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.sql.init.mode=always")
@ActiveProfiles("jpa")
@Import(ScreeningCreateRollbackIntegrationTest.FailureConfig.class)
class ScreeningCreateRollbackIntegrationTest {
    @Autowired private ScreeningService screeningService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FailingSeatRepository failingSeats;

    @Test
    void createRollsBackScreeningAndEarlierSeatsWhenSeatInsertFails() {
        String title = "JpaRollback" + System.nanoTime();
        failingSeats.armFailure();
        try {
            assertThatThrownBy(() -> screeningService.create(title, "Description", 90,
                    LocalDateTime.of(2027, 2, 3, 18, 0), "Hall 12", BigDecimal.TEN))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Injected seat persistence failure");
        } finally {
            failingSeats.disarmFailure();
        }

        Long attemptedScreeningId = failingSeats.getAttemptedScreeningId();
        assertThat(attemptedScreeningId).isPositive();
        assertThat(failingSeats.getSuccessfullySavedSeats()).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM screenings WHERE id = ? OR movie_title = ?",
                Integer.class, attemptedScreeningId, title)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM seats WHERE screening_id = ?",
                Integer.class, attemptedScreeningId)).isZero();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureConfig {
        @Bean
        @Primary
        FailingSeatRepository failingSeatRepository(@Qualifier("seatJpaAdapter") SeatRepository delegate) {
            return new FailingSeatRepository(delegate);
        }
    }

    static class FailingSeatRepository implements SeatRepository {
        private final SeatRepository delegate;
        private final AtomicInteger successfulSaves = new AtomicInteger();
        private final AtomicLong attemptedScreeningId = new AtomicLong();
        private final AtomicBoolean failureArmed = new AtomicBoolean();

        FailingSeatRepository(SeatRepository delegate) {
            this.delegate = delegate;
        }

        int getSuccessfullySavedSeats() {
            return successfulSaves.get();
        }

        Long getAttemptedScreeningId() {
            return attemptedScreeningId.get();
        }

        void armFailure() {
            successfulSaves.set(0);
            failureArmed.set(true);
        }

        void disarmFailure() {
            failureArmed.set(false);
        }

        @Override
        public Seat save(Seat seat) {
            attemptedScreeningId.set(seat.screeningId());
            if (failureArmed.get() && seat.rowNumber() == 2 && seat.seatNumber() == 3) {
                throw new IllegalStateException("Injected seat persistence failure");
            }
            Seat saved = delegate.save(seat);
            successfulSaves.incrementAndGet();
            return saved;
        }

        @Override
        public Optional<Seat> findById(Long id) {
            return delegate.findById(id);
        }

        @Override
        public List<Seat> findAll() {
            return delegate.findAll();
        }

        @Override
        public void deleteById(Long id) {
            delegate.deleteById(id);
        }

        @Override
        public List<Seat> findByScreeningId(Long screeningId) {
            return delegate.findByScreeningId(screeningId);
        }
    }
}
