# 테이블 구조 가이드

본 문서는 현재 구현된 JPA 엔티티에 맞춰 CMMS11 데이터 모델을 정리한 자료입니다. 실제 스키마는 데이터베이스 Dialect에 의해 약간 달라질 수 있으나, 컬럼 구성과 길이는 아래 정의를 기준으로 관리합니다.

## 기본 원칙
- 모든 테이블은 `company_id CHAR(5)`를 선행 컬럼으로 사용합니다.
- 기본 키가 아닌 컬럼은 가급적 NULL 허용으로 두고, 애플리케이션 레이어에서 검증합니다.
- 문자열은 별도 지정이 없는 한 `VARCHAR` 타입을 사용합니다.
- 감사 컬럼(`created_at/by`, `updated_at/by`)과 소프트 삭제 플래그(`delete_mark`)는 공통 패턴을 따릅니다.
- 코드 값(`status`, `decision` 등)은 `code_type`/`code_item` 테이블 및 `DataInitializer` 시드 값을 기준으로 관리합니다.

---

## 도메인 테이블 (`domain`)
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
  created_by  VARCHAR(10),
  updated_at  TIMESTAMP,
  updated_by  VARCHAR(10),
  CONSTRAINT pk_company PRIMARY KEY (company_id)
);

CREATE TABLE site (
  company_id  CHAR(5),
  site_id     CHAR(5),
  name        VARCHAR(100),
  phone       VARCHAR(30),
  address     VARCHAR(200),
  note        VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at  TIMESTAMP,
  created_by  VARCHAR(10),
  updated_at  TIMESTAMP,
  updated_by  VARCHAR(10),
  CONSTRAINT pk_site PRIMARY KEY (company_id, site_id)
);

CREATE TABLE dept (
  company_id  CHAR(5),
  dept_id     CHAR(5),
  name        VARCHAR(100),
  phone       VARCHAR(30),
  address     VARCHAR(200),
  note        VARCHAR(500),
  parent_id   CHAR(5),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at  TIMESTAMP,
  created_by  VARCHAR(10),
  updated_at  TIMESTAMP,
  updated_by  VARCHAR(10),
  CONSTRAINT pk_dept PRIMARY KEY (company_id, dept_id)
);

CREATE TABLE member (
  company_id    CHAR(5),
  member_id     CHAR(5),
  name          VARCHAR(100),
  dept_id       CHAR(5),
  password_hash VARCHAR(100),
  email         VARCHAR(100),
  phone         VARCHAR(100),
  site_id       CHAR(5),
  position      VARCHAR(50),
  title         VARCHAR(50),
  note          VARCHAR(500),
  delete_mark   CHAR(1) DEFAULT 'N',
  created_at    TIMESTAMP,
  created_by    VARCHAR(10),
  updated_at    TIMESTAMP,
  updated_by    VARCHAR(10),
  last_login_at TIMESTAMP,
  last_login_ip VARCHAR(45),
  CONSTRAINT pk_member PRIMARY KEY (company_id, member_id)
);

CREATE TABLE func (
  company_id  CHAR(5),
  func_id     CHAR(5),
  name        VARCHAR(100),
  note        VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at  TIMESTAMP,
  created_by  VARCHAR(10),
  updated_at  TIMESTAMP,
  updated_by  VARCHAR(10),
  CONSTRAINT pk_func PRIMARY KEY (company_id, func_id)
);

CREATE TABLE storage (
  company_id  CHAR(5),
  storage_id  CHAR(5),
  name        VARCHAR(100),
  note        VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at  TIMESTAMP,
  created_by  VARCHAR(10),
  updated_at  TIMESTAMP,
  updated_by  VARCHAR(10),
  CONSTRAINT pk_storage PRIMARY KEY (company_id, storage_id)
);

