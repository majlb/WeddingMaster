package com.wedora.web;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ApiException(HttpStatus status, String message, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static class NotFound extends ApiException {
        public NotFound(String message) { super(HttpStatus.NOT_FOUND, message); }
    }

    public static class Forbidden extends ApiException {
        public Forbidden(String message) { super(HttpStatus.FORBIDDEN, message); }
    }

    public static class Unauthorized extends ApiException {
        public Unauthorized(String message) { super(HttpStatus.UNAUTHORIZED, message); }
    }

    public static class Conflict extends ApiException {
        public Conflict(String message) { super(HttpStatus.CONFLICT, message); }
    }
}
