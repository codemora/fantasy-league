package com.codemora.fantasy_league.common;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * The pagination envelope fixed in ADR 0009 -- returned instead of Spring
 * Data's Page<T> directly, so the API contract isn't coupled to Spring
 * Data's internal JSON shape (which includes framework-specific fields
 * like "pageable" and "sort" metadata).
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
