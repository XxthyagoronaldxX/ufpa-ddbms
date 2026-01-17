package com.thyagoronald.services;

import org.springframework.http.ResponseEntity;

import com.thyagoronald.pojos.QueryPojo;
import com.thyagoronald.pojos.QueryResponsePojo;

public interface ApiService {
    public ResponseEntity<QueryResponsePojo> doQueryRead(QueryPojo dto);

    public ResponseEntity<QueryResponsePojo> doQueryWrite(QueryPojo dto);
}
