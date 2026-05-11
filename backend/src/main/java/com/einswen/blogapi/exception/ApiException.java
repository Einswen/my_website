package com.einswen.blogapi.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Object detail;

    public ApiException(HttpStatus status, Object detail) {
        super(detail instanceof String message ? message : status.getReasonPhrase());
        this.status = status;
        this.detail = detail;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getDetail() {
        return detail;
    }
}
