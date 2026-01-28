package com.thyagoronald.domain.services.impl;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.thyagoronald.domain.services.RequestService;

@Service
public class RequestServiceImpl implements RequestService {
    private final RestTemplate restTemplate;

    public RequestServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public <T, R> ResponseEntity<R> doPost(String url, T requestBody, Class<R> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);

        return restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            responseType
        );
    }
}
