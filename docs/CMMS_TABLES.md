# 테이블 구조 가이드

본 문서는 CMMS 데이터 모델의 표준 규약을 요약합니다. 모든 스키마와 컬럼은 snake_case를 사용합니다.

## 기본 원칙
- 회사코드 선행: 모든 테이블의 첫 번째 키는 `company_id CHAR(5)`.
- 번호 규칙: 기본번호는 `CHAR(10)`(예: 설비번호), 도메인 코드는 `CHAR(5)`.
- 공통 길이: `name VARCHAR(100)`, `note VARCHAR(500)`.
- 삭제 정책: domain/common/master는 소프트 삭제(`delete_mark`), transaction은 물리 삭제 허용.
- 제약 원칙: PK 컬럼만 NOT NULL. 비PK 컬럼에는 NOT NULL을 두지 않습니다(애플리케이션에서 검증).

## 도메인 테이블 (domain)
요구 명칭으로 테이블을 구성합니다. 모든 비PK 컬럼은 NULL 허용입니다.
```sql
CREATE TABLE company (
  company_id  CHAR(5),
  name        VARCHAR(100),
  bizno       VARCHAR(50),
  email       VARCHAR(100),
  phone       VARCHAR(100),
  note        VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at  TIMESTAMP,
  created_by  CHAR(10),
  updated_at  TIMESTAMP,
  updated_by  CHAR(10),
  CONSTRAINT pk_company PRIMARY KEY (company_id)
);

CREATE TABLE site (
  company_id CHAR(5),
  site_id  CHAR(5),
  name       VARCHAR(100),
  phone VARCHAR(30),
  address VARCHAR(200),
  --status VARCHAR(10) DEFAULT 'ACTIVE',
  note       VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at TIMESTAMP,
  created_by CHAR(10),
  updated_at TIMESTAMP,
  updated_by CHAR(10),
  CONSTRAINT pk_site PRIMARY KEY (company_id, site_id)
);

CREATE TABLE dept (
  company_id CHAR(5),
  dept_id  CHAR(5),
  name       VARCHAR(100),
  phone VARCHAR(30),
  address VARCHAR(200),
  --status VARCHAR(10) DEFAULT 'ACTIVE',
  note       VARCHAR(500),
  parent_id CHAR(5),  -- 상위 부서코드 
  delete_mark CHAR(1) DEFAULT 'N',
  created_at TIMESTAMP,
  created_by CHAR(10),
  updated_at TIMESTAMP,
  updated_by CHAR(10),
  CONSTRAINT pk_dept PRIMARY KEY (company_id, dept_id)
);

CREATE TABLE member (
  company_id CHAR(5),
  member_id  CHAR(5),
  name       VARCHAR(100),
  dept_id CHAR(5),
  password_hash VARCHAR(100),
  email VARCHAR(100),
  phone VARCHAR(100),
  site_id  CHAR(5),  -- member의 site_id는 운영 편의를 위한 참고값임. 트랜잭션 때 기본값 설정.변경 가능 
  position VARCHAR(50),
  title VARCHAR(50),
  note       VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at TIMESTAMP,
  created_by CHAR(10),
  updated_at TIMESTAMP,
  updated_by CHAR(10),
  CONSTRAINT pk_member PRIMARY KEY (company_id, member_id)
);

CREATE TABLE func (
  company_id   CHAR(5),
  func_id CHAR(5),
  name         VARCHAR(100),
  note         VARCHAR(500),
  parent_id CHAR(5),
  delete_mark  CHAR(1) DEFAULT 'N',
  created_at   TIMESTAMP,
  created_by   CHAR(10),
  updated_at   TIMESTAMP,
  updated_by   CHAR(10),
  CONSTRAINT pk_func PRIMARY KEY (company_id, func_id)
);

CREATE TABLE storage (
  company_id  CHAR(5),
  storage_id CHAR(5),
  name        VARCHAR(100),
  note        VARCHAR(500),
  parent_id CHAR(5),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at  TIMESTAMP,
  created_by  CHAR(10),
  updated_at  TIMESTAMP,
  updated_by  CHAR(10),
  CONSTRAINT pk_storage PRIMARY KEY (company_id, storage_id)
);

-- 역할 테이블: 업무 책임과 권한을 정의하는 그룹 (예: 관리자, 기술자, 조회자)
CREATE TABLE role (
  company_id CHAR(5),
  role_id  CHAR(5),
  name       VARCHAR(100),  -- 역할명 (예: "시스템관리자", "점검기술자")
  note       VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at TIMESTAMP,
  created_by CHAR(10),
  updated_at TIMESTAMP,
  updated_by CHAR(10),
  CONSTRAINT pk_role PRIMARY KEY (company_id, role_id)
);

-- 사용자-역할 매핑 테이블: 어떤 사용자가 어떤 역할을 가지는지 정의
CREATE TABLE rolemap (
  company_id CHAR(5),
  member_id  CHAR(5),    -- 사용자 ID
  role_id  CHAR(5),      -- 역할 ID
  CONSTRAINT pk_rolemap PRIMARY KEY (company_id, member_id, role_id)
);

-- 허가 테이블: 구체적인 시스템 기능에 대한 접근 권한 정의
CREATE TABLE permission (
  company_id CHAR(5),
  permission_id CHAR(10),  -- [모듈]_[CRUD] 형식 (예: PLANT_C, INSPECTION_R)
  name       VARCHAR(100), -- 허가명 (예: "설비등록", "점검조회")
  module     VARCHAR(20),  -- 모듈명 (예: PLANT, INSPECTION, WORKORDER)
  action     CHAR(1),      -- CRUD 액션 (C=생성, R=조회, U=수정, D=삭제)
  note       VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at TIMESTAMP,
  created_by CHAR(10),
  updated_at TIMESTAMP,
  updated_by CHAR(10),
  CONSTRAINT pk_permission PRIMARY KEY (company_id, permission_id)
);

-- 역할-허가 매핑 테이블: 어떤 역할이 어떤 허가를 가지는지 정의
CREATE TABLE role_permission (
  company_id CHAR(5),
  role_id    CHAR(5),        -- 역할 ID
  permission_id CHAR(10),    -- 허가 ID
  CONSTRAINT pk_role_permission PRIMARY KEY (company_id, role_id, permission_id)
);
```

