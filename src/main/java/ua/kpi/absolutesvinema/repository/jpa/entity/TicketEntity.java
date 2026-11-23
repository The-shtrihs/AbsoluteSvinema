package ua.kpi.absolutesvinema.repository.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets", uniqueConstraints = @UniqueConstraint(name = "uk_tickets_seat", columnNames = "seat_id"))
public class TicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false, unique = true)
    private SeatEntity seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    protected TicketEntity() {
    }

    public TicketEntity(Long id, SeatEntity seat, UserEntity user, String customerName, LocalDateTime bookedAt) {
        this.id = id;
        this.seat = seat;
        this.user = user;
        this.customerName = customerName;
        this.bookedAt = bookedAt;
    }

    public Long getId() {
        return id;
    }

    public SeatEntity getSeat() {
        return seat;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }
}
