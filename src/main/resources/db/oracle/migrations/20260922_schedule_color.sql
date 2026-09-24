-- Existing Oracle schema: execute once. Existing schedules become blue.
ALTER TABLE portal_schedule ADD (
    color VARCHAR2(12 CHAR) DEFAULT 'BLUE' NOT NULL
    CONSTRAINT ck_schedule_color CHECK (color IN ('BLUE','RED','GREEN','ORANGE','PURPLE','TEAL'))
);
