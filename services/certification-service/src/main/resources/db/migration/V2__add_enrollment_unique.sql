ALTER TABLE certificates ADD CONSTRAINT uk_certificate_enrollment
UNIQUE (enrollment_id);
