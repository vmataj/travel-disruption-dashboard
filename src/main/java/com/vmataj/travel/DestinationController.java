package com.vmataj.travel;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Duration;

@Controller
class DestinationController {
    private final DestinationService destinations;

    DestinationController(DestinationService destinations) {
        this.destinations = destinations;
    }

    @GetMapping("/")
    String home() {
        return "forward:/planner.html";
    }

    @GetMapping("/api/destinations")
    ResponseEntity<List<Destination>> search(@RequestParam String q) {
        String query = q.strip();
        if (query.length() < 2 || query.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter 2 to 100 characters.");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
            .body(destinations.search(query));
    }

    @GetMapping("/api/destinations/weather")
    ResponseEntity<JsonNode> weather(@RequestParam double lat, @RequestParam double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates.");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)))
            .body(destinations.weather(lat, lon));
    }
}
