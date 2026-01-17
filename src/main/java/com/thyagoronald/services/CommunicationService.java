package com.thyagoronald.services;

import com.thyagoronald.pojos.RequestPojo;
import com.thyagoronald.pojos.ResponsePojo;

public interface CommunicationService {
    void sendHeartbeat();

    void handleHeartbeat(RequestPojo request, ResponsePojo response);

    void handleReplicate(RequestPojo request, ResponsePojo response);
}
