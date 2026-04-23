ALTER TABLE battery_record
ADD COLUMN IF NOT EXISTS capacity_retention_rate DECIMAL(5,2) COMMENT '容量保持率(%)',
ADD COLUMN IF NOT EXISTS internal_resistance_ratio DECIMAL(5,2) COMMENT '内阻增加比例',
ADD COLUMN IF NOT EXISTS cycle_count INT COMMENT '循环次数',
ADD COLUMN IF NOT EXISTS avg_temperature DECIMAL(5,2) COMMENT '平均温度(℃)';

CREATE TABLE IF NOT EXISTS health_assessment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    battery_id BIGINT NOT NULL,
    health_score INT COMMENT '最终健康分数0-100',
    health_level VARCHAR(20),
    rule_score INT,
    ml_score INT,
    suggested_scene VARCHAR(100),
    trend_data JSON COMMENT '容量衰减趋势 [{month, retention}]',
    llm_summary TEXT,
    assessment_time DATETIME,
    is_ml_enhanced BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (battery_id) REFERENCES battery_record(id)
);
