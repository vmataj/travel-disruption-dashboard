package com.vmataj.travel;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Entity
class PlannerTrip {
    @Id UUID id;
    @Column(nullable = false, length = 40) String owner;
    @Embedded Destination destination;
    @Column(nullable = false, length = 120) String title;
    @Column(nullable = false) LocalDate startDate;
    @Column(nullable = false) LocalDate endDate;
    @Column(nullable = false, length = 3) String currency;
    @Column(nullable = false, precision = 12, scale = 2) BigDecimal budget;

    @ElementCollection
    @CollectionTable(name = "planner_collaborator", joinColumns = @JoinColumn(name = "trip_id"))
    @Column(name = "username", nullable = false, length = 40)
    Set<String> collaborators = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "planner_activity", joinColumns = @JoinColumn(name = "trip_id"))
    @MapKeyColumn(name = "activity_id")
    Map<UUID, Activity> activities = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "planner_expense", joinColumns = @JoinColumn(name = "trip_id"))
    @MapKeyColumn(name = "expense_id")
    Map<UUID, Expense> expenses = new HashMap<>();

    protected PlannerTrip() {}

    PlannerTrip(String owner, Plan plan) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.destination = plan.destination();
        update(plan);
    }

    void update(Plan plan) {
        title = plan.title().strip();
        startDate = plan.startDate();
        endDate = plan.endDate();
        currency = plan.currency();
        budget = plan.budget();
    }

    View view() {
        var itinerary = activities.entrySet().stream()
            .map(entry -> new ActivityRow(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing((ActivityRow row) -> row.activity().day())
                .thenComparing(row -> row.activity().time(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ActivityRow::id))
            .toList();
        var costs = expenses.entrySet().stream().map(entry -> new ExpenseRow(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(ExpenseRow::id)).toList();
        return new View(id, owner, title, destination, startDate, endDate, currency, budget,
            collaborators.stream().sorted().toList(), itinerary, costs);
    }

    record Plan(@NotNull @Valid Destination destination, @NotBlank @Size(max = 120) String title,
                @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
                @NotNull @Digits(integer = 10, fraction = 2) @PositiveOrZero BigDecimal budget) {}

    @Embeddable
    record Activity(@NotNull @Column(name = "activity_day", nullable = false) LocalDate day,
                    @Column(name = "activity_time") LocalTime time,
                    @NotBlank @Size(max = 120) @Column(nullable = false, length = 120) String title,
                    @Size(max = 180) @Column(length = 180) String location,
                    @Size(max = 500) @Column(length = 500) String notes) {}

    @Embeddable
    record Expense(@NotBlank @Size(max = 120) @Column(nullable = false, length = 120) String title,
                   @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Category category,
                   @NotNull @Positive @Digits(integer = 10, fraction = 2)
                   @Column(nullable = false, precision = 12, scale = 2) BigDecimal amount) {}

    enum Category { TRANSPORT, LODGING, FOOD, ACTIVITIES, OTHER }

    record ActivityRow(UUID id, Activity activity) {}
    record ExpenseRow(UUID id, Expense expense) {}
    record View(UUID id, String owner, String title, Destination destination, LocalDate startDate, LocalDate endDate,
                String currency, BigDecimal budget, List<String> collaborators,
                List<ActivityRow> activities, List<ExpenseRow> expenses) {}
}
