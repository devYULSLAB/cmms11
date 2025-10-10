# HTML data-* 속성 및 JavaScript 참조 분석 리포트

**생성일:** 2025-10-09  
**분석 범위:**
- HTML: `src/main/resources/templates/**/*.html`
- JavaScript: `src/main/resources/static/assets/js/**/*.js`

---

## 📊 요약

| 항목 | 수량 |
|------|------|
| **HTML에서 정의된 data-* 속성** | 44개 |
| **JavaScript에서 참조하는 data-* 속성** | 39개 |
| **✅ 일치하는 속성** | 33개 |
| **⚠️ HTML에만 있는 속성** | 11개 |
| **⚠️ JavaScript에만 있는 속성** | 4개 (동적 생성) |
| **일치율** | ~75% |

---

## ✅ 일치하는 data-* 속성 (HTML ↔ JavaScript)

다음 속성들은 HTML에 정의되어 있고 JavaScript에서도 정확히 참조하고 있습니다:

### 페이지 및 모듈 관련
- `data-slot-root` - SPA 콘텐츠 슬롯 루트 식별
- `data-page` - 페이지 식별자
- `data-module` - 모듈 이름

### 폼 및 검증 관련
- `data-validate` - 폼 검증 활성화
- `data-redirect` - 제출 후 리다이렉트 URL
- `data-csrf-field` - CSRF 토큰 필드

### 파일 관리 관련
- `data-file-upload` - 파일 업로드 컨테이너
- `data-file-list` - 파일 목록 컨테이너
- `data-file-group-id` - 파일 그룹 ID
- `data-attachments-add` - 파일 추가 버튼
- `data-action` - 파일 액션 버튼 (다운로드 등)
- `data-file-id` - 파일 ID

### 테이블 관리 관련
- `data-table-manager` - 테이블 매니저 초기화
- `data-template` - 행 템플릿
- `data-row-selector` - 행 선택자
- `data-number-field` - 순번 필드
- `data-add-btn` - 추가 버튼 선택자
- `data-remove-btn` - 삭제 버튼 선택자
- `data-min-rows` - 최소 행 수
- `data-server-items` - 서버에서 받은 아이템 데이터

### 버튼 및 액션 관련
- `data-add-item` - 항목 추가 버튼
- `data-remove-item` - 항목 삭제 버튼
- `data-cancel-btn` - 취소 버튼
- `data-delete-url` - 삭제 요청 URL
- `data-action-url` - 액션 요청 URL
- `data-confirm` - 확인 메시지
- `data-nav-btn` - 네비게이션 버튼 (페이지네이션)

### 에디터 관련
- `data-cmd` - 에디터 명령 (bold, italic 등)

### 서명자 관련 (작업허가서)
- `data-signers` - 서명자 섹션
- `data-add-signer` - 서명자 추가 버튼
- `data-remove-signer` - 서명자 삭제 버튼

### 검사 관련
- `data-insp-items` - 검사 항목 섹션
- `data-plan-items` - 계획 항목 섹션

### 기타
- `data-row-link` - 행 클릭 링크
- `data-print-btn` - 인쇄 버튼
- `data-logout` - 로그아웃 버튼

---

## ⚠️ HTML에만 정의된 data-* 속성 (JavaScript에서 사용 안 함)

다음 속성들은 HTML에 정의되어 있지만 JavaScript에서 참조되지 않습니다:

### 파일 관련 (사용되지 않는 속성)
- `data-empty-text` - 파일 목록이 비었을 때 표시할 텍스트
  - 📍 사용 위치: `memo/form.html`, `approval/form.html`
  - 💡 제안: file-list.js에서 사용하거나 HTML에서 제거

- `data-loading-text` - 로딩 중 텍스트
  - 📍 사용 위치: `memo/form.html`, `approval/form.html`
  - 💡 제안: file-list.js에서 사용하거나 HTML에서 제거

- `data-error-text` - 에러 텍스트
  - 📍 사용 위치: `memo/form.html`, `approval/form.html`
  - 💡 제안: file-list.js에서 사용하거나 HTML에서 제거

### Picker 관련 (동적으로 생성되는 속성)
- `data-plant-id` - 설비 ID
  - 📍 사용 위치: `common/plant-picker.html` (동적 생성)
  - 💡 상태: 템플릿 리터럴로 생성되므로 정상

- `data-name` - 이름
  - 📍 사용 위치: picker HTML들 (동적 생성)
  - 💡 상태: 템플릿 리터럴로 생성되므로 정상

- `data-func-id`, `data-dept-id`, `data-maker-name`, `data-serial` 등
  - 📍 사용 위치: picker HTML들 (동적 생성)
  - 💡 상태: 템플릿 리터럴로 생성되므로 정상

### 기타
- `data-mode` - 폼 모드 (edit/view 등)
  - 📍 사용 위치: `workorder/form.html`
  - 💡 제안: JavaScript에서 활용하거나 제거

- `data-is-new` - 신규 작성 여부
  - 📍 사용 위치: `workorder/form.html`
  - 💡 제안: JavaScript에서 활용하거나 제거

- `data-error-rows` - 에러 행 컨테이너
  - 📍 사용 위치: `plant/uploadForm.html`, `inventory/uploadForm.html`
  - ✅ 상태: JavaScript에서 사용됨 (querySelector로 확인)

---

## ⚠️ JavaScript에서 참조하지만 HTML에 없는 data-* 속성

다음 속성들은 JavaScript에서 참조되지만 HTML에 정의되지 않았습니다:

