# Approval Inbox 도입 계획서

**다음 내용을 AI에게 전달하여 구현을 지시할 수 있습니다.**

---

## 📘 목차

1. [목표](#-목표)
2. [기술 스펙](#-기술-스펙)
3. [Task 및 Checklist](#-task-및-checklist)
4. [주의사항 및 리스크](#️-주의사항-및-리스크)
5. [실행 지시문](#-실행-지시문)

---

## 🎯 목표

### Approval Inbox 시스템 도입을 통한 결재 UX 및 성능 개선

#### 핵심 목표

- **순서 제어**: 결재선 순서 위반 방지 (1번 미결재 시 3번 처리 불가)
- **읽음 관리**: 읽지 않은 결재 N건 표시로 UX 개선
- **성능 개선**: approval + approval_step JOIN 제거, 단일 테이블 조회
- **알림 연계**: 푸시/이메일 알림 전송 추적 가능
- **콜백 보존**: 기존 원본 모듈 콜백 메커니즘 영향 없이 구현

#### 비목표 (Scope 제외)

- ❌ 알림 시스템 구축 (이메일/푸시) - 향후 Phase로 이관
- ❌ 다단계 결재 (PROC 상태 활용) - 현재는 1단계만
- ❌ 결재 위임/대결 기능 - 향후 확장

---

## 📐 기술 스펙

### 1. DB 스키마

**⚠️ 참고**: 
- 상태값은 `DataInitializer.java`의 APPRV, DECSN 코드 참조
- NOT NULL 제약은 PK만 적용 (나머지는 nullable)

```sql
CREATE TABLE approval_inbox (
  -- PK (NOT NULL 필수)
  company_id   CHAR(5) NOT NULL,
  inbox_id     CHAR(10) NOT NULL,
  
  -- 수신자 (NOT NULL 해제)
  member_id    CHAR(5),
  
  -- 결재 문서 연결 (NOT NULL 해제)
  approval_id  CHAR(10),
  step_no      INTEGER,
  
  -- Inbox 상태 (APPRV 코드와 통일)
  inbox_type   VARCHAR(10),        -- SUBMT(미결), APPRV(승인), REJCT(반려), CMPLT(통보확인)
  is_read      CHAR(1) DEFAULT 'N',
  read_at      TIMESTAMP,
  
  -- 알림 연계 (향후 확장)
  notified_at  TIMESTAMP,
  notification_type VARCHAR(20),   -- EMAIL, PUSH, SMS
  
  -- Denormalized 필드 (조회 성능 최적화)
  title        VARCHAR(100),
  ref_entity   VARCHAR(64),        -- INSP, WORK, WPER (MODUL 코드 참조)
  ref_id       CHAR(10),
  submitted_by CHAR(10),
  submitted_at TIMESTAMP,
  decision     VARCHAR(10),        -- APPRL(결재), AGREE(합의), INFO(참조) - DECSN 코드
  
  -- 감사 (created_at만 NOT NULL)
  created_at   TIMESTAMP NOT NULL,
  updated_at   TIMESTAMP,
  
  CONSTRAINT pk_approval_inbox PRIMARY KEY (company_id, inbox_id),
  CONSTRAINT fk_inbox_approval FOREIGN KEY (company_id, approval_id) 
    REFERENCES approval(company_id, approval_id) ON DELETE CASCADE,
  CONSTRAINT fk_inbox_step FOREIGN KEY (company_id, approval_id, step_no)
    REFERENCES approval_step(company_id, approval_id, step_no) ON DELETE CASCADE
);

-- 인덱스 (성능 최적화)
CREATE INDEX ix_inbox_member_read ON approval_inbox(company_id, member_id, is_read, inbox_type);
CREATE INDEX ix_inbox_approval ON approval_inbox(company_id, approval_id);
CREATE INDEX ix_inbox_submitted ON approval_inbox(company_id, member_id, submitted_at DESC);
CREATE INDEX ix_inbox_type_member ON approval_inbox(company_id, inbox_type, member_id);
```

**코드값 참조 (DataInitializer.java)**:
```java
// DECSN (결재자유형) - decision 필드
seedItems("DECSN", List.of(
    new SeedCodeItem("APPRL", "결재"),
    new SeedCodeItem("AGREE", "합의"),
    new SeedCodeItem("INFO", "참조")
));

// MODUL (참조모듈) - ref_entity 필드
seedItems("MODUL", List.of(
    new SeedCodeItem("INSP", "점검"),
    new SeedCodeItem("WORK", "작업지시"),
    new SeedCodeItem("WPER", "작업허가"),
    new SeedCodeItem("APPRL", "결재")
));
```

---

### 2. Inbox Type 상태

**⚠️ 참고**: inbox_type도 `DataInitializer.java`의 APPRV 코드와 통일

| inbox_type | 설명 | 전환 시점 | 연관 decision |
|------------|------|-----------|---------------|
| **SUBMT** | 미결 (제출됨) | 결재선 생성 시 (초기값) | - |
| **APPRV** | 기결 (승인) | approve() 호출 시 | APPRL, AGREE |
| **REJCT** | 반려 | reject() 호출 시 | APPRL, AGREE |
| **CMPLT** | 통보 확인 | 확인 처리 시 | **INFO**(참조) |

**매핑 변경**:
- ~~PENDING~~ → **SUBMT** (제출 - 미결재 상태)
- ~~APPROVED~~ → **APPRV** (승인)
- ~~REJECTED~~ → **REJCT** (반려)
- ~~INFORMED~~ → **CMPLT** (완료 - 통보 확인)

---

### 3. 순서 제어 규칙

**⚠️ 참고**: decision 값은 `DataInitializer.java`의 DECSN 코드 참조

| decision | 코드명 | 순서 제어 | 설명 |
|----------|--------|-----------|------|
| APPRL | 결재 | ✅ 필수 | 이전 APPRL/AGREE 모두 완료 후 가능 |
| AGREE | 합의 | ✅ 필수 | 이전 APPRL/AGREE 모두 완료 후 가능 |
| INFO | 참조 | ❌ 없음 | 순서 무관, 언제든 확인 가능 |

**매핑 규칙**:
- `APPRL` (코드) = "결재(APPROVAL)" (화면 표시)
- `AGREE` (코드) = "합의(AGREE)" (화면 표시)
- `INFO` (코드) = "참조(INFORM)" (화면 표시)

---

### 4. Java Entity

**⚠️ 참고**: nullable = false는 PK(EmbeddedId)와 created_at만 적용

```java
@Entity
@Table(name = "approval_inbox")
@Data  // Lombok getter/setter
public class ApprovalInbox {
    @EmbeddedId
    private ApprovalInboxId id;  // PK는 NOT NULL
    
    @Column(name = "member_id", length = 5)
    private String memberId;
    
    @Column(name = "approval_id", length = 10)
    private String approvalId;
    
    @Column(name = "step_no")
    private Integer stepNo;
    
    @Column(name = "inbox_type", length = 10)
    private String inboxType;  // SUBMT, APPRV, REJCT, CMPLT (APPRV 코드)
    
    @Column(name = "is_read", length = 1)
    private String isRead = "N";  // 기본값 'N'
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
    
    @Column(name = "notification_type", length = 20)
    private String notificationType;
    
    // Denormalized fields (조회 성능 최적화)
    @Column(length = 100)
    private String title;
    
    @Column(name = "ref_entity", length = 64)
    private String refEntity;  // INSP, WORK, WPER (MODUL 코드)
    
    @Column(name = "ref_id", length = 10)
    private String refId;
    
    @Column(name = "submitted_by", length = 10)
    private String submittedBy;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(length = 10)
    private String decision;  // APPRL, AGREE, INFO (DECSN 코드)
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  // 유일한 NOT NULL
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

@Embeddable
@Data
@EqualsAndHashCode
public class ApprovalInboxId implements Serializable {
    @Column(name = "company_id", length = 5, nullable = false)
    private String companyId;  // PK는 NOT NULL
    
    @Column(name = "inbox_id", length = 10, nullable = false)
    private String inboxId;  // PK는 NOT NULL
    
    // 기본 생성자
    public ApprovalInboxId() {}
    
    // 전체 생성자
    public ApprovalInboxId(String companyId, String inboxId) {
        this.companyId = companyId;
        this.inboxId = inboxId;
    }
}
```

---

## 📋 Task 및 Checklist

### TASK 1: 순서 제어 로직 구현 (선행 작업, 필수!)

**우선순위**: 🔴 높음 (버그 수정)

#### Checklist 1.1: 순서 검증 로직

- [ ] ApprovalService.processApproval() 메서드 수정
- [ ] 이전 단계 완료 여부 확인 로직 추가
  - [ ] 현재 사용자의 step_no 확인
  - [ ] step_no < 현재 단계인 step들의 decided_at 확인
  - [ ] **INFO**(참조)는 순서 체크 제외 ⚠️ DataInitializer 코드 참조
  - [ ] 미완료 발견 시 예외: "이전 결재자(N번: memberId)가 먼저 결재해야 합니다"
- [ ] 이전 단계 반려 확인
  - [ ] result="REJECT"인 경우 예외: "N번 결재자가 반려하여 결재가 종료되었습니다"
- [ ] 단위 테스트 작성
  - [ ] 정상: 순서대로 결재 (APPRL → AGREE)
  - [ ] 예외: 2번이 1번보다 먼저 결재 시도
  - [ ] 예외: 1번 반려 후 2번 결재 시도
  - [ ] 정상: **INFO**(참조)는 순서 무관

#### Checklist 1.2: 다단계 결재 완료 감지

- [ ] checkAllApproversCompleted() 메서드 추가
  - [ ] 모든 **APPRL**(결재)/**AGREE**(합의)의 decided_at 확인
  - [ ] **INFO**(참조)는 제외 ⚠️ DataInitializer 코드 참조
  - [ ] 하나라도 미결재면 status="PROC"
  - [ ] 모두 완료면 status="APPRV" → 콜백 호출
- [ ] 반려 시 즉시 종료 로직
  - [ ] result="REJCT"면 즉시 status="REJCT" → 콜백 호출
- [ ] 단위 테스트 작성
  - [ ] 1번 승인 → PROC 유지
  - [ ] 1,2번 모두 승인 → APPRV 전환
  - [ ] 1번 반려 → REJCT 전환 (2번 무시)

**참고**: 상태값은 `DataInitializer.java`의 APPRV 코드 참조
```java
seedItems("APPRV", List.of(
    new SeedCodeItem("DRAFT", "기안"),
    new SeedCodeItem("SUBMT", "제출"),
    new SeedCodeItem("PROC", "처리중"),
    new SeedCodeItem("APPRV", "승인"),
    new SeedCodeItem("REJCT", "반려"),
    new SeedCodeItem("CMPLT", "결재없이확정건")
));
```

#### 예상 산출물

- ApprovalService.java (수정)
- ApprovalServiceTest.java (신규)

#### 완료 기준

- ✅ 순서 위반 시 적절한 예외 발생
- ✅ 모든 단위 테스트 통과
- ✅ 기존 콜백 정상 동작 확인

---

### TASK 2: Inbox 스키마 및 Entity 구현

**우선순위**: 🟡 중간

#### Checklist 2.1: DB 스키마

- [ ] Flyway migration 파일 생성 (V1.1__add_approval_inbox.sql)
- [ ] approval_inbox 테이블 생성 (위 스펙 참조)
- [ ] 인덱스 4개 생성 (ix_inbox_member_read, ix_inbox_approval, ix_inbox_submitted, ix_inbox_type_member)
- [ ] FK 제약조건 2개 추가 (approval, approval_step)
- [ ] 로컬 DB에서 migration 실행 및 검증

#### Checklist 2.2: Entity 및 ID 클래스

- [ ] ApprovalInbox.java Entity 생성
  - [ ] @EmbeddedId ApprovalInboxId
  - [ ] 모든 컬럼 매핑 (inbox_type, is_read, read_at 등)
  - [ ] Denormalized 필드 매핑
- [ ] ApprovalInboxId.java EmbeddedId 생성
  - [ ] companyId + inboxId (복합키)
  - [ ] equals/hashCode 구현 (Lombok @EqualsAndHashCode)

#### Checklist 2.3: Repository

- [ ] ApprovalInboxRepository 인터페이스 생성
- [ ] 조회 메서드 정의:
  - [ ] findByIdCompanyIdAndMemberId(companyId, memberId, pageable)
  - [ ] findByIdCompanyIdAndMemberIdAndInboxType(companyId, memberId, inboxType, pageable)
  - [ ] findByIdCompanyIdAndMemberIdAndIsRead(companyId, memberId, isRead, pageable)
  - [ ] countByIdCompanyIdAndMemberIdAndIsRead(companyId, memberId, isRead)
  - [ ] findByIdCompanyIdAndApprovalIdAndMemberId(companyId, approvalId, memberId)
  - [ ] findByIdCompanyIdAndApprovalId(companyId, approvalId)
  - [ ] deleteByIdCompanyIdAndApprovalId(companyId, approvalId)

#### Checklist 2.4: DTO

- [ ] ApprovalInboxRequest.java (필요 시)
- [ ] ApprovalInboxResponse.java 생성
  - [ ] from(ApprovalInbox) 정적 팩토리 메서드
  - [ ] 모든 필드 매핑

#### 예상 산출물

- V1.1__add_approval_inbox.sql (신규)
- ApprovalInbox.java (신규)
- ApprovalInboxId.java (신규)
- ApprovalInboxRepository.java (신규)
- ApprovalInboxResponse.java (신규)

#### 완료 기준

- ✅ Migration 성공
- ✅ 테이블 및 인덱스 생성 확인
- ✅ Entity 매핑 테스트 통과

---

### TASK 3: Inbox 생성 로직 구현

**우선순위**: 🟡 중간

#### Checklist 3.1: replaceSteps 수정

- [ ] ApprovalService.replaceSteps() 메서드 수정
  - [ ] ApprovalInboxRepository 주입
  - [ ] 기존 inbox 삭제 로직 추가
    - [ ] inboxRepository.deleteByIdCompanyIdAndApprovalId(companyId, approvalId)
  - [ ] approval_step 생성 후 inbox 생성 로직 추가
    - [ ] inbox_id 자동 채번 (I + YYMMDD + 순번)
    - [ ] member_id, approval_id, step_no 설정
    - [ ] inbox_type = "**SUBMT**" (초기값 - 제출/미결) ⚠️ APPRV 코드
    - [ ] is_read = "N" (초기값)
    - [ ] decision 복사 (APPRL, AGREE, INFO)
    - [ ] Denormalized 필드 복사 (title, ref_entity, ref_id, submitted_by, submitted_at)
    - [ ] created_at 설정
  - [ ] 트랜잭션 범위 확인 (동일 @Transactional 내)

#### Checklist 3.2: submit 메서드 수정

- [ ] ApprovalService.submit() 메서드 확인
  - [ ] replaceSteps() 호출 후 inbox 자동 생성 확인
  - [ ] submitted_at 필드 inbox에 반영 확인

#### Checklist 3.3: 단위 테스트

- [ ] 결재선 3명 생성 → inbox 3개 생성 확인
- [ ] 결재선 수정 → 기존 inbox 삭제 + 신규 inbox 생성 확인
- [ ] inbox.title, ref_entity 등 denormalized 필드 정합성 확인

#### 예상 산출물

- ApprovalService.java (수정 - replaceSteps 메서드)
- ApprovalServiceTest.java (수정)

#### 완료 기준

- ✅ 결재선 생성 시 inbox 자동 생성
- ✅ Denormalized 필드 정확히 복사
- ✅ 단위 테스트 통과

---

### TASK 4: Inbox 상태 업데이트 로직

**우선순위**: 🟡 중간

#### Checklist 4.1: processApproval 수정

- [ ] ApprovalService.processApproval() 메서드 수정
  - [ ] 결재 처리 후 inbox 조회
    - [ ] inboxRepository.findByApprovalIdAndStepNo(companyId, approvalId, myStepNo)
  - [ ] inbox_type 전환 로직 ⚠️ APPRV 코드 사용
    - [ ] stepResult="APPROVE" → inbox_type="**APPRV**"
    - [ ] stepResult="REJECT" → inbox_type="**REJCT**"
    - [ ] decision="**INFO**" → inbox_type="**CMPLT**" (통보 확인 완료)
  - [ ] is_read 자동 처리
    - [ ] 결재 처리 = 읽음으로 간주
    - [ ] is_read="N"이면 "Y"로 전환
    - [ ] read_at 설정
  - [ ] updated_at 설정

#### Checklist 4.2: 예외 처리

- [ ] inbox가 없는 경우 로그만 출력 (오류 무시)
- [ ] inbox 업데이트 실패 시 롤백되지 않도록 try-catch (선택사항)

#### Checklist 4.3: 단위 테스트

- [ ] 1번 승인 → inbox_type="**APPRV**" 확인
- [ ] 2번 반려 → inbox_type="**REJCT**" 확인
- [ ] 3번 통보 → inbox_type="**CMPLT**" 확인
- [ ] is_read="Y", read_at 설정 확인

#### 예상 산출물

- ApprovalService.java (수정 - processApproval 메서드)

#### 완료 기준

- ✅ 결재 처리 시 inbox 상태 자동 업데이트
- ✅ is_read 자동 처리
- ✅ 기존 콜백 정상 동작 (영향 없음)

---

### TASK 5: Inbox 읽음 처리

**우선순위**: 🟢 낮음

#### Checklist 5.1: 읽음 처리 메서드

- [ ] ApprovalService.markInboxAsRead(inboxId) 메서드 추가
  - [ ] inbox 조회
  - [ ] is_read="N" → "Y" 전환
  - [ ] read_at 설정
  - [ ] updated_at 설정
  - [ ] 멱등성 보장 (이미 읽음이면 스킵)

#### Checklist 5.2: 자동 읽음 처리 (선택사항)

- [ ] ApprovalService.get(approvalId) 메서드 수정
  - [ ] detail.html 조회 시 자동 읽음 처리
  - [ ] 현재 사용자의 inbox 조회
  - [ ] is_read="N"이면 "Y"로 전환
  - [ ] 또는 명시적 API 호출 방식 (JavaScript에서 호출)

#### Checklist 5.3: API 엔드포인트

- [ ] ApprovalApiController 수정
  - [ ] PUT /api/approvals/inbox/{inboxId}/read 추가
  - [ ] GET /api/approvals/inbox/unread-count 추가 (읽지 않은 건수)

#### 예상 산출물

- ApprovalService.java (markInboxAsRead, get 수정)
- ApprovalApiController.java (수정)

#### 완료 기준

- ✅ 읽음 처리 API 정상 동작
- ✅ 읽지 않은 건수 조회 정상

---

### TASK 5.5: API Controller 구현 (ApprovalApiController)

**우선순위**: 🟡 중간

**⚠️ 중요**: JavaScript와 연동되는 모든 REST API 엔드포인트 구현

#### Checklist 5.5.1: 읽음 처리 API

- [ ] `PUT /api/approvals/inbox/{inboxId}/read` 구현
  ```java
  @RestController
  @RequestMapping("/api/approvals")
  public class ApprovalApiController {
      
      @Autowired
      private ApprovalService approvalService;
      
      /**
       * Inbox 읽음 처리
       * @param inboxId Inbox ID
       * @return 성공 응답
       */
      @PutMapping("/inbox/{inboxId}/read")
      public ResponseEntity<Map<String, Object>> markInboxAsRead(
          @PathVariable String inboxId
      ) {
          try {
              String companyId = sessionInfo.getCompanyId();
              String memberId = sessionInfo.getMemberId();
              
              approvalService.markInboxAsRead(companyId, inboxId, memberId);
              
              Map<String, Object> response = new HashMap<>();
              response.put("success", true);
              response.put("message", "읽음 처리되었습니다.");
              
              return ResponseEntity.ok(response);
              
          } catch (IllegalArgumentException e) {
              return ResponseEntity.badRequest()
                  .body(Map.of("success", false, "message", e.getMessage()));
          } catch (Exception e) {
              log.error("Inbox 읽음 처리 실패: {}", inboxId, e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(Map.of("success", false, "message", "읽음 처리 중 오류가 발생했습니다."));
          }
      }
  }
  ```

---

#### Checklist 5.5.2: 읽지 않은 건수 조회 API

- [ ] `GET /api/approvals/inbox/unread-count` 구현
  ```java
  /**
   * 읽지 않은 Inbox 건수 조회
   * @return { count: 5 }
   */
  @GetMapping("/inbox/unread-count")
  public ResponseEntity<Map<String, Object>> getUnreadCount() {
      try {
          String companyId = sessionInfo.getCompanyId();
          String memberId = sessionInfo.getMemberId();
          
          long count = approvalService.getUnreadInboxCount(companyId, memberId);
          
          Map<String, Object> response = new HashMap<>();
          response.put("count", count);
          
          return ResponseEntity.ok(response);
          
      } catch (Exception e) {
          log.error("읽지 않은 건수 조회 실패", e);
          return ResponseEntity.ok(Map.of("count", 0));
      }
  }
  ```

---

#### Checklist 5.5.3: Inbox 목록 조회 API (동적 로딩용)

- [ ] `GET /api/approvals/inbox` 구현
  ```java
  /**
   * Inbox 목록 조회 (AJAX용)
   * @param type inbox_type (PENDING, APPROVED, REJECTED, INFORMED, null=전체)
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return Page<ApprovalInboxResponse>
   */
  @GetMapping("/inbox")
  public ResponseEntity<Page<ApprovalInboxResponse>> getInboxList(
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
      try {
          String companyId = sessionInfo.getCompanyId();
          String memberId = sessionInfo.getMemberId();
          
          PageRequest pageRequest = PageRequest.of(page, size, 
              Sort.by(Sort.Direction.ASC, "isRead")
                  .and(Sort.by(Sort.Direction.DESC, "submittedAt"))
          );
          
          Page<ApprovalInboxResponse> inboxPage = 
              approvalService.getMyInbox(companyId, memberId, type, pageRequest);
          
          return ResponseEntity.ok(inboxPage);
          
      } catch (Exception e) {
          log.error("Inbox 목록 조회 실패", e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      }
  }
  ```

---

#### Checklist 5.5.4: Inbox 통계 조회 API (탭 배지용)

- [ ] `GET /api/approvals/inbox/stats` 구현
  ```java
  /**
   * Inbox 타입별 통계 조회 (탭 배지 업데이트용)
   * @return { pending: 5, approved: 12, rejected: 1, completed: 3 }
   */
  @GetMapping("/inbox/stats")
  public ResponseEntity<Map<String, Long>> getInboxStats() {
      try {
          String companyId = sessionInfo.getCompanyId();
          String memberId = sessionInfo.getMemberId();
          
          Map<String, Long> stats = new HashMap<>();
          stats.put("pending", approvalService.countInboxByType(companyId, memberId, "SUBMT"));   // 미결
          stats.put("approved", approvalService.countInboxByType(companyId, memberId, "APPRV"));  // 승인
          stats.put("rejected", approvalService.countInboxByType(companyId, memberId, "REJCT"));  // 반려
          stats.put("completed", approvalService.countInboxByType(companyId, memberId, "CMPLT")); // 통보확인
          
          return ResponseEntity.ok(stats);
          
      } catch (Exception e) {
          log.error("Inbox 통계 조회 실패", e);
          return ResponseEntity.ok(Map.of(
              "pending", 0L,
              "approved", 0L,
              "rejected", 0L,
              "completed", 0L
          ));
      }
  }
  ```

---

#### Checklist 5.5.5: 내 Inbox 조회 API (자동 읽음 처리용)

- [ ] `GET /api/approvals/{approvalId}/my-inbox` 구현
  ```java
  /**
   * 특정 결재 문서에 대한 내 Inbox 조회
   * (detail 페이지 자동 읽음 처리용)
   * @param approvalId 결재 문서 ID
   * @return ApprovalInboxResponse 또는 404
   */
  @GetMapping("/{approvalId}/my-inbox")
  public ResponseEntity<ApprovalInboxResponse> getMyInboxByApproval(
      @PathVariable String approvalId
  ) {
      try {
          String companyId = sessionInfo.getCompanyId();
          String memberId = sessionInfo.getMemberId();
          
          Optional<ApprovalInboxResponse> inbox = 
              approvalService.getMyInboxByApproval(companyId, approvalId, memberId);
          
          return inbox
              .map(ResponseEntity::ok)
              .orElse(ResponseEntity.notFound().build());
          
      } catch (Exception e) {
          log.error("내 Inbox 조회 실패: approvalId={}", approvalId, e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      }
  }
  ```

---

#### Checklist 5.5.6: Service 메서드 추가 (위 API를 위한)

- [ ] ApprovalService에 추가 메서드 구현
  ```java
  // ApprovalService.java
  
  /**
   * Inbox 읽음 처리
   */
  @Transactional
  public void markInboxAsRead(String companyId, String inboxId, String memberId) {
      ApprovalInbox inbox = inboxRepository
          .findById(new ApprovalInboxId(companyId, inboxId))
          .orElseThrow(() -> new IllegalArgumentException("Inbox를 찾을 수 없습니다: " + inboxId));
      
      // 권한 체크
      if (!inbox.getMemberId().equals(memberId)) {
          throw new IllegalArgumentException("본인의 Inbox만 읽음 처리할 수 있습니다.");
      }
      
      // 멱등성: 이미 읽음이면 스킵
      if ("Y".equals(inbox.getIsRead())) {
          return;
      }
      
      inbox.setIsRead("Y");
      inbox.setReadAt(LocalDateTime.now());
      inbox.setUpdatedAt(LocalDateTime.now());
      
      inboxRepository.save(inbox);
  }
  
  /**
   * 읽지 않은 Inbox 건수 조회
   */
  public long getUnreadInboxCount(String companyId, String memberId) {
      return inboxRepository.countByIdCompanyIdAndMemberIdAndIsRead(
          companyId, memberId, "N"
      );
  }
  
  /**
   * 타입별 Inbox 건수 조회
   */
  public long countInboxByType(String companyId, String memberId, String inboxType) {
      return inboxRepository.countByIdCompanyIdAndMemberIdAndInboxType(
          companyId, memberId, inboxType
      );
  }
  
  /**
   * 특정 결재 문서의 내 Inbox 조회
   */
  public Optional<ApprovalInboxResponse> getMyInboxByApproval(
      String companyId, 
      String approvalId, 
      String memberId
  ) {
      return inboxRepository
          .findByIdCompanyIdAndApprovalIdAndMemberId(companyId, approvalId, memberId)
          .map(ApprovalInboxResponse::from);
  }
  ```

---

#### Checklist 5.5.7: Repository 메서드 추가

- [ ] ApprovalInboxRepository에 추가 쿼리 메서드
  ```java
  // ApprovalInboxRepository.java
  
  public interface ApprovalInboxRepository extends JpaRepository<ApprovalInbox, ApprovalInboxId> {
      
      // 읽지 않은 건수 조회
      long countByIdCompanyIdAndMemberIdAndIsRead(
          String companyId, 
          String memberId, 
          String isRead
      );
      
      // 타입별 건수 조회
      long countByIdCompanyIdAndMemberIdAndInboxType(
          String companyId, 
          String memberId, 
          String inboxType
      );
      
      // 특정 결재의 내 inbox 조회
      Optional<ApprovalInbox> findByIdCompanyIdAndApprovalIdAndMemberId(
          String companyId, 
          String approvalId, 
          String memberId
      );
      
      // 기존 메서드들...
      Page<ApprovalInbox> findByIdCompanyIdAndMemberId(
          String companyId, 
          String memberId, 
          Pageable pageable
      );
      
      Page<ApprovalInbox> findByIdCompanyIdAndMemberIdAndInboxType(
          String companyId, 
          String memberId, 
          String inboxType, 
          Pageable pageable
      );
      
      List<ApprovalInbox> findByIdCompanyIdAndApprovalId(
          String companyId, 
          String approvalId
      );
      
      @Modifying
      @Transactional
      void deleteByIdCompanyIdAndApprovalId(
          String companyId, 
          String approvalId
      );
  }
  ```

---

#### Checklist 5.5.8: 에러 핸들링 및 로깅

- [ ] 공통 예외 처리
  ```java
  @RestControllerAdvice
  public class ApprovalApiExceptionHandler {
      
      @ExceptionHandler(IllegalArgumentException.class)
      public ResponseEntity<Map<String, Object>> handleIllegalArgument(
          IllegalArgumentException e
      ) {
          return ResponseEntity.badRequest()
              .body(Map.of(
                  "success", false,
                  "message", e.getMessage()
              ));
      }
      
      @ExceptionHandler(Exception.class)
      public ResponseEntity<Map<String, Object>> handleGenericException(
          Exception e
      ) {
          log.error("API 처리 중 오류 발생", e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of(
                  "success", false,
                  "message", "서버 오류가 발생했습니다."
              ));
      }
  }
  ```

---

#### Checklist 5.5.9: 단위 테스트 (API Controller)

- [ ] ApprovalApiControllerTest.java 생성
  ```java
  @WebMvcTest(ApprovalApiController.class)
  class ApprovalApiControllerTest {
      
      @Autowired
      private MockMvc mockMvc;
      
      @MockBean
      private ApprovalService approvalService;
      
      @Test
      void testMarkInboxAsRead_Success() throws Exception {
          // given
          String inboxId = "I250115001";
          doNothing().when(approvalService)
              .markInboxAsRead(anyString(), eq(inboxId), anyString());
          
          // when & then
          mockMvc.perform(put("/api/approvals/inbox/{inboxId}/read", inboxId)
                  .with(csrf()))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.success").value(true));
      }
      
      @Test
      void testGetUnreadCount_Success() throws Exception {
          // given
          when(approvalService.getUnreadInboxCount(anyString(), anyString()))
              .thenReturn(5L);
          
          // when & then
          mockMvc.perform(get("/api/approvals/inbox/unread-count"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.count").value(5));
      }
      
      @Test
      void testGetInboxStats_Success() throws Exception {
          // given
          when(approvalService.countInboxByType(anyString(), anyString(), eq("SUBMT")))
              .thenReturn(3L);
          when(approvalService.countInboxByType(anyString(), anyString(), eq("APPRV")))
              .thenReturn(10L);
          
          // when & then
          mockMvc.perform(get("/api/approvals/inbox/stats"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.pending").value(3))  // key는 pending이지만 실제 DB는 SUBMT
              .andExpect(jsonPath("$.approved").value(10)); // key는 approved이지만 실제 DB는 APPRV
      }
  }
  ```

---

#### 예상 산출물

- **ApprovalApiController.java** (수정 - 5개 엔드포인트 추가) ⭐
- **ApprovalService.java** (수정 - 4개 메서드 추가)
- **ApprovalInboxRepository.java** (수정 - 3개 쿼리 메서드 추가)
- **ApprovalApiExceptionHandler.java** (신규 - 선택사항)
- **ApprovalApiControllerTest.java** (신규)

---

#### 완료 기준

- ✅ 5개 API 엔드포인트 모두 구현 완료
- ✅ Service 계층 메서드 정상 동작
- ✅ Repository 쿼리 메서드 정상 동작
- ✅ 에러 핸들링 및 권한 체크 완료
- ✅ 단위 테스트 통과
- ✅ JavaScript와 API 연동 테스트 완료

---

### TASK 6: API 통합 테스트 및 검증

**우선순위**: 🟡 중간

**⚠️ 주의**: TASK 5.5에서 구현된 API들의 통합 테스트 및 검증

#### Checklist 6.1: Postman/Insomnia 테스트

- [ ] **읽음 처리 API 테스트**
  ```
  PUT /api/approvals/inbox/I250115001/read
  Headers: X-CSRF-TOKEN
  Expected: { success: true, message: "읽음 처리되었습니다." }
  ```

- [ ] **읽지 않은 건수 조회 테스트**
  ```
  GET /api/approvals/inbox/unread-count
  Expected: { count: 5 }
  ```

- [ ] **Inbox 목록 조회 테스트**
  ```
  GET /api/approvals/inbox?type=SUBMT&page=0&size=10
  Expected: Page<ApprovalInboxResponse>
  ```

- [ ] **통계 조회 테스트**
  ```
  GET /api/approvals/inbox/stats
  Expected: { pending: 3, approved: 10, rejected: 1, completed: 2 }
  
  ⚠️ 실제 DB는 SUBMT, APPRV, REJCT, CMPLT 저장됨
  ```

- [ ] **내 Inbox 조회 테스트**
  ```
  GET /api/approvals/A250115001/my-inbox
  Expected: ApprovalInboxResponse 또는 404
  ```

---

#### Checklist 6.2: 통합 테스트 (IntegrationTest)

- [ ] ApprovalInboxIntegrationTest.java 생성
  ```java
  @SpringBootTest
  @AutoConfigureMockMvc
  class ApprovalInboxIntegrationTest {
      
      @Autowired
      private MockMvc mockMvc;
      
      @Autowired
      private ApprovalService approvalService;
      
      @Autowired
      private ApprovalInboxRepository inboxRepository;
      
      @Test
      @WithMockUser(username = "C0001:user1")
      void testInboxWorkflow() throws Exception {
          // 1. 결재 상신 → inbox 생성
          // 2. 읽지 않은 건수 확인
          // 3. inbox 목록 조회
          // 4. 읽음 처리
          // 5. 읽지 않은 건수 감소 확인
      }
      
      @Test
      void testInboxStatistics() throws Exception {
          // inbox 타입별 통계 정합성 검증
      }
  }
  ```

---

#### Checklist 6.3: 에러 케이스 검증

- [ ] **권한 에러**
  - [ ] 다른 사용자의 inbox 읽음 처리 시도 → 403 또는 400
  
- [ ] **존재하지 않는 inbox**
  - [ ] 잘못된 inboxId로 읽음 처리 → 404
  
- [ ] **잘못된 타입**
  - [ ] 존재하지 않는 inbox_type 조회 → 빈 목록 반환

---

#### Checklist 6.4: 성능 테스트

- [ ] Inbox 목록 조회 성능
  - [ ] 100건 이하: < 50ms
  - [ ] 1,000건: < 100ms
  
- [ ] 읽지 않은 건수 조회
  - [ ] 인덱스 활용 확인 (EXPLAIN ANALYZE)
  - [ ] < 10ms

---

#### 예상 산출물

- ApprovalInboxIntegrationTest.java (신규)
- Postman Collection (선택사항)
- 성능 테스트 리포트

---

#### 완료 기준

- ✅ 모든 API 엔드포인트 Postman 테스트 통과
- ✅ 통합 테스트 시나리오 100% 통과
- ✅ 에러 케이스 검증 완료
- ✅ 성능 목표 달성 (조회 < 100ms)

---

### TASK 7: UI 개선 (프론트엔드 구현)

**우선순위**: 🟢 낮음

**⚠️ 중요**: **Option 1 (별도 화면)** 방식으로 구현  
→ `approval/inbox.html` 신규 생성 (기존 `list.html`과 분리)

#### 화면 구조 비교

| 구분 | 기존 List | 새로운 Inbox ⭐ |
|------|-----------|--------------|
| **URL** | `/approval/list` | `/approval/inbox` |
| **의미** | 내가 **상신한** 결재 문서 | 내가 **받은** 결재함 |
| **테이블** | `approval` | `approval_inbox` |
| **조회** | `createdBy = 나` | `memberId = 나` |
| **액션** | 수정/삭제 | 승인/반려/읽음처리 |
| **정렬** | 상신일 최신순 | 미읽음 우선 → 제출일순 |

---

#### Checklist 7.1: 네비게이션 메뉴 수정

- [ ] `defaultLayout.html` 수정
  - [ ] 결재 메뉴에 "결재함" 추가 (Inbox 진입점)
    ```html
    <nav class="sidebar-nav">
      <a href="/approval/inbox" class="nav-item">
        📥 결재함
        <span class="badge primary" id="unread-inbox-count" style="display:none;">0</span>
      </a>
      <a href="/approval/list" class="nav-item">📤 내 결재문서</a>
      <a href="/approval/form" class="nav-item">➕ 결재 작성</a>
    </nav>
    ```
  - [ ] 배지 표시 규칙
    - [ ] `count > 0`이면 표시, `count = 0`이면 숨김
    - [ ] `count ≥ 100`이면 "99+" 표시

---

#### Checklist 7.2: Inbox 화면 생성 (approval/inbox.html)

- [ ] **신규 파일 생성**: `src/main/resources/templates/approval/inbox.html`
- [ ] Thymeleaf 레이아웃 적용
  ```html
  <!doctype html>
  <html lang="ko" xmlns:th="http://www.thymeleaf.org"
        th:replace="~{layout/defaultLayout :: layout(~{::title}, ~{::#content})}">
  <head>
    <title>결재함 · CMMS</title>
  </head>
  <body>
    <div id="content" data-slot-root th:fragment="content" 
         data-page="approval-inbox" data-module="approval-inbox">
      <!-- 내용 -->
    </div>
  </body>
  </html>
  ```

- [ ] 헤더 섹션
  ```html
  <header class="appbar">
    <div class="appbar-inner">
      <div class="brand">📥 결재함</div>
      <div class="spacer"></div>
      <div class="meta">
        <span class="badge info" id="inbox-unread-summary">미읽음 0건</span>
      </div>
    </div>
  </header>
  ```

- [ ] 탭 네비게이션
  ```html
  <div class="tabs">
    <a class="tab-item active" data-inbox-type="ALL">전체</a>
    <a class="tab-item" data-inbox-type="SUBMT">
      미결 <span class="badge sm">0</span>
    </a>
    <a class="tab-item" data-inbox-type="APPRV">기결</a>
    <a class="tab-item" data-inbox-type="REJCT">반려</a>
    <a class="tab-item" data-inbox-type="CMPLT">통보</a>
  </div>
  <!-- ⚠️ data-inbox-type은 APPRV 코드 사용 -->
  ```

- [ ] 테이블 구조
  ```html
  <table class="table">
    <thead>
      <tr>
        <th>상태</th>
        <th>결재 제목</th>
        <th>기안자</th>
        <th>상신일</th>
        <th>내 역할</th>
        <th>순서</th>
        <th>액션</th>
      </tr>
    </thead>
    <tbody>
      <tr th:each="inbox : ${page.content}" 
          th:classappend="${inbox.isRead == 'N' ? 'unread' : ''}"
          th:data-inbox-id="${inbox.inboxId}">
        
        <!-- 읽음/안읽음 표시 -->
        <td>
          <span th:if="${inbox.isRead == 'N'}" class="badge warning">●</span>
          <span th:if="${inbox.isRead == 'Y'}" class="badge muted">○</span>
        </td>
        
        <!-- 결재 제목 (클릭 → detail) -->
        <td>
          <a th:href="@{/approval/detail/{id}(id=${inbox.approvalId})}" 
             th:text="${inbox.title}"
             th:classappend="${inbox.isRead == 'N' ? 'font-bold' : ''}">
            설비 점검 결재의 건
          </a>
        </td>
        
        <!-- 기안자 -->
        <td th:text="${inbox.submittedBy}">admin</td>
        
        <!-- 상신일 -->
        <td th:text="${#temporals.format(inbox.submittedAt, 'yyyy-MM-dd HH:mm')}">
          2025-01-15 14:30
        </td>
        
        <!-- 내 역할 -->
        <td>
          <span class="badge" 
                th:classappend="${inbox.decision == 'APPROVAL' ? 'primary' : 
                                  inbox.decision == 'AGREE' ? 'info' : 'muted'}"
                th:text="${inbox.decision == 'APPROVAL' ? '결재' : 
                          inbox.decision == 'AGREE' ? '합의' : '통보'}">
            결재
          </span>
        </td>
        
        <!-- 순서 -->
        <td th:text="${inbox.stepNo} + '번'">1번</td>
        
        <!-- 액션 -->
        <td class="actions">
          <a class="btn btn-sm primary" 
             th:href="@{/approval/detail/{id}(id=${inbox.approvalId})}">
            열기
          </a>
          <button class="btn btn-sm" 
                  th:if="${inbox.isRead == 'N'}"
                  onclick="markAsRead(this.dataset.inboxId)"
                  th:attr="data-inbox-id=${inbox.inboxId}">
            읽음
          </button>
        </td>
      </tr>
    </tbody>
  </table>
  ```

- [ ] 페이징 컴포넌트 (기존 list.html 참고)
- [ ] 빈 상태 메시지
  ```html
  <div th:if="${page.content.isEmpty()}" class="empty-state">
    <p>받은 결재가 없습니다.</p>
  </div>
  ```

---

#### Checklist 7.3: CSS 스타일 추가 (base.css)

- [ ] 읽지 않은 항목 스타일
  ```css
  /* 결재함 - 읽지 않은 항목 강조 */
  tr.unread {
    background-color: #f0f9ff;
    font-weight: 600;
  }
  
  tr.unread a {
    font-weight: 700;
    color: var(--primary-color);
  }
  
  /* 배지 위치 조정 */
  .nav-item .badge {
    margin-left: 8px;
    vertical-align: middle;
  }
  
  /* 탭 내 배지 */
  .tab-item .badge {
    margin-left: 4px;
    font-size: 0.75rem;
  }
  ```

---

#### Checklist 7.4: JavaScript 구현 (pages/approval.js)

- [ ] **Inbox 페이지 초기화 함수 추가**
  ```javascript
  // pages/approval.js
  
  // Inbox 페이지 초기화 (root 기반)
  initInbox: function(root) {
    console.log('Approval inbox page initialized', root);
    
    if (root.dataset.approvalInboxInit === 'true') {
      console.log('Approval inbox already initialized, skipping');
      return;
    }
    root.dataset.approvalInboxInit = 'true';
    
    this.initInboxTabs(root);
    this.initInboxTable(root);
    this.initMarkAsReadButtons(root);
  },
  
  // 탭 전환
  initInboxTabs: function(root) {
    const tabs = root.querySelectorAll('.tab-item[data-inbox-type]');
    tabs.forEach(tab => {
      tab.addEventListener('click', (e) => {
        e.preventDefault();
        const type = tab.dataset.inboxType;
        
        // 탭 활성화
        tabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        
        // 데이터 로드
        this.loadInboxList(type);
      });
    });
  },
  
  // Inbox 목록 로드
  loadInboxList: async function(inboxType = 'ALL', page = 0) {
    try {
      const params = new URLSearchParams({
        type: inboxType === 'ALL' ? '' : inboxType,
        page: page,
        size: 10
      });
      
      const response = await fetch(`/api/approvals/inbox?${params}`, {
        headers: {
          'X-CSRF-TOKEN': window.cmms.csrf.getToken()
        }
      });
      
      if (!response.ok) throw new Error('목록 조회 실패');
      
      const data = await response.json();
      this.renderInboxTable(data);
      this.updateTabBadges();
      
    } catch (error) {
      console.error('Inbox 목록 로드 실패:', error);
      window.cmms.notification.error('목록을 불러올 수 없습니다.');
    }
  },
  
  // 테이블 렌더링 (동적 갱신용)
  renderInboxTable: function(data) {
    // 페이지 전체 새로고침이 아닌 경우에만 사용
    // 초기 로드는 Thymeleaf SSR 사용
  },
  
  // 읽음 표시 버튼
  initMarkAsReadButtons: function(root) {
    root.addEventListener('click', (e) => {
      if (e.target.closest('[data-inbox-id]')?.textContent === '읽음') {
        const btn = e.target.closest('button');
        const inboxId = btn.dataset.inboxId;
        this.markInboxAsRead(inboxId, btn);
      }
    });
  },
  
  // 읽음 처리 API
  markInboxAsRead: async function(inboxId, buttonElement) {
    try {
      const response = await fetch(`/api/approvals/inbox/${inboxId}/read`, {
        method: 'PUT',
        headers: {
          'X-CSRF-TOKEN': window.cmms.csrf.getToken()
        }
      });
      
      if (!response.ok) throw new Error('읽음 처리 실패');
      
      // UI 업데이트
      const row = buttonElement.closest('tr');
      row.classList.remove('unread');
      buttonElement.remove(); // 읽음 버튼 제거
      
      // 배지 업데이트
      this.updateUnreadBadge();
      
      window.cmms.notification.success('읽음 처리되었습니다.');
      
    } catch (error) {
      console.error('읽음 처리 실패:', error);
      window.cmms.notification.error('읽음 처리할 수 없습니다.');
    }
  },
  
  // 탭별 배지 업데이트
  updateTabBadges: async function() {
    try {
      const response = await fetch('/api/approvals/inbox/stats', {
        headers: { 'X-CSRF-TOKEN': window.cmms.csrf.getToken() }
      });
      
      if (!response.ok) return;
      
      const stats = await response.json();
      // { pending: 5, approved: 12, rejected: 1, completed: 3 }
      
      // ⚠️ data-inbox-type은 APPRV 코드 (SUBMT, APPRV, REJCT, CMPLT)
      document.querySelector('[data-inbox-type="SUBMT"] .badge')
        .textContent = stats.pending || 0;
      
    } catch (error) {
      console.error('탭 배지 업데이트 실패:', error);
    }
  }
  ```

- [ ] **페이지 등록**
  ```javascript
  window.cmms.pages.register('approval-inbox', function(root) {
    window.cmms.approval.initInbox(root);
  });
  ```

---

#### Checklist 7.5: 헤더 배지 실시간 업데이트 (main.js 또는 layout)

- [ ] 전역 함수 추가 (defaultLayout.html 하단 또는 main.js)
  ```javascript
  // 읽지 않은 결재 건수 업데이트
  window.updateUnreadInboxBadge = async function() {
    try {
      const response = await fetch('/api/approvals/inbox/unread-count', {
        headers: { 'X-CSRF-TOKEN': window.cmms.csrf.getToken() }
      });
      
      if (!response.ok) return;
      
      const data = await response.json();
      const count = data.count || 0;
      
      const badge = document.getElementById('unread-inbox-count');
      if (!badge) return;
      
      if (count > 0) {
        badge.textContent = count >= 100 ? '99+' : count;
        badge.style.display = 'inline-block';
      } else {
        badge.style.display = 'none';
      }
      
    } catch (error) {
      console.error('배지 업데이트 실패:', error);
    }
  };
  
  // 초기 로드 + 1분마다 갱신
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', updateUnreadInboxBadge);
  } else {
    updateUnreadInboxBadge();
  }
  
  setInterval(updateUnreadInboxBadge, 60000); // 1분마다
  ```

---

#### Checklist 7.6: Detail 페이지 읽음 처리 (approval/detail.html)

- [ ] detail.html 진입 시 자동 읽음 처리
  ```javascript
  // pages/approval.js - initDetail 함수에 추가
  
  initDetail: function(root) {
    // ... 기존 코드 ...
    
    // 자동 읽음 처리
    this.autoMarkAsRead(root);
  },
  
  autoMarkAsRead: async function(root) {
    const approvalId = root.querySelector('[data-approval-id]')?.dataset.approvalId;
    if (!approvalId) return;
    
    try {
      // 내 inbox 조회
      const response = await fetch(
        `/api/approvals/${approvalId}/my-inbox`,
        {
          headers: { 'X-CSRF-TOKEN': window.cmms.csrf.getToken() }
        }
      );
      
      if (!response.ok) return;
      
      const inbox = await response.json();
      
      // 읽지 않은 경우에만 처리
      if (inbox && inbox.isRead === 'N') {
        await this.markInboxAsRead(inbox.inboxId);
      }
      
    } catch (error) {
      console.error('자동 읽음 처리 실패:', error);
    }
  }
  ```

---

#### Checklist 7.7: Controller 추가 (ApprovalPageController.java)

- [ ] Inbox 페이지 라우팅
  ```java
  @GetMapping("/inbox")
  public String inbox(
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Model model
  ) {
      String companyId = sessionInfo.getCompanyId();
      String memberId = sessionInfo.getMemberId();
      
      PageRequest pageRequest = PageRequest.of(page, size);
      Page<ApprovalInboxResponse> inboxPage = 
          approvalService.getMyInbox(companyId, memberId, type, pageRequest);
      
      model.addAttribute("page", inboxPage);
      model.addAttribute("currentType", type == null ? "ALL" : type);
      
      return "approval/inbox";
  }
  ```

---

#### 예상 산출물

- `templates/approval/inbox.html` (신규) ⭐
- `templates/layout/defaultLayout.html` (수정 - 메뉴 추가)
- `static/assets/js/pages/approval.js` (수정 - inbox 함수 추가)
- `static/assets/css/base.css` (수정 - unread 스타일)
- `ApprovalPageController.java` (수정 - /inbox 라우팅)
- `ApprovalApiController.java` (수정 - 통계 API 추가)

---

#### 완료 기준

- ✅ `/approval/inbox` URL로 결재함 접근 가능
- ✅ 헤더 배지에 읽지 않은 건수 실시간 표시 (1분마다 갱신)
- ✅ Inbox 목록에서 미결/기결/반려/통보 탭 전환
- ✅ 읽지 않은 항목 시각적 강조 (배경색 + 굵은 글씨)
- ✅ "읽음" 버튼 클릭 시 즉시 반영
- ✅ Detail 페이지 진입 시 자동 읽음 처리
- ✅ 기존 `/approval/list`와 명확히 구분됨

---

### TASK 8: 통합 테스트

**우선순위**: 🔴 높음

#### Checklist 8.1: 시나리오 테스트

##### 시나리오 1: 순차 결재

**⚠️ 참고**: DataInitializer 코드값 사용 (inbox_type도 APPRV 코드와 통일)

- [ ] Inspection 실적 결재 상신 (admin)
  - [ ] 결재선: 1번 user1(**APPRL**), 2번 user2(**APPRL**), 3번 user3(**INFO**)
  - [ ] inbox 3개 생성 확인 (inbox_type="**SUBMT**")
- [ ] user1 승인 → inbox[1].inbox_type="**APPRV**", approval.status="**PROC**"
- [ ] user2 승인 → inbox[2].inbox_type="**APPRV**", approval.status="**APPRV**" → 콜백 호출
  - [ ] inspection.status="**APPRV**" 확인 ⭐
- [ ] user3 통보 확인 → inbox[3].inbox_type="**CMPLT**"

##### 시나리오 2: 순서 위반 방지
- [ ] user2가 먼저 승인 시도 → 예외: "이전 결재자(1번: user1)가 먼저 결재해야 합니다"
- [ ] user3(**INFO**/참조)는 언제든 확인 가능 → 정상 처리

##### 시나리오 3: 중간 반려
- [ ] user1 반려 → inbox[1].inbox_type="**REJCT**", approval.status="**REJCT**" → 콜백 호출
  - [ ] inspection.status="**DRAFT**" 확인 (콜백으로 복원) ⭐
- [ ] user2 결재 시도 → 예외: "1번 결재자가 반려하여 결재가 종료되었습니다"

##### 시나리오 4: 읽음 처리
- [ ] user1이 detail.html 조회 → inbox[1].is_read="Y"
- [ ] user1의 unread count 감소 확인

#### Checklist 8.2: 콜백 정합성 확인

- [ ] Inspection: onActualApprovalApprove 정상 호출
- [ ] WorkOrder: onPlanApprovalApprove, onActualApprovalApprove 정상 호출
- [ ] WorkPermit: onPlanApprovalApprove 정상 호출
- [ ] 반려 시 onActualApprovalReject 정상 호출
- [ ] 삭제 시 onActualApprovalDelete 정상 호출

#### Checklist 8.3: 성능 테스트

- [ ] Inbox 목록 조회 시간 측정 (JOIN 제거 효과)
- [ ] 읽지 않은 건수 조회 시간 측정
- [ ] 결재 처리 시간 (inbox 업데이트 포함)

#### 완료 기준

- ✅ 모든 시나리오 테스트 통과
- ✅ 콜백 100% 정상 동작 (원본 모듈 상태 변경 확인)
- ✅ 성능 목표 달성 (Inbox 조회 < 100ms)

---

### TASK 9: 문서 업데이트

**우선순위**: 🟢 낮음

#### Checklist 9.1: CMMS_TABLES.md

- [ ] approval_inbox 테이블 스키마 추가
- [ ] 인덱스 정보 추가
- [ ] inbox_type 상태 설명 추가

#### Checklist 9.2: CMMS_STRUCTURES.md

- [ ] Inbox 아키텍처 섹션 추가
- [ ] 순서 제어 로직 설명 추가
- [ ] Inbox 생성/업데이트 흐름도 추가

#### Checklist 9.3: CMMS_PRD.md

- [ ] 결재 프로세스에 Inbox 설명 추가
- [ ] "읽지 않은 결재 N건" 기능 요구사항 추가

#### 예상 산출물

- CMMS_TABLES.md (수정)
- CMMS_STRUCTURES.md (수정)
- CMMS_PRD.md (수정)

---

## ⚠️ 주의사항 및 리스크

### 1. 트랜잭션 범위

- ✅ approval, approval_step, approval_inbox 모두 동일 트랜잭션 내
- ✅ Rollback 시 모두 롤백됨 (데이터 정합성 보장)

### 2. 데이터 동기화

- ⚠️ Denormalized 필드 (title, ref_entity 등) 수동 동기화 필요
- ✅ approval 수정 시 inbox도 업데이트 (replaceSteps에서 처리)

### 3. 성능 영향

- ✅ 조회 성능: 개선 (JOIN 제거)
- ⚠️ 쓰기 성능: 약간 감소 (inbox INSERT/UPDATE 추가)
- ✅ 전체적으로 성능 향상 (읽기가 쓰기보다 훨씬 많음)

### 4. 기존 데이터 마이그레이션

- [ ] 기존 approval_step → inbox 변환 스크립트 필요
- [ ] 또는 새로 상신된 결재부터 inbox 생성 (점진적 도입)

---

## 📝 실행 지시문

### Approval Inbox 시스템을 다음 순서로 구현해 주세요:

**⚠️ 중요**: 
- 모든 코드값은 `DataInitializer.java` 참조
- decision: **APPRL**(결재), **AGREE**(합의), **INFO**(참조)
- status: **DRAFT**, **SUBMT**, **PROC**, **APPRV**, **REJCT**, **CMPLT**
- NOT NULL 제약은 PK와 created_at만 적용

#### 1. TASK 1 (순서 제어 로직) - 필수 선행 작업
   - ApprovalService.processApproval() 수정
   - 이전 단계 완료 여부 확인
   - **INFO**(참조)는 순서 무관 처리 ⚠️
   - 다단계 결재 완료 감지 로직
   - 단위 테스트 작성

#### 2. TASK 2 (Inbox 스키마 및 Entity)
   - Flyway migration 파일 생성
   - ApprovalInbox Entity 생성
   - ApprovalInboxRepository 생성
   - DTO 생성

#### 3. TASK 3 (Inbox 생성 로직)
   - ApprovalService.replaceSteps() 수정
   - 결재선 생성 시 inbox 자동 생성
   - Denormalized 필드 복사

#### 4. TASK 4 (Inbox 상태 업데이트)
   - ApprovalService.processApproval() 수정
   - 결재 처리 시 inbox_type 전환
   - is_read 자동 처리

#### 5. TASK 5 (읽음 처리)
   - markInboxAsRead() 메서드 추가
   - 읽음 처리 API 추가

#### 6. TASK 6 (Inbox 조회 API)
   - getMyInbox() 메서드 추가
   - Inbox 목록 조회 API 추가

#### 7. TASK 7 (UI 개선)
   - 헤더 배지 추가 (읽지 않은 건수)
   - Inbox 목록 화면 개선

#### 8. TASK 8 (통합 테스트)
   - 전체 시나리오 테스트
   - 콜백 정합성 확인

#### 9. TASK 9 (문서 업데이트)
   - CMMS_TABLES.md 업데이트
   - CMMS_STRUCTURES.md 업데이트

---

## 🔒 중요 제약사항

- ✅ **코드값 일관성**: `DataInitializer.java`의 APPRV, DECSN, MODUL 코드 반드시 준수
- ✅ **기존 콜백 메커니즘 100% 보존**
- ✅ **원본 모듈** (Inspection, WorkOrder, WorkPermit) 영향 없음
- ✅ **트랜잭션 안전성 보장**
- ✅ **NOT NULL 최소화**: PK와 created_at 제외하고 모두 nullable
- ✅ **각 TASK별로 사용자 승인** 받고 다음 진행

---

## 📚 참조 코드 (DataInitializer.java)

```java
// 결재 상태 (APPRV) - approval.status 및 inbox_type에 사용
DRAFT  - 기안
SUBMT  - 제출 (inbox_type의 미결 상태로도 사용)
PROC   - 처리중
APPRV  - 승인 (inbox_type의 기결 상태로도 사용)
REJCT  - 반려 (inbox_type의 반려 상태로도 사용)
CMPLT  - 결재없이확정건 (inbox_type의 통보확인으로도 사용)

// 결재자 유형 (DECSN) - decision 필드에 사용
APPRL  - 결재
AGREE  - 합의
INFO   - 참조

// 참조 모듈 (MODUL) - ref_entity 필드에 사용
INSP   - 점검
WORK   - 작업지시
WPER   - 작업허가
APPRL  - 결재

// ⚠️ inbox_type도 APPRV 코드 재사용:
// - 초기 생성: SUBMT (제출/미결)
// - 승인 완료: APPRV
// - 반려: REJCT
// - 통보 확인: CMPLT
```
