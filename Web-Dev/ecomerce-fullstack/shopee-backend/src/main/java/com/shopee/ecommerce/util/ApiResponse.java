package com.shopee.ecommerce.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard API response wrapper for all successful responses.
 *
 * <p>Consistent envelope format:
 * <pre>
 * // Single resource:
 * {
 *   "success":   true,
 *   "message":   "Product created",
 *   "data":      { ... },
 *   "timestamp": "2024-01-01T12:00:00"
 * }
 *
 * // Paginated list:
 * {
 *   "success":    true,
 *   "data":       [ ... ],
 *   "page":       0,
 *   "size":       20,
 *   "totalElements": 150,
 *   "totalPages": 8,
 *   "last":       false
 * }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean       success;
    private final String        message;
    private final T             data;
    private final LocalDateTime timestamp;

    // Pagination fields (only populated for paged responses)
    private final Integer  page;
    private final Integer  size;
    private final Long     totalElements;
    private final Integer  totalPages;
    private final Boolean  last;

    // ── Factory methods ───────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<List<T>> ofPage(Page<T> page) {
        return ApiResponse.<List<T>>builder()
                .success(true)
                .data(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiResponse<Void> message(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
