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
    public void refresh(String owner, FeedScenario scenario) {
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
            String key = key(owner, update.code());
            Trip trip = repository.findById(key)
                .orElseGet(() -> new Trip(key, update.route(), update.departure()));
            trip.owner = owner;
            trip.route = update.route();
            trip.departure = update.departure();
            Trip.Status status = scenario == FeedScenario.RECOVERY && update.code().equals("TD104")
                ? Trip.Status.ON_TIME : update.status();
            trip.update(status, time);
            repository.save(trip);
        }
    }

    @Transactional(readOnly = true)
    public List<Row> search(String owner, String route, LocalDate departure, Trip.Status status) {
        return repository.search(owner, route, departure, status).stream().map(Row::from).toList();
    }

    @Transactional(readOnly = true)
    public Detail detail(String owner, String code) {
        Trip trip = find(owner, code);
        return new Detail(Row.from(trip), trip.history.stream()
            .map(event -> new Event(event.status().name().replace('_', ' '), UTC.format(event.occurredAt())))
            .toList());
    }

    @Transactional
    public void setFollowUp(String owner, String code, boolean flagged) {
        find(owner, code).followUp = flagged;
    }

    private Trip find(String owner, String code) {
        return repository.findById(key(owner, code))
            .filter(trip -> java.util.Objects.equals(trip.owner, owner))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String key(String owner, String code) {
        return owner == null ? code : owner + ":" + code;
    }

    public record Row(String code, String route, LocalDate departure, String status,
                      String statusLabel, String checkedAt, boolean followUp) {
        static Row from(Trip trip) {
            String status = trip.status.name();
            String code = trip.owner == null ? trip.code : trip.code.substring(trip.owner.length() + 1);
            return new Row(code, trip.route, trip.departure, status,
                status.replace('_', ' '), UTC.format(trip.checkedAt), trip.followUp);
        }
    }
    public record Event(String status, String time) {}
    public record Detail(Row trip, List<Event> history) {}
    enum FeedScenario { BASELINE, RECOVERY, UNAVAILABLE }
}
