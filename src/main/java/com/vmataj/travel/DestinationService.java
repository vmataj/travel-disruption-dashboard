package com.vmataj.travel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class DestinationService {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper json;
    private final String geocodingUrl;
    private final String forecastUrl;

    DestinationService(ObjectMapper json,
                       @Value("${travel.geocoding-url:https://geocoding-api.open-meteo.com/v1/search}") String geocodingUrl,
                       @Value("${travel.forecast-url:https://api.open-meteo.com/v1/forecast}") String forecastUrl) {
        this.json = json;
        this.geocodingUrl = geocodingUrl;
        this.forecastUrl = forecastUrl;
    }

    List<Destination> search(String query) {
        String name = URLEncoder.encode(query, StandardCharsets.UTF_8);
        JsonNode data = fetch(geocodingUrl + "?name=" + name + "&count=10&language=en&format=json");
        List<Destination> matches = new ArrayList<>();
        for (JsonNode row : data.path("results")) {
            if (!row.hasNonNull("country") || !row.hasNonNull("latitude") || !row.hasNonNull("longitude")) continue;
            matches.add(new Destination(row.path("id").asLong(), row.path("name").asText(),
                row.path("admin1").asText(null), row.path("country").asText(), row.path("country_code").asText(null),
                row.path("latitude").asDouble(), row.path("longitude").asDouble(), row.path("timezone").asText(null),
                row.hasNonNull("population") ? row.path("population").asLong() : null));
        }
        return matches;
    }

    JsonNode weather(double lat, double lon) {
        JsonNode data = fetch(forecastUrl + "?latitude=" + lat + "&longitude=" + lon
            + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_gusts_10m_max"
            + "&timezone=auto&forecast_days=7");
        if (!data.path("current").isObject() || !data.path("daily").isObject()) throw unavailable();
        return data;
    }

    private JsonNode fetch(String url) {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
            .header("Accept", "application/json").GET().build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw unavailable();
            JsonNode data = json.readTree(response.body());
            if (data == null || !data.isObject() || data.path("error").asBoolean()) throw unavailable();
            return data;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException exception) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Travel data is temporarily unavailable.");
    }
}
