-- Existing requests remain unassigned and retain the shared administrator inbox.
ALTER TABLE portal_leave ADD (
    approver_id NUMBER(19,0) CONSTRAINT fk_leave_approver REFERENCES portal_employee(id)
);
