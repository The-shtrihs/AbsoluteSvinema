package ua.kpi.absolutesvinema.model;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** A separate, mutable draft for each request to the IoC container. */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BookingDraft {
    private String customerName;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
