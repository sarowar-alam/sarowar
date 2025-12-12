CREATE TABLE IF NOT EXISTS measurements (
 id SERIAL PRIMARY KEY,
 weight_kg NUMERIC NOT NULL,
 height_cm NUMERIC NOT NULL,
 age INTEGER NOT NULL,
 sex VARCHAR(10) NOT NULL,
 activity_level VARCHAR(30),
 bmi NUMERIC NOT NULL,
 bmi_category VARCHAR(30),
 bmr INTEGER,
 daily_calories INTEGER,
 created_at TIMESTAMPTZ DEFAULT now()
);