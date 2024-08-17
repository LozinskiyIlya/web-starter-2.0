package com.starter.web.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class SubscriptionDto {

    @NotNull
    private Double price;

    @NotNull
    private String currency;

    @NotNull
    private Instant createdAt;

    @NotNull
    private Instant endsAt;

}
