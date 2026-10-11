package dev.harish.portfolio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"owner.username=harish", "owner.password=test-owner-password"})
@AutoConfigureMockMvc
class GallerySecurityTest {
    private static final Path DIRECTORY;
    static {
        try { DIRECTORY = Files.createTempDirectory("portfolio-gallery-test-"); }
        catch (Exception error) { throw new ExceptionInInitializerError(error); }
    }
    @DynamicPropertySource static void directory(DynamicPropertyRegistry settings) {
        settings.add("gallery.directory", DIRECTORY::toString);
    }
    @Autowired MockMvc mvc;

    private MockMultipartFile picture() {
        return new MockMultipartFile("file", "moment.png", "image/png", Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aT1sAAAAASUVORK5CYII="));
    }

    @Test void visitorsCanReadButCannotWrite() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Security-Policy"));
        mvc.perform(get("/api/photos")).andExpect(status().isOk());
        mvc.perform(multipart("/api/photos").file(picture()).param("album", "Visitors").with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/photos/00000000-0000-0000-0000-000000000000").with(csrf())
                .contentType("application/json").content("{\"caption\":\"No\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/photos/00000000-0000-0000-0000-000000000000").with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/session")).andExpect(status().isOk()).andExpect(jsonPath("$.owner").value(false));
    }

    @Test void cleanPageUrlsSupportDirectLinksAndRefresh() throws Exception {
        for (String path : new String[] {"/home", "/photos", "/contact", "/about", "/owner"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        }
        mvc.perform(get("/private-files")).andExpect(status().isUnauthorized());
    }

    @Test void ownerCanPublishEditAndDeleteAndVisitorsSeePublishedPhotos() throws Exception {
        String body = mvc.perform(multipart("/api/photos").file(picture()).param("album", "A day out")
                .param("date", "2026-10-11").with(user("harish").roles("OWNER")).with(csrf()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.album").value("A day out"))
                .andReturn().getResponse().getContentAsString();
        var matcher = Pattern.compile("\"id\"\\s*:\\s*\"([a-f0-9-]+)\"").matcher(body);
        assertTrue(matcher.find());
        String id = matcher.group(1);
        mvc.perform(get("/api/photos/" + id + "/image")).andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mvc.perform(patch("/api/photos/" + id).with(user("harish").roles("OWNER")).with(csrf())
                .contentType("application/json").content("{\"caption\":\"A good day\",\"date\":\"2025-08-15\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.caption").value("A good day"))
                .andExpect(jsonPath("$.date").value("2025-08-15"));
        mvc.perform(patch("/api/photos/" + id).with(user("harish").roles("OWNER")).with(csrf())
                .contentType("application/json").content("{\"date\":\"2025-02-30\",\"caption\":\"Should not save\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/photos/" + id).with(user("harish").roles("OWNER")).with(csrf())
                .contentType("application/json").content("{\"date\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.date").value(""))
                .andExpect(jsonPath("$.caption").value("A good day"));
        mvc.perform(delete("/api/photos/" + id).with(user("harish").roles("OWNER")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/photos/" + id + "/image")).andExpect(status().isNotFound());
    }

    @Test void csrfAndImageValidationCannotBeBypassed() throws Exception {
        mvc.perform(multipart("/api/photos").file(picture()).param("album", "No CSRF")
                .with(user("harish").roles("OWNER"))).andExpect(status().isForbidden());
        var fakeImage = new MockMultipartFile("file", "fake.png", "image/png", "<script>bad</script>".getBytes());
        mvc.perform(multipart("/api/photos").file(fakeImage).param("album", "Fake")
                .with(user("harish").roles("OWNER")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/photos").file(picture()).param("album", "Wrong role")
                .with(user("visitor").roles("USER")).with(csrf())).andExpect(status().isForbidden());
    }

    @Test void loginUsesOwnerPasswordAndLogoutEndsSession() throws Exception {
        mvc.perform(post("/api/login").with(csrf()).param("username", "harish").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/login").with(csrf())
                .param("username", "harish").param("password", "test-owner-password"))
                .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/session").session(session)).andExpect(jsonPath("$.owner").value(true));
        mvc.perform(post("/api/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
    }

    @Test void gallerySurvivesReloadAndRejectsTraversal() throws Exception {
        var store = new GalleryStore(DIRECTORY.toString());
        var saved = store.add(picture(), "Persistent moments", "2026-10-11", "Toronto", "Still here");
        try {
            var reloaded = new GalleryStore(DIRECTORY.toString());
            assertTrue(reloaded.list().stream().anyMatch(photo -> photo.id().equals(saved.id())));
            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> reloaded.imagePath("../../owner-access.local.txt"));
        } finally { store.delete(saved.id()); }
    }
}
