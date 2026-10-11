package dev.harish.portfolio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GalleryStore {
    public record Photo(String id, String name, String album, String date, String location,
            String caption, long created, String url) {}
    private final Path directory;
    public GalleryStore(@Value("${gallery.directory}") String directory) throws IOException {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }

    public synchronized List<Photo> list() throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().matches("[0-9a-f-]{36}\\.properties"))
                    .map(path -> {
                        try { return photo(read(path)); }
                        catch (IOException error) { throw new IllegalStateException("Gallery metadata unavailable"); }
                    }).sorted(Comparator.comparingLong(Photo::created).reversed()).toList();
        }
    }

    public synchronized Photo add(MultipartFile file, String album, String date, String location, String caption) throws IOException {
        album = field(album, 60, true);
        date = validDate(date);
        location = field(location, 100, false);
        caption = field(caption, 160, false);
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024) throw badRequest("Pictures must be under 20 MB");
        byte[] bytes = file.getBytes();
        String mime = imageType(bytes);
        String id = UUID.randomUUID().toString();
        String name = file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename().replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        if (name.isBlank() || name.length() > 200) name = "photo";
        var settings = new Properties();
        settings.setProperty("id", id); settings.setProperty("name", name);
        settings.setProperty("album", album); settings.setProperty("date", date);
        settings.setProperty("location", location); settings.setProperty("caption", caption);
        settings.setProperty("created", Long.toString(System.currentTimeMillis()));
        settings.setProperty("mime", mime);
        Path image = imagePath(id);
        Files.write(image, bytes);
        try { write(settings); }
        catch (IOException error) { Files.deleteIfExists(image); throw error; }
        return photo(settings);
    }

    public synchronized Photo update(String id, java.util.Map<String, String> changes) throws IOException {
        var settings = read(metadataPath(id));
        if (changes.containsKey("caption")) settings.setProperty("caption", field(changes.get("caption"), 160, false));
        if (changes.containsKey("date")) settings.setProperty("date", validDate(changes.get("date")));
        write(settings);
        return photo(settings);
    }

    public synchronized void delete(String id) throws IOException {
        read(metadataPath(id));
        Files.deleteIfExists(metadataPath(id));
        Files.deleteIfExists(imagePath(id));
    }

    public Path imagePath(String id) { return directory.resolve(validId(id) + ".image"); }
    public synchronized String contentType(String id) throws IOException { return read(metadataPath(id)).getProperty("mime"); }
    private Path metadataPath(String id) { return directory.resolve(validId(id) + ".properties"); }
    private static String validId(String id) {
        try {
            if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException();
            return id;
        } catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
    }
    private Properties read(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) { properties.load(reader); }
        return properties;
    }
    private void write(Properties properties) throws IOException {
        String id = properties.getProperty("id");
        Path temporary = directory.resolve(validId(id) + ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary)) { properties.store(writer, "Gallery photo"); }
            Files.move(temporary, metadataPath(id), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
    }
    private static Photo photo(Properties p) {
        String id = p.getProperty("id");
        return new Photo(id, p.getProperty("name"), p.getProperty("album"), p.getProperty("date"),
                p.getProperty("location", ""), p.getProperty("caption", ""), Long.parseLong(p.getProperty("created")),
                "/api/photos/" + validId(id) + "/image");
    }
    private static String field(String value, int limit, boolean required) {
        String text = value == null ? "" : value.trim();
        if ((required && text.isEmpty()) || text.length() > limit) throw badRequest("Invalid photo details");
        return text;
    }
    private static String validDate(String value) {
        String date = field(value, 10, false);
        if (!date.isEmpty()) {
            if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw badRequest("Invalid photo date");
            try { LocalDate.parse(date); }
            catch (RuntimeException error) { throw badRequest("Invalid photo date"); }
        }
        return date;
    }
    static String imageType(byte[] bytes) {
        if (bytes.length >= 12) {
            if ((bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "image/jpeg";
            if (bytes[0] == (byte) 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71
                    && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return "image/png";
            String header = new String(bytes, 0, 12, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (header.startsWith("GIF87a") || header.startsWith("GIF89a")) return "image/gif";
            if (header.startsWith("RIFF") && header.endsWith("WEBP")) return "image/webp";
        }
        throw badRequest("Use JPG, PNG, GIF, or WebP images");
    }
    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
