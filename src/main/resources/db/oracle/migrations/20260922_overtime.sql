-- Existing Oracle schema: execute once. No existing data is changed.
CREATE TABLE portal_overtime (
    id NUMBER(19,0) NOT NULL PRIMARY KEY,
    version NUMBER(19,0) NOT NULL,
    employee_id NUMBER(19,0) NOT NULL REFERENCES portal_employee(id),
    approver_id NUMBER(19,0) NOT NULL REFERENCES portal_employee(id),
    work_date DATE NOT NULL,
    start_time VARCHAR2(255 CHAR) NOT NULL,
    end_time VARCHAR2(255 CHAR) NOT NULL,
    reason VARCHAR2(1000 CHAR) NOT NULL,
    status VARCHAR2(20 CHAR) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    requested_at TIMESTAMP(9) NOT NULL,
    reviewed_at TIMESTAMP(9),
    review_comment VARCHAR2(500 CHAR)
);
CREATE INDEX ix_overtime_employee_date ON portal_overtime(employee_id,work_date);
CREATE INDEX ix_overtime_approver_status ON portal_overtime(approver_id,status,requested_at);
