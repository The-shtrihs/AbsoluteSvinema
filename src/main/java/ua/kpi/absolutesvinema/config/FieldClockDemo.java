package ua.kpi.absolutesvinema.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class FieldClockDemo {
    @Autowired
    private Clock clock;

    public Instant now() {
        return clock.instant();
    }
}
