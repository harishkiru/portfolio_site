package dev.harish.portfolio;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ListeningController {
    private final LastFmService lastFm;
    public ListeningController(LastFmService lastFm) { this.lastFm = lastFm; }

    @GetMapping("/api/lastfm/recent")
    public ResponseEntity<?> recent() {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(lastFm.recentListening());
        } catch (IllegalStateException unavailable) {
            return ResponseEntity.status(503).cacheControl(CacheControl.noStore())
                    .body(Map.of("message", "Last.fm temporarily unavailable"));
        }
    }
}
