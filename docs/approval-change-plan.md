# 결재 모듈 재구축 및 업무 모듈 연계 계획

**작성일**: 2025-10-19  
**작성 대상**: 개발팀 / 아키텍처 팀  
**개요**: 기존 Handler 기반 결재 통지 방식을 제거하고, REST + Outbox/Webhook 구조로 결재 모듈을 재구축한 뒤 Inspection을 시작으로 모든 업무 모듈을 순차 전환한다.

---

## 1. 배경 및 목표
- **배경**: 현재 결재 모듈은 Facade + Handler 패턴으로 업무 모듈과 긴밀히 결합되어 있으며, 상신/결재선 입력/상태 통지가 동기 호출로 엮여 있다.
- **목표**
  1. 결재 상신을 REST API로 표준화 (`POST /api/approvals`).
  2. 결재 결과 통지를 Outbox + Webhook으로 비동기화하여 결합도를 낮춘다.
  3. 결재선 입력을 Inspection 등 업무 화면에서 처리해 상태 일관성을 확보한다.
  4. 토글 없이 신규 구조로 대체하고, 기존 핸들러 및 Facade 코드를 제거한다.
  5. 개발 단계이므로 DB 마이그레이션 위험이 없으며, JPA DDL 생성을 활용한다.

---

## 2-1. 시스템 구성 (AS-IS)

**현재 아키텍처 (Facade + Handler 패턴)**

```
업무 화면(Inspection 등)
   └─ 결재 상신 버튼 → InspectionApiController.submitPlanApproval()
                       └─ InspectionApprovalFacade.submitPlanApproval()
                           ├─ ApprovalService.create() - 결재 문서 생성 (DRAFT)
                           ├─ Inspection.status = SUBMT 업데이트
                           └─ 결재선 입력은 별도 페이지에서 처리

결재 모듈
   ├─ ApprovalService: 상신, 승인/반려 처리
   └─ ApprovalService.notifyRefModule() - 동기 콜백
       └─ ApprovalRefHandler 인터페이스 통해 Handler 호출

업무 모듈 Handler
   ├─ InspectionApprovalHandler.handle(action, refId, refStage)
   │   └─ InspectionService.onApprovalApprove/Reject/Delete()
   ├─ WorkOrderApprovalHandler
   └─ WorkPermitApprovalHandler
```

**상신 흐름 (현재)**
1. Inspection 목록/상세 화면 → "결재 상신" 버튼 클릭
2. `InspectionApprovalFacade.submitPlanApproval(inspectionId)` 호출
3. Facade 내부에서:
   - 결재 본문 자동 생성 (`buildPlanApprovalContent`)
   - `ApprovalService.create(request)` - 빈 결재선으로 DRAFT 생성
   - `Inspection.status = SUBMT`, `approvalId` 저장
4. 결재 문서 상세 페이지로 이동 → 결재선 입력 후 상신
5. 결재자가 승인/반려 시:
   - `ApprovalService.approve/reject()` 호출
   - `notifyRefModule()` → Handler 동기 호출
   - `InspectionService.onApprovalApprove()` → `inspection.status = APPRV`

**문제점**
- **긴밀한 결합**: Facade가 양쪽 Service에 의존 → 순환 참조 위험
- **동기 호출**: 결재 결과 통지가 트랜잭션 내에서 동기 처리 → 실패 시 롤백
- **결재선 입력 분리**: 상신과 결재선 입력이 2단계로 나뉨 → UX 불편
- **확장성 부족**: 새 업무 모듈 추가 시 Handler/Facade 구현 필요
- **재시도 불가**: 콜백 실패 시 복구 메커니즘 없음

---

## 2-2. 시스템 구성 (TO-BE)

**신규 아키텍처 (REST + Outbox/Webhook 패턴)**

