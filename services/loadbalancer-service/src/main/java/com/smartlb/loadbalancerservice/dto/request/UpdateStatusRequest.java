package com.smartlb.loadbalancerservice.dto.request;

import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for updating instance status (ACTIVE, INACTIVE, DRAINING, DECOMMISSIONED).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private InstanceStatus status;
}