CREATE TABLE role (
  company_id  CHAR(5),
  role_id     CHAR(5),
  name        VARCHAR(100),
  note        VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at  TIMESTAMP,
  created_by  VARCHAR(10),
  updated_at  TIMESTAMP,
  updated_by  VARCHAR(10),
  CONSTRAINT pk_role PRIMARY KEY (company_id, role_id)
);
```
> 참고: 역할-권한 매핑 테이블(`rolemap`, `permission`, `role_permission`)은 아직 엔티티로 구현되지 않았습니다.

---

## 공통 코드 (`code`)
```sql
CREATE TABLE code_type (
  company_id  CHAR(5),
  code_type   CHAR(5),
  name        VARCHAR(100),
  note        VARCHAR(500),
  delete_mark CHAR(1) DEFAULT 'N',
  created_at  TIMESTAMP,
  created_by  VARCHAR(10),
  updated_at  TIMESTAMP,
  updated_by  VARCHAR(10),
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

---

## 마스터 (`plant`, `inventory`)
```sql
CREATE TABLE plant (
  company_id           CHAR(5),
  plant_id             CHAR(10),
  name                 VARCHAR(100),
  asset_id             CHAR(5),
  site_id              CHAR(5),
  dept_id              CHAR(5),
  func_id              CHAR(5),
  maker_name           VARCHAR(100),
  spec                 VARCHAR(100),
  model                VARCHAR(100),
  serial               VARCHAR(100),
  install_date         DATE,
  depre_id             CHAR(5),
  depre_period         INTEGER,
  purchase_cost        DECIMAL(18, 2),
  residual_value       DECIMAL(18, 2),
  inspection_yn        CHAR(1),
  psm_yn               CHAR(1),
  workpermit_yn        CHAR(1),
  inspection_interval  INTEGER,
  last_inspection      DATE,
  next_inspection      DATE,
  file_group_id        CHAR(10),
  note                 VARCHAR(500),
  delete_mark          CHAR(1) DEFAULT 'N',
  created_at           TIMESTAMP,
  created_by           VARCHAR(10),
  updated_at           TIMESTAMP,
  updated_by           VARCHAR(10),
  CONSTRAINT pk_plant PRIMARY KEY (company_id, plant_id)
);

CREATE TABLE inventory (
  company_id   CHAR(5),
  inventory_id CHAR(10),
  name         VARCHAR(100),
  unit         VARCHAR(20),
  unit_cost    DECIMAL(15, 2),
  quantity     DECIMAL(15, 2),
  storage_id   CHAR(10),
  asset_id     CHAR(5),
  dept_id      CHAR(5),
  maker_name   VARCHAR(100),
  spec         VARCHAR(100),
  model        VARCHAR(100),
  serial       VARCHAR(100),
  file_group_id CHAR(10),
  note         VARCHAR(500),
  delete_mark  CHAR(1) DEFAULT 'N',
  created_at   TIMESTAMP,
  created_by   VARCHAR(10),
  updated_at   TIMESTAMP,
  updated_by   VARCHAR(10),
  CONSTRAINT pk_inventory PRIMARY KEY (company_id, inventory_id)
);
```

---

## 트랜잭션 테이블
```sql
CREATE TABLE inspection (
  company_id    CHAR(5),
  inspection_id CHAR(10),
  name          VARCHAR(100),
  plant_id      CHAR(10),
  job_id        CHAR(5),
  site_id       CHAR(5),
  dept_id       CHAR(5),
  member_id     CHAR(5),
  planned_date  DATE,
  actual_date   DATE,
  status        VARCHAR(10),
  stage         VARCHAR(10),
  ref_entity    VARCHAR(10),
  ref_id        CHAR(10),
  ref_stage     VARCHAR(10),
  approval_id   CHAR(10),
  file_group_id CHAR(10),
  note          VARCHAR(500),
  created_at    TIMESTAMP,
  created_by    VARCHAR(10),
  updated_at    TIMESTAMP,
  updated_by    VARCHAR(10),
  CONSTRAINT pk_inspection PRIMARY KEY (company_id, inspection_id)
);
CREATE INDEX ix_inspection_approval ON inspection(company_id, approval_id);

CREATE TABLE inspection_item (
  company_id    CHAR(5),
  inspection_id CHAR(10),
  line_no       INTEGER,
  name          VARCHAR(100),
  method        VARCHAR(100),
  min_val       VARCHAR(50),
  max_val       VARCHAR(50),
  std_val       VARCHAR(50),
  unit          VARCHAR(50),
  result_val    VARCHAR(50),
  note          VARCHAR(500),
  CONSTRAINT pk_inspection_item PRIMARY KEY (company_id, inspection_id, line_no)
);

CREATE TABLE work_order (
  company_id   CHAR(5),
  order_id     CHAR(10),
  name         VARCHAR(100),
  plant_id     CHAR(10),
  job_id       CHAR(5),
  site_id      CHAR(5),
  dept_id      CHAR(5),
  member_id    CHAR(5),
  planned_date DATE,
  planned_cost  DECIMAL(18, 2),
  planned_labor DECIMAL(18, 2),
  actual_date   DATE,
  actual_cost   DECIMAL(18, 2),
  actual_labor  DECIMAL(18, 2),
  status        VARCHAR(10),
  stage         VARCHAR(10),
  ref_entity    VARCHAR(10),
  ref_id        CHAR(10),
  ref_stage     VARCHAR(10),
  approval_id   CHAR(10),
  file_group_id CHAR(10),
  note          VARCHAR(500),
  created_at    TIMESTAMP,
  created_by    VARCHAR(10),
  updated_at    TIMESTAMP,
  updated_by    VARCHAR(10),
  CONSTRAINT pk_work_order PRIMARY KEY (company_id, order_id)
);
CREATE INDEX ix_work_order_approval ON work_order(company_id, approval_id);

CREATE TABLE work_order_item (
  company_id CHAR(5),
  order_id   CHAR(10),
  line_no    INTEGER,
  name       VARCHAR(100),
  method     VARCHAR(100),
  result     VARCHAR(100),
  note       VARCHAR(500),
  created_at TIMESTAMP,
  created_by VARCHAR(10),
  updated_at TIMESTAMP,
  updated_by VARCHAR(10),
  CONSTRAINT pk_work_order_item PRIMARY KEY (company_id, order_id, line_no)
);

CREATE TABLE work_permit (
  company_id     CHAR(5),
  permit_id      CHAR(10),
  name           VARCHAR(100),
  plant_id       CHAR(10),
  job_id         CHAR(5),
  site_id        CHAR(5),
  dept_id        CHAR(5),
  member_id      CHAR(5),
  planned_date   DATE,
  actual_date    DATE,
  work_summary   VARCHAR(500),
  hazard_factor  VARCHAR(500),
  safety_factor  VARCHAR(500),
  checksheet_json TEXT,
  status         VARCHAR(10),
  stage          VARCHAR(10),
  ref_entity     VARCHAR(10),
  ref_id         CHAR(10),
  ref_stage      VARCHAR(10),
  approval_id    CHAR(10),
  file_group_id  CHAR(10),
  note           VARCHAR(500),
  created_at     TIMESTAMP,
  created_by     VARCHAR(10),
  updated_at     TIMESTAMP,
  updated_by     VARCHAR(10),
  CONSTRAINT pk_work_permit PRIMARY KEY (company_id, permit_id)
);
CREATE INDEX ix_work_permit_approval ON work_permit(company_id, approval_id);

CREATE TABLE work_permit_item (
  company_id CHAR(5),
  permit_id  CHAR(10),
  line_no    INTEGER,
  name       VARCHAR(100),
  signature  TEXT,
  note       VARCHAR(500),
  created_at TIMESTAMP,
  created_by VARCHAR(10),
  updated_at TIMESTAMP,
  updated_by VARCHAR(10),
  CONSTRAINT pk_work_permit_item PRIMARY KEY (company_id, permit_id, line_no)
);

CREATE TABLE inventory_stock (
  company_id   CHAR(5),
  storage_id   CHAR(5),
  inventory_id CHAR(10),
  qty          DECIMAL(18, 3),
  amount       DECIMAL(18, 2),
  updated_at   TIMESTAMP,
  updated_by   VARCHAR(10),
  CONSTRAINT pk_inventory_stock PRIMARY KEY (company_id, storage_id, inventory_id)
);

CREATE TABLE inventory_history (
  company_id   CHAR(5),
  history_id   CHAR(10),
  inventory_id CHAR(10),
  storage_id   CHAR(5),
  tx_type      CHAR(5),
  ref_no       CHAR(10),
  ref_line     INTEGER,
  tx_date      DATE,
  in_qty       DECIMAL(18, 3),
  out_qty      DECIMAL(18, 3),
  unit_cost    DECIMAL(18, 6),
  amount       DECIMAL(18, 2),
  note         VARCHAR(500),
  created_at   TIMESTAMP,
  created_by   VARCHAR(10),
  updated_at   TIMESTAMP,
  updated_by   VARCHAR(10),
  CONSTRAINT pk_inventory_history PRIMARY KEY (company_id, history_id)
);

CREATE TABLE inventory_closing (
  company_id   CHAR(5),
  yyyymm       CHAR(6),
  storage_id   CHAR(5),
  inventory_id CHAR(10),
  begin_qty    DECIMAL(18, 3),
  begin_amount DECIMAL(18, 2),
  in_qty       DECIMAL(18, 3),
  in_amount    DECIMAL(18, 2),
  out_qty      DECIMAL(18, 3),
  out_amount   DECIMAL(18, 2),
  move_qty     DECIMAL(18, 2),
  move_amount  DECIMAL(18, 2),
  adj_qty      DECIMAL(18, 3),
  adj_amount   DECIMAL(18, 2),
  end_qty      DECIMAL(18, 3),
  end_amount   DECIMAL(18, 2),
  status       VARCHAR(5),
  closed_at    TIMESTAMP,
  closed_by    VARCHAR(10),
  CONSTRAINT pk_inventory_closing PRIMARY KEY (company_id, yyyymm, storage_id, inventory_id)
);

CREATE TABLE memo (
  company_id    CHAR(5),
  memo_id       CHAR(10),
  title         VARCHAR(100),
  content       TEXT,
  plant_id      CHAR(10),
  stage         VARCHAR(10),
  status        VARCHAR(10),
  approval_id   CHAR(10),
  ref_entity    VARCHAR(64),
  ref_id        CHAR(10),
  ref_stage     VARCHAR(10),
  file_group_id CHAR(10),
  created_at    TIMESTAMP,
  created_by    VARCHAR(10),
  updated_at    TIMESTAMP,
  updated_by    VARCHAR(10),
  CONSTRAINT pk_memo PRIMARY KEY (company_id, memo_id)
);
```

---

## 결재 및 Webhook
```sql
CREATE TABLE approval (
  company_id      CHAR(5),
  approval_id     CHAR(10),
  title           VARCHAR(100),
  status          VARCHAR(10),
  ref_entity      VARCHAR(64),
  ref_id          CHAR(10),
  ref_stage       VARCHAR(10),
  callback_url    VARCHAR(255),
  idempotency_key VARCHAR(100) UNIQUE,
  content         TEXT,
  file_group_id   CHAR(10),
  submitted_at    TIMESTAMP,
  completed_at    TIMESTAMP,
  created_at      TIMESTAMP,
  created_by      VARCHAR(10),
  updated_at      TIMESTAMP,
  updated_by      VARCHAR(10),
  CONSTRAINT pk_approval PRIMARY KEY (company_id, approval_id)
);
CREATE INDEX ix_approval_ref ON approval(company_id, ref_entity, ref_id);
CREATE INDEX ix_approval_ref_stage ON approval(company_id, ref_entity, ref_id, ref_stage);

CREATE TABLE approval_step (
  company_id  CHAR(5),
  approval_id CHAR(10),
  step_no     INTEGER,
  member_id   CHAR(5),
  decision    VARCHAR(10),
  result      VARCHAR(10),
  decided_at  TIMESTAMP,
  comment     VARCHAR(500),
  CONSTRAINT pk_approval_step PRIMARY KEY (company_id, approval_id, step_no)
);

CREATE TABLE approval_inbox (
  company_id        CHAR(5),
  inbox_id          CHAR(10),
  member_id         CHAR(5),
  approval_id       CHAR(10),
  step_no           INTEGER,
  inbox_type        VARCHAR(10),
  is_read           CHAR(1) DEFAULT 'N',
  read_at           TIMESTAMP,
  notified_at       TIMESTAMP,
  notification_type VARCHAR(20),
  title             VARCHAR(100),
  ref_entity        VARCHAR(64),
  ref_id            CHAR(10),
  submitted_by      VARCHAR(10),
  submitted_at      TIMESTAMP,
  decision          VARCHAR(10),
  created_at        TIMESTAMP NOT NULL,
  updated_at        TIMESTAMP,
  CONSTRAINT pk_approval_inbox PRIMARY KEY (company_id, inbox_id)
);
CREATE INDEX ix_approval_inbox_member ON approval_inbox(company_id, member_id, inbox_type);

CREATE TABLE approval_outbox (
  id               BIGINT AUTO_INCREMENT,
  company_id       CHAR(5)     NOT NULL,
  approval_id      CHAR(10)    NOT NULL,
  callback_url     VARCHAR(255) NOT NULL,
  idempotency_key  VARCHAR(100),
  event_type       VARCHAR(20) NOT NULL,
  status           VARCHAR(10) NOT NULL,
  payload          TEXT        NOT NULL,
  retry_count      INTEGER     NOT NULL DEFAULT 0,
  last_error_message VARCHAR(500),
  last_attempt_at  TIMESTAMP,
  next_attempt_at  TIMESTAMP,
  created_at       TIMESTAMP   NOT NULL,
  updated_at       TIMESTAMP,
  CONSTRAINT pk_approval_outbox PRIMARY KEY (id)
);
CREATE INDEX ix_approval_outbox_status ON approval_outbox(status, next_attempt_at);

CREATE TABLE approval_webhook_log (
  id           BIGINT AUTO_INCREMENT,
  outbox_id    BIGINT,
  company_id   CHAR(5)  NOT NULL,
  approval_id  CHAR(10) NOT NULL,
  webhook_url  VARCHAR(255) NOT NULL,
  http_status  INTEGER,
  response_body TEXT,
  error_message VARCHAR(500),
  created_at   TIMESTAMP NOT NULL,
  CONSTRAINT pk_approval_webhook_log PRIMARY KEY (id)
);

CREATE TABLE webhook_idempotency (
  company_id      CHAR(5),
  idempotency_key VARCHAR(100),
  processed_at    TIMESTAMP NOT NULL,
  CONSTRAINT pk_webhook_idempotency PRIMARY KEY (company_id, idempotency_key)
);
```

---

## 파일 관리
```sql
CREATE TABLE file_group (
  company_id    CHAR(5),
  file_group_id CHAR(10),
  ref_entity    VARCHAR(64),
  ref_id        CHAR(10),
  note          VARCHAR(500),
  delete_mark   CHAR(1) DEFAULT 'N',
  created_at    TIMESTAMP,
  created_by    VARCHAR(10),
  updated_at    TIMESTAMP,
  updated_by    VARCHAR(10),
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
  mime           VARCHAR(100),
  size           BIGINT,
  checksum_sha256 CHAR(64),
  storage_path   VARCHAR(255),
  note           VARCHAR(500),
  delete_mark    CHAR(1) DEFAULT 'N',
  created_at     TIMESTAMP,
  created_by     VARCHAR(10),
  updated_at     TIMESTAMP,
  updated_by     VARCHAR(10),
  CONSTRAINT pk_file_item PRIMARY KEY (company_id, file_group_id, file_id)
);
CREATE UNIQUE INDEX ux_file_item_line ON file_item(company_id, file_group_id, line_no);
CREATE INDEX ix_file_item_path ON file_item(company_id, storage_path);
```

---

## 시퀀스 (`common.seq`)
```sql
CREATE TABLE sequence (
  company_id  CHAR(5),
  module_code CHAR(1),
  date_key    CHAR(6),
  next_seq    INTEGER DEFAULT 1,
  CONSTRAINT pk_sequence PRIMARY KEY (company_id, module_code, date_key)
);
```

---

## 상태값 표준화 요약

### 결재(Approval) 상태
| status | 설명 |
|--------|------|
| DRAFT  | 임시 저장 |
| SUBMT  | 상신 |
| PROC   | 진행 중 (확장용) |
| APPRV  | 승인 완료 |
| REJCT  | 반려 |
| CNCLD  | 상신 취소 |

### 업무 모듈 Stage/Status 조합
- `stage`는 `PLN`(계획), `ACT`(실적)을 사용합니다. WorkPermit은 기본적으로 `PLN` 단계만 사용하지만 확장을 위해 `ACT` 값도 수용하도록 되어 있습니다.
- `status`는 `DRAFT`, `SUBMT`, `APPRV`, `REJCT`, `CMPLT`를 기본으로 합니다.

**예시**
- `PLN+DRAFT` : 계획 작성 중
- `PLN+SUBMT` : 계획 결재 상신
- `PLN+APPRV` : 계획 결재 승인
- `ACT+SUBMT` : 실적 결재 상신
- `ACT+APPRV` : 실적 결재 승인
- `*-CMPLT`  : 결재 없이 확정 처리

--- 

필요 시 엔티티 변경 사항을 반영하여 본 문서를 최신 상태로 유지해 주세요.
