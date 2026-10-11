package dev.harish.portfolio;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

public class LastFmService {
    public record Track(String name, String artist, String album, String url,
            String coverUrl, Long playedAt, boolean nowPlaying) {}
    public record RecentListening(String username, Track track) {}

    private final RestClient client;
    private final String username;
    private final String apiKey;
    private final LongSupplier clock;
    private RecentListening cached;
    private long expiresAt;
    private long retryAt;

    public LastFmService(RestClient client, String username, String apiKey) {
        this(client, username, apiKey, System::currentTimeMillis);
    }

    LastFmService(RestClient client, String username, String apiKey, LongSupplier clock) {
        this.client = client;
        this.username = username;
        this.apiKey = apiKey;
        this.clock = clock;
    }

    public synchronized RecentListening recentListening() {
        long now = clock.getAsLong();
        if (cached != null && now < expiresAt) return cached;
        if (username.isBlank() || apiKey.isBlank() || now < retryAt) {
            throw new IllegalStateException("Listening data unavailable");
        }
        try {
            var response = client.get().uri(builder -> builder.path("/2.0/")
                    .queryParam("method", "user.getrecenttracks").queryParam("user", username)
                    .queryParam("api_key", apiKey).queryParam("format", "json")
                    .queryParam("limit", "1").build()).retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response == null || response.containsKey("error") || !(response.get("recenttracks") instanceof Map)) {
                throw new IllegalStateException("Invalid listening response");
            }
            Object tracks = object(response.get("recenttracks")).get("track");
            Object first = tracks instanceof List<?> list ? (list.isEmpty() ? null : list.getFirst()) : tracks;
            Track track = first == null ? null : parseTrack(object(first));
            cached = new RecentListening(username, track);
            expiresAt = clock.getAsLong() + 60000;
            retryAt = 0;
            return cached;
        } catch (RuntimeException failure) {
            retryAt = clock.getAsLong() + 60000;
            throw new IllegalStateException("Listening data unavailable");
        }
    }

    static Track parseTrack(Map<?, ?> track) {
        String name = text(track.get("name"));
        if (name.isBlank()) throw new IllegalStateException("Missing track name");
        String cover = "";
        if (track.get("image") instanceof List<?> images) {
            for (Object image : images) {
                String candidate = https(text(object(image).get("#text")));
                if (!candidate.isEmpty()) cover = candidate;
            }
        }
        Long playedAt = null;
        try {
            long seconds = Long.parseLong(text(object(track.get("date")).get("uts")));
            if (seconds > 0) playedAt = Math.multiplyExact(seconds, 1000);
        } catch (NumberFormatException | ArithmeticException ignored) {}
        return new Track(name, text(object(track.get("artist")).get("#text")),
                text(object(track.get("album")).get("#text")), https(text(track.get("url"))),
                cover, playedAt, "true".equals(text(object(track.get("@attr")).get("nowplaying"))));
    }

    private static Map<?, ?> object(Object value) { return value instanceof Map<?, ?> map ? map : Map.of(); }
    private static String text(Object value) { return value instanceof String string ? string : ""; }
    private static String https(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null ? value : "";
        } catch (IllegalArgumentException ignored) { return ""; }
    }
}
