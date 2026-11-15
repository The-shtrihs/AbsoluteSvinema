package ua.kpi.absolutesvinema.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ua.kpi.absolutesvinema.service.UserService;

@Component
public class DemoUserData implements CommandLineRunner {
    private final UserService userService;

    public DemoUserData(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (userService.findByEmail("admin@svinema.ua").isEmpty()) {
            userService.createWithRole("admin@svinema.ua", "admin123", "Svinema Admin", "ROLE_ADMIN");
        }
        if (userService.findByEmail("user@svinema.ua").isEmpty()) {
            userService.createWithRole("user@svinema.ua", "user123", "Danylo Marchenko", "ROLE_USER");
        }
    }
}
