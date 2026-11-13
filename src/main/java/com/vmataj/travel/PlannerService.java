package com.vmataj.travel;

import static org.springframework.http.HttpStatus.*;

import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("private")
@Transactional
class PlannerService {
    private final PlannerTripRepository trips;
    private final SavedDestinationRepository favorites;
    private final UserDetailsManager users;

    PlannerService(PlannerTripRepository trips, SavedDestinationRepository favorites, UserDetailsManager users) {
        this.trips = trips;
        this.favorites = favorites;
        this.users = users;
    }

    @Transactional(readOnly = true)
    Workspace workspace(String user) {
        return new Workspace(trips.visibleTo(user).stream().map(PlannerTrip::view).toList(),
            favorites.findByOwnerOrderByDestinationName(user).stream().map(favorite -> favorite.destination).toList());
    }

    void saveDestination(String user, Destination destination) {
        favorites.save(new SavedDestination(user, destination));
    }

    void removeDestination(String user, long placeId) {
        favorites.deleteById(SavedDestination.key(user, placeId));
    }

    PlannerTrip.View create(String user, PlannerTrip.Plan plan) {
        checkPlan(plan);
        return trips.save(new PlannerTrip(user, plan)).view();
    }

    PlannerTrip.View update(String user, UUID id, PlannerTrip.Plan plan) {
        PlannerTrip trip = owned(user, id);
        checkPlan(plan);
        if (!trip.destination.equals(plan.destination())) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose a destination when creating a trip.");
        }
        if (!trip.expenses.isEmpty() && !trip.currency.equals(plan.currency())) {
            throw new ResponseStatusException(CONFLICT, "Remove existing expenses before changing the trip currency.");
        }
        if (trip.activities.values().stream().anyMatch(activity ->
                activity.day().isBefore(plan.startDate()) || activity.day().isAfter(plan.endDate()))) {
            throw new ResponseStatusException(CONFLICT, "The new dates must include every itinerary activity.");
        }
        trip.update(plan);
        return trip.view();
    }

    void delete(String user, UUID id) {
        trips.delete(owned(user, id));
    }

    PlannerTrip.View activity(String user, UUID id, UUID rowId, PlannerTrip.Activity activity) {
        PlannerTrip trip = editable(user, id);
        if (activity.day().isBefore(trip.startDate) || activity.day().isAfter(trip.endDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "Pick an activity date within the trip.");
        }
        put(trip.activities, rowId, activity);
        return trip.view();
    }

    PlannerTrip.View removeActivity(String user, UUID id, UUID rowId) {
        PlannerTrip trip = editable(user, id);
        trip.activities.remove(rowId);
        return trip.view();
    }

    PlannerTrip.View expense(String user, UUID id, UUID rowId, PlannerTrip.Expense expense) {
        PlannerTrip trip = editable(user, id);
        put(trip.expenses, rowId, expense);
        return trip.view();
    }

    PlannerTrip.View removeExpense(String user, UUID id, UUID rowId) {
        PlannerTrip trip = editable(user, id);
        trip.expenses.remove(rowId);
        return trip.view();
    }

    PlannerTrip.View share(String user, UUID id, String username) {
        PlannerTrip trip = owned(user, id);
        if (user.equals(username)) throw new ResponseStatusException(BAD_REQUEST, "You already own this trip.");
        if (!users.userExists(username)) throw new ResponseStatusException(NOT_FOUND, "That account could not be found.");
        trip.collaborators.add(username);
        return trip.view();
    }

    PlannerTrip.View unshare(String user, UUID id, String username) {
        PlannerTrip trip = owned(user, id);
        trip.collaborators.remove(username);
        return trip.view();
    }

    private PlannerTrip editable(String user, UUID id) {
        return trips.lock(id).filter(trip -> user.equals(trip.owner) || trip.collaborators.contains(user))
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Trip not found."));
    }

    private PlannerTrip owned(String user, UUID id) {
        PlannerTrip trip = editable(user, id);
        if (!user.equals(trip.owner)) throw new ResponseStatusException(FORBIDDEN, "Only the trip owner can do that.");
        return trip;
    }

    private void checkPlan(PlannerTrip.Plan plan) {
        if (plan.endDate().isBefore(plan.startDate())) {
            throw new ResponseStatusException(BAD_REQUEST, "The return date must be on or after departure.");
        }
        try {
            Currency.getInstance(plan.currency());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose a valid currency.");
        }
    }

    private <T> void put(Map<UUID, T> rows, UUID rowId, T value) {
        if (rowId != null && !rows.containsKey(rowId)) {
            throw new ResponseStatusException(NOT_FOUND, "That entry no longer exists.");
        }
        rows.put(rowId == null ? UUID.randomUUID() : rowId, value);
    }

    record Workspace(List<PlannerTrip.View> trips, List<Destination> favorites) {}
}
