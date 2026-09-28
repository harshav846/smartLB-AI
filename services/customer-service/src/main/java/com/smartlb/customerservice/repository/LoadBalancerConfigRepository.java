package com.smartlb.customerservice.repository;

import com.smartlb.customerservice.entity.LoadBalancerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoadBalancerConfigRepository extends JpaRepository<LoadBalancerConfig, UUID> {

    Optional<LoadBalancerConfig> findByWebsiteIdAndDeletedAtIsNull(UUID websiteId);
}
