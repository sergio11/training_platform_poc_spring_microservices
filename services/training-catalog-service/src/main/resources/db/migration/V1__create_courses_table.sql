CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    instructor VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL,
    enrolled_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
