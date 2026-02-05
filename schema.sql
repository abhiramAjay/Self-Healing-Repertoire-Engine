-- 1. THE WORK (The Composition/Song)
CREATE TABLE works (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    iswc VARCHAR(15) UNIQUE, -- International Standard Musical Work Code
    title VARCHAR(255) NOT NULL,
    work_type VARCHAR(50), -- e.g., Original, Adaptation
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. THE RECORDING (The Master/Audio)
CREATE TABLE recordings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    isrc VARCHAR(12) UNIQUE, -- International Standard Recording Code
    work_id UUID REFERENCES works (id), -- The "Healing" Link
    recording_title VARCHAR(255) NOT NULL,
    duration_ms INTEGER,
    is_instrumental BOOLEAN DEFAULT FALSE,
    release_date DATE
);

-- 3. INTERESTED PARTIES (Songwriters/Publishers)
CREATE TABLE parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    ipi_number VARCHAR(11) UNIQUE, -- International Standard Name Identifier
    full_name VARCHAR(255) NOT NULL,
    party_type VARCHAR(50) -- e.g., Composer, Author, Publisher
);

-- 4. THE SPLITS (Who owns what percentage of the Work)
CREATE TABLE work_splits (
    id SERIAL PRIMARY KEY,
    work_id UUID REFERENCES works (id),
    party_id UUID REFERENCES parties (id),
    share_percentage DECIMAL(5, 2) NOT NULL, -- e.g., 33.33
    role VARCHAR(50), -- e.g., Lyricist, Composer
    CONSTRAINT total_share_check CHECK (
        share_percentage > 0
        AND share_percentage <= 100
    )
);