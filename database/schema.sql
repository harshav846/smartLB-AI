-- =========================================================================
-- SmartLB-AI Database Schema Definition (Phases 1, 2, 3, & 4)
-- =========================================================================
-- Enforces Third Normal Form (3NF), strict Typing, UUID primary keys, 
-- audit tracking, soft deletes, security diagnostics, traffic logging,
-- and AI-driven automated scaling configurations.
-- =========================================================================

-- Enable pgcrypto extension for gen_random_uuid() (standard for older Postgres, built-in post Postgres 13)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================
-- Reusable Components (Triggers & Functions)
-- ==========================================

-- Trigger function to automatically update 'updated_at' columns on row updates.
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ==========================================
-- Table: organizations
-- ==========================================
-- Represents the tenant companies/customers using the platform.
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    company_email VARCHAR(255),
    website VARCHAR(255),
    country VARCHAR(100),
    timezone VARCHAR(100),
    subscription_type VARCHAR(50) DEFAULT 'FREE',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    
    -- Constraints
    CONSTRAINT uq_organizations_name UNIQUE (name),
    CONSTRAINT uq_organizations_slug UNIQUE (slug),
    CONSTRAINT chk_organizations_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- Trigger for organizations updated_at
CREATE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: users
-- ==========================================
-- Stores authentication, profiles, and ties users to an organization (Tenant).
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    profile_image VARCHAR(500),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    last_password_change TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    
    -- Constraints & Foreign Keys
    CONSTRAINT fk_users_organizations FOREIGN KEY (organization_id) 
        REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'PENDING', 'SUSPENDED', 'DELETED'))
);

-- Self-referencing auditing trace after tables are setup
ALTER TABLE organizations ADD CONSTRAINT fk_organizations_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE organizations ADD CONSTRAINT fk_organizations_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL;

-- Trigger for users updated_at
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: roles
-- ==========================================
-- Holds available application capability roles.
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    
    -- Constraints
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT fk_roles_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_roles_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Trigger for roles updated_at
CREATE TRIGGER trg_roles_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: user_roles
-- ==========================================
-- Junction table representing Many-to-Many association between users and roles.
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Composite Primary Key & Foreign Keys
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id)
        REFERENCES roles(id) ON DELETE CASCADE
);

-- ==========================================
-- Table: websites
-- ==========================================
-- Represents the target web properties registered for load balancing.
CREATE TABLE websites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    domain_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    environment VARCHAR(50) NOT NULL DEFAULT 'PRODUCTION',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    verification_token VARCHAR(255),
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    -- Constraints
    CONSTRAINT fk_websites_organizations FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_websites_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_websites_updated_by FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_websites_domain UNIQUE (domain_name),
    CONSTRAINT chk_websites_environment CHECK (environment IN ('DEVELOPMENT', 'STAGING', 'PRODUCTION')),
    CONSTRAINT chk_websites_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_websites_verification CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED'))
);

-- Trigger for websites updated_at
CREATE TRIGGER trg_websites_updated_at
    BEFORE UPDATE ON websites
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: backend_servers
-- ==========================================
-- Represents individual target backend servers mapping traffic behind a website.
CREATE TABLE backend_servers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    website_id UUID NOT NULL,
    server_name VARCHAR(255) NOT NULL,
    private_ip VARCHAR(45) NOT NULL, -- Supports IPv4 and IPv6 lengths
    public_ip VARCHAR(45),
    port INTEGER NOT NULL,
    protocol VARCHAR(10) NOT NULL DEFAULT 'HTTP',
    weight INTEGER NOT NULL DEFAULT 1,
    priority INTEGER NOT NULL DEFAULT 1,
    max_connections INTEGER NOT NULL DEFAULT 1000,
    current_connections INTEGER NOT NULL DEFAULT 0,
    cpu_usage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    memory_usage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    disk_usage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    health_status VARCHAR(50) NOT NULL DEFAULT 'HEALTHY',
    server_status VARCHAR(50) NOT NULL DEFAULT 'ONLINE',
    response_time_ms INTEGER NOT NULL DEFAULT 0,
    last_health_check TIMESTAMP WITH TIME ZONE,
    cpu_cores INTEGER,
    total_memory_gb INTEGER,
    total_storage_gb INTEGER,
    server_tag VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    -- Constraints
    CONSTRAINT fk_backend_servers_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE,
    CONSTRAINT fk_backend_servers_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_backend_servers_updated_by FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_backend_servers_protocol CHECK (protocol IN ('HTTP', 'HTTPS')),
    CONSTRAINT chk_backend_servers_health CHECK (health_status IN ('HEALTHY', 'UNHEALTHY', 'DEGRADED')),
    CONSTRAINT chk_backend_servers_status CHECK (server_status IN ('ONLINE', 'OFFLINE', 'MAINTENANCE')),
    CONSTRAINT chk_backend_servers_port CHECK (port > 0 AND port <= 65535),
    CONSTRAINT chk_backend_servers_weight CHECK (weight > 0),
    CONSTRAINT chk_backend_servers_priority CHECK (priority >= 0)
);

