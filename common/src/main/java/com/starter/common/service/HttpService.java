package com.starter.common.service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpService {

    private RestTemplate rest;
    private HttpHeaders headers;

    @PostConstruct
    public void init() {
        rest = new RestTemplate();
        headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        headers.add("Accept", MediaType.ALL_VALUE);
    }

    public String get(String uri) {
        return this.getT(uri, String.class);
    }

    public <T> T getT(String uri, Class<T> responseClass) {
        HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
        ResponseEntity<T> responseEntity = rest.exchange(uri, HttpMethod.GET, requestEntity, responseClass);
        return responseEntity.getBody();
    }

    public <T> T getT(String uri, Class<T> responseClass, HttpHeaders customHeaders) {
        HttpEntity<String> requestEntity = new HttpEntity<>("", customHeaders);
        ResponseEntity<T> responseEntity = rest.exchange(uri, HttpMethod.GET, requestEntity, responseClass);
        return responseEntity.getBody();
    }

    public <T> T getT(String uri, ParameterizedTypeReference<T> responseType) {
        HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
        ResponseEntity<T> responseEntity = rest.exchange(uri, HttpMethod.GET, requestEntity, responseType);
        return responseEntity.getBody();
    }

    public String post(String uri, String json) {
        return this.postT(uri, json, String.class);
    }

    public <ReqT, ResT> ResT postT(String uri, ReqT body, Class<ResT> responseClass) {
        HttpEntity<ReqT> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<ResT> responseEntity = rest.exchange(uri, HttpMethod.POST, requestEntity, responseClass);
        return responseEntity.getBody();
    }

    public <ReqT, ResT> ResT postT(String uri, ReqT body, Class<ResT> responseClass, HttpHeaders customHeaders) {
        HttpEntity<ReqT> requestEntity = new HttpEntity<>(body, customHeaders);
        ResponseEntity<ResT> responseEntity = rest.exchange(uri, HttpMethod.POST, requestEntity, responseClass);
        return responseEntity.getBody();
    }

    public <ReqT, ResT> ResT postT(String uri, ReqT body, ParameterizedTypeReference<ResT> responseType) {
        HttpEntity<ReqT> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<ResT> responseEntity = rest.exchange(uri, HttpMethod.POST, requestEntity, responseType);
        return responseEntity.getBody();
    }
}

