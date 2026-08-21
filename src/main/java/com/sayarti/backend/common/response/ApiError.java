package com.sayarti.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, Map<String, String> details) {

    public ApiError(String code, String message) {
        this(code, message, null);
    }
}