```
업무 화면(Inspection 등)
   ├─ 결재선 모달 입력 → POST /api/approvals
   ├─ 결재 상태 조회 → GET /api/approvals/{id} /line /opinions
   └─ Webhook 수신 → Inspection 상태 갱신

결재 모듈
   ├─ ApprovalService: 상신, 상태 전이, Outbox 작성
   ├─ OutboxScheduler: PENDING 이벤트 → Webhook 발송
   └─ ApprovalInbox: 결재자 할일(Pull) 기능 유지

업무 모듈 (Inspection 예시)
   ├─ InspectionService: 비즈니스 로직만 담당
   ├─ InspectionApiController
   │   └─ POST /api/inspections/approvals/webhook
   └─ Webhook 수신 → 서명 검증 → 멱등키 확인 → 상태 갱신
```

**상신 흐름 (개선 후)**
1. Inspection 상세 화면에서 "결재 상신" 버튼 클릭 → 결재선 입력 모달 노출
2. 모달에서 결재자 선택, 결재선 구성 완료
3. `POST /api/approvals` 호출 (한 번에 상신):
   - `title`, `refEntity=INSP`, `refId`, `refStage=PLN`
   - `steps[]` - 결재선 정보
   - `callbackUrl` - Webhook 엔드포인트
   - `idempotencyKey` - 중복 방지 키
4. 결재 모듈은 `approval`, `approval_step`을 SUBMT 상태로 저장하고 Outbox 이벤트를 생성
5. 결재 진행 후 최종 승인/반려 시:
   - Outbox 이벤트 생성 (PENDING)
   - OutboxScheduler가 주기적으로 PENDING 이벤트 스캔
   - Webhook POST 요청 → `POST /api/inspections/approvals/webhook`
   - Inspection이 수신하여 상태 갱신 (APPRV/REJCT)
6. 결재자의 Inbox 기능(approval_inbox)은 내부 사용을 위해 유지

**개선 효과**
- **낮은 결합도**: REST API로 표준화 → Handler/Facade 제거
- **비동기 통신**: Outbox 패턴 → 실패해도 결재 트랜잭션 영향 없음
- **자동 재시도**: Webhook 실패 시 스케줄러가 재전송
- **멱등성**: 중복 Webhook 수신해도 안전
- **확장성**: 새 모듈은 Webhook 엔드포인트만 구현
- **UX 개선**: 결재선 입력과 상신이 한 화면에서 완료

---

## 3. AS-IS와 TO-BE 비교 요약

| 항목 | AS-IS (현재) | TO-BE (개선) |
|------|-------------|-------------|
| **상신 방식** | Facade 호출 → Service 직접 호출 | REST API (POST /api/approvals) |
| **결재선 입력** | 별도 페이지 (2단계) | 모달에서 한 번에 처리 |
| **콜백 방식** | 동기 Handler 호출 | 비동기 Webhook (Outbox 패턴) |
| **의존성** | Handler/Facade로 강결합 | REST로 느슨한 결합 |
| **실패 처리** | 트랜잭션 롤백, 복구 불가 | 자동 재시도, 모니터링 가능 |
| **멱등성** | 없음 | idempotencyKey로 보장 |
| **확장성** | 새 모듈마다 Handler 구현 | Webhook 엔드포인트만 추가 |

---

## 4. 데이터 모델 및 스키마
- 개발 환경에서는 Hibernate `ddl-auto=update`로 스키마 변경을 반영한다.
- 신규 컬럼/테이블
  - `approval.callback_url` (VARCHAR 255)
  - `approval.idempotency_key` (VARCHAR 100, UNIQUE)
  - `approval_outbox` (PENDING/SENT/FAILED 상태, retry 정보 포함)
  - `approval_webhook_log` (발송 기록)
  - `webhook_idempotency` (회사ID + 멱등키, 처리 시각 저장)
- Flyway 스크립트는 운영 전환 시 작성한다.

---

## 5. 구현 순서