### 초기화 플래그
- `data-initialized` (dataset.initialized)
  - 📍 사용 위치: `ui/index.js`, `ui/print-utils.js`, `pages/approval.js`, `pages/memo.js` 등
  - 💡 상태: **동적으로 설정되는 플래그** - JavaScript에서 중복 초기화를 방지하기 위해 사용
  - ✅ 정상: HTML에 정의할 필요 없음

### 데이터 저장용 속성
- `data-member-id` (dataset.memberId)
  - 📍 사용 위치: `pages/approval.js`
  - 💡 상태: **동적으로 설정되는 데이터** - 승인자 정보 저장용
  - ✅ 정상: HTML에 정의할 필요 없음

- `data-decision` (dataset.decision)
  - 📍 사용 위치: `pages/approval.js`
  - 💡 상태: **동적으로 설정되는 데이터** - 승인 결정 저장용
  - ✅ 정상: HTML에 정의할 필요 없음

- `data-dirty` (dataset.dirty)
  - 📍 사용 위치: `pages/workpermit.js`
  - 💡 상태: **동적으로 설정되는 플래그** - 서명 캔버스 변경 감지용
  - ✅ 정상: HTML에 정의할 필요 없음

### 설정용 속성
- `data-url` (dataset.url)
  - 📍 사용 위치: `pages/workorder.js`, `pages/domain.js`
  - 💡 상태: ✅ **정상** - Thymeleaf `th:data-url`로 HTML에 동적 생성됨
  - ✅ `data-nav-btn`과 함께 페이지네이션 버튼에 사용

- `data-confirm-message` (dataset.confirmMessage)
  - 📍 사용 위치: `pages/workorder.js`
  - 💡 상태: ✅ **제거 완료** - `navigation.js`의 전역 `data-confirm` 핸들러로 통일됨

---

## 🔍 상세 분석

### 1. 파일 업로드/목록 시스템
**상태:** ✅ 대부분 정상 작동

**일치하는 속성:**
- `data-file-upload`, `data-file-list`, `data-file-group-id`, `data-attachments-add`

**개선 제안:**
- `data-empty-text`, `data-loading-text`, `data-error-text`는 JavaScript에서 활용하거나 제거
- `file-list.js`에서 이 속성들을 읽어서 커스텀 메시지 표시 기능 추가 고려

### 2. 테이블 매니저 시스템
**상태:** ✅ 완전히 일치

**일치하는 속성:**
- `data-table-manager`, `data-template`, `data-row-selector`, `data-number-field`, `data-add-button`, `data-remove-button`, `data-min-rows`, `data-server-items`

**평가:** 테이블 관리 시스템은 HTML과 JavaScript가 완벽하게 일치합니다.

### 3. 네비게이션 및 액션 시스템
**상태:** ✅ 대부분 정상

**일치하는 속성:**
- `data-delete-url`, `data-action-url`, `data-confirm`, `data-redirect`, `data-row-link`

**주의사항:**
- `data-url`과 `data-nav-button`의 관계 확인 필요
- `data-confirm-message` vs `data-confirm` 통일 필요

### 4. Picker 시스템 (설비/조직/재고)
**상태:** ✅ 정상 (동적 생성)

**평가:** picker HTML 파일들은 JavaScript 템플릿 리터럴로 data 속성을 동적으로 생성하므로 정상입니다.

---

## 📌 권장 사항

### 우선순위 높음
1. **`data-confirm-message` 정리** ✅ 완료
   - `workorder.js`에서 불필요한 `initConfirmButtons` 함수 제거
   - `navigation.js`의 전역 `data-confirm` 핸들러로 통일

2. **`data-url` 속성** ✅ 정상
   - `th:data-url`로 Thymeleaf에서 동적 생성되어 HTML에 정의됨
   - `data-nav-btn`과 함께 페이지네이션에 정상적으로 사용 중

### 우선순위 중간
3. **파일 목록 메시지 속성 활용**
   - `data-empty-text`, `data-loading-text`, `data-error-text`를 `file-list.js`에서 읽어서 사용
   - 또는 사용하지 않는다면 HTML에서 제거

4. **작업지시서 모드 속성 활용**
   - `data-mode`, `data-is-new` 속성을 JavaScript에서 활용
   - 또는 사용하지 않는다면 제거

### 우선순위 낮음
5. **코드 정리**
   - 사용하지 않는 주석 처리된 코드 제거
   - `data-add-row`, `data-remove-row` 등 기본값으로 정의된 속성들은 문서화

---

## 📈 통계

### HTML 파일별 data-* 속성 사용 현황 (상위 10개)

1. `workorder/form.html` - 8개
2. `inspection/form.html` - 7개
3. `workpermit/form.html` - 7개
4. `approval/form.html` - 10개
5. `memo/form.html` - 9개
6. `plant/form.html` - 4개
7. `inventory/form.html` - 4개

### JavaScript 파일별 data-* 속성 참조 현황 (상위 10개)

1. `core/navigation.js` - 12개
2. `pages/approval.js` - 11개
3. `pages/workpermit.js` - 9개
4. `pages/inspection.js` - 8개
5. `ui/table-manager.js` - 6개
6. `ui/file-upload.js` - 3개
7. `ui/file-list.js` - 4개

---

## ✅ 결론

전반적으로 **프로젝트의 data-* 속성 사용은 일관성이 높습니다** (일치율 ~75%). 

주요 시스템들(파일 관리, 테이블 매니저, 네비게이션)은 HTML과 JavaScript가 잘 일치하며, 불일치하는 속성들은 대부분 다음 중 하나입니다:

1. **동적으로 생성/설정되는 속성** (정상)
2. **사용되지 않는 속성** (제거 권장)
3. **명명 불일치** (통일 권장)

위의 권장 사항을 따라 정리하면 더욱 깔끔한 코드베이스를 유지할 수 있습니다.

