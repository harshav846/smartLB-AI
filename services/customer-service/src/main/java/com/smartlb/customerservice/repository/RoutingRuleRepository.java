package com.smartlb.customerservice.repository;

import com.smartlb.customerservice.entity.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {

    List<RoutingRule> findAllByWebsiteIdAndDeletedAtIsNullOrderByPriorityAsc(UUID websiteId);

    Optional<RoutingRule> findByIdAndWebsiteIdAndDeletedAtIsNull(UUID id, UUID websiteId);
}
