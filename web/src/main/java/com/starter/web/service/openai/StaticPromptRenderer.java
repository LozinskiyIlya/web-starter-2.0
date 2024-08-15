package com.starter.web.service.openai;


import java.time.Instant;
import java.time.ZoneId;

public class StaticPromptRenderer {

    public static final String INSIGHTS_INSTRUCTIONS = """
            Analyse the following bills and give short 2 sentences suggestions 'insights'.
            Analyse trends over time, categories, amounts, and other patterns.
            Do not go into great details by mentioning exact bill entry anywhere, keep it simple and clear.
            Do not include markup, your response should look like a short paragraph. The shorter the better.
            """;
    public static final String PRE_PROCESS_PROMPT = """
            Is this a payment related message?
            Respond with nothing more than valid JSON (WITHOUT ``` marks) of the format:
            {
                "payment_related": true | false
            }
            It is most likely true if there is an amount and the purpose mentioned in the message.
            """;
    public static final String FILE_PROMPT = "%s\n%s\nAnalyse the file according to your instructions";
    public static final String FORCE_FILE_USE_PROMPT = "Yes, you DO have the file. In case of error try again. DO NOT include any comments, respond only with the resulting JSON filled according to the file's content.";
    public static final String VISION_USER_PROMPT = "User has sent the file along with the CAPTION: %s\n";
    public static final String VISION_PROMPT = "Extract Amount, currency, purpose/place, category, and datetime if present, no additional comments please. To do this, consider CAPTION at the first place, and only then file content.";
    private static final String DEFAULT_CURRENCY_PROMPT = "If currency is not parseable use %s";
    private static final int MAX_USER_TEXT_LENGTH = 1024;

    public static String trimUserMessage(String text) {
        final var withed = text != null && text.length() > MAX_USER_TEXT_LENGTH ? text.substring(0, MAX_USER_TEXT_LENGTH) + "..." : text;
        return withed.trim();
    }

    public static String runInstructions(String defaultCurrency) {
        final var dateInstruction = "Current date: " + Instant.now().atZone(ZoneId.of("UTC"));
        final var currencyInstruction = defaultCurrency != null ? String.format(DEFAULT_CURRENCY_PROMPT, defaultCurrency) : "";
        return dateInstruction + "\n" + currencyInstruction;
    }

    public static String fullFilePrompt(String caption, String defaultCurrency) {
        final var withedCaption = caption != null ? trimUserMessage(caption) : "";
        final var runInstructions = runInstructions(defaultCurrency);
        return String.format(FILE_PROMPT, runInstructions, withedCaption);
    }
}
