package com.thyagoronald.utils;

public class ProtocolConst {
    private ProtocolConst() {
    }

    public static final String HEARTBEAT_REQUEST = "HEARTBEAT";
    public static final String HEARTBEAT_RESPONSE = "ALIVE";

    public static final String REPLICATE_PREFIX = "REPLICATE ";
    public static final String REPLICATE_SUCCESS = "REPLICATE_SUCCESS";

    public static final String QUERY_PREFIX = "QUERY ";
    public static final String QUERY_SUCCESS = "QUERY_SUCCESS";

    public static final String ERROR_RESPONSE = "ERROR";
}
