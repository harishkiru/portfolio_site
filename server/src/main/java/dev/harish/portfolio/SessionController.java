package dev.harish.portfolio;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
    @GetMapping("/api/session")
    public ResponseEntity<?> session(Authentication authentication, CsrfToken csrf) {
        boolean owner = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "owner", owner, "csrfToken", csrf.getToken(), "csrfHeader", csrf.getHeaderName()));
    }
}
