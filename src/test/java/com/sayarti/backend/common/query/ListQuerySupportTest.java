package com.sayarti.backend.common.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sayarti.backend.common.exception.ApiException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class ListQuerySupportTest {
    private static final Map<String, String> FIELDS = Map.of("date", "eventDate", "cost", "amount");

    @Test
    void createsAllowlistedAscendingAndDescendingDeterministicSorts() {
        var ascending = ListQuerySupport.pageable(1, 5, "cost", "asc", "date",
                Sort.Direction.DESC, FIELDS);
        assertThat(ascending.getPageNumber()).isEqualTo(1);
        assertThat(ascending.getSort().getOrderFor("amount").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(ascending.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);

        var descending = ListQuerySupport.pageable(0, 20, "date", "desc", "date",
                Sort.Direction.DESC, FIELDS);
        assertThat(descending.getSort().getOrderFor("eventDate").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void preservesDefaultSortAndOptionalSecondaryTieBreaker() {
        var pageable = ListQuerySupport.pageable(0, 20, null, null, "date",
                Sort.Direction.DESC, FIELDS, "createdAt");
        assertThat(pageable.getSort().toList()).extracting(Sort.Order::getProperty)
                .containsExactly("eventDate", "createdAt", "id");
    }

    @Test
    void rejectsUnknownFieldsDirectionsAndReversedRanges() {
        assertValidation(() -> ListQuerySupport.pageable(0, 20, "deletedAt", "asc", "date",
                Sort.Direction.DESC, FIELDS));
        assertValidation(() -> ListQuerySupport.pageable(0, 20, "date", "sideways", "date",
                Sort.Direction.DESC, FIELDS));
        assertValidation(() -> ListQuerySupport.validateRange(Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")));
        ListQuerySupport.validateRange(Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private void assertValidation(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getErrorCode().name())
                .isEqualTo("VALIDATION_ERROR");
    }
}
