CREATE TABLE certificates (
    id BIGSERIAL PRIMARY KEY,
    certificate_number VARCHAR(255) NOT NULL UNIQUE,
    enrollment_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    issued_at TIMESTAMP NOT NULL
);
