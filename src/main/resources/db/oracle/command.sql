INSERT INTO portal_employee (
    id, version, login_id, password, name, position_name, department,
    role, active, annual_units, hire_date, email, phone
) VALUES (
             portal_seq.NEXTVAL, 0, 'admin01', '1234',
             '관리자', '팀장', '경영지원팀', 'ADMIN', 1, 15, TO_DATE('2020-01-01', 'YYYY-MM-DD'),
             'admin@company.com', '010-1234-5678'
         );

INSERT INTO portal_employee (
    id, version, login_id, password, name, position_name, department,
    role, active, annual_units, hire_date, email, phone
) VALUES (
             portal_seq.NEXTVAL, 0, 'user01', '$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGH',
             '박지원', '대리', '개발팀', 'EMPLOYEE', 1, 15, TO_DATE('2022-03-01', 'YYYY-MM-DD'),
             'user01@company.com', '010-9876-5432'
         );


DELETE FROM portal_employee WHERE login_id = 'admin01';


COMMIT;