-- Trigger for backend_servers updated_at
CREATE TRIGGER trg_backend_servers_updated_at
    BEFORE UPDATE ON backend_servers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: ssl_certificates
-- ==========================================
-- Represents SSL configuration settings for a website (Nullable One-to-One).
CREATE TABLE ssl_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    website_id UUID NOT NULL,
    certificate_provider VARCHAR(255) NOT NULL,
    issue_date TIMESTAMP WITH TIME ZONE NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    certificate_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    -- Constraints
    CONSTRAINT fk_ssl_certificates_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE,
    CONSTRAINT fk_ssl_certificates_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_ssl_certificates_updated_by FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_ssl_certificates_website UNIQUE (website_id), -- Enforce One-to-One mapping
    CONSTRAINT chk_ssl_certificates_status CHECK (certificate_status IN ('ACTIVE', 'EXPIRING', 'EXPIRED', 'REVOKED'))
);

-- Trigger for ssl_certificates updated_at
CREATE TRIGGER trg_ssl_certificates_updated_at
    BEFORE UPDATE ON ssl_certificates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: load_balancer_configs
-- ==========================================
-- Holds load balancing structural logic parameters for a website (Strict One-to-One).
CREATE TABLE load_balancer_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    website_id UUID NOT NULL,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'ROUND_ROBIN',
    sticky_sessions BOOLEAN NOT NULL DEFAULT FALSE,
    session_timeout INTEGER NOT NULL DEFAULT 3600, -- Seconds
    health_check_interval INTEGER NOT NULL DEFAULT 30, -- Seconds
    request_timeout INTEGER NOT NULL DEFAULT 15, -- Seconds
    retry_attempts INTEGER NOT NULL DEFAULT 3,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    -- Constraints
    CONSTRAINT fk_load_balancer_configs_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE,
    CONSTRAINT fk_load_balancer_configs_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_load_balancer_configs_updated_by FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_load_balancer_configs_website UNIQUE (website_id), -- Enforce One-to-One mapping
    CONSTRAINT chk_lb_algorithm CHECK (algorithm IN ('ROUND_ROBIN', 'LEAST_CONNECTIONS', 'WEIGHTED_ROUND_ROBIN', 'IP_HASH', 'AI_ROUTING')),
    CONSTRAINT chk_lb_session_timeout CHECK (session_timeout >= 0),
    CONSTRAINT chk_lb_health_interval CHECK (health_check_interval > 0),
    CONSTRAINT chk_lb_request_timeout CHECK (request_timeout > 0),
    CONSTRAINT chk_lb_retry_attempts CHECK (retry_attempts >= 0)
);

-- Trigger for load_balancer_configs updated_at
CREATE TRIGGER trg_load_balancer_configs_updated_at
    BEFORE UPDATE ON load_balancer_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: routing_rules
