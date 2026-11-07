package com.vmataj.travel;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class SimulatedFeed {
    record Update(String code, String route, LocalDate departure, Trip.Status status) {}

    List<Update> fetch() {
        LocalDate today = LocalDate.now();
        return List.of(
            new Update("TD104", "Paris → Rome", today, Trip.Status.DELAYED),
            new Update("TD206", "Paris → Berlin", today, Trip.Status.CANCELLED),
            new Update("TD311", "Rome → Tirana", today, Trip.Status.ON_TIME),
            new Update("TD420", "Berlin → Paris", today.plusDays(1), Trip.Status.ON_TIME)
        );
    }
}
