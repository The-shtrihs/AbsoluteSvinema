package ua.kpi.absolutesvinema.service;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.kpi.absolutesvinema.model.Screening;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ScreeningTransactionTest {

    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2027, 3, 1, 19, 0);

    @Autowired
    private ScreeningManagement screenings;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    @Test
    void serviceIsTransactionalProxyBehindInterface() {
        assertThat(AopUtils.isAopProxy(screenings)).isTrue();
    }

    @Test
    void successfulCreateCommitsScreeningAndFifteenSeats() {
        int screeningsBefore = count("SELECT COUNT(*) FROM screenings");
        int seatsBefore = count("SELECT COUNT(*) FROM seats");

        Screening created = screenings.create("Tx Success", "Description", 100, STARTS_AT,
                "Hall 4", new BigDecimal("150.00"));

        assertThat(count("SELECT COUNT(*) FROM screenings")).isEqualTo(screeningsBefore + 1);
        assertThat(count("SELECT COUNT(*) FROM seats")).isEqualTo(seatsBefore + 15);
        assertThat(count("SELECT COUNT(*) FROM seats WHERE screening_id = ?", created.id())).isEqualTo(15);
    }

    @Test
    void failureOnThirdSeatRollsBackScreeningAndEarlierSeats() {
        int screeningsBefore = count("SELECT COUNT(*) FROM screenings");
        int seatsBefore = count("SELECT COUNT(*) FROM seats");

        jdbcTemplate.execute("ALTER TABLE seats ADD CONSTRAINT tmp_no_seat_3 CHECK (seat_number <> 3) NOCHECK");
        try {
            assertThatThrownBy(() -> screenings.create("Tx Rollback", "Description", 100, STARTS_AT,
                    "Hall 4", new BigDecimal("150.00")))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.execute("ALTER TABLE seats DROP CONSTRAINT tmp_no_seat_3");
        }

        assertThat(count("SELECT COUNT(*) FROM screenings WHERE movie_title = 'Tx Rollback'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM screenings")).isEqualTo(screeningsBefore);
        assertThat(count("SELECT COUNT(*) FROM seats")).isEqualTo(seatsBefore);
    }
}
