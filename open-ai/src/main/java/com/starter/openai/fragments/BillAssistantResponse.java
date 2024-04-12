package com.starter.openai.fragments;


import lombok.Data;

import java.util.Arrays;

@Data
public class BillAssistantResponse {
    private String buyer;
    private String seller;
    private String purpose;
    private String currency;
    private String amount;
    private String mentionedDate;
    private String dateFormat;
    private String[] tags;

    public static BillAssistantResponse EMPTY() {
        final var response = new BillAssistantResponse();
        response.setBuyer("");
        response.setSeller("");
        response.setPurpose("");
        response.setCurrency("");
        response.setAmount("");
        response.setMentionedDate("");
        response.setDateFormat("");
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
                ", mentionedDate='" + mentionedDate + '\'' +
                ", dateFormat='" + dateFormat + '\'' +
                ", tags=" + Arrays.toString(tags) +
                '}';
    }
}
