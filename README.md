# Travel Disruption Dashboard

I built this to practice handling a flaky status feed. Filter departures, review the trip history, and flag trips for follow-up. The feed is simulated so you can reproduce delays and outages.

[Live app](https://travel-disruption-dashboard.onrender.com/)

Java 21, Spring Boot, Spring MVC, Thymeleaf and Spring Data JPA. Docker Compose runs MySQL locally. The hosted app uses PostgreSQL on Neon.

## Run it

Run `docker compose up --build` and open [localhost:8080](http://localhost:8080).

## Tests

Run `mvn test` for the JUnit 5, Mockito and Spring MockMvc tests. Run `mvn test -Dbrowser.tests=true` to include Selenium WebDriver tests in headless Chrome. Mockito checks what happens when the feed fails, MockMvc covers filters and private trip data, and Selenium filters the departures board.
