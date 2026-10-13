ALTER TABLE appliances
    ADD COLUMN visitor_id UUID;

-- Preserve existing records without assigning them to a public visitor.
UPDATE appliances
SET visitor_id = gen_random_uuid()
WHERE visitor_id IS NULL;

ALTER TABLE appliances
    ALTER COLUMN visitor_id SET NOT NULL;

CREATE INDEX idx_appliances_visitor_name_id
    ON appliances (visitor_id, name, id);