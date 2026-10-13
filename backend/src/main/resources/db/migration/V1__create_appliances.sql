CREATE TABLE appliances (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    power_watts NUMERIC(12, 3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT appliances_name_not_blank
        CHECK (LENGTH(TRIM(name)) > 0),

    CONSTRAINT appliances_power_non_negative
        CHECK (power_watts >= 0)
);