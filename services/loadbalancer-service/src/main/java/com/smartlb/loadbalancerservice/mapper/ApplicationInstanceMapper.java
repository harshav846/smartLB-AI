package com.smartlb.loadbalancerservice.mapper;

import com.smartlb.loadbalancerservice.dto.response.InstanceHealthResponse;
import com.smartlb.loadbalancerservice.dto.response.InstanceResponse;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper converting between {@link ApplicationInstance} entities and response DTOs.
 */
@Mapper(componentModel = "spring")
public interface ApplicationInstanceMapper {

    InstanceResponse toResponse(ApplicationInstance entity);

    List<InstanceResponse> toResponseList(List<ApplicationInstance> entities);

    @Mapping(target = "instanceId", source = "id")
    InstanceHealthResponse toHealthResponse(ApplicationInstance entity);
}
