-- Existing Oracle schema: run once before starting the updated application.
CREATE TABLE portal_correction (
    id NUMBER(19,0) NOT NULL PRIMARY KEY,
    version NUMBER(19,0) NOT NULL,
    employee_id NUMBER(19,0) NOT NULL REFERENCES portal_employee(id),
    approver_id NUMBER(19,0) NOT NULL REFERENCES portal_employee(id),
    reviewer_id NUMBER(19,0) REFERENCES portal_employee(id),
    work_date DATE NOT NULL,
    original_version NUMBER(19,0),
    original_check_in TIMESTAMP(9),
    original_check_out TIMESTAMP(9),
    proposed_check_in TIMESTAMP(9) NOT NULL,
    proposed_check_out TIMESTAMP(9) NOT NULL,
    reason VARCHAR2(1000 CHAR) NOT NULL,
    status VARCHAR2(20 CHAR) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    requested_at TIMESTAMP(9) NOT NULL,
    reviewed_at TIMESTAMP(9),
    review_comment VARCHAR2(500 CHAR)
);
CREATE INDEX ix_correction_employee_date ON portal_correction(employee_id,work_date);
CREATE INDEX ix_correction_status ON portal_correction(status,requested_at);
