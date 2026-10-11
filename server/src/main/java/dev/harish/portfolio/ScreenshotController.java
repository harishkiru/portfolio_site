package dev.harish.portfolio;

import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sharex")
public class ScreenshotController {
    private final ScreenshotStore screenshots;
    private final String baseUrl;

    public ScreenshotController(ScreenshotStore screenshots, @Value("${sharex.host}") String host) {
        if (!host.matches("[A-Za-z0-9.-]+")) throw new IllegalArgumentException("Invalid screenshot hostname");
        this.screenshots = screenshots;
        this.baseUrl = "https://" + host;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam MultipartFile file) throws IOException {
        String id = screenshots.add(file);
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(Map.of("id", id, "url", baseUrl + "/" + id));
    }

    @DeleteMapping("/{id:[A-Za-z0-9_-]{32}}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws IOException {
        screenshots.delete(id);
        return ResponseEntity.noContent().build();
    }
}
