package com.smartlb.loadbalancerservice.algorithm;

import com.smartlb.loadbalancerservice.entity.ApplicationInstance;

import java.util.List;

/**
 * Strategy interface for selecting an application instance from a list of eligible candidates.
 */
public interface LoadBalancingStrategy {

    /**
     * Selects an instance from the list of healthy candidate instances.
     *
     * @param instances non-empty list of available healthy instances
     * @param metadata client request context
     * @return the selected ApplicationInstance
     */
    ApplicationInstance selectInstance(List<ApplicationInstance> instances, ClientRequestMetadata metadata);

    /**
     * Identifies which algorithm this strategy implements.
     */
    LoadBalancingAlgorithm getAlgorithm();
}
