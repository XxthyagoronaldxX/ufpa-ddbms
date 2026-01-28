package com.thyagoronald.domain.services;

import org.springframework.http.ResponseEntity;

public interface RequestService {
    <T, R> ResponseEntity<R> doPost(String url, T requestBody, Class<R> responseType);
}
