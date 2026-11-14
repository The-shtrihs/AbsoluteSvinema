package ua.kpi.absolutesvinema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ua.kpi.absolutesvinema.service.ScreeningService;
import ua.kpi.absolutesvinema.service.TicketService;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScreeningMvcTest {
    @LocalServerPort
    private int port;
    @Autowired
    private ScreeningService screenings;
    @Autowired
    private TicketService tickets;

    @Autowired
    private WebApplicationContext context;

    @Test
    void homeUsesIndexTemplateAndLinksToScreenings() throws Exception {
        var result = MockMvcBuilders.webAppContextSetup(context).build()
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getModelAndView()).isNotNull();
        assertThat(result.getModelAndView().getViewName()).isEqualTo("index");
        assertThat(result.getResponse().getContentAsString()).contains("href=\"/screenings\"");
    }

    @Test
    void rendersPagesAndHandlesBookingAndErrors() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var screening = screenings.create("Test film", "A full <description>", 113,
                    LocalDateTime.of(2027, 1, 10, 18, 30), "Test hall", new BigDecimal("175.00"));
            var seatId = screenings.findSeats(screening.id()).getFirst().id();
            var path = "/screenings/" + screening.id();
            var list = get(client, "/screenings");
            assertThat(list.statusCode()).isEqualTo(200);
            assertThat(list.body()).contains("Test film", "Test hall", "175.00", "113", "10.01.2027 18:30");
            var detail = get(client, path);
            assertThat(detail.statusCode()).isEqualTo(200);
            assertThat(detail.body()).contains("A full &lt;description&gt;", "113", "Вільне");
            assertThat(detail.body().split("name=\"seatId\"", -1)).hasSize(16);

            var booked = post(client, path, seatId, "Anna");
            assertThat(booked.statusCode()).isEqualTo(302);
            assertThat(booked.headers().firstValue("Location").orElseThrow().split(";", 2)[0])
                    .isEqualTo("http://localhost:" + port + path);
            assertThat(tickets.isSeatBooked(seatId)).isTrue();
            var confirmation = getWithCookies(client, path, booked);
            assertThat(confirmation.body()).contains("Місце успішно заброньовано.", "Зайняте");
            assertThat(confirmation.body()).doesNotContain("name=\"seatId\" value=\"" + seatId + "\"");

            var duplicate = post(client, path, seatId, "Ivan");
            assertThat(duplicate.statusCode()).isEqualTo(302);
            assertThat(getWithCookies(client, path, duplicate).body()).contains("Це місце вже зайняте");
            assertThat(tickets.findAll().stream().filter(ticket -> ticket.seatId().equals(seatId))).hasSize(1);

            var freeSeat = screenings.findSeats(screening.id()).get(1).id();
            var invalid = post(client, path, freeSeat, "  ");
            assertThat(invalid.statusCode()).isEqualTo(302);
            assertThat(getWithCookies(client, path, invalid).body()).contains("Вкажіть непорожнє ім’я покупця");
            assertThat(tickets.isSeatBooked(freeSeat)).isFalse();
            assertThat(get(client, "/screenings/999999").statusCode()).isEqualTo(404);
            assertThat(post(client, path, 999999L, "Anna").statusCode()).isEqualTo(404);

            var other = screenings.create("Other", "Description", 90, LocalDateTime.now().plusDays(1),
                    "Other hall", BigDecimal.TEN);
            var otherSeat = screenings.findSeats(other.id()).getFirst().id();
            assertThat(post(client, path, otherSeat, "Anna").statusCode()).isEqualTo(404);
            assertThat(tickets.isSeatBooked(otherSeat)).isFalse();
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(url(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithCookies(HttpClient client, String path, HttpResponse<?> previous)
            throws Exception {
        List<String> cookies = previous.headers().allValues("Set-Cookie").stream()
                .map(cookie -> cookie.split(";", 2)[0]).toList();
        assertThat(cookies).isNotEmpty();
        return client.send(HttpRequest.newBuilder(url(path)).header("Cookie", String.join("; ", cookies))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String path, Long seatId, String name) throws Exception {
        String body = "seatId=" + seatId + "&customerName=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        return client.send(HttpRequest.newBuilder(url(path + "/book"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
