-- V4: Add authorities table
CREATE TABLE IF NOT EXISTS authorities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    jurisdiction VARCHAR(100) NOT NULL,
    department VARCHAR(50) DEFAULT 'Environmental Protection',
    contact_email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
