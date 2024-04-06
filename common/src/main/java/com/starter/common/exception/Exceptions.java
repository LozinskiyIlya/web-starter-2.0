package com.starter.common.exception;

public class Exceptions {

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

        @Override
        public String getMessage() {
            return "Wrong user";
        }
    }

    public static class MissingPasswordException extends RuntimeException {

        @Override
        public String getMessage() {
            return "Missing password";
        }
    }

    public static class UserNotFoundException extends RuntimeException {

        @Override
        public String getMessage() {
            return "User not found";
        }
    }


    public static class InvalidOtpException extends RuntimeException {

        @Override
        public String getMessage() {
            return "Invalid or expired code";
        }
    }
}
