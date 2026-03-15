package com.shopee.ecommerce.module.media;

import com.shopee.ecommerce.infrastructure.storage.StorageService;
import com.shopee.ecommerce.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Media upload endpoints.
 *
 * POST /api/media/upload         — multipart upload (authenticated users)
 * POST /api/media/presign        — get presigned S3 PUT URL (admin/seller)
 * DELETE /api/media              — delete by URL (admin)
 */
@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {

        String url = storageService.upload(file, folder);
        return ApiResponse.success(Map.of("url", url));
    }

    @PostMapping("/presign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ApiResponse<StorageService.PresignedUploadUrl> presign(
            @RequestParam String folder,
            @RequestParam String contentType,
            @RequestParam String filename) {

        return ApiResponse.success(storageService.presignUpload(folder, contentType, filename));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@RequestParam String url) {
        storageService.delete(url);
        return ApiResponse.success(null);
    }
}