### Phase A – 결재 모듈 REST/API 재구축
1. **API 설계 확정**
   - `POST /api/approvals`: 상신 (title, refEntity, refId, refStage, steps[], callbackUrl, idempotencyKey)
   - `GET /api/approvals/{id}` / `/line` / `/opinions`
   - `POST /api/approvals/{id}/approve|reject|cancel`
   2. **엔티티 및 서비스 수정**
      - `Approval`, `ApprovalStep`, `ApprovalInbox` 필드 추가 및 리팩터.
      - `ApprovalService.create()`에서 멱등키 검사, 상태 SUBMT 설정.
      - `ApprovalService.create()`는 `approval`/`approval_step` 저장과 Outbox 적재까지 단일 트랜잭션으로 처리하고, Webhook 발송은 커밋 이후 스케줄러가 수행.
      - `ApprovalService.processApproval()`에서 Inbox 업데이트 후 Outbox 이벤트 생성하며, Inbox 동기화는 서비스 레이어에서 직접 처리하고 기존 Handler 의존을 유지하지 않는다.
   3. **Outbox & Scheduler**
      - `ApprovalOutboxRepository`, `ApprovalWebhookLogRepository` 구현.
      - `WebhookScheduler` 작성: PENDING 이벤트 → Webhook POST; 실패 시 retryCount 증가.
      - 재시도 간격과 횟수는 프로퍼티로 조정 가능하도록 `app.webhook.retry.*` 값을 ConfigurationProperties로 노출.
   4. **Webhook 보안**
      - HMAC-SHA256 서명 (`X-Approval-Signature`), 서명 키 프로퍼티화 (`approval-dev.properties`/`approval-prod.properties`).
   5. **멱등성 처리**
   - 클라이언트에서 `회사ID_엔티티_참조ID_단계_UUID8` 형태로 멱등키 생성.
   - 서버는 형식 검증 후 중복 시 기존 응답 반환.
6. **핸들러/Facade 제거**
   - `ApprovalRefHandler` 인터페이스 및 구현, `InspectionApprovalHandler` 등 기존 Handler/Fascade 코드 삭제.
   - `notifyRefModule`는 Webhook 전송만 담당.

### Phase B – Inspection 모듈 전환
1. **REST 연계 서비스**
   - `InspectionService`에 `ApprovalClient` 주입: `submitPlanApproval`, `submitActualApproval`에서 REST 호출.
   - 상신 성공 시 `inspection.status = SUBMT`, `inspection.approvalId` 저장.
   - 실패/타임아웃 시 `DRAFT` 롤백 및 재시도 안내.
2. **결재선 입력 UI**
   - `templates/common/approval-line-modal.html`: 결재선 모달 템플릿 및 자동완성 UI.
   - Inspection 상세 화면(`templates/inspection/detail.html`)에 모달 포함.
   - `assets/js/ui/approval-line-modal.js`: 모달 열기, 결재자 자동완성(`GET /api/members/approval-candidates`), 상신 로직.
   3. **Webhook 수신**
      - `POST /api/inspections/approvals/webhook`: 서명 검증 → 멱등키 검사 → 상태 전이(`updateStatusIfCurrentStatus`).
      - 성공 시 멱등키 저장, 알림 발송 등 후처리.
      - Webhook 수신 서비스는 성공 시 `200 OK`, 재시도 대상은 `5xx`, 요청 오류는 `4xx`로 응답해 스케줄러 재시도 기준을 명확히 한다.

### Phase C – 기타 업무 모듈 확장
1. WorkOrder 모듈 상신 화면/서비스를 Inspection과 동일 패턴으로 개편.
2. WorkPermit 등 결재를 사용하는 모든 모듈에 동일 모달/REST 구조 재사용.
3. 각 모듈 전환 시 구 Facade/Handler 삭제 및 테스트 수행.

---

