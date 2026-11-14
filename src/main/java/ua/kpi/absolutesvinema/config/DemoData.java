package ua.kpi.absolutesvinema.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

@Component
public class DemoData implements CommandLineRunner {
    private final ScreeningService screeningService;
    private final Clock clock;

    public DemoData(ScreeningService screeningService, Clock clock) {
        this.screeningService = screeningService;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (!screeningService.findAll().isEmpty()) {
            return;
        }
        var tomorrow = LocalDate.now(clock).plusDays(1);
        screeningService.create("Interstellar",
                "A team of explorers travels through a wormhole to find a new home for humanity.",
                169, tomorrow.atTime(18, 0), "Hall 1", new BigDecimal("180.00"));
        screeningService.create("Pulp Fiction",
                "The lives of two hitmen, a boxer, and a gangster's wife intertwine in Los Angeles.",
                154, tomorrow.atTime(19, 0), "Hall 2", new BigDecimal("160.00"));
        screeningService.create("The Lord of the Rings: The Return of the King",
                "Frodo and Sam approach Mount Doom as their friends rally against Sauron's armies.",
                201, tomorrow.atTime(17, 30), "Hall 3", new BigDecimal("200.00"));
    }
}
