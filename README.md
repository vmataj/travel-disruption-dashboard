# Travel Disruption Dashboard

A Java web application for reviewing fictional trip disruptions. Spring MVC renders the dashboard and trip pages with Thymeleaf. A small JavaScript client requests feed refreshes.

## Run

Use Java 21 and Maven with `mvn spring-boot:run` (H2), or run `docker compose up --build` (MySQL). Open http://localhost:8080.

1. Filter the board to **Delayed** trips.
2. Open **TD104** and flag it for follow-up.
3. Reload the trip page and confirm the flag is still visible.
4. Return to the board and refresh the simulated feed.

## Design

The simulated feed needs no external credentials. A successful refresh updates the last-check time and adds a history entry when the status changes. A failed fetch returns HTTP 503 without replacing the stored snapshot. Spring Security protects write requests with CSRF tokens. The demo uses fictional data and is intended to run locally.

## Verify

Run `mvn verify`. Integration tests render the filtered board, flag a trip, and use Mockito to simulate a feed failure.
