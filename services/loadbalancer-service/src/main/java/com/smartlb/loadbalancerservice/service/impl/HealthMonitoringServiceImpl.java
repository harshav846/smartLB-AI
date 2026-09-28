package com.smartlb.loadbalancerservice.service.impl;

import com.smartlb.loadbalancerservice.dto.response.InstanceHealthResponse;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import com.smartlb.loadbalancerservice.exception.ResourceNotFoundException;
import com.smartlb.loadbalancerservice.mapper.ApplicationInstanceMapper;
import com.smartlb.loadbalancerservice.repository.ApplicationInstanceRepository;
import com.smartlb.loadbalancerservice.service.HealthMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link HealthMonitoringService} executing scheduled background health checks,
 * tracking failure thresholds, managing health status transitions, and handling errors gracefully.
 */
@Slf4j
@Service
public class HealthMonitoringServiceImpl implements HealthMonitoringService {

    private final ApplicationInstanceRepository instanceRepository;
    private final ApplicationInstanceMapper instanceMapper;
    private final RestTemplate restTemplate;
    private final int failureThreshold;
    private final int timeoutMs;
    private final boolean healthCheckEnabled;

    @Autowired
    public HealthMonitoringServiceImpl(
            ApplicationInstanceRepository instanceRepository,
            ApplicationInstanceMapper instanceMapper,
            @Value("${app.health-check.failure-threshold:3}") int failureThreshold,
            @Value("${app.health-check.timeout-ms:3000}") int timeoutMs,
            @Value("${app.health-check.enabled:true}") boolean healthCheckEnabled) {
        this(instanceRepository, instanceMapper, createRestTemplate(timeoutMs), failureThreshold, timeoutMs, healthCheckEnabled);
    }

    public HealthMonitoringServiceImpl(
            ApplicationInstanceRepository instanceRepository,
            ApplicationInstanceMapper instanceMapper,
            RestTemplate restTemplate,
            int failureThreshold,
            int timeoutMs,
            boolean healthCheckEnabled) {
        this.instanceRepository = instanceRepository;
        this.instanceMapper = instanceMapper;
        this.restTemplate = restTemplate;
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
        this.healthCheckEnabled = healthCheckEnabled;
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    @Override
    @Scheduled(fixedRateString = "${app.health-check.interval-ms:30000}")
    @Transactional
    public void performScheduledHealthChecks() {
        if (!healthCheckEnabled) {
            log.debug("Health monitoring checks are disabled via configuration.");
            return;
        }

        log.debug("Executing scheduled health checks for active application instances...");
        List<ApplicationInstance> activeInstances = instanceRepository.findAllByStatusAndDeletedAtIsNull(InstanceStatus.ACTIVE);

        for (ApplicationInstance instance : activeInstances) {
            checkAndEvaluateInstanceHealth(instance);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InstanceHealthResponse getSingleInstanceHealth(UUID organizationId, UUID instanceId) {
        ApplicationInstance instance = instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application instance with ID " + instanceId + " not found"));
        return instanceMapper.toHealthResponse(instance);
    }

    private void checkAndEvaluateInstanceHealth(ApplicationInstance instance) {
        String healthUrl = buildHealthUrl(instance);
        log.debug("Pinging health endpoint for instance '{}' (ID: {}) at {}", instance.getName(), instance.getId(), healthUrl);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                handleHealthCheckSuccess(instance);
            } else {
                handleHealthCheckFailure(instance, "HTTP status " + response.getStatusCode().value());
            }
        } catch (Exception ex) {
            handleHealthCheckFailure(instance, ex.getMessage());
        }
    }

    private void handleHealthCheckSuccess(ApplicationInstance instance) {
        log.debug("Health check succeeded for instance '{}' (ID: {})", instance.getName(), instance.getId());
        instance.setConsecutiveFailures(0);
        instance.setLastHealthCheck(Instant.now());
        instance.setHealthStatus(HealthStatus.HEALTHY);
        instanceRepository.save(instance);
    }

    private void handleHealthCheckFailure(ApplicationInstance instance, String reason) {
        int currentFailures = instance.getConsecutiveFailures() + 1;
        instance.setConsecutiveFailures(currentFailures);
        instance.setLastHealthCheck(Instant.now());

        if (currentFailures >= failureThreshold) {
            log.warn("Instance '{}' (ID: {}) reached failure threshold ({}/{}). Marking UNHEALTHY. Reason: {}",
                    instance.getName(), instance.getId(), currentFailures, failureThreshold, reason);
            instance.setHealthStatus(HealthStatus.UNHEALTHY);
        } else {
            log.info("Instance '{}' (ID: {}) failed health check ({}/{}). Reason: {}",
                    instance.getName(), instance.getId(), currentFailures, failureThreshold, reason);
        }

        instanceRepository.save(instance);
    }

    private String buildHealthUrl(ApplicationInstance instance) {
        String baseUrl = instance.getBaseUrl();
        String endpoint = instance.getHealthCheckEndpoint();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }

        return baseUrl + endpoint;
    }
}
