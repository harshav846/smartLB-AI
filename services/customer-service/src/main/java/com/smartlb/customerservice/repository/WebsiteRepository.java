package com.smartlb.customerservice.repository;

import com.smartlb.customerservice.entity.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebsiteRepository extends JpaRepository<Website, UUID> {

    Optional<Website> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    List<Website> findAllByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    boolean existsByDomainNameAndDeletedAtIsNull(String domainName);

    Optional<Website> findByDomainNameAndDeletedAtIsNull(String domainName);
}
