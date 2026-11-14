package ua.kpi.absolutesvinema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import ua.kpi.absolutesvinema.config.FieldClockDemo;
import ua.kpi.absolutesvinema.config.SetterClockDemo;
import ua.kpi.absolutesvinema.model.BookingDraft;
import ua.kpi.absolutesvinema.service.ScreeningService;
import ua.kpi.absolutesvinema.service.TicketService;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DependencyInjectionTest {
    @Autowired
    private ApplicationContext context;

    @Test
    void constructorInjectionAndSingleton() {
        var screenings = context.getBean(ScreeningService.class);
        var tickets = context.getBean(TicketService.class);
        assertThat(context.getBean(ScreeningService.class)).isSameAs(screenings);
        assertThat(context.getBean(TicketService.class)).isSameAs(tickets);
        // Calls use the constructor-injected repositories.
        assertThat(screenings.findAll()).isNotEmpty();
        assertThat(tickets.findAll()).isNotNull();
    }

    @Test
    void setterAndFieldInjectionUseClockBean() {
        var clock = context.getBean(Clock.class);
        var before = clock.instant();
        var setterTime = context.getBean(SetterClockDemo.class).now();
        var fieldTime = context.getBean(FieldClockDemo.class).now();
        var after = clock.instant();
        assertThat(setterTime).isBetween(before, after);
        assertThat(fieldTime).isBetween(before, after);
    }

    @Test
    void prototypeCreatesIndependentDrafts() {
        var provider = context.getBeanProvider(BookingDraft.class);
        var first = provider.getObject();
        var second = provider.getObject();
        assertThat(second).isNotSameAs(first);
        first.setCustomerName("Anna");
        assertThat(second.getCustomerName()).isNull();
    }
}
