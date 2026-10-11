package dev.harish.portfolio;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LastFmServiceTest {
    private static final String SONG = """
        {"recenttracks":{"track":[{"name":"Test Song","artist":{"#text":"Test Artist"},
        "album":{"#text":"Test Album"},"url":"https://www.last.fm/music/test",
        "image":[{"#text":""},{"#text":"https://example.com/cover.jpg"}],
        "@attr":{"nowplaying":"true"}}]}}
        """;

    @Test void cachesForOneMinuteThenRefreshes() {
        var builder = RestClient.builder().baseUrl("https://ws.audioscrobbler.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        var clock = new AtomicLong(1000);
        var service = new LastFmService(builder.build(), "McGodFTW", "test-key", clock::get);
        server.expect(queryParam("method", "user.getrecenttracks")).andRespond(withSuccess(SONG, MediaType.APPLICATION_JSON));
        var first = service.recentListening();
        assertTrue(first.track().nowPlaying());
        assertEquals("Test Artist", first.track().artist());
        assertEquals("https://example.com/cover.jpg", first.track().coverUrl());
        clock.set(60999);
        assertSame(first, service.recentListening());
        server.verify();
        server.reset();
        server.expect(queryParam("user", "McGodFTW")).andRespond(withSuccess("{\"recenttracks\":{\"track\":[]}}", MediaType.APPLICATION_JSON));
        clock.set(61000);
        assertNull(service.recentListening().track());
        server.verify();
    }

    @Test void handlesPastTrackAndMissingArtwork() {
        var builder = RestClient.builder().baseUrl("https://ws.audioscrobbler.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(queryParam("limit", "1")).andRespond(withSuccess("""
            {"recenttracks":{"track":{"name":"Past Song","artist":{"#text":"Artist"},"date":{"uts":"1700000000"}}}}
            """, MediaType.APPLICATION_JSON));
        var track = new LastFmService(builder.build(), "user", "test-key").recentListening().track();
        assertFalse(track.nowPlaying());
        assertEquals(1700000000000L, track.playedAt());
        assertEquals("", track.coverUrl());
        server.verify();
    }

    @Test void apiErrorsAreSanitizedAndThrottled() {
        var builder = RestClient.builder().baseUrl("https://ws.audioscrobbler.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(queryParam("api_key", "test-key")).andRespond(withSuccess("{\"error\":10,\"message\":\"Invalid API key\"}", MediaType.APPLICATION_JSON));
        var service = new LastFmService(builder.build(), "user", "test-key");
        var error = assertThrows(IllegalStateException.class, service::recentListening);
        assertEquals("Listening data unavailable", error.getMessage());
        assertThrows(IllegalStateException.class, service::recentListening);
        assertEquals(503, new ListeningController(service).recent().getStatusCode().value());
        server.verify();
    }
}
