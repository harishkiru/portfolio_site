package dev.harish.portfolio;

public interface ScreenshotBucket {
    long usedBytes();
    void put(String id, byte[] bytes, String contentType);
    long size(String id);
    void delete(String id);
}
