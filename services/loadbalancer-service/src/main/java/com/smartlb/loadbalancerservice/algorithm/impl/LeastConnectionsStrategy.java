package com.smartlb.loadbalancerservice.algorithm.impl;

import com.smartlb.loadbalancerservice.algorithm.ClientRequestMetadata;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategy;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Least Connections strategy: routes to the server handling the smallest number of active concurrent requests.
 */
@Component
public class LeastConnectionsStrategy implements LoadBalancingStrategy {

    @Override
    public ApplicationInstance selectInstance(List<ApplicationInstance> instances, ClientRequestMetadata metadata) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        return instances.stream()
                .min(Comparator.comparingInt(i -> i.getCurrentConnections() != null ? i.getCurrentConnections() : 0))
                .orElse(instances.get(0));
    }

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.LEAST_CONNECTIONS;
    }
}
