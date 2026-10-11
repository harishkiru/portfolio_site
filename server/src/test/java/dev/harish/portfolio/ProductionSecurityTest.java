package dev.harish.portfolio;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ProductionSecurityTest {
    @TempDir Path directory;

    @Test void productionRejectsMissingAndWeakPasswordsWithoutCreatingCredentialFiles() {
        var security = new OwnerSecurity();
        String file = directory.resolve("credentials.properties").toString();
        assertThrows(IllegalStateException.class, () -> security.ownerAccount("harish", "", file, true));
        assertThrows(IllegalStateException.class, () -> security.ownerAccount("harish", "short-password", file, true));
        assertFalse(Files.exists(Path.of(file)));
        assertFalse(Files.exists(directory.resolve("owner-access.local.txt")));
    }

    @Test void productionAcceptsConfiguredPasswordAndRejectsBcryptTruncation() throws Exception {
        var security = new OwnerSecurity();
        String file = directory.resolve("credentials.properties").toString();
        var users = security.ownerAccount("harish", "a-long-unique-owner-password", file, true);
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(
                "a-long-unique-owner-password", users.loadUserByUsername("harish").getPassword().substring(8)));
        assertThrows(IllegalStateException.class, () -> security.ownerAccount("harish", "é".repeat(37), file, true));
        assertFalse(Files.exists(Path.of(file)));
    }
}
