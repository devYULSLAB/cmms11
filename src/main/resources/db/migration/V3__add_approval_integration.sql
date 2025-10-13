-- Inspection-Approval 통합을 위한 마이그레이션
-- 작성일: 2025-10-10
-- 설명: inspection, work_order, work_permit 테이블에 approval_id 컬럼 추가

-- inspection 테이블에 approval_id 추가
ALTER TABLE inspection 
ADD COLUMN approval_id CHAR(10) AFTER status;

CREATE INDEX ix_inspection_approval ON inspection(company_id, approval_id);

-- work_order 테이블에 approval_id 추가
ALTER TABLE work_order 
ADD COLUMN approval_id CHAR(10) AFTER status;

CREATE INDEX ix_work_order_approval ON work_order(company_id, approval_id);

-- work_permit 테이블에 approval_id 추가
ALTER TABLE work_permit 
ADD COLUMN approval_id CHAR(10) AFTER status;

CREATE INDEX ix_work_permit_approval ON work_permit(company_id, approval_id);

-- approval 테이블에 ref_entity, ref_id 인덱스 추가 (이미 있다면 무시)
CREATE INDEX IF NOT EXISTS ix_approval_ref ON approval(company_id, ref_entity, ref_id);

