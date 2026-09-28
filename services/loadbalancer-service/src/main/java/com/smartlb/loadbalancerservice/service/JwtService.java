package com.smartlb.loadbalancerservice.service;

import java.util.List;
import java.util.UUID;

/**
 * Interface contract for JWT validation and claim extraction in loadbalancer-service.
 */
public interface JwtService {

    boolean validateToken(String token);

    UUID extractUserId(String token);

    UUID extractOrganizationId(String token);

    String extractEmail(String token);

    List<String> extractRoles(String token);

    boolean isTokenExpired(String token);
}
