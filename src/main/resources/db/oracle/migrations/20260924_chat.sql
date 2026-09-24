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
