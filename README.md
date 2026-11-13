# Travel Disruption Dashboard

I built this trip planner to practice working with live location and weather APIs. Search cities, check a seven-day forecast, and save places. Sign in to build itineraries, track expenses, or share a trip with another account. The departures board uses a simulated feed for delays and outages.

[Live app](https://travel-disruption-dashboard.onrender.com/)

Java 21, Spring Boot, Spring Security, Spring Data JPA, Thymeleaf, and JavaScript. Docker Compose runs MySQL locally. The hosted app uses PostgreSQL on Neon.

Run `docker compose up --build` and open [localhost:8080](http://localhost:8080).

`mvn test` runs JUnit 5, Mockito, and Spring MockMvc tests. Add `-Dbrowser.tests=true` for Selenium WebDriver tests in headless Chrome.

Data from [Open-Meteo and GeoNames](https://open-meteo.com/en/docs/geocoding-api) and [OpenStreetMap](https://www.openstreetmap.org/copyright). Lisbon photo by [milos milosevic](https://commons.wikimedia.org/wiki/File:Lisbon%27s_panorama_from_Alfama_-_panoramio.jpg), [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/), cropped in the dashboard.
