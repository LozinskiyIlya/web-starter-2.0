package com.starter.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collections;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({UnauthorizedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Map<String, String> handleUnauthorizedException(Exception e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    @ExceptionHandler({DuplicateEmailException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public Map<String, String> handleConflictException(Exception e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    @ExceptionHandler({WrongUserException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public Map<String, String> handleForbiddenException(Exception e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    @ExceptionHandler({MissingPasswordException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, String> handleBadRequestException(Exception e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException() {
            super("User with this email already exists");
        }
    }

    public static class WrongUserException extends RuntimeException {
    }

    public static class MissingPasswordException extends RuntimeException {
    }
}
