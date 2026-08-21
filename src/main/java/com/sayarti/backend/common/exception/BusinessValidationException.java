package com.sayarti.backend.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessValidationException extends ApiException {
    public BusinessValidationException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
