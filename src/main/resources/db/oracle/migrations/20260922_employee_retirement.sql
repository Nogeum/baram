-- Preserve account rows referenced by work history; hide deleted accounts from active management.
ALTER TABLE portal_employee ADD (deleted_at TIMESTAMP(9), deleted_by_login VARCHAR2(60 CHAR));
