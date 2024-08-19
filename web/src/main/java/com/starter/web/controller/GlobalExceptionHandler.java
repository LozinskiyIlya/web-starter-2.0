package com.starter.web.controller;

import com.starter.common.exception.Exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({UnauthorizedException.class, InvalidOtpException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Map<String, String> handleUnauthorizedException(Exception e) {
        return errorMap(e);
    }

    @ExceptionHandler({DuplicateEmailException.class, AllowedResourceCountExceeded.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public Map<String, String> handleConflictException(Exception e) {
        return errorMap(e);
    }

    @ExceptionHandler({WrongUserException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public Map<String, String> handleForbiddenException(Exception e) {
        return errorMap(e);
    }

    @ExceptionHandler({MissingPasswordException.class, ValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, String> handleBadRequestException(Exception e) {
        return errorMap(e);
    }

    @ExceptionHandler({UserNotFoundException.class, ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String, String> handleNotFoundException(Exception e) {
        return errorMap(e);
    }

    @ExceptionHandler({RecognitionException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Map<String, String> handleServerError(Exception e) {
        return errorMap(e);
    }

    @ExceptionHandler({RateLimitException.class})
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ResponseBody
    public Map<String, String> handleRateLimitError(Exception e) {
        return errorMap(e);
    }

    private static Map<String, String> errorMap(Exception e) {
        return Collections.singletonMap("error", e.getMessage());
    }
}
