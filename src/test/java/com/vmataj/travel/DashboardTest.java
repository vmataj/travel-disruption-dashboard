package com.vmataj.travel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardTest {
    @Autowired MockMvc mvc;
    @Autowired TripService service;
    @MockitoSpyBean SimulatedFeed feed;

    @Test
    void dashboardFiltersAndPersistsFollowUp() throws Exception {
        MvcResult page = mvc.perform(get("/").param("status", "DELAYED"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("TD104")))
            .andExpect(content().string(not(containsString("TD206"))))
            .andReturn();
        CsrfToken token = csrf(page);
        mvc.perform(post("/trips/TD104/follow-up").session(session(page))
                .param(token.getParameterName(), token.getToken()))
            .andExpect(redirectedUrl("/trips/TD104"));
        mvc.perform(get("/trips/TD104"))
            .andExpect(content().string(containsString("Flagged for follow-up")));
        mvc.perform(post("/trips/TD104/follow-up").session(session(page))
                .param(token.getParameterName(), token.getToken()).param("flagged", "false"))
            .andExpect(redirectedUrl("/trips/TD104"));
        mvc.perform(get("/trips/TD104"))
            .andExpect(content().string(not(containsString("Flagged for follow-up"))));
    }

    @Test
    void failedFeedPreservesLastSnapshot() throws Exception {
        var before = service.search(null, null, null, null);
        doThrow(new IllegalStateException("Simulated timeout")).when(feed).fetch();
        MvcResult page = mvc.perform(get("/")).andExpect(status().isOk()).andReturn();
        CsrfToken token = csrf(page);
        mvc.perform(post("/api/feed/refresh").session(session(page))
                .header(token.getHeaderName(), token.getToken()))
            .andExpect(status().isServiceUnavailable());
        assertThat(service.search(null, null, null, null)).isEqualTo(before);
    }

    private CsrfToken csrf(MvcResult page) {
        return (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
    }

    @Test
    void recoveryAddsHistoryAndOutageLeavesItInPlace() throws Exception {
        MvcResult page = mvc.perform(get("/")).andExpect(status().isOk()).andReturn();
        CsrfToken token = csrf(page);
        mvc.perform(post("/api/feed/refresh").session(session(page))
                .header(token.getHeaderName(), token.getToken()).param("scenario", "RECOVERY"))
            .andExpect(status().isNoContent());
        var recovered = service.detail(null, "TD104");
        assertThat(recovered.trip().status()).isEqualTo("ON_TIME");
        assertThat(recovered.history().getLast().status()).isEqualTo("ON TIME");
        mvc.perform(post("/api/feed/refresh").session(session(page))
                .header(token.getHeaderName(), token.getToken()).param("scenario", "UNAVAILABLE"))
            .andExpect(status().isServiceUnavailable());
        assertThat(service.detail(null, "TD104")).isEqualTo(recovered);
        service.refresh(null, TripService.FeedScenario.BASELINE);
    }

    private MockHttpSession session(MvcResult page) {
        return (MockHttpSession) page.getRequest().getSession(false);
    }
}
