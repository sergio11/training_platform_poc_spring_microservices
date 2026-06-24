CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_enrollment_employee_course_status
        UNIQUE (employee_id, course_id, status)
);

CREATE INDEX idx_enrollments_employee_id ON enrollments(employee_id);
CREATE INDEX idx_enrollments_course_id ON enrollments(course_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
