package dev.harish.portfolio;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScreenshotStore {
    private final ScreenshotBucket bucket;
    private final long capacity;
    private final SecureRandom random = new SecureRandom();
    private Long used;

    public ScreenshotStore(ScreenshotBucket bucket, @Value("${sharex.capacity-bytes}") long capacity) {
        this.bucket = bucket;
        this.capacity = capacity;
        if (capacity < 1) throw new IllegalArgumentException("Screenshot storage capacity must be positive");
    }

    public synchronized String add(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Images must be under 20 MB");
        }
        byte[] bytes = file.getBytes();
        String contentType = GalleryStore.imageType(bytes);
        if (used == null) used = bucket.usedBytes();
        if (bytes.length > capacity - used) {
            throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE, "Screenshot storage is full");
        }
        byte[] entropy = new byte[24];
        random.nextBytes(entropy);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        try {
            bucket.put(id, bytes, contentType);
            used += bytes.length;
            return id;
        } catch (RuntimeException error) {
            used = null;
            throw error;
        }
    }

    public synchronized void delete(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{32}")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        long size = bucket.size(id);
        try {
            bucket.delete(id);
            if (used != null) used = Math.max(0, used - size);
        } catch (RuntimeException error) {
            used = null;
            throw error;
        }
    }
}
