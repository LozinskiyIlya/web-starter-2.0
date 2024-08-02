package com.starter.web.configuration.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class BodyModificationInterceptor implements Interceptor {
    private static final String THREAD_RUN_URL = "https://api.openai.com/v1/threads/runs";
    private static final String CHAT_COMPLETION_URL = "https://api.openai.com/v1/chat/completions";
    private static final List<String> TARGET_URLS = List.of(THREAD_RUN_URL, CHAT_COMPLETION_URL);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String url = originalRequest.url().toString();

        if (TARGET_URLS.contains(url)) {
            RequestBody originalBody = originalRequest.body();
            if (originalBody != null) {
                Buffer buffer = new Buffer();
                originalBody.writeTo(buffer);
                String oldBodyString = buffer.readUtf8();

                // Transform the request body
                String newBodyString = switch (url) {
                    case THREAD_RUN_URL -> transformThreadRun(oldBodyString);
                    case CHAT_COMPLETION_URL -> transformChatCompletion(oldBodyString);
                    default -> throw new IllegalArgumentException("Unexpected value: " + url);
                };

                RequestBody newBody = RequestBody.create(newBodyString, MediaType.get("application/json; charset=utf-8"));
                Request modifiedRequest = originalRequest.newBuilder()
                        .method(originalRequest.method(), newBody)
                        .build();

                return chain.proceed(modifiedRequest);
            }
        }

        return chain.proceed(originalRequest);
    }

    private String transformThreadRun(String oldBodyString) throws IOException {
        // Deserialize the old body into a JsonNode
        JsonNode oldBodyJson = objectMapper.readTree(oldBodyString);

        // Create the new body structure
        ObjectNode newBodyJson = objectMapper.createObjectNode();
        ObjectNode threadNode = objectMapper.createObjectNode();
        ArrayNode messagesNode = objectMapper.createArrayNode();

        // Copy and transform the messages
        ArrayNode oldMessagesNode = (ArrayNode) oldBodyJson.path("thread").path("messages");
        for (JsonNode message : oldMessagesNode) {
            ObjectNode newMessageNode = objectMapper.createObjectNode();
            newMessageNode.put("role", message.path("role").asText());
            newMessageNode.put("content", message.path("content").asText());

            // Transform the file_ids into attachments
            if (message.has("file_ids")) {
                ArrayNode fileIdsNode = (ArrayNode) message.path("file_ids");
                ArrayNode attachmentsNode = objectMapper.createArrayNode();
                for (JsonNode fileId : fileIdsNode) {
                    ObjectNode attachmentNode = objectMapper.createObjectNode();
                    attachmentNode.put("file_id", fileId.asText());

                    ArrayNode toolsNode = objectMapper.createArrayNode();
                    ObjectNode fileSearchNode = objectMapper.createObjectNode();
                    fileSearchNode.put("type", "file_search");
                    toolsNode.add(fileSearchNode);

                    ObjectNode codeInterpreterNode = objectMapper.createObjectNode();
                    codeInterpreterNode.put("type", "code_interpreter");
                    toolsNode.add(codeInterpreterNode);

                    attachmentNode.set("tools", toolsNode);
                    attachmentsNode.add(attachmentNode);
                }
                newMessageNode.set("attachments", attachmentsNode);
            }

            messagesNode.add(newMessageNode);
        }

        threadNode.set("messages", messagesNode);
        newBodyJson.set("thread", threadNode);
        newBodyJson.set("metadata", oldBodyJson.path("metadata"));
        newBodyJson.set("assistant_id", oldBodyJson.path("assistant_id"));

        // Serialize the new body to a string
        return objectMapper.writeValueAsString(newBodyJson);
    }

    private String transformChatCompletion(String oldBodyString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Deserialize the old body into a JsonNode
        JsonNode oldBodyJson = objectMapper.readTree(oldBodyString);

        // Create the new body structure
        ObjectNode newBodyJson = objectMapper.createObjectNode();
        ArrayNode messagesNode = objectMapper.createArrayNode();

        // Copy and transform the messages
        ArrayNode oldMessagesNode = (ArrayNode) oldBodyJson.path("messages");

        // Transform only vision requests
        if (!oldMessagesNode.get(0).path("role").asText().equals("vision")) {
            return oldBodyString;
        }

        for (JsonNode message : oldMessagesNode) {
            ObjectNode newMessageNode = objectMapper.createObjectNode();
            newMessageNode.put("role", "user");

            // Transform the content
            ArrayNode contentArrayNode = objectMapper.createArrayNode();
            String contentString = message.path("content").asText();
            JsonNode contentJsonNode = objectMapper.readTree(contentString);
            for (JsonNode contentItem : contentJsonNode) {
                ObjectNode contentNode = objectMapper.createObjectNode();
                contentNode.put("type", contentItem.path("type").asText());
                if (contentItem.has("text")) {
                    contentNode.put("text", contentItem.path("text").asText());
                }
                if (contentItem.has("image_url")) {
                    ObjectNode imageUrlNode = (ObjectNode) contentItem.path("image_url");
                    ObjectNode imageNode = objectMapper.createObjectNode();
                    imageNode.put("url", imageUrlNode.path("url").asText());
                    imageNode.put("detail", imageUrlNode.path("detail").asText());
                    contentNode.set("image_url", imageNode);
                }
                contentArrayNode.add(contentNode);
            }
            newMessageNode.set("content", contentArrayNode);
            messagesNode.add(newMessageNode);
        }

        newBodyJson.set("messages", messagesNode);
        newBodyJson.put("n", oldBodyJson.path("n").asInt());
        newBodyJson.put("max_tokens", oldBodyJson.path("max_tokens").asInt());
        newBodyJson.put("model", oldBodyJson.path("model").asText());

        // Serialize the new body to a string
        return objectMapper.writeValueAsString(newBodyJson);
    }

}
