# CHECK_BY_CODEX.md

## 0. 완료된 개선 사항 ✅

### 2025-10-13: 보안 강화 및 안정성 개선
- ✅ **CSRF 토큰 처리 통합**: `getCSRFToken()` 메서드 단일화 (쿠키 우선, meta 태그 대체)
- ✅ **다중값 지원 추가**: `formDataToJSON()` 메서드로 체크박스 다중 선택, 배열 필드 자동 처리
- ✅ **에러 처리 강화**: 403 응답 자동 감지 및 CSRF 에러 변환 로직 추가
- ✅ **파일 삭제 정책 확립**: 소프트 삭제 방안 1 채택 (물리 파일 원위치 유지, 90일 후 배치 삭제)
- ✅ **로그인 보안 강화**: `:` 입력 차단 (한글/특수문자 허용), 회사 목록 동적 로딩, 통일된 에러 메시지
- ✅ **회사 관리 개선**: LoginController 추가, DataInitializer에 3개 회사 seed, Thymeleaf 동적 렌더링
- ✅ **문서 업데이트**: `CMMS_JAVASCRIPT.md`, `CMMS_TABLES.md`, `CMMS_STRUCTURES.md` 전면 개선

**해결된 이슈**:
- Line 128-129: CSRF 토큰 중복 정의 → 통합 완료
- Line 130-131: FormData 다중값 미지원 → 배열 변환 로직 추가
- Line 27-28: `FormData` 단일값만 지원 문제 → 해결
- Line 24-26: CSRF 토큰 중복 로직 → 통합
- Line 141-142: 파일 삭제 정책 미정 → 소프트 삭제 방안 확정 및 문서화
- Line 28-29: 로그인 `:` 구분자 우회 가능성 → `:` 차단 (한글/특수문자 허용)
- Line 168-170: 로그인 실패 메시지 노출 → 통일된 메시지로 변경
- 회사 선택 하드코딩 → LoginController + DataInitializer로 동적 로딩

