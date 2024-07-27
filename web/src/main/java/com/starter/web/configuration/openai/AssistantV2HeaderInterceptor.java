package com.starter.web.configuration.openai;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AssistantV2HeaderInterceptor implements Interceptor {
    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request modifiedRequest = originalRequest.newBuilder()
                .header("OpenAI-Beta", "assistants=v2")
                .build();
        return chain.proceed(modifiedRequest);
    }
}