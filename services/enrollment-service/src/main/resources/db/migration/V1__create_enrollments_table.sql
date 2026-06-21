CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);
