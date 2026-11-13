# Waypoint Travel Planner

I built Waypoint to bring the things I check before a trip into one place. Search for a city, explore the map, and check the seven-day forecast. Sign in to save trips, build an itinerary, track spending by category, and invite another traveler to edit the plan. A separate departures board demonstrates how the app handles simulated delays and outages.

[Live app](https://travel-disruption-dashboard.onrender.com/)

Built with Java 21, Spring Boot, Spring Security, Spring Data JPA, Thymeleaf, and JavaScript. Docker Compose runs MySQL locally. Render hosts the app with PostgreSQL on Neon.

Run `docker compose up --build` and open [localhost:8080](http://localhost:8080).

`mvn test` runs JUnit 5, Mockito, and Spring MockMvc tests for access control and feed failures. Add `-Dbrowser.tests=true` to run the Selenium WebDriver tests for destination search, saved places, and the departures board.

Data from [Open-Meteo and GeoNames](https://open-meteo.com/en/docs/geocoding-api) and [OpenStreetMap](https://www.openstreetmap.org/copyright). Lisbon photo by [milos milosevic](https://commons.wikimedia.org/wiki/File:Lisbon%27s_panorama_from_Alfama_-_panoramio.jpg), [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/), cropped in the dashboard.
