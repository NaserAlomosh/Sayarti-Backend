package com.sayarti.backend.security.oauth;

import com.sayarti.backend.common.exception.ApiException;
import com.sayarti.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class GoogleAuthenticationException extends ApiException {
    public GoogleAuthenticationException(String message) {
        super(ErrorCode.AUTH_GOOGLE_LOGIN_FAILED, HttpStatus.UNAUTHORIZED, message);
    }
}
