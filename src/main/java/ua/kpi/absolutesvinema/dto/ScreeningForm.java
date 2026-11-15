package ua.kpi.absolutesvinema.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import ua.kpi.absolutesvinema.model.Screening;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ScreeningForm {
    @NotBlank(message = "Movie title must not be blank")
    private String movieTitle;

    @NotBlank(message = "Movie description must not be blank")
    private String movieDescription;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer movieDurationMinutes;

    @NotNull(message = "Start date and time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startsAt;

    @NotBlank(message = "Hall must not be blank")
    private String hall;

    @NotNull(message = "Ticket price is required")
    @DecimalMin(value = "0.0", message = "Ticket price must be non-negative")
    private BigDecimal ticketPrice;

    public ScreeningForm() {
    }

    public ScreeningForm(String movieTitle, String movieDescription, Integer movieDurationMinutes,
                         LocalDateTime startsAt, String hall, BigDecimal ticketPrice) {
        this.movieTitle = movieTitle;
        this.movieDescription = movieDescription;
        this.movieDurationMinutes = movieDurationMinutes;
        this.startsAt = startsAt;
        this.hall = hall;
        this.ticketPrice = ticketPrice;
    }

    public static ScreeningForm from(Screening screening) {
        return new ScreeningForm(
                screening.movieTitle(),
                screening.movieDescription(),
                screening.movieDurationMinutes(),
                screening.startsAt(),
                screening.hall(),
                screening.ticketPrice()
        );
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getMovieDescription() {
        return movieDescription;
    }

    public void setMovieDescription(String movieDescription) {
        this.movieDescription = movieDescription;
    }

    public Integer getMovieDurationMinutes() {
        return movieDurationMinutes;
    }

    public void setMovieDurationMinutes(Integer movieDurationMinutes) {
        this.movieDurationMinutes = movieDurationMinutes;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public String getHall() {
        return hall;
    }

    public void setHall(String hall) {
        this.hall = hall;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }
}
