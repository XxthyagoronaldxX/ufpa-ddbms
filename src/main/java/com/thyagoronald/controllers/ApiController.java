package com.thyagoronald.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.thyagoronald.configs.DbConfig;
import com.thyagoronald.errors.ReplicateHostException;
import com.thyagoronald.pojos.QueryPojo;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.utils.DbUtil;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class ApiController {
    private final CommunicationService communicationService;

    @PostMapping("query")
    public ResponseEntity<Object> doQuery(@RequestBody QueryPojo dto) {
        try {
            String query = dto.getQuery();

            try (Connection connection = DbConfig.getConnection()) {
                connection.setAutoCommit(false);
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    if (query.startsWith("SELECT")) {
                        ResultSet rs = ps.executeQuery();

                        return ResponseEntity.ok(DbUtil.findDynamicData(rs));
                    } else {
                        ps.executeUpdate();
                        communicationService.sendReplication(dto.getQuery());
                        connection.commit();
                        return ResponseEntity.ok("SQL EXECUTED SUCCESSFULLY");
                    }
                } catch (SQLException | ReplicateHostException ex) {
                    connection.rollback();
                    return ResponseEntity.status(500).body("SQL EXECUTION ERROR: " + ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            return ResponseEntity.status(500).body("Error executing query");
        }
    }
}
