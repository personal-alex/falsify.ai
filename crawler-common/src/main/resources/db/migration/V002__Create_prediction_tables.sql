-- Migration script to create prediction-related tables
-- This script creates the predictions, analysis_jobs, prediction_instances, and junction tables

-- Create predictions table
CREATE TABLE IF NOT EXISTS predictions (
    id BIGSERIAL PRIMARY KEY,
    prediction_text TEXT NOT NULL,
    prediction_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes on predictions table for efficient querying
CREATE INDEX IF NOT EXISTS idx_prediction_text ON predictions USING gin(to_tsvector('english', prediction_text));
CREATE INDEX IF NOT EXISTS idx_prediction_type ON predictions(prediction_type);
CREATE INDEX IF NOT EXISTS idx_prediction_created_at ON predictions(created_at);

-- Create analysis_jobs table
CREATE TABLE IF NOT EXISTS analysis_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    total_articles INTEGER DEFAULT 0,
    processed_articles INTEGER DEFAULT 0,
    predictions_found INTEGER DEFAULT 0,
    error_message TEXT,
    analysis_type VARCHAR(50) DEFAULT 'mock'
);

-- Create indexes on analysis_jobs table
CREATE INDEX IF NOT EXISTS idx_analysis_job_id ON analysis_jobs(job_id);
CREATE INDEX IF NOT EXISTS idx_analysis_status ON analysis_jobs(status);
CREATE INDEX IF NOT EXISTS idx_analysis_started_at ON analysis_jobs(started_at);

-- Create prediction_instances table (tracks specific prediction extractions)
CREATE TABLE IF NOT EXISTS prediction_instances (
    id BIGSERIAL PRIMARY KEY,
    prediction_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    analysis_job_id BIGINT NOT NULL,
    confidence_score DECIMAL(3,2) DEFAULT 0.00,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    extracted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    context TEXT,
    FOREIGN KEY (prediction_id) REFERENCES predictions(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (analysis_job_id) REFERENCES analysis_jobs(id) ON DELETE CASCADE
);

-- Create indexes on prediction_instances table
CREATE INDEX IF NOT EXISTS idx_prediction_instance_prediction ON prediction_instances(prediction_id);
CREATE INDEX IF NOT EXISTS idx_prediction_instance_article ON prediction_instances(article_id);
CREATE INDEX IF NOT EXISTS idx_prediction_instance_job ON prediction_instances(analysis_job_id);
CREATE INDEX IF NOT EXISTS idx_prediction_instance_rating ON prediction_instances(rating);
CREATE INDEX IF NOT EXISTS idx_prediction_instance_extracted_at ON prediction_instances(extracted_at);

-- Create article_predictions junction table (many-to-many relationship)
CREATE TABLE IF NOT EXISTS article_predictions (
    article_id BIGINT NOT NULL,
    prediction_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, prediction_id),
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (prediction_id) REFERENCES predictions(id) ON DELETE CASCADE
);

-- Create indexes on junction table
CREATE INDEX IF NOT EXISTS idx_article_predictions_article ON article_predictions(article_id);
CREATE INDEX IF NOT EXISTS idx_article_predictions_prediction ON article_predictions(prediction_id);

-- Create analysis_job_articles junction table (many-to-many relationship)
CREATE TABLE IF NOT EXISTS analysis_job_articles (
    analysis_job_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    PRIMARY KEY (analysis_job_id, article_id),
    FOREIGN KEY (analysis_job_id) REFERENCES analysis_jobs(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
);

-- Create indexes on analysis job articles junction table
CREATE INDEX IF NOT EXISTS idx_analysis_job_articles_job ON analysis_job_articles(analysis_job_id);
CREATE INDEX IF NOT EXISTS idx_analysis_job_articles_article ON analysis_job_articles(article_id);

-- Add constraints to ensure data integrity
ALTER TABLE prediction_instances ADD CONSTRAINT chk_confidence_score 
    CHECK (confidence_score >= 0.00 AND confidence_score <= 1.00);

-- Add constraint to ensure valid analysis status values
ALTER TABLE analysis_jobs ADD CONSTRAINT chk_analysis_status 
    CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'));

-- Add constraint to ensure valid analysis type values
ALTER TABLE analysis_jobs ADD CONSTRAINT chk_analysis_type 
    CHECK (analysis_type IN ('mock', 'llm'));

-- Create composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_prediction_instances_article_job ON prediction_instances(article_id, analysis_job_id);
CREATE INDEX IF NOT EXISTS idx_prediction_instances_rating_confidence ON prediction_instances(rating, confidence_score);

-- Create partial indexes for active jobs
CREATE INDEX IF NOT EXISTS idx_analysis_jobs_active ON analysis_jobs(started_at) 
    WHERE status IN ('PENDING', 'RUNNING');

-- Add comments for documentation
COMMENT ON TABLE predictions IS 'Stores unique predictions extracted from articles';
COMMENT ON TABLE analysis_jobs IS 'Tracks prediction analysis job lifecycle and progress';
COMMENT ON TABLE prediction_instances IS 'Stores specific instances of predictions found in analysis jobs';
COMMENT ON TABLE article_predictions IS 'Many-to-many relationship between articles and predictions';
COMMENT ON TABLE analysis_job_articles IS 'Many-to-many relationship between analysis jobs and articles';

COMMENT ON COLUMN predictions.prediction_text IS 'The actual prediction text extracted from articles';
COMMENT ON COLUMN predictions.prediction_type IS 'Category of prediction (political, economic, sports, etc.)';
COMMENT ON COLUMN analysis_jobs.job_id IS 'Unique identifier for the analysis job (UUID)';
COMMENT ON COLUMN analysis_jobs.status IS 'Current status of the analysis job';
COMMENT ON COLUMN prediction_instances.confidence_score IS 'AI confidence score for the prediction (0.0-1.0)';
COMMENT ON COLUMN prediction_instances.rating IS 'Human or AI rating of prediction quality (1-5 stars)';
COMMENT ON COLUMN prediction_instances.context IS 'Surrounding text context where prediction was found';