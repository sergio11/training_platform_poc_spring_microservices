ALTER TABLE enrollments ADD CONSTRAINT uk_enrollment_employee_course_status
UNIQUE (employee_id, course_id, status);
