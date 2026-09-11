INSERT INTO moods (name, icon, color_hex, sort_order) VALUES
    ('Great', '★', '#4BAE8D', 1),
    ('Good', '●', '#5B8DEF', 2),
    ('Okay', '◆', '#E7B64B', 3),
    ('Low', '☂', '#8B7FD6', 4),
    ('Rough', '≈', '#EC7D72', 5)
ON DUPLICATE KEY UPDATE icon = VALUES(icon), color_hex = VALUES(color_hex), sort_order = VALUES(sort_order);

INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('theme', 'SYSTEM'),
    ('accent_color', 'BLUE'),
    ('date_format', 'MMM d, yyyy'),
    ('first_day_of_week', 'MONDAY')
ON DUPLICATE KEY UPDATE setting_value = setting_value;
