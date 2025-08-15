-- Migration to update rating column from INTEGER to DOUBLE
-- This allows for decimal ratings (e.g., 3.5 stars instead of just 3 or 4)

-- Update the prediction_instances table
ALTER TABLE prediction_instances 
ALTER COLUMN rating TYPE DOUBLE PRECISION;

-- Update any existing integer ratings to have .0 decimal places
-- This is automatically handled by PostgreSQL when converting INTEGER to DOUBLE PRECISION

-- Add a comment to document the change
COMMENT ON COLUMN prediction_instances.rating IS 'Rating from 1.0 to 5.0 stars (supports decimal values)';