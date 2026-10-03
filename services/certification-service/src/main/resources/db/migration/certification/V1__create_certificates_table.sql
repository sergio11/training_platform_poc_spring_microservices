CREATE TABLE certificates (
    id BIGSERIAL PRIMARY KEY,
    certificate_number VARCHAR(20) NOT NULL UNIQUE,
    enrollment_id BIGINT NOT NULL UNIQUE,
    employee_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    issued_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_certificates_employee_id ON certificates(employee_id);
CREATE INDEX idx_certificates_course_id ON certificates(course_id);
