package com.starter.web.fragments;


import lombok.Data;

import java.time.Instant;
import java.util.Arrays;

@Data
public class BillAssistantResponse {
    private String buyer;
    private String seller;
    private String purpose;
    private String currency;
    private String amount;
    private Instant mentionedDate;
    private String[] tags;

    public static BillAssistantResponse EMPTY() {
        final var response = new BillAssistantResponse();
        response.setBuyer("");
        response.setSeller("");
        response.setPurpose("");
        response.setCurrency("");
        response.setAmount("");
        response.setMentionedDate(Instant.now());
        response.setTags(new String[0]);
        return response;
    }

    @Override
    public String toString() {
        return "BillAssistantResponse{" +
                "buyer='" + buyer + '\'' +
                ", seller='" + seller + '\'' +
                ", purpose='" + purpose + '\'' +
                ", currency='" + currency + '\'' +
                ", amount='" + amount + '\'' +
                ", dateRow='" + mentionedDate + '\'' +
                ", tags=" + Arrays.toString(tags) +
                '}';
    }
}
