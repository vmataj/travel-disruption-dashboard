package com.vmataj.travel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripFeedFailureTest {
    @Mock TripRepository trips;
    @Mock SimulatedFeed feed;

    @Test
    void anUpstreamFailureLeavesTheLastSnapshotUntouched() {
        when(feed.fetch()).thenThrow(new IllegalStateException("Feed disconnected"));
        var service = new TripService(trips, feed);

        var error = assertThrows(ResponseStatusException.class,
            () -> service.refresh("alice", TripService.FeedScenario.BASELINE));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verifyNoInteractions(trips);
    }
}
