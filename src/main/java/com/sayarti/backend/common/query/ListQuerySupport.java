package com.sayarti.backend.common.query;

import com.sayarti.backend.common.exception.ApiException;
import com.sayarti.backend.common.exception.ErrorCode;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

/** Builds safe, deterministic pageable sorts from endpoint-specific public-field allowlists. */
public final class ListQuerySupport {
    private ListQuerySupport() { }

    public static Pageable pageable(int page, int size, String sortBy, String sortDirection,
            String defaultSortBy, Sort.Direction defaultDirection, Map<String, String> fields) {
        return pageable(page, size, sortBy, sortDirection, defaultSortBy, defaultDirection,
                fields, null);
    }

    public static Pageable pageable(int page, int size, String sortBy, String sortDirection,
            String defaultSortBy, Sort.Direction defaultDirection, Map<String, String> fields,
            String defaultSecondaryField) {
        boolean usingDefaultField = sortBy == null || sortBy.isBlank();
        String publicField = sortBy == null || sortBy.isBlank() ? defaultSortBy : sortBy;
        String entityField = fields.get(publicField);
        if (entityField == null) {
            invalid("Unsupported sortBy; supported values are: " + String.join(", ", fields.keySet()));
        }
        Sort.Direction direction = direction(sortDirection, defaultDirection);
        Sort sort = Sort.by(direction, entityField);
        if (usingDefaultField && defaultSecondaryField != null
                && !defaultSecondaryField.equals(entityField)) {
            sort = sort.and(Sort.by(direction, defaultSecondaryField));
        }
        if (!"id".equals(entityField)) {
            sort = sort.and(Sort.by(direction, "id"));
        }
        return PageRequest.of(page, size, sort);
    }

    public static void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            invalid("from must be earlier than or equal to to");
        }
    }

    private static Sort.Direction direction(String value, Sort.Direction defaultDirection) {
        if (value == null || value.isBlank()) return defaultDirection;
        if ("asc".equalsIgnoreCase(value)) return Sort.Direction.ASC;
        if ("desc".equalsIgnoreCase(value)) return Sort.Direction.DESC;
        invalid("Unsupported sortDirection; supported values are: asc, desc");
        throw new IllegalStateException("unreachable");
    }

    private static void invalid(String message) {
        throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
    }
}