-- ==========================================
-- Represents URL path-routing override settings matching a website context.
CREATE TABLE routing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    website_id UUID NOT NULL,
    path_pattern VARCHAR(255) NOT NULL,
    target_server UUID, -- Target server override
    priority INTEGER NOT NULL DEFAULT 1,
    rule_type VARCHAR(50) NOT NULL DEFAULT 'PATH_BASED',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    -- Constraints
    CONSTRAINT fk_routing_rules_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE,
    CONSTRAINT fk_routing_rules_backend_servers FOREIGN KEY (target_server)
        REFERENCES backend_servers(id) ON DELETE SET NULL,
    CONSTRAINT fk_routing_rules_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_routing_rules_updated_by FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_routing_rules_priority CHECK (priority >= 0),
    CONSTRAINT chk_routing_rules_type CHECK (rule_type IN ('PATH_BASED', 'HEADER_BASED', 'AI_ROUTING'))
);

-- Trigger for routing_rules updated_at
CREATE TRIGGER trg_routing_rules_updated_at
    BEFORE UPDATE ON routing_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =========================================================================
-- Phase 3 Tables (Telemetry, Logging, Alerting & Audit)
-- =========================================================================

-- ==========================================
-- Table: health_checks
-- ==========================================
-- Append-only event history tracker monitoring health of backend servers.
CREATE TABLE health_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    latency_ms INTEGER NOT NULL,
    cpu_usage NUMERIC(5, 2) NOT NULL,
    memory_usage NUMERIC(5, 2) NOT NULL,
    disk_usage NUMERIC(5, 2) NOT NULL,
    health_reason VARCHAR(500),
    checked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_health_checks_servers FOREIGN KEY (server_id)
        REFERENCES backend_servers(id) ON DELETE CASCADE,
    CONSTRAINT chk_health_checks_status CHECK (status IN ('HEALTHY', 'UNHEALTHY', 'DEGRADED'))
);

-- ==========================================
-- Table: request_logs
-- ==========================================
-- High-throughput load balancer execution logs for audit and analysis.
CREATE TABLE request_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    website_id UUID NOT NULL,
    backend_server_id UUID,
    client_ip VARCHAR(45) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    url_path TEXT NOT NULL,
    response_status INTEGER NOT NULL,
    response_time_ms INTEGER NOT NULL,
    load_balancing_algorithm_used VARCHAR(50) NOT NULL,
    request_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_request_logs_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_logs_servers FOREIGN KEY (backend_server_id)
        REFERENCES backend_servers(id) ON DELETE SET NULL
);

-- ==========================================
-- Table: traffic_metrics
-- ==========================================
-- Aggregated operational logging data for graph compilation.
CREATE TABLE traffic_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    website_id UUID NOT NULL,
    requests_per_minute INTEGER NOT NULL DEFAULT 0,
    requests_per_hour INTEGER NOT NULL DEFAULT 0,
    average_latency NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    peak_requests INTEGER NOT NULL DEFAULT 0,
    bandwidth_used BIGINT NOT NULL DEFAULT 0, -- Bytes
    error_rate NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    active_connections INTEGER NOT NULL DEFAULT 0,
    metric_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_traffic_metrics_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE
);

-- ==========================================
-- Table: notifications
-- ==========================================
-- User and Organization notifications.
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id UUID, -- Optional user target (nullable implies org-wide alert)
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    -- Constraints
    CONSTRAINT fk_notifications_organizations FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_users FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_updated_by FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_notifications_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('UNREAD', 'READ', 'ARCHIVED'))
);

-- Trigger for notifications updated_at
CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: audit_logs
-- ==========================================
-- System-wide auditing trace logging changes.
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID,
    organization_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB, -- Unstructured payload details

    -- Constraints
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_logs_organizations FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE
);


-- =========================================================================
-- Phase 4 Tables (AI routing model & Auto-scaling policies)
-- =========================================================================

-- ==========================================
-- Table: model_versions
-- ==========================================
-- Tracks classification framework and telemetry accuracy of AI routing models.
CREATE TABLE model_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_name VARCHAR(150) NOT NULL,
    version VARCHAR(50) NOT NULL,
    framework VARCHAR(100) NOT NULL,
    accuracy NUMERIC(5, 4),
    precision_score NUMERIC(5, 4),
    recall_score NUMERIC(5, 4),
    f1_score NUMERIC(5, 4),
    training_date TIMESTAMP WITH TIME ZONE NOT NULL,
    deployment_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    model_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    -- Constraints
    CONSTRAINT fk_model_versions_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_model_versions_updated_by FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_model_versions_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

