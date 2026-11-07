package com.vmataj.travel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class TripService {
    private static final DateTimeFormatter UTC =
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    private final TripRepository repository;
    private final SimulatedFeed feed;

    TripService(TripRepository repository, SimulatedFeed feed) {
        this.repository = repository;
        this.feed = feed;
    }

    @Transactional
    public void refresh(FeedScenario scenario) {
        if (scenario == FeedScenario.UNAVAILABLE) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated feed outage");
        }
        List<SimulatedFeed.Update> snapshot;
        try {
            snapshot = feed.fetch();
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Feed unavailable", exception);
        }
        Instant time = Instant.now();
        for (var update : snapshot) {
            Trip trip = repository.findById(update.code())
                .orElseGet(() -> new Trip(update.code(), update.route(), update.departure()));
            trip.route = update.route();
            trip.departure = update.departure();
            Trip.Status status = scenario == FeedScenario.RECOVERY && update.code().equals("TD104")
                ? Trip.Status.ON_TIME : update.status();
            trip.update(status, time);
            repository.save(trip);
        }
    }

    @Transactional(readOnly = true)
    public List<Row> search(String route, LocalDate departure, Trip.Status status) {
        return repository.search(route, departure, status).stream().map(Row::from).toList();
    }

    @Transactional(readOnly = true)
    public Detail detail(String code) {
        Trip trip = find(code);
        return new Detail(Row.from(trip), trip.history.stream()
            .map(event -> new Event(event.status().name().replace('_', ' '), UTC.format(event.occurredAt())))
            .toList());
    }

    @Transactional
    public void setFollowUp(String code, boolean flagged) {
        find(code).followUp = flagged;
    }

    private Trip find(String code) {
        return repository.findById(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record Row(String code, String route, LocalDate departure, String status,
                      String statusLabel, String checkedAt, boolean followUp) {
        static Row from(Trip trip) {
            String status = trip.status.name();
            return new Row(trip.code, trip.route, trip.departure, status,
                status.replace('_', ' '), UTC.format(trip.checkedAt), trip.followUp);
        }
    }
    public record Event(String status, String time) {}
    public record Detail(Row trip, List<Event> history) {}
    enum FeedScenario { BASELINE, RECOVERY, UNAVAILABLE }
}
