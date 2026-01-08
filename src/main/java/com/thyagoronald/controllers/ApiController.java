package com.thyagoronald.controllers;

import java.sql.SQLException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.thyagoronald.pojos.QueryPojo;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.services.DbService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class ApiController {
    private final DbService dbService;
    private final CommunicationService communicationService;

    @PostMapping("query")
    public ResponseEntity<String> doQuery(@RequestBody QueryPojo dto) {
        try {
            dbService.execute(dto.getQuery());
            communicationService.sendReplication(dto.getQuery());
        } catch (SQLException ex) {
            return ResponseEntity.status(500).body("Error executing query");
        }

        return ResponseEntity.ok("Query received");
    }
}
