package dev.harish.portfolio;

import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
public class ScreenshotSecurity {
    @Bean
    @Order(1)
    SecurityFilterChain screenshotFilterChain(HttpSecurity http,
            @Value("${sharex.upload-token}") String token) throws Exception {
        if (!token.isBlank() && token.length() < 32) {
            throw new IllegalStateException("ShareX upload token must be at least 32 characters");
        }
        http.securityMatcher("/api/sharex/**")
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/sharex/upload").access((authentication, context) ->
                    new AuthorizationDecision(validToken(context.getRequest().getHeader("Authorization"), token)))
                .requestMatchers(new RegexRequestMatcher("/api/sharex/[A-Za-z0-9_-]{32}", "DELETE"))
                    .access((authentication, context) -> new AuthorizationDecision(
                        validToken(context.getRequest().getHeader("Authorization"), token)))
                .anyRequest().denyAll())
            .headers(headers -> headers
                .contentSecurityPolicy(policy -> policy.policyDirectives("default-src 'none'; frame-ancestors 'none'; sandbox"))
                .referrerPolicy(policy -> policy.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> response.setStatus(
                    request.getMethod().equals("GET") || request.getMethod().equals("HEAD") ? 404 : 401))
                .accessDeniedHandler((request, response, exception) -> response.setStatus(
                    request.getMethod().equals("GET") || request.getMethod().equals("HEAD") ? 404 : 401)));
        return http.build();
    }

    private static boolean validToken(String authorization, String token) {
        if (token.isBlank() || authorization == null || authorization.length() > 1024) return false;
        return MessageDigest.isEqual(("Bearer " + token).getBytes(StandardCharsets.UTF_8),
                authorization.getBytes(StandardCharsets.UTF_8));
    }
}
