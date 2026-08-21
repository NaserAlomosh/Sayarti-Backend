package com.sayarti.backend.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                          .setControllerAdvice(new GlobalExceptionHandler())
                          .setValidator(validator)
                          .build();
    }

    @Test
    void returnsFieldDetailsForInvalidRequest() throws Exception {
        mockMvc.perform(post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.name").value("must not be blank"));
    }

    @Test
    void mapsExplicitApiException() throws Exception {
        mockMvc.perform(post("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @RestController
    static class TestController {
        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody TestRequest request) {
        }

        @PostMapping("/test/missing")
        void missing() {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found");
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
