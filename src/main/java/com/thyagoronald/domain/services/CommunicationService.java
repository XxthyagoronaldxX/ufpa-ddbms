package com.thyagoronald.domain.services;

import com.thyagoronald.domain.pojos.HistoryPojo;
import com.thyagoronald.domain.pojos.HostPojo;
import com.thyagoronald.domain.pojos.RequestPojo;
import com.thyagoronald.domain.pojos.ResponsePojo;

public interface CommunicationService {
    void sendHeartbeat();

    void sendHistory(HistoryPojo history);

    void sendHistoryReset(int nodeId);

    boolean sendRecover(HostPojo host);

    void handleHistoryReset(RequestPojo request, ResponsePojo response);

    void handleRecover(RequestPojo request, ResponsePojo response);

    void handleHistory(RequestPojo request, ResponsePojo response);

    void handleHeartbeat(RequestPojo request, ResponsePojo response);

    void handleReplicate(RequestPojo request, ResponsePojo response);
}
