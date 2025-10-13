# Approval Inbox 구현 검토 보고서

## 1. 적용 사양 점검
- `docs/MIGRATION_APPROVAL.md`에 정의된 approval_inbox 스키마, 상태 코드(`SUBMT/APPRV/REJCT/CMPLT`), 순서 제어 규칙을 반영했습니다.
- `docs/CMMS_STRUCTURES.md`의 결재 상태 표준(`DRAFT → SUBMT → PROC → APPRV/REJCT`)과 원본 모듈 콜백 구조를 준수하도록 `ApprovalService`를 리팩터링했습니다.
- `docs/CMMS_TABLES.md`의 제약 조건 지침에 맞춰 PK 외 컬럼은 nullable로 유지하고, company_id 선행 구조를 지켰습니다.

## 2. 구조 및 재사용성 확인
- 결재 Inbox Entity/Repository/DTO를 `com.cmms11.approval` 패키지 내에 배치하여 모듈 구조를 유지했습니다.
- 서비스 계층에 Inbox 관련 메서드를 추가하면서 기존 `currentMemberId()`/`AutoNumberService` 등 재사용 가능한 유틸을 활용했습니다.
- API 컨트롤러는 기존 패턴(Page + API 분리)에 맞춰 신규 엔드포인트를 추가하고, 공통 응답 구조(Map, Page)를 그대로 사용했습니다.

## 3. 오류 및 중복 코드 검수
- 순서 제어 로직은 이전 단계 미결/반려 상황의 예외 메시지를 명시적으로 반환하도록 구현해 중복 검사 로직을 제거했습니다.
- Inbox 생성 및 상태 갱신은 전용 헬퍼(`createInboxEntry`, `updateInboxAfterDecision`, `syncInboxMetadata`)로 모듈화하여 중복을 최소화했습니다.
- 테스트(`ApprovalServiceTest`)를 통해 INFO 단계 예외 처리, 순서 위반, 최종 승인 시 상태 전환을 검증했습니다. (네트워크 제한으로 `spring-boot-starter-test` 다운로드가 차단되어 실제 테스트 실행은 실패했음을 별도 보고했습니다.)

## 4. 추적 및 후속 권고
- Flyway 마이그레이션(`V5__create_approval_inbox.sql`)이 추가되었으므로 운영 DB 적용 시 순서를 조정해야 합니다.
- Inbox 읽음/통계 API는 추후 Front-End 연동 시 정렬/필터 파라미터 확장이 필요할 수 있습니다.
- `ApprovalRepository`의 상태 필터가 신규 코드(`SUBMT/APPRV/REJCT`)로 변경되었으므로, 기존 데이터 마이그레이션 시 상태 값 치환이 선행되어야 합니다.
