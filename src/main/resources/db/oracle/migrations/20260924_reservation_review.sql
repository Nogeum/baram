-- Existing ACTIVE reservations remain confirmed. New requests start PENDING.
alter table portal_reservation add reviewer_id number(19,0);
alter table portal_reservation add reviewed_at timestamp(6);
alter table portal_reservation add review_comment varchar2(500 char);
alter table portal_reservation add constraint fk_reservation_reviewer foreign key (reviewer_id) references portal_employee(id);
