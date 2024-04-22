package com.starter.web.fragments;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MessageClassificationResponse {

    @JsonProperty("payment_related")
    private boolean isPaymentRelated;
}
