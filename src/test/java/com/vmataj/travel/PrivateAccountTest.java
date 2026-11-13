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
        mvc.perform(get("/").param("status", "CANCELLED"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Sample departures")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("TD206")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("TD104"))));
        mvc.perform(get("/trips/TD206"))
            .andExpect(redirectedUrl("http://localhost/login.html"));
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
