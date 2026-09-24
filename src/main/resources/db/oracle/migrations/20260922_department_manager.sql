-- Separate department management permission from job title and system administrator role.
-- Existing accounts keep their current privileges until a system administrator grants this permission.
ALTER TABLE portal_employee ADD (
    department_manager NUMBER(10,0) DEFAULT 0 NOT NULL
    CONSTRAINT ck_employee_dept_manager CHECK (department_manager IN (0,1))
);
