package com.thyagoronald.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thyagoronald.domain.services.ApiService;
import com.thyagoronald.domain.pojos.QueryPojo;
import com.thyagoronald.domain.pojos.QueryResponsePojo;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("api")
public class ApiController {
    private final ApiService apiService;

    @GetMapping("read")
    public ResponseEntity<QueryResponsePojo> doQueryReadBalanced(@RequestParam("query") String query) {
        QueryPojo balancedDto = new QueryPojo();

        balancedDto.setQuery(query);

        return apiService.doQueryRead(balancedDto);
    }

    @PostMapping("read")
    public ResponseEntity<QueryResponsePojo> doQueryRead(@RequestBody QueryPojo dto) {
        return apiService.doQueryRead(dto);
    }

    @PostMapping("write")
    public ResponseEntity<QueryResponsePojo> doQueryWrite(@RequestBody QueryPojo dto) {
        return apiService.doQueryWrite(dto);
    }
}
