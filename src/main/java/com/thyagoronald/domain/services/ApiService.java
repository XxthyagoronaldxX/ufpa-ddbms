package com.thyagoronald.domain.services;

import org.springframework.http.ResponseEntity;

import com.thyagoronald.domain.pojos.QueryPojo;
import com.thyagoronald.domain.pojos.QueryResponsePojo;

public interface ApiService {
    public ResponseEntity<QueryResponsePojo> doQueryRead(QueryPojo dto);

    public ResponseEntity<QueryResponsePojo> doQueryWrite(QueryPojo dto);
}
