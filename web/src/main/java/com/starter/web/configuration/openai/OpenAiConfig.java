package com.starter.web.configuration.openai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.theokanning.openai.assistants.AssistantToolsEnum;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
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
@EnableConfigurationProperties({OpenAiProperties.class, AssistantProperties.class, OllamaProperties.class})
@RequiredArgsConstructor
public class OpenAiConfig {

    private final OpenAiProperties openAiProperties;
    private final OllamaProperties ollamaProperties;

    @Bean
    OpenAiService openAiService() {
        ObjectMapper mapper = CustomObjectMapper.create();
        OkHttpClient client = defaultClient(openAiProperties.getToken(), openAiProperties.getTimeout())
                .newBuilder()
                .addInterceptor(new AssistantV2HeaderInterceptor())
                .addInterceptor(new BodyModificationInterceptor())
//                .addInterceptor(new OllamaRequestModifyingInterceptor(ollamaProperties.getBaseUrl()))
//                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        Retrofit retrofit = defaultRetrofit(client, mapper);
        OpenAiApi api = retrofit.create(OpenAiApi.class);
        return new OpenAiService(api);
    }

    static class AssistantToolsEnumDeserializer extends JsonDeserializer<AssistantToolsEnum> {
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

    static class CustomObjectMapper {
        static ObjectMapper create() {
            ObjectMapper mapper = defaultObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(AssistantToolsEnum.class, new AssistantToolsEnumDeserializer());
            mapper.registerModule(module);
            return mapper;
        }
    }
}
