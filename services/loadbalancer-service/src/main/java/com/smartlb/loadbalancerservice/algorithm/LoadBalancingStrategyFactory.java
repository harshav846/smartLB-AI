package com.smartlb.loadbalancerservice.algorithm;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy factory providing lookup for the configured or requested load balancing algorithm.
 */
@Component
public class LoadBalancingStrategyFactory {

    private final Map<LoadBalancingAlgorithm, LoadBalancingStrategy> strategyMap = new EnumMap<>(LoadBalancingAlgorithm.class);

    public LoadBalancingStrategyFactory(List<LoadBalancingStrategy> strategies) {
        for (LoadBalancingStrategy strategy : strategies) {
            strategyMap.put(strategy.getAlgorithm(), strategy);
        }
    }

    public LoadBalancingStrategy getStrategy(LoadBalancingAlgorithm algorithm) {
        LoadBalancingStrategy strategy = strategyMap.get(algorithm != null ? algorithm : LoadBalancingAlgorithm.ROUND_ROBIN);
        if (strategy == null) {
            return strategyMap.get(LoadBalancingAlgorithm.ROUND_ROBIN);
        }
        return strategy;
    }
}
