package com.thyagoronald.domain.pojos;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponsePojo {
    private int node;
    private String message;
    private List<Map<String, Object>> result;
}
