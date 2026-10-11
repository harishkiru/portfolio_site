package dev.harish.portfolio;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class PortfolioApplication {
    public static void main(String[] args) {
        SpringApplication.run(PortfolioApplication.class, args);
    }

    @Bean
    LastFmService lastFmService(@Value("${lastfm.username}") String username,
            @Value("${lastfm.api-key}") String apiKey) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));
        var client = RestClient.builder().baseUrl("https://ws.audioscrobbler.com")
                .requestFactory(factory).build();
        return new LastFmService(client, username, apiKey);
    }
}
