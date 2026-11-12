package com.vmataj.travel;

import java.time.LocalDate;
import java.security.Principal;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final boolean privateMode;

    DashboardController(TripService service, TripRepository repository, Environment environment) {
        this.service = service;
        this.repository = repository;
        this.privateMode = environment.acceptsProfiles(Profiles.of("private"));
    }

    @Bean
    @Profile("!private")
    ApplicationRunner seedTrips() {
        return args -> {
            if (!repository.existsById("TD104")) service.refresh(null, TripService.FeedScenario.BASELINE);
        };
    }

    @Bean
    @Profile("!private")
    SecurityFilterChain demoAccess(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
    }

    @GetMapping("/")
    String dashboard(@RequestParam(required = false) String route,
                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
                     @RequestParam(required = false) Trip.Status status, Model model, Principal actor) {
        if (privateMode && actor == null) return "redirect:/signup.html";
        String owner = owner(actor);
        model.addAttribute("account", owner);
        model.addAttribute("routes", repository.routes(owner));
        model.addAttribute("rows", service.search(owner, route == null || route.isBlank() ? null : route, day, status));
        model.addAttribute("route", route);
        model.addAttribute("day", day);
        model.addAttribute("status", status == null ? "" : status.name());
        return "dashboard";
    }

    @GetMapping("/trips/{code}")
    String detail(@PathVariable String code, Model model, Principal actor) {
        var detail = service.detail(owner(actor), code);
        model.addAttribute("trip", detail.trip());
        model.addAttribute("history", detail.history());
        return "trip";
    }

    @PostMapping("/trips/{code}/follow-up")
    String followUp(@PathVariable String code, @RequestParam(defaultValue = "true") boolean flagged,
                    RedirectAttributes redirect, Principal actor) {
        service.setFollowUp(owner(actor), code, flagged);
        redirect.addAttribute("code", code);
        redirect.addFlashAttribute("message", flagged ? "Follow-up recorded." : "Follow-up cleared.");
        return "redirect:/trips/{code}";
    }

    @PostMapping("/api/feed/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    void refresh(@RequestParam(defaultValue = "BASELINE") TripService.FeedScenario scenario, Principal actor) {
        service.refresh(owner(actor), scenario);
    }

    private String owner(Principal actor) {
        return privateMode ? actor.getName() : null;
    }
}
