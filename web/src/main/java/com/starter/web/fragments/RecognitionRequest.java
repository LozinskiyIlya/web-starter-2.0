package com.starter.web.fragments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RecognitionRequest {
    private UUID groupId;
    @NotBlank(message = "Bill details must be present")
    private String details;
    @NotNull(message = "Recognition type must be present")
    private RecognitionType type;

    public enum RecognitionType {
        TEXT, IMAGE
    }
}

