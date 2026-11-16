package ua.kpi.absolutesvinema.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated list response wrapper")
public record PageResponse<T>(
        @Schema(description = "List of items on current page")
        List<T> content,

        @Schema(description = "Current zero-based page index", example = "0")
        int page,

        @Schema(description = "Number of items requested per page", example = "10")
        int size,

        @Schema(description = "Total number of items matching filter criteria", example = "42")
        long totalElements,

        @Schema(description = "Total number of pages", example = "5")
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 || totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
