-- approval_inbox 테이블 생성
CREATE TABLE IF NOT EXISTS approval_inbox (
    company_id CHAR(5) NOT NULL,
    inbox_id CHAR(10) NOT NULL,
    member_id CHAR(5),
    approval_id CHAR(10),
    step_no INTEGER,
    inbox_type VARCHAR(10),
    is_read CHAR(1) DEFAULT 'N',
    read_at TIMESTAMP,
    notified_at TIMESTAMP,
    notification_type VARCHAR(20),
    title VARCHAR(100),
    ref_entity VARCHAR(64),
    ref_id CHAR(10),
    submitted_by CHAR(10),
    submitted_at TIMESTAMP,
    decision VARCHAR(10),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT pk_approval_inbox PRIMARY KEY (company_id, inbox_id),
    CONSTRAINT fk_inbox_approval FOREIGN KEY (company_id, approval_id)
        REFERENCES approval(company_id, approval_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_inbox_step FOREIGN KEY (company_id, approval_id, step_no)
        REFERENCES approval_step(company_id, approval_id, step_no)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_inbox_member_read
    ON approval_inbox(company_id, member_id, is_read, inbox_type);
CREATE INDEX IF NOT EXISTS ix_inbox_approval
    ON approval_inbox(company_id, approval_id);
CREATE INDEX IF NOT EXISTS ix_inbox_submitted
    ON approval_inbox(company_id, member_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS ix_inbox_type_member
    ON approval_inbox(company_id, inbox_type, member_id);
