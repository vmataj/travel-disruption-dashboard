package com.vmataj.travel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.security.Principal;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("private")
@RequestMapping("/api/planner")
class PlannerController {
    private final PlannerService planner;

    PlannerController(PlannerService planner) {
        this.planner = planner;
    }

    @GetMapping
    ResponseEntity<PlannerService.Workspace> workspace(Principal user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(planner.workspace(user.getName()));
    }

    @PutMapping("/favorites")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void favorite(Principal user, @Valid @RequestBody Destination destination) {
        planner.saveDestination(user.getName(), destination);
    }

    @DeleteMapping("/favorites/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unfavorite(Principal user, @PathVariable long placeId) {
        planner.removeDestination(user.getName(), placeId);
    }

    @PostMapping("/trips")
    @ResponseStatus(HttpStatus.CREATED)
    PlannerTrip.View create(Principal user, @Valid @RequestBody PlannerTrip.Plan plan) {
        return planner.create(user.getName(), plan);
    }

    @PutMapping("/trips/{id}")
    PlannerTrip.View update(Principal user, @PathVariable UUID id, @Valid @RequestBody PlannerTrip.Plan plan) {
        return planner.update(user.getName(), id, plan);
    }

    @DeleteMapping("/trips/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Principal user, @PathVariable UUID id) {
        planner.delete(user.getName(), id);
    }

    @PostMapping("/trips/{id}/activities")
    PlannerTrip.View addActivity(Principal user, @PathVariable UUID id, @Valid @RequestBody PlannerTrip.Activity activity) {
        return planner.activity(user.getName(), id, null, activity);
    }

    @PutMapping("/trips/{id}/activities/{row}")
    PlannerTrip.View editActivity(Principal user, @PathVariable UUID id, @PathVariable UUID row,
                                  @Valid @RequestBody PlannerTrip.Activity activity) {
        return planner.activity(user.getName(), id, row, activity);
    }

    @DeleteMapping("/trips/{id}/activities/{row}")
    PlannerTrip.View deleteActivity(Principal user, @PathVariable UUID id, @PathVariable UUID row) {
        return planner.removeActivity(user.getName(), id, row);
    }

    @PostMapping("/trips/{id}/expenses")
    PlannerTrip.View addExpense(Principal user, @PathVariable UUID id, @Valid @RequestBody PlannerTrip.Expense expense) {
        return planner.expense(user.getName(), id, null, expense);
    }

    @PutMapping("/trips/{id}/expenses/{row}")
    PlannerTrip.View editExpense(Principal user, @PathVariable UUID id, @PathVariable UUID row,
                                 @Valid @RequestBody PlannerTrip.Expense expense) {
        return planner.expense(user.getName(), id, row, expense);
    }

    @DeleteMapping("/trips/{id}/expenses/{row}")
    PlannerTrip.View deleteExpense(Principal user, @PathVariable UUID id, @PathVariable UUID row) {
        return planner.removeExpense(user.getName(), id, row);
    }

    @PutMapping("/trips/{id}/collaborators")
    PlannerTrip.View share(Principal user, @PathVariable UUID id, @Valid @RequestBody Invite invite) {
        return planner.share(user.getName(), id, invite.username());
    }

    @DeleteMapping("/trips/{id}/collaborators/{username}")
    PlannerTrip.View unshare(Principal user, @PathVariable UUID id, @PathVariable String username) {
        return planner.unshare(user.getName(), id, username);
    }

    record Invite(@NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9_-]{2,39}") String username) {}
}
