package com.starter.web.configuration.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;

@RequiredArgsConstructor
public class OllamaRequestModifyingInterceptor implements Interceptor {
    private static final String ASSISTANT_MODEL = "assistant";
    private static final String TARGET_URL = "https://api.openai.com/v1/chat/completions";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final URI newBase;

    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String url = originalRequest.url().toString();
        if (url.equals(TARGET_URL)) {
            //add ollama params to body
            RequestBody originalBody = originalRequest.body();
            RequestBody newBody = addOllamaParams(originalBody);

            //replace host with new base
            HttpUrl newUrl = originalRequest.url().newBuilder()
                    .scheme(newBase.getScheme())
                    .host(newBase.getHost())
                    .port(newBase.getPort())
                    .build();

            Request newRequest = originalRequest.newBuilder()
                    .method(originalRequest.method(), newBody)
                    .url("http://localhost:11434/api/generate")
                    .build();
            return chain.proceed(newRequest);
        }
        return chain.proceed(originalRequest);
    }

    private RequestBody addOllamaParams(RequestBody origin) throws IOException {
        if (origin != null) {
            Buffer buffer = new Buffer();
            origin.writeTo(buffer);
            String oldBodyString = buffer.readUtf8();
            // Parse the original body string to JSON
            JsonNode oldBodyJson = objectMapper.readTree(oldBodyString);
            // Cast JsonNode to ObjectNode for modification
            ObjectNode newBodyJson = (ObjectNode) oldBodyJson;
            // Add new parameters to the JSON object
            newBodyJson.put("model", ASSISTANT_MODEL);
            newBodyJson.put("stream", false);
            newBodyJson.put("format", "json");
            newBodyJson.put("keep_alive", -1);
            newBodyJson.put("stop", "");
            newBodyJson.put("prompt", oldBodyJson.get("messages").toString());
            // Convert the updated JSON object back to a string
            String newBodyString = newBodyJson.toString();
            // Create a new RequestBody with the updated JSON content
            return RequestBody.create(newBodyString, MediaType.get("application/json; charset=utf-8"));
        }
        return RequestBody.create("{}", MediaType.get("application/json; charset=utf-8"));
    }
}