package com.starter.web.fragments;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OcrResponse {

    @JsonProperty("ParsedResults")
    private List<ParsedResult> parsedResults;

    @Data
    public static class ParsedResult {

        @JsonProperty("ParsedText")
        private String parsedText;
    }
}
