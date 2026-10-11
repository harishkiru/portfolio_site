package dev.harish.portfolio;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/photos")
public class GalleryController {
    private final GalleryStore gallery;
    public GalleryController(GalleryStore gallery) { this.gallery = gallery; }
    @GetMapping
    public ResponseEntity<List<GalleryStore.Photo>> list() throws IOException {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(gallery.list());
    }
    @GetMapping("/{id}/image")
    public ResponseEntity<FileSystemResource> image(@PathVariable String id) throws IOException {
        String type = gallery.contentType(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .body(new FileSystemResource(gallery.imagePath(id)));
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GalleryStore.Photo> add(@RequestParam MultipartFile file, @RequestParam String album,
            @RequestParam(defaultValue = "") String date, @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String caption) throws IOException {
        return ResponseEntity.status(201).body(gallery.add(file, album, date, location, caption));
    }
    @PatchMapping("/{id}")
    public GalleryStore.Photo update(@PathVariable String id, @RequestBody Map<String, String> input) throws IOException {
        return gallery.update(id, input);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws IOException {
        gallery.delete(id);
        return ResponseEntity.noContent().build();
    }
}
