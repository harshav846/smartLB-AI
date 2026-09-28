package com.smartlb.loadbalancerservice.repository;

import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link ApplicationInstance} entities.
 * Enforces tenant isolation by requiring organizationId on data access methods.
 */
@Repository
public interface ApplicationInstanceRepository extends JpaRepository<ApplicationInstance, UUID> {

    Optional<ApplicationInstance> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    List<ApplicationInstance> findAllByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    List<ApplicationInstance> findAllByOrganizationIdAndStatusAndDeletedAtIsNull(UUID organizationId, InstanceStatus status);

    @Query("SELECT a FROM ApplicationInstance a WHERE a.organizationId = :organizationId " +
           "AND a.status = 'ACTIVE' AND a.healthStatus = 'HEALTHY' AND a.deletedAt IS NULL")
    List<ApplicationInstance> findHealthyInstancesByOrganizationId(@Param("organizationId") UUID organizationId);

    boolean existsByNameAndOrganizationIdAndDeletedAtIsNull(String name, UUID organizationId);

    boolean existsByHostAndPortAndOrganizationIdAndDeletedAtIsNull(String host, Integer port, UUID organizationId);

    @Query("SELECT a FROM ApplicationInstance a WHERE a.status = 'ACTIVE' AND a.healthStatus = 'HEALTHY' AND a.deletedAt IS NULL")
    List<ApplicationInstance> findAllHealthyInstances();

    List<ApplicationInstance> findAllByStatusAndDeletedAtIsNull(InstanceStatus status);
}
