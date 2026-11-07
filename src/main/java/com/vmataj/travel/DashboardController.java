package com.vmataj.travel;

import java.time.LocalDate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class DashboardController {
    private final TripService service;
    private final TripRepository repository;

    DashboardController(TripService service, TripRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Bean
    ApplicationRunner seedTrips() {
        return args -> {
            if (repository.count() == 0) service.refresh(TripService.FeedScenario.BASELINE);
        };
    }

    @Bean
    SecurityFilterChain demoAccess(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
    }

    @GetMapping("/")
    String dashboard(@RequestParam(required = false) String route,
                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
                     @RequestParam(required = false) Trip.Status status, Model model) {
        model.addAttribute("routes", repository.routes());
        model.addAttribute("rows", service.search(route == null || route.isBlank() ? null : route, day, status));
        model.addAttribute("route", route);
        model.addAttribute("day", day);
        model.addAttribute("status", status == null ? "" : status.name());
        return "dashboard";
    }

    @GetMapping("/trips/{code}")
    String detail(@PathVariable String code, Model model) {
        var detail = service.detail(code);
        model.addAttribute("trip", detail.trip());
        model.addAttribute("history", detail.history());
        return "trip";
    }

    @PostMapping("/trips/{code}/follow-up")
    String followUp(@PathVariable String code, @RequestParam(defaultValue = "true") boolean flagged,
                    RedirectAttributes redirect) {
        service.setFollowUp(code, flagged);
        redirect.addAttribute("code", code);
        redirect.addFlashAttribute("message", flagged ? "Follow-up recorded." : "Follow-up cleared.");
        return "redirect:/trips/{code}";
    }

    @PostMapping("/api/feed/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    void refresh(@RequestParam(defaultValue = "BASELINE") TripService.FeedScenario scenario) {
        service.refresh(scenario);
    }
}
