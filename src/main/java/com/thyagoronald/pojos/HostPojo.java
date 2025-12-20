package com.thyagoronald.pojos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class HostPojo {
    private String host;
    private int port;
    private boolean isAlive;
}
