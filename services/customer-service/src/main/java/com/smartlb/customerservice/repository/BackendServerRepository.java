package com.smartlb.customerservice.repository;

import com.smartlb.customerservice.entity.BackendServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackendServerRepository extends JpaRepository<BackendServer, UUID> {

    Optional<BackendServer> findByIdAndWebsiteIdAndDeletedAtIsNull(UUID id, UUID websiteId);

    List<BackendServer> findAllByWebsiteIdAndDeletedAtIsNull(UUID websiteId);

    boolean existsByWebsiteIdAndServerNameAndDeletedAtIsNull(UUID websiteId, String serverName);

    boolean existsByWebsiteIdAndPrivateIpAndPortAndDeletedAtIsNull(UUID websiteId, String privateIp, Integer port);

    List<BackendServer> findAllByWebsiteIdAndHealthStatusAndServerStatusAndDeletedAtIsNull(
            UUID websiteId, String healthStatus, String serverStatus);
}
