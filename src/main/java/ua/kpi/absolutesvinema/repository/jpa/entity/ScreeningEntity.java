package ua.kpi.absolutesvinema.repository.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screenings")
public class ScreeningEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_title", nullable = false, length = 255)
    private String movieTitle;

    @Column(name = "movie_description", nullable = false, columnDefinition = "text")
    private String movieDescription;

    @Column(name = "movie_duration_minutes", nullable = false)
    private int movieDurationMinutes;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "hall", nullable = false, length = 100)
    private String hall;

    @Column(name = "ticket_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @OneToMany(mappedBy = "screening")
    private List<SeatEntity> seats = new ArrayList<>();

    protected ScreeningEntity() {
    }

    public ScreeningEntity(Long id, String movieTitle, String movieDescription, int movieDurationMinutes,
                           LocalDateTime startsAt, String hall, BigDecimal ticketPrice) {
        this.id = id;
        this.movieTitle = movieTitle;
        this.movieDescription = movieDescription;
        this.movieDurationMinutes = movieDurationMinutes;
        this.startsAt = startsAt;
        this.hall = hall;
        this.ticketPrice = ticketPrice;
    }

    public Long getId() {
        return id;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public String getMovieDescription() {
        return movieDescription;
    }

    public int getMovieDurationMinutes() {
        return movieDurationMinutes;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public String getHall() {
        return hall;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public List<SeatEntity> getSeats() {
        return seats;
    }
}
