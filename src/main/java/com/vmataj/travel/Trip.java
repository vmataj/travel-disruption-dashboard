package com.vmataj.travel;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
class Trip {
    enum Status { ON_TIME, DELAYED, CANCELLED }

    @Id String code;
    String owner;
    String route;
    LocalDate departure;
    @Enumerated(EnumType.STRING)
    Status status;
    Instant checkedAt;
    boolean followUp;
    @Version long version;

    @ElementCollection
    @OrderColumn(name = "event_order")
    List<StatusEvent> history = new ArrayList<>();

    protected Trip() {}

    Trip(String code, String route, LocalDate departure) {
        this.code = code;
        this.route = route;
        this.departure = departure;
    }

    void update(Status next, Instant time) {
        if (status != next) history.add(new StatusEvent(next, time));
        status = next;
        checkedAt = time;
    }

    @Embeddable
    record StatusEvent(@Enumerated(EnumType.STRING) Status status, Instant occurredAt) {}
}
