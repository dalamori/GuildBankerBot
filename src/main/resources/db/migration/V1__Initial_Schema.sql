CREATE TABLE loot_items (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `quantity` FLOAT NOT NULL DEFAULT 0.0,
    `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_seen` TIMESTAMP NOT NULL,
    `held_by` VARCHAR(255) NOT NULL,
    CONSTRAINT `pk_id` PRIMARY KEY (`id`),
    CONSTRAINT `uniq_name_heldby` UNIQUE (`name`, `held_by`)
);

CREATE TABLE loot_logs (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `loot_id` BIGINT NOT NULL,
    `type` VARCHAR(12) NOT NULL,
    `who_said` VARCHAR(255) NOT NULL,
    `reason` VARCHAR(512) NOT NULL,
    CONSTRAINT `pk_id` PRIMARY KEY (`id`),
    CONSTRAINT `fk_loot_logs_on_loot` FOREIGN KEY (`loot_id`) REFERENCES loot_items (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE bot_users (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `snowflake` VARCHAR(255) NOT NULL,
    `auth_level` VARCHAR(12) NOT NULL,
    CONSTRAINT `pk_id` PRIMARY KEY (`id`),
    CONSTRAINT `uniq_snowflake` UNIQUE (`snowflake`)
);