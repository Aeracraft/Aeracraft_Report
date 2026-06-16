-- AeracraftReport Database Schema

-- Main reports table
CREATE TABLE IF NOT EXISTS aeracraft_reports (
    id VARCHAR(36) PRIMARY KEY,
    reporter VARCHAR(255) NOT NULL,
    target VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    evidence_hash VARCHAR(64),
    linked_ban_id VARCHAR(255),
    server_name VARCHAR(100),
    handled_by VARCHAR(255),
    resolution_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Evidence table
CREATE TABLE IF NOT EXISTS aeracraft_evidence (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id VARCHAR(36) NOT NULL,
    evidence_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (report_id) REFERENCES aeracraft_reports(id) ON DELETE CASCADE
);

-- Audit log table
CREATE TABLE IF NOT EXISTS aeracraft_audit_log (
    id VARCHAR(36) PRIMARY KEY,
    report_id VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL,
    operator VARCHAR(255) NOT NULL,
    operator_ip VARCHAR(45),
    before_snapshot TEXT,
    after_snapshot TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Player language preferences table
CREATE TABLE IF NOT EXISTS aeracraft_player_language (
    player_uuid VARCHAR(36) PRIMARY KEY,
    player_name VARCHAR(255) NOT NULL,
    language_code VARCHAR(10) NOT NULL DEFAULT 'zh_CN',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Player points table
CREATE TABLE IF NOT EXISTS aeracraft_player_points (
    id VARCHAR(36) PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL UNIQUE,
    player_name VARCHAR(255) NOT NULL,
    points INTEGER NOT NULL DEFAULT 0,
    total_valid_reports INTEGER NOT NULL DEFAULT 0,
    total_reports INTEGER NOT NULL DEFAULT 0,
    last_decay_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_status_created ON aeracraft_reports(status, created_at);
CREATE INDEX IF NOT EXISTS idx_target ON aeracraft_reports(target);
CREATE INDEX IF NOT EXISTS idx_reporter ON aeracraft_reports(reporter);
CREATE INDEX IF NOT EXISTS idx_audit_report ON aeracraft_audit_log(report_id);
CREATE INDEX IF NOT EXISTS idx_audit_operator ON aeracraft_audit_log(operator);
CREATE INDEX IF NOT EXISTS idx_points_player ON aeracraft_player_points(player_uuid);