-- Trigger for model_versions updated_at
CREATE TRIGGER trg_model_versions_updated_at
    BEFORE UPDATE ON model_versions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: ai_predictions
-- ==========================================
-- Holds AI traffic load and routing recommendation predictions.
CREATE TABLE ai_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    website_id UUID NOT NULL,
    prediction_type VARCHAR(100) NOT NULL,
    predicted_requests INTEGER NOT NULL DEFAULT 0,
    predicted_cpu_usage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    predicted_memory_usage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    predicted_response_time INTEGER NOT NULL DEFAULT 0,
    confidence_score NUMERIC(5, 4) NOT NULL,
    recommended_action VARCHAR(255) NOT NULL,
    model_version_id UUID,
    prediction_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    -- Constraints
    CONSTRAINT fk_ai_predictions_organizations FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_predictions_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_predictions_models FOREIGN KEY (model_version_id)
        REFERENCES model_versions(id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_predictions_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_predictions_updated_by FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_confidence_score CHECK (confidence_score >= 0.0000 AND confidence_score <= 1.0000)
);

-- Trigger for ai_predictions updated_at
CREATE TRIGGER trg_ai_predictions_updated_at
    BEFORE UPDATE ON ai_predictions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: ai_training_data
-- ==========================================
-- Append-only time-series data supplying parameters for training and grading ML models.
CREATE TABLE ai_training_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    website_id UUID NOT NULL,
    backend_server_id UUID NOT NULL,
    requests_per_minute INTEGER NOT NULL DEFAULT 0,
    cpu_usage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    memory_usage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    disk_usage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    active_connections INTEGER NOT NULL DEFAULT 0,
    average_latency NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    error_rate NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    bandwidth_used BIGINT NOT NULL DEFAULT 0,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_ai_training_data_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_training_data_servers FOREIGN KEY (backend_server_id)
        REFERENCES backend_servers(id) ON DELETE CASCADE
);

-- ==========================================
-- Table: scaling_policies
-- ==========================================
-- Policy constraints controlling dynamic server spawning logic.
CREATE TABLE scaling_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    website_id UUID NOT NULL,
    policy_name VARCHAR(150) NOT NULL,
    min_servers INTEGER NOT NULL DEFAULT 1,
    max_servers INTEGER NOT NULL DEFAULT 5,
    cpu_threshold NUMERIC(5, 2),
    memory_threshold NUMERIC(5, 2),
    latency_threshold INTEGER,
    requests_threshold INTEGER,
    cooldown_period_seconds INTEGER NOT NULL DEFAULT 300,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    -- Constraints
    CONSTRAINT fk_scaling_policies_organizations FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_scaling_policies_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE,
    CONSTRAINT fk_scaling_policies_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_scaling_policies_updated_by FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_scaling_policies_min CHECK (min_servers >= 1),
    CONSTRAINT chk_scaling_policies_max CHECK (max_servers >= min_servers),
    CONSTRAINT chk_scaling_policies_cpu CHECK (cpu_threshold >= 0.00 AND cpu_threshold <= 100.00),
    CONSTRAINT chk_scaling_policies_memory CHECK (memory_threshold >= 0.00 AND memory_threshold <= 100.00),
    CONSTRAINT chk_scaling_policies_cooldown CHECK (cooldown_period_seconds >= 0)
);

-- Trigger for scaling_policies updated_at
CREATE TRIGGER trg_scaling_policies_updated_at
    BEFORE UPDATE ON scaling_policies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Table: scaling_events
-- ==========================================
-- Chronological event logging table tracking scale adjustments executions.
CREATE TABLE scaling_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL,
    website_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    previous_server_count INTEGER NOT NULL,
    new_server_count INTEGER NOT NULL,
    trigger_reason VARCHAR(500) NOT NULL,
    execution_status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    execution_time_ms INTEGER NOT NULL DEFAULT 0,
    event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_scaling_events_policies FOREIGN KEY (policy_id)
        REFERENCES scaling_policies(id) ON DELETE CASCADE,
    CONSTRAINT fk_scaling_events_websites FOREIGN KEY (website_id)
        REFERENCES websites(id) ON DELETE CASCADE,
    CONSTRAINT chk_scaling_events_type CHECK (event_type IN ('SCALE_UP', 'SCALE_DOWN')),
    CONSTRAINT chk_scaling_events_status CHECK (execution_status IN ('SUCCESS', 'FAILED', 'PENDING')),
    CONSTRAINT chk_scaling_events_prev CHECK (previous_server_count >= 0),
    CONSTRAINT chk_scaling_events_new CHECK (new_server_count >= 0)
);


