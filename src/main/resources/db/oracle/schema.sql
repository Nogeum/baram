-- Oracle 11.2 schema, including reservation approval audit (2026-09-24).
create sequence portal_seq start with 1 increment by 1;
create table portal_amendment (end_date date, start_date date, approver_id number(19,0) not null, created_at timestamp(9) not null, employee_id number(19,0) not null, id number(19,0) not null, original_version number(19,0) not null, replacement_id number(19,0), request_id number(19,0) not null, reviewed_at timestamp(9), reviewer_id number(19,0), source_attendance_version number(19,0), version number(19,0) not null, action varchar2(20 char) not null, leave_kind varchar2(20 char), request_type varchar2(20 char) not null, status varchar2(20 char) not null, review_comment varchar2(500 char), reason varchar2(1000 char) not null, original_summary varchar2(2000 char) not null, end_time varchar2(255 char), start_time varchar2(255 char), primary key (id));
create table portal_attendance (early_departure number(10,0) not null check ((early_departure in (0,1))), late_arrival number(10,0) not null check ((late_arrival in (0,1))), work_date date not null, check_in timestamp(9) not null, check_out timestamp(9), employee_id number(19,0) not null, id number(19,0) not null, version number(19,0) not null, primary key (id), constraint uk_attendance_day unique (employee_id, work_date));
create table portal_correction (work_date date not null, approver_id number(19,0) not null, employee_id number(19,0) not null, id number(19,0) not null, original_check_in timestamp(9), original_check_out timestamp(9), original_version number(19,0), proposed_check_in timestamp(9) not null, proposed_check_out timestamp(9) not null, requested_at timestamp(9) not null, reviewed_at timestamp(9), reviewer_id number(19,0), version number(19,0) not null, status varchar2(20 char) not null check ((status in ('PENDING','APPROVED','REJECTED','CANCELLED'))), review_comment varchar2(500 char), reason varchar2(1000 char) not null, primary key (id));
create table portal_document (amount number(15,2), current_step number(10,0) not null, end_date date, start_date date, author_id number(19,0) not null, created_at timestamp(9) not null, id number(19,0) not null, version number(19,0) not null, kind varchar2(20 char) not null, status varchar2(20 char) not null, title varchar2(160 char) not null, content clob not null, primary key (id));
create table portal_document_step (step_order number(10,0) not null, document_id number(19,0) not null, id number(19,0) not null, reviewed_at timestamp(9), reviewer_id number(19,0) not null, version number(19,0) not null, status varchar2(20 char) not null, review_comment varchar2(500 char), primary key (id));
create table portal_employee (active number(10,0) not null check ((active in (0,1))), annual_units number(10,0) not null, department_manager number(10,0) default 0 not null check ((department_manager in (0,1))), hire_date date, deleted_at timestamp(9), id number(19,0) not null, version number(19,0) not null, role varchar2(20 char) not null check ((role in ('EMPLOYEE','ADMIN'))), extension_number varchar2(30 char), phone varchar2(30 char), position_name varchar2(40 char) not null, deleted_by_login varchar2(60 char), login_id varchar2(60 char) not null, department varchar2(80 char) not null, name varchar2(80 char) not null, password varchar2(100 char) not null, email varchar2(120 char), job_description varchar2(500 char), primary key (id), constraint uk_employee_login unique (login_id));
create table portal_file (created_at timestamp(9) not null, file_size number(19,0) not null, id number(19,0) not null, owner_id number(19,0) not null, uploader_id number(19,0) not null, version number(19,0) not null, owner_type varchar2(20 char) not null, filename varchar2(200 char) not null, data blob not null, primary key (id));
create table portal_holiday (holiday_date date not null unique, author_id number(19,0) not null, created_at timestamp(9) not null, id number(19,0) not null, version number(19,0) not null, kind varchar2(20 char) not null, name varchar2(120 char) not null, primary key (id));
create table portal_leave (charged_units number(10,0) not null, end_date date not null, start_date date not null, approver_id number(19,0), employee_id number(19,0) not null, id number(19,0) not null, requested_at timestamp(9) not null, reviewed_at timestamp(9), reviewer_id number(19,0), version number(19,0) not null, kind varchar2(20 char) not null check ((kind in ('ANNUAL','HALF_AM','HALF_PM','SICK'))), status varchar2(20 char) not null check ((status in ('PENDING','APPROVED','REJECTED','CANCELLED'))), review_comment varchar2(500 char), reason varchar2(1000 char) not null, primary key (id));
create table portal_library (author_id number(19,0) not null, created_at timestamp(9) not null, id number(19,0) not null, updated_at timestamp(9) not null, version number(19,0) not null, visibility varchar2(20 char) not null, department varchar2(80 char) not null, title varchar2(160 char) not null, content clob not null, primary key (id));
create table portal_notice (author_id number(19,0) not null, created_at timestamp(9) not null, id number(19,0) not null, version number(19,0) not null, title varchar2(160 char) not null, content clob not null, primary key (id));
create table portal_notification (read_flag number(10,0) not null check ((read_flag in (0,1))), created_at timestamp(9) not null, id number(19,0) not null, recipient_id number(19,0) not null, version number(19,0) not null, target_path varchar2(200 char), message varchar2(300 char) not null, primary key (id));
create table portal_overtime (work_date date not null, approver_id number(19,0) not null, employee_id number(19,0) not null, id number(19,0) not null, requested_at timestamp(9) not null, reviewed_at timestamp(9), version number(19,0) not null, status varchar2(20 char) not null check ((status in ('PENDING','APPROVED','REJECTED','CANCELLED'))), review_comment varchar2(500 char), reason varchar2(1000 char) not null, end_time varchar2(255 char) not null, start_time varchar2(255 char) not null, primary key (id));
create table portal_reservation (created_at timestamp(9) not null, employee_id number(19,0) not null, ends_at timestamp(9) not null, id number(19,0) not null, resource_id number(19,0) not null, reviewed_at timestamp(9), reviewer_id number(19,0), starts_at timestamp(9) not null, version number(19,0) not null, status varchar2(20 char) not null, title varchar2(160 char) not null, review_comment varchar2(500 char), primary key (id));
create table portal_resource (active number(10,0) not null check ((active in (0,1))), capacity number(10,0) not null, id number(19,0) not null, version number(19,0) not null, kind varchar2(20 char) not null, name varchar2(120 char) not null, location varchar2(200 char), primary key (id));
create table portal_schedule (assigned number(10,0) not null check ((assigned in (0,1))), event_date date not null, employee_id number(19,0) not null, id number(19,0) not null, version number(19,0) not null, color varchar2(12 char) default 'BLUE' not null check ((color in ('BLUE','RED','GREEN','ORANGE','PURPLE','TEAL'))), title varchar2(120 char) not null, memo varchar2(500 char), end_time varchar2(255 char), start_time varchar2(255 char), primary key (id));
create table portal_task (due_date date not null, reminder_date date, assignee_id number(19,0) not null, created_at timestamp(9) not null, creator_id number(19,0) not null, id number(19,0) not null, version number(19,0) not null, status varchar2(20 char) not null, title varchar2(160 char) not null, description clob not null, primary key (id));
create table portal_task_comment (author_id number(19,0) not null, created_at timestamp(9) not null, id number(19,0) not null, task_id number(19,0) not null, version number(19,0) not null, content clob not null, primary key (id));
alter table portal_amendment add constraint FK8jvo9eye7i084s9613ylm6kq8 foreign key (approver_id) references portal_employee;
alter table portal_amendment add constraint FKmxdojmv8sd5f1t916dpjpr0f foreign key (employee_id) references portal_employee;
alter table portal_amendment add constraint FK2r16maqq54b0qpbkrjn46yq4c foreign key (reviewer_id) references portal_employee;
alter table portal_attendance add constraint FK93pwbfnmwby3e8bmswot95rac foreign key (employee_id) references portal_employee;
alter table portal_correction add constraint FKof0ukwxmp1kn3803qx4hcy649 foreign key (approver_id) references portal_employee;
alter table portal_correction add constraint FKfgtplwy0v9g1p3bjtb7aoh4i8 foreign key (employee_id) references portal_employee;
alter table portal_correction add constraint FKq560axyn7kp5qec81d6otdoqr foreign key (reviewer_id) references portal_employee;
alter table portal_document add constraint FK1j23ifbbshp7hj9sxq2oliow7 foreign key (author_id) references portal_employee;
alter table portal_document_step add constraint FK89dgfaeka1hjaumjyqbd95d47 foreign key (document_id) references portal_document;
alter table portal_document_step add constraint FKn5ox64paoah0oxlk6lkv4a4pd foreign key (reviewer_id) references portal_employee;
alter table portal_file add constraint FKtonvjsyx6ve9lsftdqiecjg28 foreign key (uploader_id) references portal_employee;
alter table portal_holiday add constraint FK6qsyovyej99d4jj9443j43ghj foreign key (author_id) references portal_employee;
alter table portal_leave add constraint FKnfvsiiusxo9nt1njesvs3n1q8 foreign key (approver_id) references portal_employee;
alter table portal_leave add constraint FKnm6vxwlayon7cqv8nkudbi96k foreign key (employee_id) references portal_employee;
alter table portal_leave add constraint FK6jki5cqvqnj3dsyuxehq704bp foreign key (reviewer_id) references portal_employee;
alter table portal_library add constraint FKphineosyy36a0h81m3cs65mec foreign key (author_id) references portal_employee;
alter table portal_notice add constraint FK2sjocke1vb6s4ewp5umbt0hsf foreign key (author_id) references portal_employee;
alter table portal_notification add constraint FKpe4jam80k1airap0139isbdkk foreign key (recipient_id) references portal_employee;
alter table portal_overtime add constraint FKanuifyp9uvjg6umgumhtax2g4 foreign key (approver_id) references portal_employee;
alter table portal_overtime add constraint FK8nfqbf4ysueotng53a0dj73mg foreign key (employee_id) references portal_employee;
alter table portal_reservation add constraint FKg91w4meh79lo0m1anm2vq8mgg foreign key (employee_id) references portal_employee;
alter table portal_reservation add constraint FKpvuxpr4op416u430upncql32p foreign key (resource_id) references portal_resource;
alter table portal_reservation add constraint FKj5ess84jyyyt7f5lpc43fh8p3 foreign key (reviewer_id) references portal_employee;
alter table portal_schedule add constraint FKh5qpfd3csonccu0uyr4s06lgx foreign key (employee_id) references portal_employee;
alter table portal_task add constraint FK1q3fh5iloj41w80s1rlaqkqud foreign key (assignee_id) references portal_employee;
alter table portal_task add constraint FK9msfd6gw9yn5xf5rdt0y3lgel foreign key (creator_id) references portal_employee;
alter table portal_task_comment add constraint FKiid63eeqgrq3bnfyka22napug foreign key (author_id) references portal_employee;
alter table portal_task_comment add constraint FKjylmdiwd5w4sw7a2s6ca5wylc foreign key (task_id) references portal_task;

