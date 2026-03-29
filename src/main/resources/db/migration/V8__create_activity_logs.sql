CREATE TABLE activity_logs (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    action      VARCHAR(100)  NOT NULL,
    entity_type VARCHAR(100)  NOT NULL,
    entity_id   BIGINT,
    username    VARCHAR(255)  NOT NULL,
    description VARCHAR(1000),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_logs_username    ON activity_logs(username);
CREATE INDEX idx_activity_logs_entity      ON activity_logs(entity_type, entity_id);
CREATE INDEX idx_activity_logs_created_at  ON activity_logs(created_at);