-- ==========================================
-- Indexes for Optimization (Phases 1-4)
-- ==========================================

-- Index for lowercase unique email searches (efficient case-insensitive lookup)
CREATE UNIQUE INDEX idx_users_email_lower ON users (LOWER(email)) WHERE deleted_at IS NULL;

-- Indexes supporting Soft delete performance (filter active assets quickly)
CREATE INDEX idx_organizations_deleted_at ON organizations (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_deleted_at ON users (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_websites_deleted_at ON websites (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_backend_servers_deleted_at ON backend_servers (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_ssl_certificates_deleted_at ON ssl_certificates (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_load_balancer_configs_deleted_at ON load_balancer_configs (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_routing_rules_deleted_at ON routing_rules (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_notifications_deleted_at ON notifications (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_model_versions_deleted_at ON model_versions (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_ai_predictions_deleted_at ON ai_predictions (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_scaling_policies_deleted_at ON scaling_policies (deleted_at) WHERE deleted_at IS NULL;

-- Foreign Key indexing for joins
CREATE INDEX idx_users_organization_id ON users (organization_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
CREATE INDEX idx_websites_organization_id ON websites (organization_id);
CREATE INDEX idx_backend_servers_website_id ON backend_servers (website_id);
CREATE INDEX idx_routing_rules_website_id ON routing_rules (website_id);
CREATE INDEX idx_routing_rules_target_server ON routing_rules (target_server);

-- Telemetry / Monitoring Indexes
CREATE INDEX idx_health_checks_server_id ON health_checks (server_id);
CREATE INDEX idx_health_checks_checked_at ON health_checks (checked_at DESC);
CREATE INDEX idx_request_logs_website_id ON request_logs (website_id);
CREATE INDEX idx_request_logs_server_id ON request_logs (backend_server_id);
CREATE INDEX idx_request_logs_timestamp ON request_logs (request_timestamp DESC);
CREATE INDEX idx_traffic_metrics_website_time ON traffic_metrics (website_id, metric_timestamp DESC);

-- Alerts & Audit Indexes
CREATE INDEX idx_notifications_org_user ON notifications (organization_id, user_id, status);
CREATE INDEX idx_audit_logs_org_actor ON audit_logs (organization_id, actor_user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs (timestamp DESC);

-- AI Routing & Auto-scaling Indexes
CREATE INDEX idx_ai_predictions_web_time ON ai_predictions (website_id, prediction_timestamp DESC);
CREATE INDEX idx_ai_training_data_web_srv ON ai_training_data (website_id, backend_server_id, recorded_at DESC);
CREATE INDEX idx_scaling_policies_web ON scaling_policies (website_id);
CREATE INDEX idx_scaling_events_policy_time ON scaling_events (policy_id, event_timestamp DESC);
CREATE INDEX idx_model_versions_name_ver ON model_versions (model_name, version);

-- Filter/Status efficiency indexes
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_organizations_status ON organizations (status);
CREATE INDEX idx_websites_status ON websites (status);
CREATE INDEX idx_backend_servers_health_status ON backend_servers (health_status);
CREATE INDEX idx_backend_servers_server_status ON backend_servers (server_status);

-- ==========================================
-- Reference Data Seeding
-- ==========================================
INSERT INTO roles (id, name, description) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN', 'Platform-level administrator with full access to all organizations.'),
    (gen_random_uuid(), 'ORG_ADMIN', 'Organization-level administrator with full access to their own organization resources.'),
    (gen_random_uuid(), 'OPERATOR', 'Operations operator authorized to configure load balancers, scale policies, and view metrics.'),
    (gen_random_uuid(), 'VIEWER', 'ReadOnly portal user who can only view load balancer setup, status reports, and monitoring metrics.');