## 공통 코드 (code)
명칭은 `code_type`, `code_item`을 사용합니다.
```sql
CREATE TABLE code_type (
  company_id CHAR(5),
  code_type  CHAR(5),
  name       VARCHAR(100),
  note       VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at TIMESTAMP,
  created_by CHAR(10),
  updated_at TIMESTAMP,
  updated_by CHAR(10),
  CONSTRAINT pk_code_type PRIMARY KEY (company_id, code_type)
);

CREATE TABLE code_item (
  company_id CHAR(5),
  code_type  CHAR(5),
  code       CHAR(5),
  name       VARCHAR(100),
  note       VARCHAR(500),
  CONSTRAINT pk_code_item PRIMARY KEY (company_id, code_type, code)
);
```

## 마스터 (master)
```sql
CREATE TABLE plant (
  company_id CHAR(5),
  -- 기본정보 
  plant_id     CHAR(10),
  name       VARCHAR(100),
  asset_id  CHAR(5),  -- code_type="ASSET"
  site_id  CHAR(5),
  dept_id  CHAR(5),
  func_id CHAR(5),
  -- 제조사 정보
  maker_name  VARCHAR(100),
  spec  VARCHAR(100),
  model VARCHAR(100),
  serial  VARCHAR(100),
  -- 재무정보
  install_date  DATE,
  depre_id  CHAR(5),  -- code_type="DEPRE"
  depre_period  INTEGER,  -- 상각기간:year 
  purchase_cost DECIMAL(18,2), -- 취득가
  residual_value  DECIMAL(18,2), -- 잔존가 
  -- 운영 플래그 
  inspection_yn CHAR(1),
  psm_yn  CHAR(1),
  workpermit_yn CHAR(1),
  -- 점검 정보
  inspection_interval INTEGER,  -- 점검주기:month
  last_inspection DATE,
  next_inspection DATE,

  file_group_id CHAR(10),
  note       VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  --status  VARCHAR(10) DEFAULT 'ACTIVE',
  created_at TIMESTAMP,
  created_by CHAR(10),
  updated_at TIMESTAMP,
  updated_by CHAR(10),
  CONSTRAINT pk_plant_master PRIMARY KEY (company_id, plant_id)
);

CREATE TABLE inventory (
  company_id CHAR(5),
  -- 기본정보   
  inventory_id     CHAR(10),
  name       VARCHAR(100),
  asset_id  CHAR(5),  -- code_type="ASSET"
  dept_id  CHAR(5),
  -- 제조사 정보
  maker_name  VARCHAR(100),
  spec  VARCHAR(100),
  model VARCHAR(100),
  serial  VARCHAR(100),

  file_group_id CHAR(10),
  note       VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  --status  VARCHAR(10) DEFAULT 'ACTIVE',
  created_at TIMESTAMP,
  created_by CHAR(10),
  updated_at TIMESTAMP,
  updated_by CHAR(10),
  CONSTRAINT pk_inventory_master PRIMARY KEY (company_id, inventory_id)
);
```

