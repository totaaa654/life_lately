USE life_lately;

INSERT INTO moods (name, icon, color_hex, sort_order) VALUES
    ('Great', 'sun', '#E9B937', 1),
    ('Good', 'leaf', '#65AE90', 2),
    ('Okay', 'cloud', '#6F9ED8', 3),
    ('Low', 'drop', '#9A86DA', 4),
    ('Rough', 'rain', '#E57C6C', 5)
ON DUPLICATE KEY UPDATE icon = VALUES(icon), color_hex = VALUES(color_hex), sort_order = VALUES(sort_order);

INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('theme', 'SYSTEM'),
    ('accent_color', 'BLUE'),
    ('date_format', 'MMM d, yyyy'),
    ('first_day_of_week', 'MONDAY')
ON DUPLICATE KEY UPDATE setting_value = setting_value;
