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
 * Weighted Round Robin selection proportional to each instance's configured weight.
 */
@Component
public class WeightedRoundRobinStrategy implements LoadBalancingStrategy {

    private final Map<UUID, AtomicInteger> tenantCounters = new ConcurrentHashMap<>();

    @Override
    public ApplicationInstance selectInstance(List<ApplicationInstance> instances, ClientRequestMetadata metadata) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        int totalWeight = instances.stream()
                .mapToInt(i -> Math.max(1, i.getWeight() != null ? i.getWeight() : 1))
                .sum();

        UUID orgId = metadata != null && metadata.getOrganizationId() != null
                ? metadata.getOrganizationId()
                : instances.get(0).getOrganizationId();

        AtomicInteger counter = tenantCounters.computeIfAbsent(orgId, k -> new AtomicInteger(0));
        int targetTicket = Math.abs(counter.getAndIncrement() % totalWeight);

        int cumulative = 0;
        for (ApplicationInstance instance : instances) {
            int w = Math.max(1, instance.getWeight() != null ? instance.getWeight() : 1);
            cumulative += w;
            if (targetTicket < cumulative) {
                return instance;
            }
        }

        return instances.get(0);
    }

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.WEIGHTED_ROUND_ROBIN;
    }
}