create index ix_task_assignee_due on portal_task(assignee_id,due_date);
create index ix_document_author on portal_document(author_id,created_at);
create index ix_document_reviewer on portal_document_step(reviewer_id,status);
create index ix_file_owner on portal_file(owner_type,owner_id);
create index ix_reservation_time on portal_reservation(resource_id,starts_at,ends_at);
create index ix_amendment_request on portal_amendment(request_type,request_id,status);
create index ix_library_visibility on portal_library(visibility,department);

-- Private one-to-one messages. Existing business rows are not modified.
create table portal_chat_message (
    id number(19,0) not null,
    version number(19,0) not null,
    sender_id number(19,0) not null,
    recipient_id number(19,0) not null,
    content clob not null,
    client_id varchar2(36 char) not null,
    sent_at timestamp(6) not null,
    read_at timestamp(6),
    primary key (id),
    constraint uk_chat_sender_client unique (sender_id,client_id),
    constraint fk_chat_sender foreign key (sender_id) references portal_employee(id),
    constraint fk_chat_recipient foreign key (recipient_id) references portal_employee(id),
    constraint ck_chat_not_self check (sender_id <> recipient_id)
);
create index ix_chat_sender_peer on portal_chat_message(sender_id,recipient_id,id);
create index ix_chat_recipient_read on portal_chat_message(recipient_id,read_at,sender_id,id);

-- Optional employee profile photo stored in portal_file (PROFILE).
ALTER TABLE portal_employee ADD profile_file_id NUMBER(19);
