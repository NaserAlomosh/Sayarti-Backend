package com.sayarti.backend.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiResponseTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesStandardSuccessResponse() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsBytes(ApiResponse.success(Map.of("id", 1))));

        assertThat(json.path("success").asBoolean()).isTrue();
        assertThat(json.path("data").path("id").asInt()).isEqualTo(1);
        assertThat(json.has("message")).isTrue();
        assertThat(json.path("message").isNull()).isTrue();
    }

    @Test
    void serializesStandardErrorResponse() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(
                ErrorResponse.of("RESOURCE_NOT_FOUND", "Resource not found")));

        assertThat(json.path("success").asBoolean()).isFalse();
        assertThat(json.path("error").path("code").asText()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(json.path("error").path("message").asText()).isEqualTo("Resource not found");
    }
}
