package com.smartlb.loadbalancerservice.algorithm;

import com.smartlb.loadbalancerservice.algorithm.impl.IpHashStrategy;
import com.smartlb.loadbalancerservice.algorithm.impl.LeastConnectionsStrategy;
import com.smartlb.loadbalancerservice.algorithm.impl.RoundRobinStrategy;
import com.smartlb.loadbalancerservice.algorithm.impl.WeightedRoundRobinStrategy;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for all built-in load balancing strategies.
 */
class LoadBalancingStrategyTest {

    private final UUID ORG_ID = UUID.randomUUID();

    /* ── helpers ──────────────────────────────────────────────────────── */

    private ApplicationInstance instance(String name, int weight, int connections) {
        return ApplicationInstance.builder()
                .organizationId(ORG_ID)
                .name(name)
                .host("10.0.0." + name)
                .port(8080)
                .baseUrl("http://10.0.0." + name + ":8080")
                .status(InstanceStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .weight(weight)
                .currentConnections(connections)
                .consecutiveFailures(0)
                .build();
    }

    private ClientRequestMetadata metadata(String clientIp) {
        return ClientRequestMetadata.builder()
                .organizationId(ORG_ID)
                .clientIp(clientIp)
                .method("GET")
                .path("/test")
                .headers(Map.of())
                .build();
    }

    /* ── Round Robin ──────────────────────────────────────────────────── */

    @Test
    void roundRobin_distributesAcrossAllInstances() {
        RoundRobinStrategy strategy = new RoundRobinStrategy();
        ApplicationInstance a = instance("A", 1, 0);
        ApplicationInstance b = instance("B", 1, 0);
        ApplicationInstance c = instance("C", 1, 0);
        List<ApplicationInstance> pool = List.of(a, b, c);

        // After 3 calls each instance should appear at least once in 6 calls
        long countA = 0, countB = 0, countC = 0;
        for (int i = 0; i < 6; i++) {
            ApplicationInstance selected = strategy.selectInstance(pool, metadata("1.2.3.4"));
            if (selected == a) countA++;
            else if (selected == b) countB++;
            else countC++;
        }
        assertThat(countA).isGreaterThan(0);
        assertThat(countB).isGreaterThan(0);
        assertThat(countC).isGreaterThan(0);
    }

    @Test
    void roundRobin_singleInstance_alwaysReturnsSame() {
        RoundRobinStrategy strategy = new RoundRobinStrategy();
        ApplicationInstance only = instance("only", 1, 0);
        for (int i = 0; i < 5; i++) {
            assertThat(strategy.selectInstance(List.of(only), metadata("x"))).isSameAs(only);
        }
    }

    /* ── Least Connections ────────────────────────────────────────────── */

    @Test
    void leastConnections_selectsInstanceWithFewestConnections() {
        LeastConnectionsStrategy strategy = new LeastConnectionsStrategy();
        ApplicationInstance busy   = instance("busy",   1, 10);
        ApplicationInstance idle   = instance("idle",   1,  0);
        ApplicationInstance medium = instance("medium", 1,  5);

        ApplicationInstance selected = strategy.selectInstance(
                List.of(busy, idle, medium), metadata("x"));

        assertThat(selected).isSameAs(idle);
    }

    /* ── IP Hash ──────────────────────────────────────────────────────── */

    @Test
    void ipHash_sameIpAlwaysMapsToSameInstance() {
        IpHashStrategy strategy = new IpHashStrategy();
        List<ApplicationInstance> pool = List.of(
                instance("A", 1, 0),
                instance("B", 1, 0),
                instance("C", 1, 0)
        );

        String ip = "192.168.1.100";
        ApplicationInstance first = strategy.selectInstance(pool, metadata(ip));

        for (int i = 0; i < 10; i++) {
            assertThat(strategy.selectInstance(pool, metadata(ip))).isSameAs(first);
        }
    }

    @Test
    void ipHash_differentIpsMayProduceDifferentInstances() {
        IpHashStrategy strategy = new IpHashStrategy();
        List<ApplicationInstance> pool = List.of(
                instance("A", 1, 0),
                instance("B", 1, 0),
                instance("C", 1, 0)
        );

        // Try 20 different IPs; expect at least 2 different instances to be chosen
        long distinctInstances = List.of(
                "10.0.0.1", "10.0.0.2", "10.0.0.3", "10.0.0.4",
                "192.168.100.1", "172.16.0.5", "8.8.8.8", "1.1.1.1",
                "203.0.113.1", "198.51.100.7"
        ).stream()
                .map(ip -> strategy.selectInstance(pool, metadata(ip)))
                .distinct()
                .count();

        assertThat(distinctInstances).isGreaterThanOrEqualTo(2);
    }

    /* ── Weighted Round Robin ─────────────────────────────────────────── */

    @Test
    void weightedRoundRobin_higherWeightInstanceSelectedMoreOften() {
        WeightedRoundRobinStrategy strategy = new WeightedRoundRobinStrategy();
        ApplicationInstance light  = instance("light",  1, 0);
        ApplicationInstance heavy  = instance("heavy",  5, 0);
        List<ApplicationInstance> pool = List.of(light, heavy);

        int lightCount = 0, heavyCount = 0;
        for (int i = 0; i < 60; i++) {
            ApplicationInstance selected = strategy.selectInstance(pool, metadata("ip"));
            if (selected == light) lightCount++;
            else heavyCount++;
        }
        // heavy should be chosen roughly 5x more often than light
        assertThat(heavyCount).isGreaterThan(lightCount);
    }

    /* ── Factory ──────────────────────────────────────────────────────── */

    @Test
    void factory_nullAlgorithm_returnsRoundRobin() {
        RoundRobinStrategy rrStrategy = new RoundRobinStrategy();
        LoadBalancingStrategyFactory factory = new LoadBalancingStrategyFactory(List.of(rrStrategy));

        LoadBalancingStrategy strategy = factory.getStrategy(null);
        assertThat(strategy.getAlgorithm()).isEqualTo(LoadBalancingAlgorithm.ROUND_ROBIN);
    }

    @Test
    void factory_unknownAlgorithm_returnsRoundRobin() {
        RoundRobinStrategy rrStrategy = new RoundRobinStrategy();
        LoadBalancingStrategyFactory factory = new LoadBalancingStrategyFactory(List.of(rrStrategy));

        // Request AI_ASSISTED which is not registered → should fall back to ROUND_ROBIN
        LoadBalancingStrategy strategy = factory.getStrategy(LoadBalancingAlgorithm.AI_ASSISTED);
        assertThat(strategy.getAlgorithm()).isEqualTo(LoadBalancingAlgorithm.ROUND_ROBIN);
    }
}
