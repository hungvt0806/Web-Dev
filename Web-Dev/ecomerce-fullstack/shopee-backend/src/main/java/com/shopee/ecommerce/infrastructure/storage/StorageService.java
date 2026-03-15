package com.shopee.ecommerce.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client    s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.cdn-url:}")
    private String cdnUrl;

    @Value("${aws.s3.region:ap-northeast-1}")
    private String region;

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024L; // 10 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    // ── Upload ─────────────────────────────────────────────────

    /**
     * Upload a single file to S3 under the given folder prefix.
     *
     * @param file   multipart file to upload
     * @param folder e.g. "products", "avatars", "categories"
     * @return public URL of the uploaded object
     */
    public String upload(MultipartFile file, String folder) {
        validateFile(file);

        String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
        String key = folder + "/" + UUID.randomUUID() + "." + ext;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .cacheControl("public, max-age=31536000, immutable")
                    .metadata(Map.of(
                            "original-name", sanitizeHeaderValue(file.getOriginalFilename()),
                            "upload-timestamp", String.valueOf(System.currentTimeMillis())
                    ))
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            String url = resolvePublicUrl(key);
            log.info("Uploaded {} → {} ({} bytes)", file.getOriginalFilename(), key, file.getSize());
            return url;

        } catch (IOException e) {
            throw new StorageException("Failed to read file bytes: " + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("S3 upload failed for key={}: {}", key, e.getMessage());
            throw new StorageException("S3 upload failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Upload multiple files to S3 under the given folder prefix.
     * Preserves order — returned URL list matches the input file list.
     *
     * @param files  list of multipart files
     * @param folder e.g. "products", "avatars"
     * @return list of public URLs in the same order as input
     */
    public List<String> uploadAll(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) return List.of();
        List<String> urls = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            urls.add(upload(file, folder));
        }
        return urls;
    }

    /**
     * Delete an object from S3 by its full URL or key.
     */
    public void delete(String urlOrKey) {
        String key = extractKey(urlOrKey);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("Deleted S3 object: {}", key);
        } catch (S3Exception e) {
            log.warn("S3 delete failed for {}: {}", key, e.getMessage());
        }
    }

    /**
     * Generate a presigned PUT URL for direct browser-to-S3 upload.
     * Expires in 15 minutes.
     */
    public PresignedUploadUrl presignUpload(String folder, String contentType, String originalName) {
        validateContentType(contentType);
        String ext = getExtension(originalName);
        String key = folder + "/" + UUID.randomUUID() + "." + ext;

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(put -> put
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build())
                .build());

        return new PresignedUploadUrl(
                presigned.url().toString(),
                resolvePublicUrl(key),
                key,
                Duration.ofMinutes(15).toSeconds()
        );
    }

    // ── Private helpers ────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new StorageException("File too large: max 10 MB, got " + file.getSize() + " bytes");
        }
        validateContentType(file.getContentType());
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new StorageException(
                    "Unsupported content type: " + contentType +
                            ". Allowed: " + ALLOWED_CONTENT_TYPES);
        }
    }

    private String resolvePublicUrl(String key) {
        if (cdnUrl != null && !cdnUrl.isBlank()) {
            return cdnUrl.stripTrailing() + "/" + key;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot + 1).toLowerCase() : "jpg";
    }

    private String extractKey(String urlOrKey) {
        if (urlOrKey.startsWith("http")) {
            int idx = urlOrKey.indexOf(bucket);
            if (idx >= 0) {
                String after = urlOrKey.substring(idx + bucket.length());
                return after.startsWith("/") ? after.substring(1) : after;
            }
        }
        return urlOrKey;
    }

    private String sanitizeHeaderValue(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "_");
    }

    // ── DTOs ──────────────────────────────────────────────────

    public record PresignedUploadUrl(
            String uploadUrl,
            String publicUrl,
            String s3Key,
            long   expiresInSeconds
    ) {}
}