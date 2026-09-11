CREATE DATABASE IF NOT EXISTS life_lately
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE life_lately;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(40) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS moods (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    icon VARCHAR(16) NOT NULL,
    color_hex CHAR(7) NOT NULL,
    sort_order INT NOT NULL,
    INDEX idx_moods_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS entries (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    mood_id BIGINT UNSIGNED NOT NULL,
    entry_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_entries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_entries_mood FOREIGN KEY (mood_id) REFERENCES moods(id),
    INDEX idx_entries_user (user_id),
    INDEX idx_entries_date (entry_date),
    INDEX idx_entries_mood (mood_id),
    INDEX idx_entries_deleted (deleted_at),
    INDEX idx_entries_favorite (is_favorite),
    FULLTEXT INDEX idx_entries_search (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    color_hex CHAR(7) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS entry_tags (
    entry_id BIGINT UNSIGNED NOT NULL,
    tag_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (entry_id, tag_id),
    CONSTRAINT fk_entry_tags_entry FOREIGN KEY (entry_id) REFERENCES entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_entry_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    INDEX idx_entry_tags_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(80) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_settings (
    user_id BIGINT UNSIGNED NOT NULL,
    setting_key VARCHAR(80) NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, setting_key),
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
