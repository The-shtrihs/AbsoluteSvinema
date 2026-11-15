package ua.kpi.absolutesvinema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ThymeleafViewTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ScreeningService screeningService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void testXssProtectionInScreeningAndDetails() throws Exception {
        String xssPayload = "<script>alert('xss-attack')</script>";
        Screening xssScreening = screeningService.create(
                "XSS Film " + xssPayload,
                "Description with " + xssPayload,
                120,
                LocalDateTime.now().plusDays(2),
                "IMAX",
                new BigDecimal("220.00")
        );

        // Verify screenings list escapes the script tag
        mockMvc.perform(get("/screenings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss-attack&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString(xssPayload))));

        // Verify screening details escapes the script tag
        mockMvc.perform(get("/screenings/" + xssScreening.id()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss-attack&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString(xssPayload))));
    }

    @Test
    void testThymeleafConstructsRenderSuccessfully() throws Exception {
        Screening screening = screeningService.create(
                "Construct Showcase",
                "Testing Elvis and Switch constructs",
                180, // > 150 min (epic switch case)
                LocalDateTime.now().plusDays(3),
                "VIP",
                new BigDecimal("300.00")
        );

        mockMvc.perform(get("/screenings/" + screening.id()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("VIP Hall")))
                .andExpect(content().string(containsString("Available")))
                .andExpect(content().string(containsString("Cinema Screen")));

        mockMvc.perform(get("/screenings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Epic Feature")))
                .andExpect(content().string(containsString("VIP Lounge")));
    }
}
