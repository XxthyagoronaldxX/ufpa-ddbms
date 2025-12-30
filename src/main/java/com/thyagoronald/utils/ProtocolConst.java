package com.thyagoronald.utils;

public class ProtocolConst {
    private ProtocolConst() {
    }

    public static final String HEARTBEAT_PREFIX = "HEARTBEAT ";
    public static final String HEARTBEAT_SUCCESS = "ALIVE";

    public static final String REPLICATE_PREFIX = "REPLICATE ";
    public static final String REPLICATE_SUCCESS = "REPLICATE_SUCCESS";

    public static final String QUERY_PREFIX = "QUERY ";
    public static final String QUERY_SUCCESS = "QUERY_SUCCESS";

    public static final String LOADBALANCER_PREFIX = "LOADBALANCER ";
    public static final String LOADBALANCER_SUCCESS = "LOADBALANCER_SUCCESS";

    public static final String ERROR_RESPONSE = "ERROR";
    public static final String ERROR_SERVER_BUSY_RESPONSE = "ERROR_SERVER_BUSY";
    public static final String ERROR_INTERNAL_SERVER_RESPONSE = "ERROR_INTERNAL_SERVER";
}
