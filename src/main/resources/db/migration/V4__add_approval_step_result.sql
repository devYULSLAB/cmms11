-- V4: approval_step 테이블에 result 컬럼 추가
-- 결재 역할(decision)과 결재 결과(result)를 분리하여 누가 반려했는지 추적 가능

ALTER TABLE approval_step 
ADD COLUMN result VARCHAR(10) COMMENT '결재 결과: APPROVE(승인), REJECT(반려), PENDING(대기) 또는 NULL';

-- 기존 데이터 마이그레이션
-- decided_at이 있는 경우: APPROVE (과거 데이터는 모두 승인으로 간주)
-- decided_at이 NULL인 경우: result도 NULL (대기 상태)
UPDATE approval_step 
SET result = 'APPROVE' 
WHERE decided_at IS NOT NULL;

-- decision 컬럼 길이 확장 (CHAR(5) -> VARCHAR(10))
ALTER TABLE approval_step 
MODIFY COLUMN decision VARCHAR(10) COMMENT '결재 역할: APPROVAL(결재), AGREE(합의), INFORM(통보)';

