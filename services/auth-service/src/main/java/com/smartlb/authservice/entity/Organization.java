package com.smartlb.authservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Domain model representing a multi-tenant corporate Organization.
 */
@Entity
@Table(name = "organizations")
@SQLDelete(sql = "UPDATE organizations SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Organization name is required")
    @Size(min = 3, max = 100, message = "Organization name must be between 3 and 100 characters")
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @NotBlank(message = "Organization slug is required")
    @Size(max = 255, message = "Slug cannot exceed 255 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens only")
    @Column(name = "slug", unique = true, nullable = false)
    private String slug;

    @Email(message = "Invalid company email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Column(name = "company_email")
    private String companyEmail;

    @Size(max = 255, message = "Website URI cannot exceed 255 characters")
    @Column(name = "website")
    private String website;

    @Size(max = 100, message = "Country name cannot exceed 100 characters")
    @Column(name = "country")
    private String country;

    @Size(max = 100, message = "Timezone string cannot exceed 100 characters")
    @Column(name = "timezone")
    private String timezone;

    @Size(max = 50, message = "Subscription type cannot exceed 50 characters")
    @Column(name = "subscription_type")
    @Builder.Default
    private String subscriptionType = "FREE";

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status cannot exceed 50 characters")
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();
}