## 6. 설정 및 운영 사항
- **프로퍼티 구조**
  - `application.properties` + `spring.config.import=optional:classpath:/approval-${spring.profiles.active}.properties`
      - 속성:  
        ```
        storage.type=local
        app.approval.base-url=http://localhost:8080
        app.webhook.callback-base=http://localhost:8080
        app.webhook.security.secret-key=cmms11_dev_secret_key
        app.webhook.retry.max-attempts=5
        app.webhook.retry.backoff-millis=5000
        ```
      - 재시도 한계 초과 시 이벤트는 `FAILED` 상태로 유지하고, 알람 연계는 추후 운영 설계에서 정의한다.
- **스케줄러**
  - 현재는 단일 인스턴스 → Spring `@Scheduled`만 사용 (`fixedDelay=5000` 등).
  - 다중 인스턴스 전환 시 ShedLock 적용을 고려 (Phase E 이후).
- **모니터링**
  - `GET /api/approvals/monitoring/outbox-status`: pending/failed 건수, oldest 이벤트.
  - `GET /api/approvals/monitoring/failed`: 실패 이벤트 목록.
  - `POST /api/approvals/monitoring/outbox/{eventId}/retry`: 수동 재시도.

---

## 7. 테스트 전략
- **단위 테스트**
  - 멱등키 검증, Outbox 이벤트 생성, Webhook 서명 생성/검증, 재시도 로직.
- **통합 테스트**
  - 상신 → 승인/반려 → Outbox → Webhook 수신 → Inspection 상태 업데이트.
  - 실패 시나리오: Webhook Time-out, 중복 Webhook 수신, 서명 오류.
- **향후 작업**
  - Outbox/Webhook 로깅과 외부 알림 연동, UI 자동화 테스트 확대는 Phase B 완료 이후 별도 계획으로 정리한다.
- **UI 테스트**
  - 결재선 모달에서 결재자 선택/삭제/상신, 오류 메시지 동작.
  - 모달 닫기 후 재오픈 시 데이터 초기화 여부.

---

## 8. 잔여 제거 작업
- 결재 모듈에서 기존 Handler, Facade, 관련 설정/빈 삭제.
- Inspection 모듈의 `InspectionApprovalFacade`, `InspectionApprovalHandler` 제거 및 테스트 정리.
- WorkOrder/WorkPermit 등에서 동일 구조 삭제 및 신규 연계로 대체.

---

## 9. 일정 제안
| 주차 | 작업 항목 |
|------|-----------|
| 1주차 | Phase A 전체 (REST/API + Outbox/Webhook + 멱등/보안) |
| 2주차 | Phase B (Inspection UI/Service/Webhook) + 통합 테스트 |
| 3주차 | Phase C (WorkOrder 및 기타 모듈 전환) |
| 4주차 | 잔여 코드 삭제, 모니터링/운영 스크립트 정리 |

---

## 10. 추가 검토 사항
- Outbox 스케줄러 대신 Inbox 진입 시점에 Pending 결재를 동기화하는 방식을 검토한다. 단, 승인 직후 상태 전환(예: `prepare-actual` 선행조건)이 지연될 수 있으므로 비즈니스 영향 분석이 필요하다.
- 새 방식은 Outbox 큐의 동시성 제어, 멱등 처리, 외부 시스템 연동 시나리오까지 재설계해야 하므로 추가 검토 후 결정한다.
- 위 사유로 인해 Phase A~C 범위에서는 기존 Outbox + Webhook 스케줄러 체계를 유지하고, 개선안은 별도 실험 계획을 마련한 뒤 도입한다.

---

## 11. 결론
- 토글 없이 신규 결재 모듈을 완성하고, 기존 Handler/Fascade 코드를 제거한 뒤 Inspection부터 순차적으로 연계 모듈을 수정한다.
- 개발 단계이므로 Hibernate DDL을 사용하고, 운영 시점에 맞춰 Flyway 스크립트를 별도로 작성한다.
- 본 문서 한 개로 설계/구현/전환/운영까지 일관된 지침을 확보하였다.