## 1. 검토 범위와 기준
  - 검토 대상: CMMS11 아키텍처/테이블/자바스크립트 문서와 PRD (`docs/CMMS_STRUCTURES.md`, `docs/CMMS_PRD.md`, `docs/CMMS_TABLES.md`, `docs/
  CMMS_JAVASCRIPT.md`).
  - 점검 기준: 모듈 표준화·재사용성, 보안, 사용자 편의성, 코드 오류 가능성, 데이터/필드 일관성.
  - 레이어 관점: UX/UI → JavaScript → Controller → Service → Repository 순서로 모듈별 확인.

  ## 2. 공통 컴포넌트 점검 (로그인, 레이아웃, 파일, Picker, 로그아웃)

  ### 2.1 로그인
  - **표준화/재사용성**: ✅ **완료** - `LoginController` 추가, 회사 목록 동적 로딩 구현. 파싱 로직은 `MemberUserDetailsService`에 고정.
  - **보안**: ✅ **완료** - `:` 차단 (한글/특수문자 허용), 다중 `:` 우회 방지, 로그인 실패 메시지 통일 (`LoginController.java`, `login.html:93-113`, `MemberUserDetailsService.java:47-50`, `SecurityConfig.java:79-85`).
  - **사용자 편의성**: ✅ **완료** - 회사 목록 동적 로딩 (DB 기반). localStorage 최근 회사 기억 기능은 향후 검토.
  - **코드 오류 가능성**: 세션에 저장하는 `sessionInfo` 필드 정의가 문서화돼 있지 않아 후행 모듈이 임의 필드명을 참조할 위험. DTO/세션 모델 스키마를
  명확히 정의해야 함.

  ### 2.2 레이아웃 & SPA 네비게이션
  - **표준화/재사용성**: `data-form-manager` 기반 SPA 제출(`docs/CMMS_JAVASCRIPT.md:395-520`)은 재사용 가능한 훅이지만, 파일 업로드/배열 처리 로직을
  별도 유틸로 분리해 마스터 모듈에도 재사용 가능하도록 확장 필요.
  - **보안**: ✅ **완료** - `getCSRFToken` 중복 정의 제거, 쿠키 우선 + meta 태그 대체 방식으로 통합 (`navigation.js:506-523`)
  - **사용자 편의성**: ✅ **완료** - `FormData` → JSON 매핑 다중값 지원 추가, `formDataToJSON()` 메서드로 체크박스/배열 필드 자동 처리 (`navigation.js:460-500`)
  - **코드 오류 가능성**: `redirectTemplate` 치환 로직은 `result`에 `id` 필드가 없는 경우 대비로 여러 필드를 순서로 시도하지만, 응답 키와 템플릿 플
  레이스홀더가 어긋나면 조용히 실패하고 잘못된 URL이 생성될 수 있음. 서버 응답 스키마를 공통화하거나 404를 사용자에게 표시하는 가드 필요.

  ### 2.3 파일 모듈
  - **표준화/재사용성**: 업로드/목록 위젯이 `ui/file-upload.js`, `ui/file-list.js`로 나뉘어 재사용 구조를 갖춤(`docs/CMMS_JAVASCRIPT.md:683-700`).
  다만 API 응답 스키마(`fileGroupId`, `items`)를 명세화해 back-end와 동기화 필요.
  - **보안**: ✅ **완료** - 소프트 삭제 정책 문서화 완료. `delete_mark` 컬럼 추가, 90일 유예 기간 후 물리 삭제 (`docs/CMMS_STRUCTURES.md:285-311`).
  - **사용자 편의성**: 업로드 시 용량/확장자 오류 메시지가 JS 레벨에서 사용자 친화적으로 표시되는지 확인 필요. 실패 후 재시도 UX 명세 미비.
  - **코드 오류 가능성**: 업로드 후 숨김 필드 `fileGroupId` 주입(`docs/CMMS_JAVASCRIPT.md:424-433`)은 폼에 해당 name이 없으면 조용히 실패. 필수 필드
  여부를 사전에 체크하거나 기본 hidden 필드 렌더 필요.

  ### 2.4 Picker
  - **표준화/재사용성**: 경량 모듈 로딩으로 성능 이점이 있으나(`docs/CMMS_JAVASCRIPT.md:164-170`), 메인 초기화와 별도로 관리되므로 공통 유틸
  (`notification`, `dataLoader`) 로딩 순서를 문서화해야 함.
  - **보안**: Picker는 대량 검색을 수행하므로 서버 측 페이징/권한 필터 가이드 필요.
  - **사용자 편의성**: 선택 후 모듈 간 데이터 바인딩 규칙(API 반환 필드명 등) 정의 요구.

  ### 2.5 로그아웃
  - **표준화/재사용성**: `api/auth.js`에서 처리된다고 명시되어 있으나(`docs/CMMS_JAVASCRIPT.md:52-64`), SPA 네비게이션 시 세션/스토리지 정리 표준을
  문서화해야 함.
  - **보안**: 로그아웃 시 서버 세션 무효화, `XSRF-TOKEN` 재발급, 스토리지 wipe 등 체크리스트 필요.
  - **사용자 편의성**: 다중 탭 사용 시 동기화(브로드캐스트 채널) 여부 검토.

  ## 3. 모듈별 레이어 점검

  ### 3.1 Plant (설비 마스터)
  - **UX/UI**: 마스터 모듈은 HTML POST 기반(`docs/CMMS_STRUCTURES.md:89-104`), SPA 미적용 화면의 일관된 스타일 및 필수 항목 마킹 필요.
  - **JavaScript**: 최소화된 스크립트를 사용하지만 파일 첨부/점검 주기 계산 등 공통 위젯 재사용 여부 검토.
  - **Controller**: `PageController`/`ApiController` 분리 원칙 준수 필요(`docs/CMMS_STRUCTURES.md:81-85`). 생성 시 ID는 `generateMasterId` 사용 권장
  (`docs/CMMS_STRUCTURES.md:206-213`).
  - **Service**: 운영 플래그(`inspection_yn`, `psm_yn`, `workpermit_yn`)가 연계 모듈 접근 제어에 쓰이므로 트랜잭션 처리 시 후행 모듈과 일관된 계약
  필요(`docs/CMMS_TABLES.md:206-214`).
  - **Repository**: `delete_mark` 기본값을 활용하므로 조회 시 항상 `delete_mark='N'` 조건을 강제하는 베이스 리포지토리 메서드 제공 필요.

  ### 3.2 Inventory (자재 마스터 및 수불)
  - **UX/UI**: 재고 수량/금액 표시 시 단위와 소숫점 자리수 표준화 필요(`docs/CMMS_TABLES.md:377-420`).
  - **JavaScript**: 수불/입출고 폼 제출 시 배열 필드(`items[i].qty`)가 많으므로 `data-form-manager`의 배열 처리 로직을 실제 예시와 함께 검증해야 함.
  - **Controller**: 재고 수불 API는 동시성(decrement) 이슈가 있으니 optimistic locking 또는 DB 수준 제약 문서 필요.
  - **Service**: 월마감/재고평가 등 배치 로직을 전용 서비스로 분리하고 재고 회전율 KPI와 연결(`docs/CMMS_PRD.md:117-162`).
  - **Repository**: `inventory_history`/`inventory_closing`에 복합 인덱스를 추가해 조회 성능 확약 필요.

  ### 3.3 Inspection (예방점검)
  - **UX/UI**: 상태가 `stage+status` 조합으로 관리되므로 버튼 활성화 조건을 상태별로 명확히 안내 필요 (`docs/CMMS_TABLES.md:254-277`).
  - **JavaScript**: ✅ **정상** - `status == 'DRAFT'` 검사는 올바름. DB 컬럼이 `stage`, `status`로 분리되어 있음.
  - **Controller**: 계획/실적 API 분리(`.../submit-plan-approval`, `.../submit-actual-approval`) 시 Stage 값 검증 필수.
  - **Service**: 결재 반려 시 상태 복원 규칙(`docs/CMMS_TABLES.md:520-523`)을 서비스 계층에서 통합 제공해야 함.
  - **Repository**: 점검 항목 `inspection_item` 다량 insert를 위한 배치 메서드 표준화 필요.

  ### 3.4 WorkOrder (작업지시)
  - **UX/UI**: 계획/실적 2단계를 UI에서 명확히 구분하고 상태에 따라 입력 필드를 잠금해야 함(`docs/CMMS_STRUCTURES.md:202-227`, `docs/
  CMMS_TABLES.md:295-320`).
  - **JavaScript**: ✅ **정상** - `status == 'DRAFT'` 검사는 올바름.
  - **Controller**: Stage(`PLN`/`ACT`)별 승인 루트를 분리하고 승인 완료 후 실적 전환 API(`ready-actual`) 권한 체크 필요(`docs/CMMS_TABLES.md:318-
  320`).
  - **Service**: 비용/노무 필드가 모두 nullable이므로 검증 로직에서 필수값 조건을 명확히 하고 단위 일관성 확보.
  - **Repository**: 작업 아이템 테이블(`work_order_item`)의 `method/result` 컬럼 정의 확인 및 다국어 지원 대비.

  ### 3.5 WorkPermit (작업허가)
  - **UX/UI**: 안전 체크리스트 JSON 필드(`checksheet_json`)를 시각화할 편집 UI 가이드 필요(`docs/CMMS_TABLES.md:336-375`).
  - **JavaScript**: 서명 이미지(`signature`) 업로드 시 파일 위젯 재사용 여부 확인.
  - **Controller**: 결재 단계는 단일(Stage 없음)이라 Stage null 처리 로직 검증.
  - **Service**: hazard/safety factor 필드가 NULL 허용이라도 저장 전 정규화 필요.
  - **Repository**: 허가서-아이템 간 cascade 삭제 정책 문서화 필요.

  ### 3.6 Approval (결재)
  - **UX/UI**: 단계별 진행 상황 및 반려 시 사용자 메시지 표준 필요.
  - **JavaScript**: Approval 모듈은 SPA 제출 후 결과 모달로 반환되므로 `notification` 표준 메시지를 사용하도록 통합.
  - **Controller**: `ref_entity`, `ref_id`, `ref_stage` 관계 검증 로직을 공통 컴포넌트로 분리해 다른 모듈에서 중복 구현 방지.
  - **Service**: Seed 코드 오탈자(`APPRL`)와 상태 코드(`APPRV`) 불일치로 Enum 매핑 시 예외 발생 가능(`docs/CMMS_STRUCTURES.md:232-243`).
  - **Repository**: `approval_step`의 `decision/result` 값이 텍스트라 Enum 변환시 타이포 검출 단위테스트 필요.

  ### 3.7 Memo (게시판)
  - **UX/UI**: 파일 첨부/댓글 구성 명확화 필요; 상태 컬럼 `APPROV` 주석 설명과 실제 활용 여부 확인(`docs/CMMS_TABLES.md:440-446`).
  - **JavaScript**: SPA로 전환 시 목록/상세 pagination 컴포넌트 재사용.
  - **Controller/Service**: 권한(본인만 수정) 강제 로직 표준화.
  - **Repository**: soft delete 정책(`delete_mark`) 반영.

  ### 3.8 Member & RBAC
  - **UX/UI**: 권한 코드(예: `WORKORDER_U`)를 사용자가 이해하기 쉽게 표시 필요(`docs/CMMS_STRUCTURES.md:185-198`).
  - **JavaScript**: 권한 변경 시 즉시 반영(로그아웃 강제 등) UX 정의.
  - **Controller**: 입력 시 비밀번호 정책 및 감사 로그 확보.
  - **Service**: `findByCompanyIdAndMemberId` 예외 메시지 표준화(`docs/CMMS_STRUCTURES.md:108-117`).
  - **Repository**: `rolemap` 다중 역할 지원 시 unique 제약 확인.

  ### 3.9 코드 & 시퀀스
  - **UX/UI**: 코드 관리 화면에서 사용처 표시 필요.
  - **JavaScript**: 코드 선택 컴포넌트가 여러 모듈에 공유되므로 Picker 확장.
  - **Controller/Service**: ✅ **정상** - DataInitializer에서 `"INVNT"` 정상 사용 중.
  - **Repository**: `sequence` 테이블 관리 시 모듈 코드(1글자)와 Seed 코드(5글자) 변환 매핑 명확화.

  ## 4. 명명·데이터 일관성 이슈

  1. ✅ **완료** - **Seed 모듈 코드**: DataInitializer에서 `"INVNT"` 정상 사용 중 - 문서 오타였음.
  2. ✅ **완료** - **결재 상태 코드**: CMMS_TABLES.md에서 이미 통일됨 (`DRAFT`, `SUBMT`, `APPRV`, `REJCT`, `CMPLT`).
  3. ✅ **정상** - **JavaScript 결재 버튼 조건**: `status == 'DRAFT'` 비교는 올바름. DB 필드가 `stage`, `status`로 분리되어 있고, `PLN+DRAFT`는 문서 표기법일 뿐 실제 status 컬럼값은 `DRAFT`임.
  4. ✅ **완료** - **CSRF 토큰 추출 중복 정의**: 쿠키 기반/메타 기반 통합 완료 (`navigation.js:506-523`)
  5. ✅ **완료** - **FormData 다중값 미지원**: `formDataToJSON()` 메서드로 다중값/배열 처리 로직 추가 (`navigation.js:460-500`)
  6. ✅ **완료** - **파일 삭제 정책**: 소프트 삭제 방안 1 채택. `delete_mark` 컬럼 추가, 물리 파일 원위치 유지, 90일 후 배치 삭제 정책 문서화.
  7. **Approval result 기본값**: `approval_step.result`는 결재 대기 시 `NULL`을 사용함. Entity 주석과 CMMS_TABLES.md 표기 통일 필요 (`APPRV`, `REJCT`, `NULL`).

  ## 5. HTML 헤더 보안 강화 제안 (추후 검토)

  - 현재 문서에서는 `X-CSRF-TOKEN`만 언급됨(`docs/CMMS_JAVASCRIPT.md:460-463`). 추가로 다음 헤더를 권장:
    - `Strict-Transport-Security`: HTTPS 강제
    - `Content-Security-Policy`: 스크립트/리소스 로드 제한, 특히 SPA 환경에서 XSS 방어.
    - `X-Content-Type-Options: nosniff`, `X-Frame-Options`(`DENY` 또는 `SAMEORIGIN`), `Referrer-Policy`.
    - `Permissions-Policy`: 브라우저 권한 최소화.
    - `Cross-Origin-Opener-Policy`/`Cross-Origin-Embedder-Policy`: 필요 시.
  - CSRF 토큰 외에도 SameSite 쿠키 속성 및 OTP/Device binding 도입 검토.

  ## 6. 권장 후속 조치 (우선순위)

  1. ✅ **완료** - 결재 관련 상태 코드 통일 및 검증 (`DRAFT`, `SUBMT`, `APPRV`, `REJCT`, `CMPLT`)
  2. ✅ **완료** - Seed 코드 검증 (`INVNT` 정상 사용)
  3. ✅ **완료** - CSRF 토큰 처리 함수 단일화
  4. ✅ **완료** - `data-form-manager` 폼 전송 로직에 다중값/에러 처리 개선
  5. ✅ **완료** - 파일 모듈 삭제 정책 명확화 (소프트 삭제 방안 1 채택)
  6. ✅ **완료** - CMMS_TABLES.md 문서 정확성 개선 (`approval_step.result`, `file_group/item.delete_mark`)
  7. ✅ **완료** - 로그인 보안 강화 (`:` 입력 필터링, 에러 메시지 통일)
  8. 추가 보안 헤더 적용 검토 (추후)

  ## 7. 다음 우선순위 작업

  ### 높음 (High)
  1. ✅ **완료** - **파일 삭제 정책**: 소프트 삭제 방안 1 채택 완료
  
  2. ✅ **완료** - **CMMS_TABLES.md 문서 정확성**: 모든 항목 개선 완료
  
  ### 중간 (Medium)
  3. ✅ **완료** - **로그인 보안 강화 및 개선**:
     - `:` 차단 (한글/특수문자 허용), 다중 `:` 우회 방지
     - 로그인 실패 메시지 통일 ("아이디 또는 비밀번호가 일치하지 않습니다")
     - LoginController 추가, 회사 목록 동적 로딩 (DB 기반)
     - DataInitializer에 3개 회사 seed (CHROK, HPS, KEPS)
  
  4. **공통 컴포넌트 재사용성 개선**:
     - `data-form-manager` 배열 처리 로직을 마스터 모듈에도 확장
     - Picker 로딩 순서 문서화
  
  ### 낮음 (Low - 추후)
  5. **보안 헤더 추가 검토**: CSP, HSTS, X-Frame-Options 등
  6. **redirectTemplate 치환 로직 개선**: 404 에러 명시적 처리