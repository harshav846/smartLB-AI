package com.smartlb.loadbalancerservice.algorithm;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * Metadata extracted from an incoming client HTTP request for routing decisions.
 */
@Getter
@Builder
public class ClientRequestMetadata {
    private final UUID organizationId;
    private final String clientIp;
    private final String method;
    private final String path;
    private final Map<String, String> headers;
}
