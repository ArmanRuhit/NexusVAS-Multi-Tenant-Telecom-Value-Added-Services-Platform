-- PostgreSQL initialization script for NexusVAS
-- Creates all 7 databases and enables PGVector extension

-- Create databases
CREATE DATABASE auth_db;
CREATE DATABASE operator_db;
CREATE DATABASE subscription_db;
CREATE DATABASE billing_db;
CREATE DATABASE campaign_db;
CREATE DATABASE notification_db;
CREATE DATABASE ai_db;

-- Enable PGVector extension in ai_db
\c ai_db;
CREATE EXTENSION IF NOT EXISTS vector;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE auth_db TO nexusvas;
GRANT ALL PRIVILEGES ON DATABASE operator_db TO nexusvas;
GRANT ALL PRIVILEGES ON DATABASE subscription_db TO nexusvas;
GRANT ALL PRIVILEGES ON DATABASE billing_db TO nexusvas;
GRANT ALL PRIVILEGES ON DATABASE campaign_db TO nexusvas;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO nexusvas;
GRANT ALL PRIVILEGES ON DATABASE ai_db TO nexusvas;