## 트랜잭션 (transaction)
헤더는 생성/수정 정보를 두고, 아이템 테이블에는 감사/소프트삭제 컬럼을 두지 않습니다.
```sql
CREATE TABLE inspection (
  company_id    CHAR(5),
  -- 기본정보 
  inspection_id CHAR(10),
  name          VARCHAR(100),
  plant_id  CHAR(10),
  job_id  CHAR(5),  --code_type="JOBTP"
  site_id CHAR(5),
  -- 작업정보
  dept_id CHAR(5),
  member_id CHAR(5),
  planned_date  DATE,
  actual_date DATE,

  status  VARCHAR(10),  -- PLN_APPROV(계획자동확정), ACT_DRAFT(실적작성), ACT_SUBMIT(실적결재상신), ACT_PROC(실적결재중), ACT_APPROV(실적승인완료)
  approval_id CHAR(10), -- approval 테이블 연결 (ACT_SUBMIT 시 생성, refStage="ACT")
  file_group_id CHAR(10),
  note          VARCHAR(500),
  created_at    TIMESTAMP,
  created_by    CHAR(10),
  updated_at    TIMESTAMP,
  updated_by    CHAR(10),
  CONSTRAINT pk_inspection PRIMARY KEY (company_id, inspection_id)
);
CREATE INDEX ix_inspection_approval ON inspection(company_id, approval_id);

CREATE TABLE inspection_item (
  company_id CHAR(5),
  inspection_id CHAR(10),
  line_no INTEGER,
  name  VARCHAR(100),
  method     VARCHAR(100),
  min_val   VARCHAR(50),
  max_val   VARCHAR(50),
  std_val   VARCHAR(50),
  unit       VARCHAR(50),
  result_val VARCHAR(50),
  note       VARCHAR(500),
  CONSTRAINT pk_inspection_item PRIMARY KEY (company_id, inspection_id,line_no)
);

CREATE TABLE work_order (
  company_id  CHAR(5),
  -- 기본정보 
  order_id       CHAR(10),  
  name        VARCHAR(100),
  plant_id  CHAR(10),
  job_id  CHAR(5),  --code_type="JOBTP"
  site_id   CHAR(5),
  --작업정보 
  dept_id CHAR(5),
  member_id CHAR(5),
  planned_date  DATE,
  planned_cost  DECIMAL(18,2),
  planned_labor DECIMAL(18,2),
  actual_date DATE,
  actual_cost  DECIMAL(18,2),
  actual_labor DECIMAL(18,2),

  status  VARCHAR(10),  -- PLN_DRAFT(계획작성), PLN_SUBMIT(계획결재상신), PLN_PROC(계획결재중), PLN_APPROV(계획승인), ACT_DRAFT(실적작성), ACT_SUBMIT(실적결재상신), ACT_PROC(실적결재중), ACT_APPROV(실적승인완료)
  approval_id CHAR(10), -- 최근 approval 테이블 연결 (2단계 결재: 계획 refStage="PLN", 실적 refStage="ACT")
  file_group_id CHAR(10),
  note        VARCHAR(500),
  created_at  TIMESTAMP,
  created_by  CHAR(10),
  updated_at  TIMESTAMP,
  updated_by  CHAR(10),
  CONSTRAINT pk_work_order PRIMARY KEY (company_id, order_id)
);
CREATE INDEX ix_work_order_approval ON work_order(company_id, approval_id);

CREATE TABLE work_order_item (
  company_id CHAR(5),
  order_id      CHAR(10),
  line_no    INTEGER,
  name  VARCHAR(100),
  method     VARCHAR(100),
  result     VARCHAR(100),
  note       VARCHAR(500),
  CONSTRAINT pk_work_order_item PRIMARY KEY (company_id, order_id, line_no)
);

CREATE TABLE work_permit (
  company_id  CHAR(5),
  -- 기본정보 
  permit_id       CHAR(10),
  name        VARCHAR(100),
  plant_id  CHAR(10),
  job_id  CHAR(5),  --code_type="PERMT"
  site_id   CHAR(5),
  --작업정보 
  dept_id CHAR(5),
  member_id CHAR(5),
  planned_date  DATE,
  actual_date DATE,
  --안전정보
  work_summary  VARCHAR(500),
  hazard_factor VARCHAR(500),
  safety_factor VARCHAR(500),
  
  checksheet_json  LONGTEXT,  -- 안전작업 체크리스트 JSON

  status  VARCHAR(10),  -- PLN_DRAFT(계획작성), PLN_SUBMIT(계획결재상신), PLN_PROC(계획결재중), PLN_APPROV(계획승인완료)
  approval_id CHAR(10), -- approval 테이블 연결 (PLN_SUBMIT 시 생성, refStage=null)
  file_group_id CHAR(10),
  note        VARCHAR(500),
  created_at  TIMESTAMP,
  created_by  CHAR(10),
  updated_at  TIMESTAMP,
  updated_by  CHAR(10),
  CONSTRAINT pk_work_permit PRIMARY KEY (company_id, permit_id)
);
CREATE INDEX ix_work_permit_approval ON work_permit(company_id, approval_id);

CREATE TABLE work_permit_item (
  company_id CHAR(5),
  permit_id      CHAR(10),
  line_no    INTEGER,
  name VARCHAR(100),
  signature  LONGTEXT, -- 이미지 , base64
  CONSTRAINT pk_work_permit_item PRIMARY KEY (company_id, permit_id, line_no)
);

CREATE TABLE inventory_stock (
  company_id   CHAR(5),
  storage_id   CHAR(5),
  inventory_id CHAR(10),
  qty          DECIMAL(18,3),
  amount       DECIMAL(18,2),
  updated_at   TIMESTAMP,
  updated_by   CHAR(10),
  CONSTRAINT pk_inventory_stock PRIMARY KEY (company_id, storage_id, inventory_id)
);

CREATE TABLE inventory_history (
  company_id   CHAR(5),
  history_id      CHAR(10),
  inventory_id CHAR(10),
  storage_id   CHAR(5),
  tx_type      CHAR(5),      
  ref_no       CHAR(10),
  ref_line     INTEGER,
  tx_date      DATE,
  in_qty       DECIMAL(18,3),
  out_qty      DECIMAL(18,3),
  unit_cost    DECIMAL(18,6),
  amount       DECIMAL(18,2),
  note         VARCHAR(500),
  created_at   TIMESTAMP,
  created_by   CHAR(10),
  updated_at   TIMESTAMP,
  updated_by   CHAR(10),
  CONSTRAINT pk_inventory_history PRIMARY KEY (company_id, history_id)
);

CREATE TABLE inventory_closing (
  company_id   CHAR(5),
  yyyymm           CHAR(6),     
  storage_id   CHAR(5),
  inventory_id CHAR(10),
  begin_qty    DECIMAL(18,3),
  begin_amount DECIMAL(18,2),
  in_qty       DECIMAL(18,3),
  in_amount    DECIMAL(18,2),
  out_qty      DECIMAL(18,3),
  out_amount   DECIMAL(18,2),
  move_qty     DECIMAL(18,2),
  move_amount  DECIMAL(18,2),
  adj_qty      DECIMAL(18,3),
  adj_amount   DECIMAL(18,2),
  end_qty      DECIMAL(18,3),
  end_amount   DECIMAL(18,2),
  status       VARCHAR(10),
  closed_at    TIMESTAMP,
  closed_by    CHAR(10),
  CONSTRAINT pk_inventory_closing PRIMARY KEY (company_id, yyyymm, storage_id, inventory_id)
);

CREATE TABLE memo (
  company_id CHAR(5),
  memo_id    CHAR(10),
  title      VARCHAR(100),
  content    LONGTEXT,
  ref_entity VARCHAR(64),
  ref_id     CHAR(10),
  stage      VARCHAR(10),  -- PLN, ACT (선택적, 업무 모듈 연계 시 사용)
  status     VARCHAR(10),  -- DRAFT, SUBMIT, PROC, APPROV (선택적, 업무 모듈 연계 시 사용)
  file_group_id CHAR(10),
  created_at TIMESTAMP,
  created_by CHAR(10),
  updated_at TIMESTAMP,
  updated_by CHAR(10),
  CONSTRAINT pk_memo PRIMARY KEY (company_id, memo_id)
);

CREATE TABLE approval (
  company_id  CHAR(5),
  approval_id CHAR(10),
  title       VARCHAR(100),
  status  VARCHAR(10),  -- DRAFT(임시저장), SUBMIT(결재상신), PROC(결재진행중), APPROV(최종승인), REJECT(반려)
  ref_entity  VARCHAR(64), -- 원본 모듈 (INSP, WORK, WPER)
  ref_id      CHAR(10),    -- 원본 문서 ID
  ref_stage   VARCHAR(10), -- 단계 (null=단일결재, PLAN=계획, ACT=실적) - WorkOrder 2단계 결재용
  content     LONGTEXT,    -- 결재 본문 (HTML)
  file_group_id CHAR(10),
  submitted_at TIMESTAMP,  -- 상신 일시
  completed_at TIMESTAMP,  -- 완료 일시
  created_at  TIMESTAMP,
  created_by  CHAR(10),
  updated_at  TIMESTAMP,
  updated_by  CHAR(10),
  CONSTRAINT pk_approval PRIMARY KEY (company_id, approval_id)
);
CREATE INDEX ix_approval_ref ON approval(company_id, ref_entity, ref_id);
CREATE INDEX ix_approval_ref_stage ON approval(company_id, ref_entity, ref_id, ref_stage);

CREATE TABLE approval_step (
  company_id  CHAR(5),
  approval_id CHAR(10),
  step_no     INTEGER,
  member_id   CHAR(5),
  decision    VARCHAR(10),  -- 결재 역할: APPROVAL(결재), AGREE(합의), INFORM(통보)
  result      VARCHAR(10),  -- 결재 결과: APPROVE(승인), REJECT(반려), PENDING(대기) 또는 NULL
  decided_at  TIMESTAMP,    -- 결재 완료 일시
  comment     VARCHAR(500), -- 결재 의견
  CONSTRAINT pk_approval_step PRIMARY KEY (company_id, approval_id, step_no)
);

CREATE TABLE file_group (
  company_id    CHAR(5),
  file_group_id CHAR(10),
  ref_entity    VARCHAR(64), 
  ref_id        CHAR(10),
  note          VARCHAR(500),
  --delete_mark   CHAR(1) DEFAULT 'N',
  created_at    TIMESTAMP,
  created_by    CHAR(10),
  updated_at    TIMESTAMP,
  updated_by    CHAR(10),
  CONSTRAINT pk_file_group PRIMARY KEY (company_id, file_group_id)
);
CREATE INDEX ix_file_group_ref ON file_group(company_id, ref_entity, ref_id);

CREATE TABLE file_item (
  company_id     CHAR(5),
  file_group_id  CHAR(10),
  file_id        CHAR(10),
  line_no        INTEGER,        
  original_name  VARCHAR(255),
  stored_name    VARCHAR(255),
  ext            VARCHAR(10),
  mime           VARCHAR(100),  --content_type
  size           BIGINT,
  checksum_sha256 CHAR(64),
  storage_path   VARCHAR(255),   
  note           VARCHAR(500),
  --delete_mark    CHAR(1) DEFAULT 'N',
  created_at     TIMESTAMP,
  created_by     CHAR(10),
  updated_at     TIMESTAMP,
  updated_by     CHAR(10),
  CONSTRAINT pk_file_item PRIMARY KEY (company_id, file_group_id, file_id)
);
CREATE UNIQUE INDEX ux_file_item_ord ON file_item(company_id, file_group_id, line_no);
CREATE INDEX ix_file_item_path ON file_item(company_id, storage_path);

## 시퀀스 관리 (sequence)
```sql
CREATE TABLE sequence (
  company_id CHAR(5),
  module_code CHAR(1),
  date_key CHAR(6),        -- 트랜잭션:YYMMDD/마스터:000000
  next_seq INTEGER DEFAULT 1,
  CONSTRAINT pk_sequence PRIMARY KEY (company_id, module_code, date_key)
);
```

---

## 상태값 표준화 요약

### 상태 규칙

**업무 모듈 상태**: `{PLN|ACT}_{DRAFT|SUBMIT|PROC|APPROV}`
**결재 모듈 상태**: `{DRAFT|SUBMIT|PROC|APPROV|REJECT}`

### Approval 모듈

| 상태 | 설명 |
|------|------|
| DRAFT | 임시저장 (결재선 미입력, 수정/삭제 가능) |
| SUBMIT | 결재 상신 ⭐ (approval/form.html에서 결재선 입력 후 전환) |
| PROC | 다단계 결재 진행 중 (향후 확장용) |
| APPROV | 최종 승인 완료 |
| REJECT | 반려 (원본 모듈 *_DRAFT로 복원) |

**ref_stage 값**:
- `null`: 단일 결재 (Inspection, WorkPermit)
- `"PLN"`: 계획 단계 결재 (WorkOrder)
- `"ACT"`: 실적 단계 결재 (WorkOrder)

### Inspection 모듈

| 상태 | 설명 |
|------|------|
| PLN_APPROV | 계획 자동 확정 (결재 없음) |
| ACT_DRAFT | 실적 작성 중 (수정 가능) |
| ACT_SUBMIT | 실적 결재 상신 (수정 차단) |
| ACT_PROC | 실적 결재 진행 중 (향후 확장용) |
| ACT_APPROV | 실적 승인 완료 (최종 완료) |

**결재 흐름**: 
- 계획: PLN_APPROV (자동 확정)
- 실적: PLN_APPROV → **[실적 입력 버튼]** → ACT_DRAFT → ACT_SUBMIT → ACT_APPROV

### WorkOrder 모듈 (2단계 결재)

| 상태 | 설명 |
|------|------|
| PLN_DRAFT | 계획 작성 중 (수정 가능) |
| PLN_SUBMIT | 계획 결재 상신 (수정 차단, ref_stage="PLN") |
| PLN_PROC | 계획 결재 진행 중 (향후 확장용) |
| PLN_APPROV | 계획 승인 완료 (실적 입력 가능) |
| ACT_DRAFT | 실적 작성 중 (수정 가능) |
| ACT_SUBMIT | 실적 결재 상신 (수정 차단, ref_stage="ACT") |
| ACT_PROC | 실적 결재 진행 중 (향후 확장용) |
| ACT_APPROV | 실적 승인 완료 (최종 완료) |

**결재 흐름**: 
- 계획: PLN_DRAFT → PLN_SUBMIT → PLN_APPROV
- 실적: PLN_APPROV → **[실적 입력 버튼]** → ACT_DRAFT → ACT_SUBMIT → ACT_APPROV

**실적 입력 버튼**: detail.html에서 `POST /workorder/{id}/ready-actual` 호출 → PLN_APPROV → ACT_DRAFT 전환 → form으로 리다이렉트

### WorkPermit 모듈

| 상태 | 설명 |
|------|------|
| PLN_DRAFT | 계획 작성 중 (수정 가능) |
| PLN_SUBMIT | 계획 결재 상신 (수정 차단, ref_stage=null) |
| PLN_PROC | 계획 결재 진행 중 (향후 확장용) |
| PLN_APPROV | 계획 승인 완료 (최종 완료) |

**결재 흐름**: PLN_DRAFT → PLN_SUBMIT → PLN_APPROV

### 반려 시 상태 복원

Approval.status가 REJECT로 전환되면 원본 모듈 상태를 DRAFT로 복원하여 수정 후 재상신 가능하게 합니다.

| 원본 모듈 상태 | 반려 후 복원 상태 |
|-------------|----------------|
| *_SUBMIT | *_DRAFT |
| *_PROC | *_DRAFT |
