package com.thyagoronald.domain.pojos;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryResponsePojo {
    private int node;
    private String message;
    private List<Map<String, Object>> result;
}
