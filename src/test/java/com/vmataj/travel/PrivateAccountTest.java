package com.vmataj.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=${PRIVATE_TEST_JDBC_URL:jdbc:h2:mem:private-accounts;DB_CLOSE_DELAY=-1}")
@AutoConfigureMockMvc
@ActiveProfiles("private")
class PrivateAccountTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void accountChangesDoNotAffectAnotherDashboard() throws Exception {
        MockHttpSession alice = signupAndLogin("alice");
        MockHttpSession bob = signupAndLogin("bob");
        var token = token(alice);
        mvc.perform(post("/trips/TD104/follow-up").session(alice)
                .header(token.csrfHeader(), token.csrfToken()).param("flagged", "true"))
            .andExpect(redirectedUrl("/trips/TD104"));
        mvc.perform(post("/api/feed/refresh").session(alice)
                .header(token.csrfHeader(), token.csrfToken()).param("scenario", "RECOVERY"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/trips/TD104").session(alice))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Flagged for follow-up")));
        mvc.perform(get("/trips/TD104").session(bob))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("Flagged for follow-up"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("DELAYED")));
    }

    @Test
    void visitorsCanFilterSampleTripsWhilePrivateRoutesStayProtected() throws Exception {
        mvc.perform(get("/disruptions").param("status", "CANCELLED"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Sample departures")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("TD206")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("TD104"))));
        mvc.perform(get("/trips/TD206"))
            .andExpect(redirectedUrl("http://localhost/login.html"));
    }

    @Test
    void collaboratorsCanEditSharedTripsAndLoseAccessWhenRemoved() throws Exception {
        MockHttpSession owner = signupAndLogin("tripowner");
        MockHttpSession friend = signupAndLogin("tripfriend");
        MockHttpSession outsider = signupAndLogin("tripoutsider");
        var ownerToken = token(owner);
        var friendToken = token(friend);
        var outsiderToken = token(outsider);
        String place = """
            {"placeId":2267057,"name":"Lisbon","country":"Portugal","countryCode":"PT",
             "latitude":38.7167,"longitude":-9.1333,"timezone":"Europe/Lisbon"}
            """;
        String plan = """
            {"title":"Weekend in Lisbon","startDate":"2027-05-14","endDate":"2027-05-17",
             "currency":"EUR","budget":"450.00","destination":%s}
            """.formatted(place);
        String activity = """
            {"day":"2027-05-15","time":"10:00","title":"Visit the castle","location":"Alfama"}
            """;
        String expense = """
            {"title":"Train tickets","category":"TRANSPORT","amount":"18.50"}
            """;
        mvc.perform(get("/")).andExpect(forwardedUrl("/planner.html"));
        mvc.perform(get("/api/planner")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/planner/favorites").session(owner).header(ownerToken.csrfHeader(), ownerToken.csrfToken())
                .contentType("application/json").content(place))
            .andExpect(status().isNoContent());
        byte[] created = mvc.perform(post("/api/planner/trips").session(owner)
                .header(ownerToken.csrfHeader(), ownerToken.csrfToken()).contentType("application/json").content(plan))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsByteArray();
        String tripUrl = "/api/planner/trips/" + json.readTree(created).path("id").asText();
        mvc.perform(get("/api/planner").session(outsider))
            .andExpect(jsonPath("$.trips").isEmpty()).andExpect(jsonPath("$.favorites").isEmpty());
        mvc.perform(post(tripUrl + "/activities").session(outsider).header(outsiderToken.csrfHeader(), outsiderToken.csrfToken())
                .contentType("application/json").content(activity))
            .andExpect(status().isNotFound());
        mvc.perform(put(tripUrl + "/collaborators").session(owner).header(ownerToken.csrfHeader(), ownerToken.csrfToken())
                .contentType("application/json").content("{\"username\":\"tripfriend\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.collaborators[0]").value("tripfriend"));
        mvc.perform(post(tripUrl + "/activities").session(friend).header(friendToken.csrfHeader(), friendToken.csrfToken())
                .contentType("application/json").content(activity.replace("2027-05-15", "2027-05-18")))
            .andExpect(status().isBadRequest());
        mvc.perform(post(tripUrl + "/activities").session(friend).header(friendToken.csrfHeader(), friendToken.csrfToken())
                .contentType("application/json").content(activity))
            .andExpect(status().isOk()).andExpect(jsonPath("$.activities[0].activity.title").value("Visit the castle"));
        mvc.perform(post(tripUrl + "/expenses").session(friend).header(friendToken.csrfHeader(), friendToken.csrfToken())
                .contentType("application/json").content(expense))
            .andExpect(status().isOk()).andExpect(jsonPath("$.expenses[0].expense.amount").value(18.5));
        mvc.perform(delete(tripUrl).session(friend).header(friendToken.csrfHeader(), friendToken.csrfToken()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/planner").session(owner))
            .andExpect(jsonPath("$.favorites[0].name").value("Lisbon"))
            .andExpect(jsonPath("$.trips[0].activities[0].activity.title").value("Visit the castle"))
            .andExpect(jsonPath("$.trips[0].expenses[0].expense.amount").value(18.5));
        mvc.perform(delete(tripUrl + "/collaborators/tripfriend").session(owner)
                .header(ownerToken.csrfHeader(), ownerToken.csrfToken()))
            .andExpect(status().isOk());
        mvc.perform(get("/api/planner").session(friend)).andExpect(jsonPath("$.trips").isEmpty());
    }

    private MockHttpSession signupAndLogin(String username) throws Exception {
        MockHttpSession session = new MockHttpSession();
        var token = token(session);
        mvc.perform(post("/account/signup").session(session)
                .header(token.csrfHeader(), token.csrfToken()).contentType("application/json")
                .content(json.writeValueAsString(new AccountController.Signup(username, "a-local-test-password"))))
            .andExpect(status().isCreated());
        return (MockHttpSession) mvc.perform(post("/login").session(session)
                .param("_csrf", token.csrfToken()).param("username", username).param("password", "a-local-test-password"))
            .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }

    private AccountController.Session token(MockHttpSession session) throws Exception {
        byte[] body = mvc.perform(get("/account").session(session))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        return json.readValue(body, AccountController.Session.class);
    }
}
