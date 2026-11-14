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
        screeningService.create("Інтерстеллар",
                "Команда дослідників вирушає крізь червоточину в пошуках нового дому для людства.",
                169, tomorrow.atTime(18, 0), "Зал 1", new BigDecimal("180.00"));
        screeningService.create("Віднесені привидами",
                "Дівчинка Тіхіро потрапляє у світ духів і намагається врятувати своїх батьків.",
                125, tomorrow.atTime(19, 0), "Зал 2", new BigDecimal("150.00"));
    }
}
