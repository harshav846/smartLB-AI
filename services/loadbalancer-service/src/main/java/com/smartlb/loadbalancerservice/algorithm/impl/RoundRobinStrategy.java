package com.smartlb.loadbalancerservice.algorithm.impl;

import com.smartlb.loadbalancerservice.algorithm.ClientRequestMetadata;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategy;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standard thread-safe Round Robin load balancing strategy with per-tenant sequencing.
 */
@Component
public class RoundRobinStrategy implements LoadBalancingStrategy {

    private final Map<UUID, AtomicInteger> tenantCounters = new ConcurrentHashMap<>();

    @Override
    public ApplicationInstance selectInstance(List<ApplicationInstance> instances, ClientRequestMetadata metadata) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        UUID orgId = metadata != null && metadata.getOrganizationId() != null
                ? metadata.getOrganizationId()
                : instances.get(0).getOrganizationId();

        AtomicInteger counter = tenantCounters.computeIfAbsent(orgId, k -> new AtomicInteger(0));
        int index = Math.abs(counter.getAndIncrement() % instances.size());
        return instances.get(index);
    }

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.ROUND_ROBIN;
    }
}
