package com.starter.web.configuration.openai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theokanning.openai.assistants.AssistantToolsEnum;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;

import java.io.IOException;

import static com.theokanning.openai.service.OpenAiService.*;

/**
 * @author ilya
 * @date 08.01.2024
 */

@Configuration
@EnableConfigurationProperties({OpenAiProperties.class, AssistantProperties.class})
@RequiredArgsConstructor
public class OpenAiConfig {

    private final OpenAiProperties openAiProperties;

    @Bean
    OpenAiService openAiService() {
        ObjectMapper mapper = CustomObjectMapper.create();
        OkHttpClient client = defaultClient(openAiProperties.getToken(), openAiProperties.getTimeout())
                .newBuilder()
                .addInterceptor(new AssistantV2HeaderInterceptor())
                .addInterceptor(new BodyModificationInterceptor())
//                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        Retrofit retrofit = defaultRetrofit(client, mapper);
        OpenAiApi api = retrofit.create(OpenAiApi.class);
        return new OpenAiService(api);
    }


    public static class AssistantV2HeaderInterceptor implements Interceptor {
        @Override
        public @NotNull Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request modifiedRequest = originalRequest.newBuilder()
                    .header("OpenAI-Beta", "assistants=v2")
                    .build();
            return chain.proceed(modifiedRequest);
        }
    }

    public static class AssistantToolsEnumDeserializer extends JsonDeserializer<AssistantToolsEnum> {
        @Override
        public AssistantToolsEnum deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            String value = p.getText();
            try {
                return AssistantToolsEnum.valueOf(value);
            } catch (IllegalArgumentException e) {
                return AssistantToolsEnum.RETRIEVAL;
            }
        }
    }

    public static class CustomObjectMapper {
        public static ObjectMapper create() {
            ObjectMapper mapper = defaultObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(AssistantToolsEnum.class, new AssistantToolsEnumDeserializer());
            mapper.registerModule(module);
            return mapper;
        }
    }


    public static class BodyModificationInterceptor implements Interceptor {
        private static final String TARGET_URL = "https://api.openai.com/v1/threads/runs";
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public @NotNull Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            String url = originalRequest.url().toString();

            if (url.equals(TARGET_URL)) {
                RequestBody originalBody = originalRequest.body();
                if (originalBody != null) {
                    Buffer buffer = new Buffer();
                    originalBody.writeTo(buffer);
                    String oldBodyString = buffer.readUtf8();

                    // Transform the request body
                    String newBodyString = transformRequestBody(oldBodyString);

                    RequestBody newBody = RequestBody.create(newBodyString, MediaType.get("application/json; charset=utf-8"));
                    Request modifiedRequest = originalRequest.newBuilder()
                            .method(originalRequest.method(), newBody)
                            .build();

                    return chain.proceed(modifiedRequest);
                }
            }

            return chain.proceed(originalRequest);
        }

        private String transformRequestBody(String oldBodyString) throws IOException {
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
    }

}
