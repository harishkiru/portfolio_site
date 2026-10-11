package dev.harish.portfolio;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"owner.password=test-owner-password",
        "sharex.upload-token=test-private-sharex-token-0123456789"})
@AutoConfigureMockMvc
class ScreenshotSecurityTest {
    private static final String AUTH = "Bearer test-private-sharex-token-0123456789";
    @Autowired MockMvc mvc;
    @MockitoBean ScreenshotBucket bucket;

    private MockMultipartFile picture() {
        return new MockMultipartFile("file", "private-name.png", "image/png", Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aT1sAAAAASUVORK5CYII="));
    }

    private String upload() throws Exception {
        String body = mvc.perform(multipart("/api/sharex/upload").file(picture()).header("Authorization", AUTH))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn().getResponse().getContentAsString();
        var matcher = Pattern.compile("\"id\":\"([A-Za-z0-9_-]{32})\"").matcher(body);
        assertTrue(matcher.find());
        String id = matcher.group(1);
        assertTrue(body.contains("https://img.harishkiru.dev/" + id));
        assertFalse(body.contains("private-name"));
        return id;
    }

    @Test void uploadsRequirePrivateTokenBeforeAccessingStorage() throws Exception {
        mvc.perform(multipart("/api/sharex/upload").file(picture())).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/sharex/upload").file(picture()).header("Authorization", "Bearer wrong"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(bucket);
        var fake = new MockMultipartFile("file", "fake.png", "image/png", "<script>bad</script>".getBytes());
        mvc.perform(multipart("/api/sharex/upload").file(fake).header("Authorization", AUTH))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bucket);
    }

    @Test void uploadedObjectsHaveRandomIdsAndCannotBeListedOrDownloadedThroughPortfolio() throws Exception {
        String first = upload();
        String second = upload();
        assertNotEquals(first, second);
        verify(bucket).put(eq(first), eq(picture().getBytes()), eq("image/png"));
        for (String path : new String[] {"/api/sharex", "/api/sharex/", "/api/sharex/upload",
                "/api/sharex/" + first}) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/" + first)).andExpect(status().isUnauthorized());
    }

    @Test void deletionRequiresTokenAndUsesTheExactObjectKey() throws Exception {
        String id = upload();
        when(bucket.size(id)).thenReturn(picture().getSize());
        mvc.perform(delete("/api/sharex/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/sharex/" + id).header("Authorization", "Bearer wrong"))
                .andExpect(status().isUnauthorized());
        verify(bucket, never()).delete(anyString());
        mvc.perform(delete("/api/sharex/" + id).header("Authorization", AUTH))
                .andExpect(status().isNoContent());
        verify(bucket).delete(id);
    }

    @Test void capacitySurvivesNewStoreInstancesAndDeletionFreesSpace() throws Exception {
        Map<String, byte[]> data = new HashMap<>();
        ScreenshotBucket memory = new ScreenshotBucket() {
            public long usedBytes() { return data.values().stream().mapToLong(bytes -> bytes.length).sum(); }
            public void put(String id, byte[] bytes, String type) { assertNull(data.putIfAbsent(id, bytes)); }
            public long size(String id) { return data.get(id).length; }
            public void delete(String id) { data.remove(id); }
        };
        var store = new ScreenshotStore(memory, picture().getSize());
        String id = store.add(picture());
        var reloaded = new ScreenshotStore(memory, picture().getSize());
        assertArrayEquals(picture().getBytes(), data.get(id));
        var full = assertThrows(ResponseStatusException.class, () -> reloaded.add(picture()));
        assertEquals(507, full.getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> reloaded.delete("../../owner.password"));
        reloaded.delete(id);
        assertFalse(data.containsKey(id));
        assertNotEquals(id, reloaded.add(picture()));
    }

    @Test void failedUploadsReconcileQuotaAndMissingCredentialsDisableStorage() throws Exception {
        ScreenshotBucket failing = mock(ScreenshotBucket.class);
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
                .when(failing).put(anyString(), any(), anyString());
        var store = new ScreenshotStore(failing, 10000000000L);
        assertThrows(ResponseStatusException.class, () -> store.add(picture()));
        assertThrows(ResponseStatusException.class, () -> store.add(picture()));
        verify(failing, times(2)).usedBytes();
        var disabled = new R2ScreenshotBucket("", "", "", "");
        assertEquals(503, assertThrows(ResponseStatusException.class, disabled::usedBytes).getStatusCode().value());
    }
}
