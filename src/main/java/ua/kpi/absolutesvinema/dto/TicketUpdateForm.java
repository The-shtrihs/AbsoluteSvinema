package ua.kpi.absolutesvinema.dto;

import jakarta.validation.constraints.NotBlank;
import ua.kpi.absolutesvinema.model.Ticket;

public class TicketUpdateForm {
    @NotBlank(message = "Customer name must not be blank")
    private String customerName;

    public TicketUpdateForm() {
    }

    public TicketUpdateForm(String customerName) {
        this.customerName = customerName;
    }

    public static TicketUpdateForm from(Ticket ticket) {
        return new TicketUpdateForm(ticket.customerName());
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
