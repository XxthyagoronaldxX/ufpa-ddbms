package com.thyagoronald.domain.utils;

public class ProtocolConst {
    private ProtocolConst() {
    }

    public static final String PROTOCOL_VERSION = "DDBMS/1.0";
    public static final String HEARTBEAT_METHOD = "HEARTBEAT";
    public static final String REPLICATE_METHOD = "REPLICATE";
    public static final String HISTORY_METHOD = "HISTORY";
    public static final String HISTORY_RESET_METHOD = "HISTORY_RESET";
    public static final String RECOVER_METHOD = "RECOVER";

    public static final String REPLICATE_COMMIT_METHOD = "REPLICATE_COMMIT";
    public static final String REPLICATE_ABORT_METHOD = "REPLICATE_ABORT";
    public static final String SUCCESS_METHOD = "SUCCESS";
    public static final String FAILURE_METHOD = "FAILURE";

    public static final String CRC_ATTR = "CRC ";
    public static final String CONTENT_ATTR = "CONTENT ";

    public static final String HEARTBEAT_SUCCESS = "ALIVE";

    public static final String ERROR_RESPONSE = "ERROR";
    public static final String ERROR_SERVER_BUSY_RESPONSE = "ERROR_SERVER_BUSY";
    public static final String ERROR_INTERNAL_SERVER_RESPONSE = "ERROR_INTERNAL_SERVER";
    public static final String ERROR_CRC_RESPONSE = "ERROR_CRC";
}
