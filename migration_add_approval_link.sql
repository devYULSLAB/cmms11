-- Inspection 테이블에 approval_id 컬럼 추가
ALTER TABLE inspection 
ADD COLUMN approval_id CHAR(10) AFTER status;

-- 인덱스 추가
CREATE INDEX ix_inspection_approval ON inspection(company_id, approval_id);

-- 상태값 업데이트 (기존 데이터 마이그레이션 필요 시)
-- PROC 상태인 데이터는 그대로 유지
-- UPDATE inspection SET status = 'PROC' WHERE status = 'INPROG'; -- 필요 시

