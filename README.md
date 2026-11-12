# Travel Disruption Dashboard

A Java web application for reviewing fictional trip disruptions. Spring MVC renders the dashboard and trip pages with Thymeleaf. A small JavaScript client requests feed refreshes.

## Run

Use Java 21 and Maven with `mvn spring-boot:run` (H2), or run `docker compose up --build` (MySQL). Open http://localhost:8080.

1. Filter the board to **Delayed** trips.
2. Open **TD104** and flag it for follow-up.
3. Reload the trip page and confirm the flag is still visible. Use **Clear follow-up** to remove it.
4. On the dashboard, clear any filters and choose **Trip TD104 recovered**, then refresh. Open the trip to see the status change in its history.
5. Choose **Feed unavailable** and refresh to verify the last successful snapshot stays visible.

## Design

The simulated feed needs no external credentials. A successful refresh updates the last-check time and adds a history entry when the status changes. A failed fetch returns HTTP 503 without replacing the stored snapshot. Spring Security protects write requests with CSRF tokens. The demo uses fictional data and is intended to run locally.

## Registered accounts

For the registered-user workflow, set `APP_PROFILE=private` in the shell before running
`docker compose up --build`. Create an account to get your own simulated trips, status history, and follow-up flags.
Docker Compose supplies persistent MySQL and allows cookies over local HTTP.
The default profile uses the original local demo workflow.

For HTTPS hosting, activate the `private` Spring profile and supply
`SERVER_ADDRESS=0.0.0.0`, `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.
Use a dedicated database for this app and inject the password from the hosting
provider's secret store. The service honors `PORT` and requires HTTPS cookies by default.
The bundled Cloud SQL connector accepts a JDBC URL of the form
`jdbc:mysql:///DATABASE?cloudSqlInstance=PROJECT:REGION:INSTANCE&socketFactory=com.google.cloud.sql.mysql.SocketFactory&cloudSqlRefreshStrategy=lazy`.
For a first Cloud Run deployment, limit the service to one instance because login sessions are held in memory.

## Free hosting on Render and Neon

Create a Neon Free PostgreSQL project for this app. Connect this repository to
Render as a Blueprint and select the Free plan. The Blueprint activates the
registered-account profile and asks for these database settings:

- `SPRING_DATASOURCE_URL` is the Neon JDBC URL, such as `jdbc:postgresql://HOST/DATABASE?sslmode=require`.
- `SPRING_DATASOURCE_USERNAME` is the Neon database role.
- `SPRING_DATASOURCE_PASSWORD` is the role password.

Enter the database credentials in Render, where they are stored as environment
variables. Use a separate Neon project for each app. Local Docker Compose
continues to use MySQL. Login sessions reset when the free web service restarts.

## Tests

Run `mvn verify`. Integration tests render the filtered board, flag a trip, and use Mockito to simulate a feed failure.
