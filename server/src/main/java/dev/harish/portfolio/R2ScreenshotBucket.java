package dev.harish.portfolio;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class R2ScreenshotBucket implements ScreenshotBucket {
    private final S3Client client;
    private final String bucket;

    public R2ScreenshotBucket(@Value("${r2.account-id}") String account,
            @Value("${r2.bucket}") String bucket,
            @Value("${r2.access-key-id}") String accessKey,
            @Value("${r2.secret-access-key}") String secretKey) {
        this.bucket = bucket;
        if (account.isBlank() || bucket.isBlank() || accessKey.isBlank() || secretKey.isBlank()) {
            client = null;
            return;
        }
        if (!account.matches("[a-f0-9]{32}") || !bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
            throw new IllegalArgumentException("Invalid R2 account or bucket");
        }
        client = S3Client.builder()
                .endpointOverride(URI.create("https://" + account + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true)
                    .chunkedEncodingEnabled(false).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(5))
                    .socketTimeout(Duration.ofSeconds(20)))
                .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(40)))
                .build();
    }

    @Override public long usedBytes() {
        requireConfigured();
        try {
            long total = 0;
            for (var page : client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucket).build())) {
                for (var object : page.contents()) total += object.size();
            }
            return total;
        } catch (RuntimeException error) { throw unavailable(); }
    }

    @Override public void put(String id, byte[] bytes, String contentType) {
        requireConfigured();
        try {
            client.putObject(request -> request.bucket(bucket).key(id).contentType(contentType)
                    .cacheControl("no-store").contentDisposition("inline").ifNoneMatch("*"),
                    RequestBody.fromBytes(bytes));
        } catch (RuntimeException error) { throw unavailable(); }
    }

    @Override public long size(String id) {
        requireConfigured();
        try { return client.headObject(request -> request.bucket(bucket).key(id)).contentLength(); }
        catch (S3Exception error) {
            if (error.statusCode() == 404) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            throw unavailable();
        } catch (RuntimeException error) { throw unavailable(); }
    }

    @Override public void delete(String id) {
        requireConfigured();
        try { client.deleteObject(request -> request.bucket(bucket).key(id)); }
        catch (RuntimeException error) { throw unavailable(); }
    }

    private void requireConfigured() { if (client == null) throw unavailable(); }
    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Image storage unavailable");
    }
    @PreDestroy public void close() { if (client != null) client.close(); }
}
