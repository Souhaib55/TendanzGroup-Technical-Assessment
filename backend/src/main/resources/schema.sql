-- Zone Table
CREATE TABLE zone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    risk_coefficient DECIMAL(5, 2) NOT NULL
);

-- Product Table
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Pricing Rule Table
CREATE TABLE pricing_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    base_rate DECIMAL(10, 2) NOT NULL,
    age_factor_young DECIMAL(5, 2) NOT NULL,
    age_factor_adult DECIMAL(5, 2) NOT NULL,
    age_factor_senior DECIMAL(5, 2) NOT NULL,
    age_factor_elderly DECIMAL(5, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- Quote Table
CREATE TABLE quote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    zone_id BIGINT NOT NULL,
    client_name VARCHAR(100) NOT NULL,
    client_age INT NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL,
    final_price DECIMAL(10, 2) NOT NULL,
    applied_rules CLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (zone_id) REFERENCES zone(id)
);

-- Quote History Table
CREATE TABLE quote_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quote_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    details CLOB NOT NULL,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quote_id) REFERENCES quote(id)
);

-- Create Indexes
CREATE INDEX idx_quote_product_id ON quote(product_id);
CREATE INDEX idx_quote_zone_id ON quote(zone_id);
CREATE INDEX idx_quote_client_name ON quote(client_name);
CREATE INDEX idx_quote_created_at ON quote(created_at);
CREATE INDEX idx_quote_history_quote_id ON quote_history(quote_id);
CREATE INDEX idx_quote_history_changed_at ON quote_history(changed_at);
CREATE INDEX idx_pricing_rule_product_id ON pricing_rule(product_id);
