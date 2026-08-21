package com.sayarti.backend.email;

import com.sayarti.backend.common.exception.ApiException;
import com.sayarti.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class EmailDeliveryException extends ApiException {
    public EmailDeliveryException() {
        super(ErrorCode.EMAIL_DELIVERY_FAILED, HttpStatus.SERVICE_UNAVAILABLE,
                "Verification email could not be delivered; please try again later");
    }
}
