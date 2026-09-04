CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(255) UNIQUE,
    employee_number VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    position VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_employees_employee_number ON employees(employee_number);
CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_employees_keycloak_user_id ON employees(keycloak_user_id);
CREATE INDEX idx_employees_active ON employees(active);
