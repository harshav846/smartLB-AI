package com.smartlb.loadbalancerservice.algorithm.impl;

import com.smartlb.loadbalancerservice.algorithm.ClientRequestMetadata;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategy;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * IP Hash strategy: computes a consistent hash of the client's IP to provide sticky sessions.
 */
@Component
public class IpHashStrategy implements LoadBalancingStrategy {

    @Override
    public ApplicationInstance selectInstance(List<ApplicationInstance> instances, ClientRequestMetadata metadata) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        String clientIp = (metadata != null && metadata.getClientIp() != null)
                ? metadata.getClientIp()
                : "127.0.0.1";

        int hash = clientIp.hashCode();
        int index = Math.abs(hash % instances.size());
        return instances.get(index);
    }

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.IP_HASH;
    }
}
