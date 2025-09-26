-- Initialize database with sample data
INSERT INTO users (name) VALUES 
    ('Alice Johnson'),
    ('Bob Smith'),
    ('Carol Davis')
ON CONFLICT DO NOTHING;