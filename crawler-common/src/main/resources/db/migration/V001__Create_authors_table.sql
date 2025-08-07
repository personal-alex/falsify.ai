-- Migration script to create authors table and add author relationship to articles
-- This script should be run when upgrading from the previous version

-- Create authors table
CREATE TABLE IF NOT EXISTS authors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on author name for efficient lookups
CREATE INDEX IF NOT EXISTS idx_author_name ON authors(name);

-- Add author_id column to articles table if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'articles' AND column_name = 'author_id') THEN
        ALTER TABLE articles ADD COLUMN author_id BIGINT;
    END IF;
END $$;

-- Create foreign key constraint from articles to authors
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_articles_author' AND table_name = 'articles') THEN
        ALTER TABLE articles ADD CONSTRAINT fk_articles_author 
        FOREIGN KEY (author_id) REFERENCES authors(id);
    END IF;
END $$;

-- Create index on articles.author_id for efficient joins
CREATE INDEX IF NOT EXISTS idx_article_author ON articles(author_id);

-- Create additional indexes for better performance
CREATE INDEX IF NOT EXISTS idx_article_url ON articles(url);
CREATE INDEX IF NOT EXISTS idx_article_crawler_source ON articles(crawler_source);
CREATE INDEX IF NOT EXISTS idx_article_created_at ON articles(created_at);

-- Insert default "Unknown Author" if it doesn't exist
INSERT INTO authors (name, avatar_url, created_at, updated_at)
SELECT 'Unknown Author', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM authors WHERE name = 'Unknown Author');

-- Update existing articles without author to use "Unknown Author"
DO $$
DECLARE
    unknown_author_id BIGINT;
BEGIN
    -- Get the Unknown Author ID
    SELECT id INTO unknown_author_id FROM authors WHERE name = 'Unknown Author';
    
    -- Update articles that don't have an author assigned
    UPDATE articles 
    SET author_id = unknown_author_id 
    WHERE author_id IS NULL;
END $$;

-- Make author_id NOT NULL after assigning default values
ALTER TABLE articles ALTER COLUMN author_id SET NOT NULL;