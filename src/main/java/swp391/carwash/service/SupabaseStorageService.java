package swp391.carwash.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import swp391.carwash.common.exception.ApiException;

@Slf4j
@Service
public class SupabaseStorageService {
    private final HttpClient httpClient;

    @Value("${washmate.supabase.url:}")
    private String supabaseUrl;

    @Value("${washmate.supabase.service-role-key:}")
    private String serviceRoleKey;

    @Value("${washmate.supabase.storage.avatar-bucket:avatars}")
    private String bucket;

    public SupabaseStorageService() {
        this(HttpClient.newHttpClient());
    }

    SupabaseStorageService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public UploadedAvatar uploadAvatar(Integer userId, MultipartFile file, String extension) {
        ensureConfigured();
        String path = "user-%d/%s.%s".formatted(userId, UUID.randomUUID(), extension);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(storageObjectUri(path))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", file.getContentType())
                    .header("x-upsert", "false")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("Supabase avatar upload failed, status={}, body={}", response.statusCode(), response.body());
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to upload avatar");
            }
            return new UploadedAvatar(buildPublicUrl(path), path);
        } catch (IOException e) {
            log.warn("Cannot upload avatar to Supabase Storage", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to upload avatar");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to upload avatar");
        }
    }

    public void deleteByPath(String path) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        if (!isConfigured()) {
            log.warn("Skipping Supabase avatar delete because storage is not configured, path={}", path);
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(storageObjectUri(path))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .DELETE()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300 && response.statusCode() != 404) {
                log.warn("Supabase avatar delete failed, path={}, status={}, body={}", path, response.statusCode(), response.body());
            }
        } catch (IOException e) {
            log.warn("Cannot delete Supabase avatar, path={}", path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while deleting Supabase avatar, path={}", path);
        }
    }

    public void deleteByPublicUrl(String publicUrl) {
        String path = pathFromPublicUrl(publicUrl);
        if (path == null) {
            return;
        }
        deleteByPath(path);
    }

    private String pathFromPublicUrl(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
            return null;
        }
        if (!isConfigured()) {
            log.warn("Skipping avatar delete because Supabase Storage is not configured");
            return null;
        }
        URI baseUri = URI.create(normalizedSupabaseUrl());
        URI avatarUri;
        try {
            avatarUri = URI.create(publicUrl);
        } catch (IllegalArgumentException e) {
            log.info("Skipping avatar delete because URL is invalid: {}", publicUrl);
            return null;
        }
        if (!baseUri.getHost().equalsIgnoreCase(avatarUri.getHost())) {
            log.info("Skipping avatar delete because URL is outside the configured Supabase project");
            return null;
        }
        String marker = "/storage/v1/object/public/" + bucket + "/";
        String path = avatarUri.getPath();
        if (path == null || !path.startsWith(marker)) {
            log.info("Skipping avatar delete because URL is not in the configured avatar bucket");
            return null;
        }
        return path.substring(marker.length());
    }

    private URI storageObjectUri(String path) {
        return URI.create(normalizedSupabaseUrl() + "/storage/v1/object/" + bucket + "/" + path);
    }

    private String buildPublicUrl(String path) {
        return normalizedSupabaseUrl() + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    private String normalizedSupabaseUrl() {
        return supabaseUrl.replaceAll("/+$", "");
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Supabase Storage is not configured");
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(supabaseUrl) && StringUtils.hasText(serviceRoleKey);
    }

    public record UploadedAvatar(String publicUrl, String path) {
    }
}
