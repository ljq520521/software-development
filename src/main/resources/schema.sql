-- MySQL 8.4+; additive initialization, never drops existing data.
CREATE TABLE IF NOT EXISTS admin_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `email` VARCHAR(191) NULL,
  `password_hash` VARCHAR(500) NULL,
  `display_name` VARCHAR(500) NULL,
  `status` VARCHAR(500) NULL,
  UNIQUE KEY uk_admin_email (email),
  KEY ix_admin_user_status (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS category (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `name` VARCHAR(500) NULL,
  `slug` VARCHAR(191) NULL,
  `description` VARCHAR(500) NULL,
  `enabled` BOOLEAN NULL,
  `sort_order` BIGINT NULL,
  UNIQUE KEY uk_category_slug (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `name` VARCHAR(500) NULL,
  `slug` VARCHAR(191) NULL,
  `sku` VARCHAR(191) NULL,
  `category_id` BIGINT NULL,
  `price_cents` BIGINT NOT NULL DEFAULT 9900,
  `currency` CHAR(3) NOT NULL DEFAULT 'CNY',
  `short_description` VARCHAR(500) NULL,
  `description_markdown` TEXT NULL,
  `age_min` BIGINT NULL,
  `age_max` BIGINT NULL,
  `environments` JSON NULL,
  `features` JSON NULL,
  `specifications` JSON NULL,
  `images` JSON NULL,
  `featured` BOOLEAN NULL,
  `seo` JSON NULL,
  `status` VARCHAR(500) NULL,
  `first_published_at` DATETIME(3) NULL,
  UNIQUE KEY uk_product_slug (`slug`),
  UNIQUE KEY uk_product_sku (`sku`),
  KEY ix_product_status (status,created_at),
  CONSTRAINT fk_product_category FOREIGN KEY(category_id) REFERENCES category(id),
  CHECK(age_min BETWEEN 0 AND 99 AND age_max BETWEEN age_min AND 99),
  CHECK(price_cents BETWEEN 1 AND 99999999),
  CHECK(currency IN ('CNY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS content (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `type` VARCHAR(500) NULL,
  `slug` VARCHAR(191) NULL,
  `title` VARCHAR(500) NULL,
  `excerpt` VARCHAR(500) NULL,
  `body_markdown` TEXT NULL,
  `cover` JSON NULL,
  `seo` JSON NULL,
  `status` VARCHAR(500) NULL,
  `first_published_at` DATETIME(3) NULL,
  `is_system` BOOLEAN NULL,
  UNIQUE KEY uk_content_slug (`slug`),
  KEY ix_content_status (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS faq (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `question` VARCHAR(500) NULL,
  `answer` TEXT NULL,
  `group_name` VARCHAR(500) NULL,
  `enabled` BOOLEAN NULL,
  `sort_order` BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS site_settings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `brand_name` VARCHAR(500) NULL,
  `tagline` VARCHAR(500) NULL,
  `contact_email` VARCHAR(500) NULL,
  `contact_phone` VARCHAR(500) NULL,
  `privacy_version` VARCHAR(500) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS home_config (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `section_order` JSON NULL,
  `enabled_sections` JSON NULL,
  `hero` JSON NULL,
  `featured_product_ids` JSON NULL,
  `dealer_cta` JSON NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS media_asset (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `url` VARCHAR(500) NULL,
  `mime_type` VARCHAR(500) NULL,
  `byte_size` BIGINT NULL,
  `width` BIGINT NULL,
  `height` BIGINT NULL,
  `original_name` VARCHAR(500) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contact_inquiry (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `name` VARCHAR(500) NULL,
  `email` VARCHAR(191) NULL,
  `country` VARCHAR(500) NULL,
  `type` VARCHAR(500) NULL,
  `subject` VARCHAR(500) NULL,
  `message` TEXT NULL,
  `product_id` BIGINT NULL,
  `privacy_version` VARCHAR(500) NULL,
  `reference` VARCHAR(191) NULL,
  `status` VARCHAR(500) NULL,
  `internal_note` TEXT NULL,
  `consent_at` DATETIME(3) NULL,
  UNIQUE KEY uk_contact_inquiry_reference (`reference`),
  KEY ix_contact_inquiry_status (status,created_at),
  CONSTRAINT fk_inquiry_product FOREIGN KEY(product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dealer_application (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `company_name` VARCHAR(500) NULL,
  `contact_name` VARCHAR(500) NULL,
  `email` VARCHAR(191) NULL,
  `phone` VARCHAR(500) NULL,
  `country` VARCHAR(500) NULL,
  `website` VARCHAR(500) NULL,
  `business_type` VARCHAR(500) NULL,
  `interested_product_ids` JSON NULL,
  `message` TEXT NULL,
  `privacy_version` VARCHAR(500) NULL,
  `reference` VARCHAR(191) NULL,
  `status` VARCHAR(500) NULL,
  `outcome` VARCHAR(500) NULL,
  `internal_note` TEXT NULL,
  `consent_at` DATETIME(3) NULL,
  `open_dedupe_key` VARCHAR(191) NULL,
  UNIQUE KEY uk_dealer_application_reference (`reference`),
  UNIQUE KEY uk_application_open (open_dedupe_key),
  KEY ix_dealer_application_status (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  `actor_id` BIGINT NULL,
  `action` VARCHAR(500) NULL,
  `entity_type` VARCHAR(500) NULL,
  `entity_id` BIGINT NULL,
  `before_data` JSON NULL,
  `after_data` JSON NULL,
  `request_id` VARCHAR(500) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS app_lock (lock_name VARCHAR(40) PRIMARY KEY) ENGINE=InnoDB;
INSERT IGNORE INTO app_lock VALUES ('catalog'),('leads'),('config'),('seed'),('commerce');
CREATE TABLE IF NOT EXISTS idempotency_record (
 endpoint VARCHAR(100) NOT NULL, key_value VARCHAR(36) NOT NULL,
 request_hash VARCHAR(64) NOT NULL, response_data JSON NOT NULL,
 expires_at DATETIME(3) NOT NULL,
 PRIMARY KEY(endpoint,key_value), KEY ix_idempotency_expiry(expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customer_order (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  order_number VARCHAR(40) NOT NULL,
  access_token_hash CHAR(64) NOT NULL,
  customer_name VARCHAR(120) NOT NULL,
  email VARCHAR(191) NOT NULL,
  phone VARCHAR(40) NOT NULL,
  shipping_address JSON NOT NULL,
  currency CHAR(3) NOT NULL,
  subtotal_cents BIGINT NOT NULL,
  shipping_cents BIGINT NOT NULL DEFAULT 0,
  total_cents BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL,
  payment_status VARCHAR(40) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  paid_at DATETIME(3) NULL,
  internal_note TEXT NULL,
  UNIQUE KEY uk_customer_order_number (order_number),
  KEY ix_customer_order_status (status, created_at),
  KEY ix_customer_order_email (email, created_at),
  CHECK(currency IN ('CNY')),
  CHECK(subtotal_cents >= 0 AND shipping_cents >= 0 AND total_cents >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(160) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  unit_price_cents BIGINT NOT NULL,
  quantity INT NOT NULL,
  line_total_cents BIGINT NOT NULL,
  CONSTRAINT fk_order_item_order FOREIGN KEY(order_id) REFERENCES customer_order(id),
  CONSTRAINT fk_order_item_product FOREIGN KEY(product_id) REFERENCES product(id),
  KEY ix_order_item_order (order_id),
  CHECK(quantity BETWEEN 1 AND 20),
  CHECK(unit_price_cents > 0 AND line_total_cents > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  order_id BIGINT NOT NULL,
  payment_number VARCHAR(50) NOT NULL,
  method VARCHAR(40) NOT NULL,
  amount_cents BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  status VARCHAR(40) NOT NULL,
  provider_reference VARCHAR(100) NOT NULL,
  paid_at DATETIME(3) NULL,
  UNIQUE KEY uk_payment_number (payment_number),
  KEY ix_payment_order (order_id, created_at),
  KEY ix_payment_status (status, created_at),
  CONSTRAINT fk_payment_order FOREIGN KEY(order_id) REFERENCES customer_order(id),
  CHECK(amount_cents > 0),
  CHECK(currency IN ('CNY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
