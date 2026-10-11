package dev.harish.portfolio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class OwnerSecurity {
    @Bean
    UserDetailsService ownerAccount(@Value("${owner.username}") String username,
            @Value("${owner.password}") String configuredPassword,
            @Value("${owner.credentials-file}") String credentialsFile,
            @Value("${owner.require-password:false}") boolean requirePassword) throws Exception {
        if (username.isBlank()) throw new IllegalStateException("Owner username is required");
        if (requirePassword && configuredPassword.length() < 16) {
            throw new IllegalStateException("Production requires an owner password of at least 16 characters");
        }
        if (configuredPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Owner password must be at most 72 UTF-8 bytes");
        }
        var encoder = new BCryptPasswordEncoder();
        String hash;
        if (!configuredPassword.isBlank()) {
            hash = encoder.encode(configuredPassword);
        } else {
            Path path = Path.of(credentialsFile).toAbsolutePath().normalize();
            var settings = new Properties();
            if (Files.exists(path)) {
                try (var reader = Files.newBufferedReader(path)) { settings.load(reader); }
                hash = settings.getProperty("passwordHash");
                if (hash == null || !hash.startsWith("$2")) throw new IllegalStateException("Invalid owner credentials file");
            } else {
                Files.createDirectories(path.getParent());
                byte[] random = new byte[24];
                new SecureRandom().nextBytes(random);
                String password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
                hash = encoder.encode(password);
                settings.setProperty("passwordHash", hash);
                try (var writer = Files.newBufferedWriter(path)) { settings.store(writer, "Owner password hash; do not publish this file"); }
                Files.writeString(path.resolveSibling("owner-access.local.txt"),
                        "Portfolio owner login\nUsername: " + username + "\nPassword: " + password
                        + "\n\nKeep private. Set OWNER_PASSWORD to replace this password.\n");
            }
        }
        return new InMemoryUserDetailsManager(User.withUsername(username).password("{bcrypt}" + hash).roles("OWNER").build());
    }

    @Bean
    SecurityFilterChain gallerySecurity(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/", "/home", "/photos", "/contact", "/about", "/owner", "/index.html", "/style.css", "/app.js", "/orbit-avatar.svg", "/orbit-avatar-purple.svg",
                        "/favicon.ico", "/THIRD-PARTY-NOTICES.txt", "/api/health", "/api/session", "/api/lastfm/recent",
                        "/api/photos", "/api/photos/*/image").permitAll()
                .requestMatchers("/api/login", "/error").permitAll()
                .requestMatchers("/api/photos/**", "/api/logout").hasRole("OWNER")
                .anyRequest().denyAll())
            .headers(headers -> headers.contentSecurityPolicy(policy -> policy.policyDirectives(
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                    + "font-src 'self' https://fonts.gstatic.com; img-src 'self' https: data: blob:; "
                    + "connect-src 'self'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'"))
                .referrerPolicy(policy -> policy.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .formLogin(login -> login.loginProcessingUrl("/api/login")
                .successHandler((request, response, authentication) -> response.setStatus(204))
                .failureHandler((request, response, exception) -> response.setStatus(401)).permitAll())
            .logout(logout -> logout.logoutUrl("/api/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                .accessDeniedHandler((request, response, exception) -> response.setStatus(403)));
        return http.build();
    }
}
