# CMMS JavaScript 개발 가이드

> **참조 문서**: [CMMS_PRD.md](./CMMS_PRD.md) - 제품 요구사항, [CMMS_STRUCTURES.md](./CMMS_STRUCTURES.md) - 기술 아키텍처

본 문서는 CMMS 시스템의 JavaScript 개발 가이드입니다. SPA 내비게이션, 모듈 시스템, 파일 업로드, KPI 대시보드 등의 프론트엔드 구현을 다룹니다.

## 📝 최근 업데이트

### 2025-10-13: data-form-manager 안정성 개선 ✅
- **CSRF 토큰 명시적 추가**: 쿠키 우선, meta 태그 대체 방식
- **다중값 지원**: 체크박스 다중 선택, 배열 필드 자동 처리
- **formDataToJSON()** 메서드 추가: 중복 key 감지 및 배열 변환
- **getCSRFToken()** 메서드 개선: 통합 토큰 추출 로직
- **에러 처리 강화**: 403 응답 자동 감지 및 CSRF 에러 변환

### 2025-10-08: ES 모듈 시스템 전환
- ~~`app.js`~~ → `main.js` + 분해된 모듈 (`core/`, `api/`, `ui/`)
- ~~`common/`~~ 폴더 삭제 → `ui/` 폴더로 통합
- 모듈 격리 및 명시적 의존성 관리 (import/export)
- Picker 페이지 경량화 (필요한 모듈만 선택적 로드)
- 하위 호환성 유지 (`window.cmms.common.DataLoader` 등)

## 1. 프로젝트 구조

### 1.1 JavaScript 파일 구조 (ES 모듈)
```
src/main/resources/static/assets/js/
├── main.js             # ES 모듈 엔트리 포인트
├── core/               # 핵심 시스템 모듈
│   ├── index.js        # Core 모듈 통합
│   ├── csrf.js         # CSRF 토큰 관리
│   ├── navigation.js   # SPA 네비게이션
│   ├── module-loader.js # 페이지 모듈 동적 로더
│   ├── pages.js        # 페이지 초기화 훅
│   └── utils.js        # 공통 유틸리티
├── api/                # API 계층 모듈
│   ├── index.js        # API 모듈 통합
│   ├── auth.js         # 인증/로그아웃
│   └── storage.js      # LocalStorage 래퍼
├── ui/                 # UI 컴포넌트 모듈
│   ├── index.js        # UI 모듈 통합
│   ├── notification.js # 알림 시스템
│   ├── file-upload.js  # 파일 업로드 위젯
│   ├── file-list.js    # 파일 목록 위젯
│   ├── table-manager.js # 테이블 관리
│   ├── data-loader.js  # 데이터 로딩
│   ├── confirm-dialog.js # 확인 다이얼로그
│   ├── validator.js    # 폼 유효성 검사
│   └── print-utils.js  # 인쇄 유틸리티
└── pages/              # 페이지별 모듈
    ├── plant.js        # 설비 관리
    ├── inventory.js    # 재고 관리
    ├── inspection.js   # 예방점검
    ├── workorder.js    # 작업지시
    ├── workpermit.js   # 작업허가
    ├── approval.js     # 결재 관리
    ├── memo.js         # 메모/게시판
    ├── member.js       # 사용자 관리
    ├── code.js         # 공통코드
    └── domain.js       # 도메인 관리
```

### 1.2 모듈 시스템 아키텍처 (ES 모듈)
```javascript
// ES 모듈에서 동적으로 구성되는 전역 네임스페이스
window.cmms = {
    // Core 모듈 (core/index.js에서 초기화)
    csrf: {},           // CSRF 토큰 관리 (core/csrf.js)
    moduleLoader: {},   // 페이지 모듈 로더 (core/module-loader.js)
    pages: {},          // 페이지 초기화 훅 (core/pages.js)
    utils: {},          // 공통 유틸리티 (core/utils.js)
    navigation: {},     // SPA 네비게이션 (core/navigation.js)
    
    // API 모듈 (api/index.js에서 초기화)
    auth: {},           // 인증/로그아웃 (api/auth.js)
    storage: {},        // LocalStorage 래퍼 (api/storage.js)
    
    // UI 모듈 (ui/index.js에서 초기화)
    notification: {},   // 알림 시스템 (ui/notification.js)
    fileUpload: {},     // 파일 업로드 (ui/file-upload.js)
    fileList: {},       // 파일 목록 (ui/file-list.js)
    tableManager: {},   // 테이블 관리 (ui/table-manager.js)
    dataLoader: {},     // 데이터 로딩 (ui/data-loader.js)
    common: {           // 하위 호환성 유지
        DataLoader: {}, // ui/data-loader.js에서 등록
        TableManager: {}, // ui/table-manager.js에서 등록
        Validator: {}   // ui/validator.js에서 등록
    },
    confirmDialog: {},  // 확인 다이얼로그 (ui/confirm-dialog.js)
    validator: {},      // 폼 유효성 검사 (ui/validator.js)
    printUtils: {}      // 인쇄 유틸리티 (ui/print-utils.js)
};
```

### 1.3 표준화된 모듈 패턴 (ES 모듈)
- **엔트리 포인트**: `main.js` - ES 모듈 시스템 초기화 및 모듈 로드 순서 관리
- **Core 모듈**: `core/` - 핵심 시스템 (CSRF, 네비게이션, 모듈 로더, 유틸리티)
- **API 모듈**: `api/` - 데이터 계층 (인증, 스토리지)
- **UI 모듈**: `ui/` - UI 컴포넌트 (알림, 파일 업로드/목록, 테이블, 데이터 로더, 다이얼로그, 유효성 검사)
- **페이지 모듈**: `pages/*.js` - `window.cmms.pages.register()` 방식으로 등록하는 페이지별 모듈
- **초기화 순서**: Core → API → UI → Navigation → 콘텐츠 로드
- **폼 처리**: `core/navigation.js`의 SPA 폼 처리로 통일 (`data-redirect` 속성 사용)
- **모듈 격리**: ES 모듈 스코프로 전역 오염 방지, import/export로 명시적 의존성 관리

## 2. 로그인부터 애플리케이션 초기화까지의 전체 흐름

### 2.1 로그인 프로세스 상세 (1단계: 로그인 페이지)

#### 2.1.1 로그인 페이지 로드 (`/auth/login.html`)
```
브라우저 → GET /auth/login.html → Spring Security (permitAll) → Thymeleaf 렌더링
```

**HTML 구조**:
```html
<form data-validate action="/api/auth/login" method="post">
  <input id="member_id" name="member_id" required />
  <input id="password" name="password" type="password" required />
  <input type="hidden" name="_csrf" th:value="${_csrf.token}" />
</form>

<script type="module">
  import { initCsrf } from './core/csrf.js';
  import { initValidator } from './ui/validator.js';
  
  // 1. CSRF 토큰 처리
  initCsrf();
  
  // 2. 폼 유효성 검사
  initValidator();
  
  // 3. 에러 메시지 표시 (URL 파라미터 기반)
  if (params.get('error')) {
    showErrorMessage('아이디 또는 비밀번호를 확인하세요.');
  }
</script>
```

**로딩 순서**:
1. HTML 파싱 완료
2. ES 모듈 (`<script type="module">`) 로드 시작
   - `core/csrf.js`: CSRF 토큰을 쿠키에서 읽어 폼에 동기화
   - `ui/validator.js`: HTML5 폼 검증 활성화
3. URL 파라미터 확인 및 에러 메시지 표시

**특징**:
- ✅ **최소한의 JavaScript**: 로그인에 필요한 모듈만 로드 (app.js 미로드)
- ✅ **독립 동작**: SPA 시스템과 분리되어 독립적으로 동작
- ✅ **폴백 지원**: JavaScript 실패 시에도 기본 폼 제출 가능

#### 2.1.2 로그인 폼 제출 (2단계: 인증 처리)
```
사용자 입력 → 폼 검증 → POST /api/auth/login → Spring Security FilterChain
```

**Spring Security 처리 흐름**:
1. **CSRF 검증**: `CsrfFilter` - 쿠키와 폼의 토큰 비교
2. **인증 필터**: `UsernamePasswordAuthenticationFilter`
   - `member_id`, `password` 추출
3. **인증 관리자**: `AuthenticationManager`
   - `MemberUserDetailsService.loadUserByUsername()` 호출
   - DB에서 사용자 정보 및 권한 조회
4. **비밀번호 검증**: `BCryptPasswordEncoder.matches()`
5. **세션 생성**: 인증 성공 시 `JSESSIONID` 쿠키 설정

**결과 처리**:
- ✅ **성공**: `HTTP 302 Redirect` → `/layout/defaultLayout.html?content=/memo/list`
- ❌ **실패**: `HTTP 302 Redirect` → `/auth/login.html?error=1`

#### 2.1.3 메인 레이아웃 로드 (3단계: SPA 초기화)
```
브라우저 → GET /layout/defaultLayout.html?content=/memo/list
         → Spring Security (authenticated 필터 통과)
         → Thymeleaf 렌더링 (사용자 정보 포함)
```

**Thymeleaf 서버 사이드 렌더링**:
```html
<header>
  <div th:text="${memberId + ' (' + companyId + ')'}">사용자</div>
  <div th:text="'부서: ' + ${deptId}">-</div>
</header>

<script th:inline="javascript">
  // 서버 설정값을 클라이언트로 전달
  window.initialContent = /*[[${content}]]*/ '/memo/list';
  window.fileUploadConfig = {
    maxSize: /*[[${fileUploadConfig.maxSize}]]*/ 10485760,
    allowedExtensions: /*[[${fileUploadConfig.allowedExtensions}]]*/ [...]
  };
</script>

<!-- 프로필 편집 팝업 처리 (즉시 실행) -->
<script>
  (function() {
    document.getElementById("btn-profile-edit").addEventListener("click", ...);
    window.addEventListener("message", ...);
  })();
</script>

<!-- ES 모듈 로드 -->
<script type="module" src="/assets/js/main.js"></script>
```

**인라인 스크립트의 역할**:
1. **서버 데이터 주입**: Thymeleaf가 서버 설정값을 JavaScript 변수로 변환
   - `window.initialContent`: 초기 로드할 콘텐츠 URL
   - `window.fileUploadConfig`: 파일 업로드 제한 설정 (환경별 다름)
2. **팝업 통신 설정**: 프로필 편집 팝업과 부모 창 간 `postMessage` 통신
   - 팝업 열기 버튼 이벤트 리스너
   - 팝업에서 메시지 수신 리스너 (프로필 업데이트 시)
3. **즉시 실행**: ES 모듈 로드 전에 실행되어 기본 기능 보장

**이유**:
- ⚡ **성능**: 서버 설정값을 API 호출 없이 바로 사용
- 🔒 **보안**: 서버에서 검증된 값만 클라이언트로 전달
- 🎯 **환경 대응**: dev/prod 환경별 설정 차이 반영

### 2.2 JavaScript 초기화 순서 (4단계: ES 모듈 시스템)

#### 2.2.1 main.js 엔트리 포인트
```javascript
// main.js - ES 모듈 진입점
import { initCore } from './core/index.js';
import { initApi } from './api/index.js';
import { initUI } from './ui/index.js';

function initialize() {
  console.log('🚀 CMMS 시스템 초기화 시작');
  
  // 1. 핵심 시스템 초기화
  initCore();    // csrf, navigation, module-loader, pages, utils
  
  // 2. API 계층 초기화
  initApi();     // auth, storage
  
  // 3. UI 컴포넌트 초기화
  initUI();      // notification, file-upload, file-list, etc.
  
  // 4. 네비게이션 시스템 초기화
  window.cmms.navigation.init();
  
  // 5. 초기 콘텐츠 로드
  window.cmms.navigation.loadContent(window.initialContent);
  
  console.log('🎉 CMMS 시스템 초기화 완료');
}

initialize();
```

#### 2.2.2 상세 초기화 단계

**1단계: Core 모듈 초기화** (`initCore()`)
```javascript
// core/index.js
export function initCore() {
  initCsrf();           // CSRF 토큰 관리 (전역 fetch 인터셉터)
  initNavigation();     // SPA 네비게이션 시스템
  initModuleLoader();   // 페이지별 모듈 동적 로더
  initPages();          // 페이지 초기화 훅 시스템
  initUtils();          // 공통 유틸리티 함수
}
```

**2단계: API 모듈 초기화** (`initApi()`)
```javascript
// api/index.js
export function initApi() {
  initAuth();           // 로그아웃 처리 ([data-logout] 버튼)
  initStorage();        // LocalStorage 래퍼
}
```

**3단계: UI 모듈 초기화** (`initUI()`)
```javascript
// ui/index.js
export function initUI() {
  initNotification();   // 알림 시스템 (success/error/warning)
  initFileUpload();     // 파일 업로드 위젯
  initFileList();       // 파일 목록 위젯
  initTableManager();   // 테이블 행 클릭 처리
  initDataLoader();     // AJAX 데이터 로딩
  initConfirmDialog();  // 확인 다이얼로그 ([data-confirm])
  initValidator();      // 폼 유효성 검사
  initPrintUtils();     // 인쇄 유틸리티
}
```

**4단계: Navigation 초기화** (`window.cmms.navigation.init()`)
```javascript
// core/navigation.js
export function initNavigation() {
  window.cmms.navigation = {
    init() {
      // 1. 클릭 이벤트 위임 (SPA 링크 처리)
      document.addEventListener('click', (e) => {
        const anchor = e.target.closest('a[href]');
        if (anchor && shouldInterceptNavigation(anchor)) {
          e.preventDefault();
          this.navigate(anchor.getAttribute('href'));
        }
      }, { capture: true });
      
      // 2. 브라우저 뒤로/앞으로 가기 처리
      window.addEventListener('popstate', (e) => {
        const content = e.state?.content || getUrlParam('content');
        this.loadContent(content, { push: false });
      });
      
      // 3. 초기 콘텐츠 로드
      const initialContent = window.initialContent || '/plant/list';
      this.loadContent(initialContent, { push: false });
    }
  };
}
```

**5단계: 콘텐츠 로드** (`loadContent()`)
```javascript
loadContent(url, { push = true } = {}) {
  const slot = document.getElementById('layout-slot');
  
  // 1. 로딩 상태 표시
  slot.innerHTML = '<div class="loading">로딩 중...</div>';
  
  // 2. AJAX로 콘텐츠 가져오기
  fetch(url)
    .then(res => res.text())
    .then(html => {
      // 3. DOM에 삽입
      slot.innerHTML = html;
      
      // 4. 히스토리 업데이트
      if (push) {
        const fullUrl = '/layout/defaultLayout.html?content=' + url;
        history.pushState({ content: url }, '', fullUrl);
      }
      
      // 5. 페이지 모듈 로드 (URL 기반)
      const moduleId = extractModuleId(url);  // '/memo/list' → 'memo'
      if (moduleId) {
        loadModule(moduleId);  // pages/memo.js 동적 로드
      }
      
      // 6. SPA 폼 처리 ([data-redirect] 속성)
      handleSPAForms();
      
      // 7. 위젯 자동 초기화
      setTimeout(() => {
        window.cmms.fileUpload.initializeContainers(document);
        window.cmms.fileList.initializeContainers(slot);
      }, 10);
    });
}
```

### 2.3 전체 로딩 타임라인

```
시간 | 단계 | 동작
-----|------|------
0ms  | 로그인 | /auth/login.html 로드
     |        | ↓ ES 모듈 (csrf.js, validator.js) 로드
     |        | ↓ 폼 검증 및 에러 표시
...  | 제출   | POST /api/auth/login
     |        | ↓ Spring Security 인증
     |        | ↓ 세션 생성
     |        | ↓ 302 Redirect
0ms  | 레이아웃 | /layout/defaultLayout.html?content=/memo/list
10ms |        | ↓ Thymeleaf 렌더링 (사용자 정보, 메뉴, 인라인 스크립트)
20ms |        | ↓ 인라인 스크립트 실행 (window.initialContent 설정)
30ms | ES모듈 | <script type="module" src="main.js">
40ms |        | ↓ initCore() - csrf, navigation, module-loader, pages, utils
50ms |        | ↓ initApi() - auth, storage
60ms |        | ↓ initUI() - notification, file-upload, file-list, etc.
70ms | 네비   | window.cmms.navigation.init()
80ms |        | ↓ 이벤트 리스너 등록 (click, popstate)
90ms | 콘텐츠 | loadContent(window.initialContent)
100ms|        | ↓ fetch('/memo/list')
150ms|        | ↓ slot.innerHTML = html
160ms|        | ↓ loadModule('memo') - pages/memo.js 동적 로드
170ms|        | ↓ handleSPAForms() - 폼 제출 처리
180ms|        | ↓ 위젯 초기화 (파일 업로드, 파일 목록)
200ms| 완료   | 🎉 사용자가 사용 가능한 상태
```

### 2.4 이전 방식과의 차이점

**구 방식 (app.js 단일 파일)**:
- ❌ 전역 스코프 오염
- ❌ 모듈 간 의존성 불명확
- ❌ 로딩 순서 문제
- ❌ 중복 코드

**신 방식 (ES 모듈)**:
- ✅ 명확한 모듈 경계
- ✅ import/export로 의존성 명시
- ✅ 트리 셰이킹 가능
- ✅ 코드 재사용성 향상

### 2.5 SPA 폼 처리 (handleSPAForms)

#### 2.5.1 data-form-manager 아키텍처

**목적**: 업무 모듈의 폼을 SPA 환경에서 API로 전송하고, 응답 결과에 따라 자동 리다이렉트

**핵심 로직**: `core/navigation.js`의 `handleSPAForms()` 메서드

#### 2.5.2 구현 상세

```javascript
// core/navigation.js
handleSPAForms: function handleSPAForms() {
  const forms = this.slot.querySelectorAll('form[data-form-manager]');
  
  forms.forEach((form) => {
    // 중복 처리 방지
    if (form.__cmmsFormManagerHandled) return;
    form.__cmmsFormManagerHandled = true;
    
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      
      try {
        // 1. 폼 속성 읽기
        const action = form.getAttribute('data-action');
        const method = form.getAttribute('data-method') || 'POST';
        const redirectTemplate = form.getAttribute('data-redirect');
        
        if (!action) {
          console.error('data-action is required for data-form-manager');
          return;
        }
        
        // 2. 파일 업로드 (파일이 있으면)
        if (window.cmms?.fileUpload) {
          try {
            const fileGroupId = await window.cmms.fileUpload.uploadFormFiles(form);
            if (fileGroupId) {
              let fileGroupIdInput = form.querySelector('[name="fileGroupId"]');
              if (!fileGroupIdInput) {
                fileGroupIdInput = document.createElement('input');
                fileGroupIdInput.type = 'hidden';
                fileGroupIdInput.name = 'fileGroupId';
                form.appendChild(fileGroupIdInput);
              }
              fileGroupIdInput.value = fileGroupId;
            }
          } catch (uploadError) {
            console.error('File upload failed:', uploadError);
            if (window.cmms?.notification) {
              window.cmms.notification.error('파일 업로드에 실패했습니다.');
            }
            return;
          }
        }
        
        // 3. FormData → JSON 변환 (다중값 지원)
        const formData = new FormData(form);
        const jsonData = this.formDataToJSON(formData);
        
        // 4. CSRF 토큰 추출
        const csrfToken = this.getCSRFToken();
        
        // 5. API 호출
        const response = await fetch(action, {
          method: method,
          headers: { 
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': csrfToken
          },
          credentials: 'same-origin',
          body: JSON.stringify(jsonData)
        });
        
        if (response.status === 403) {
          throw window.cmms.csrf.toCsrfError(response);
        }
        
        if (!response.ok) {
          const errorText = await response.text();
          throw new Error(`API 요청 실패: ${response.status} - ${errorText}`);
        }
        
        const result = await response.json();
        
        // 6. 리다이렉트 URL 생성 ({id} 치환)
        if (redirectTemplate) {
          let redirectUrl = redirectTemplate;
          
          // {id}, {inspectionId}, {orderId} 등 placeholder 치환
          const idMatches = redirectTemplate.match(/\{(\w+)\}/g);
          if (idMatches) {
            idMatches.forEach(placeholder => {
              const fieldName = placeholder.slice(1, -1);  // {id} → id
              const fieldValue = result[fieldName] || 
                                 result.inspectionId || 
                                 result.orderId || 
                                 result.permitId || 
                                 result.memoId || 
                                 result.approvalId;
              if (fieldValue) {
                redirectUrl = redirectUrl.replace(placeholder, fieldValue);
              }
            });
          }
          
          // 7. SPA 네비게이션
          this.navigate(redirectUrl);
        } else {
          if (window.cmms?.notification) {
            window.cmms.notification.success('저장되었습니다.');
          }
        }
      } catch (err) {
        console.error('폼 제출 오류:', err);
        if (window.cmms?.notification) {
          window.cmms.notification.error('요청 처리 중 오류가 발생했습니다: ' + err.message);
        }
      }
    });
  });
},

/**
 * FormData를 JSON으로 변환 (다중값 지원)
 * @param {FormData} formData - 폼 데이터
 * @returns {Object} JSON 객체
 */
formDataToJSON: function(formData) {
  const jsonData = {};
  const multiValueKeys = new Map(); // 같은 key가 여러 번 나오는 경우 추적
  
  // 1단계: 모든 값 수집 (다중값 감지)
  for (let [key, value] of formData.entries()) {
    if (!multiValueKeys.has(key)) {
      multiValueKeys.set(key, []);
    }
    multiValueKeys.get(key).push(value);
  }
  
  // 2단계: JSON 변환
  for (let [key, values] of multiValueKeys.entries()) {
    // items[0].name 형식 처리
    if (key.includes('[') && key.includes('].')) {
      const match = key.match(/^(\w+)\[(\d+)\]\.(\w+)$/);
      if (match) {
        const [, arrayName, index, fieldName] = match;
        if (!jsonData[arrayName]) jsonData[arrayName] = [];
        if (!jsonData[arrayName][index]) jsonData[arrayName][index] = {};
        jsonData[arrayName][index][fieldName] = values[0]; // 배열 필드는 단일값
        continue;
      }
    }
    
    // 다중값: 배열로 저장
    if (values.length > 1) {
      jsonData[key] = values;
    } else {
      jsonData[key] = values[0];
    }
  }
  
  // items 배열 정리 (빈 요소 제거)
  if (jsonData.items && Array.isArray(jsonData.items)) {
    jsonData.items = jsonData.items.filter(item => item && Object.keys(item).length > 0);
  }
  
  return jsonData;
},

/**
 * CSRF 토큰 추출 (통합 방식)
 * @returns {string} CSRF 토큰
 */
getCSRFToken: function() {
  // 1. 쿠키에서 추출 시도 (Spring Security 기본 방식)
  const cookies = document.cookie.split('; ');
  for (const cookie of cookies) {
    if (cookie.startsWith('XSRF-TOKEN=')) {
      return decodeURIComponent(cookie.split('=')[1]);
    }
  }
  
  // 2. meta 태그에서 추출 시도 (Thymeleaf 템플릿)
  const metaTag = document.querySelector('meta[name="_csrf"]');
  if (metaTag) {
    return metaTag.getAttribute('content') || '';
  }
  
  console.warn('CSRF token not found');
  return '';
}
```

#### 2.5.3 다중값 지원

**체크박스 다중 선택 예시**:
```html
<!-- 역할 다중 선택 -->
<label><input type="checkbox" name="roles" value="ADMIN"> 관리자</label>
<label><input type="checkbox" name="roles" value="USER"> 사용자</label>
<label><input type="checkbox" name="roles" value="VIEWER"> 조회자</label>
```

**전송 결과**:
```json
{
  "roles": ["ADMIN", "USER"]  // 선택된 항목이 배열로 전송됨
}
```

**items 배열 예시**:
```html
<!-- 점검 항목 -->
<input name="items[0].name" value="온도">
<input name="items[0].result" value="정상">
<input name="items[1].name" value="압력">
<input name="items[1].result" value="정상">
```

**전송 결과**:
```json
{
  "items": [
    {"name": "온도", "result": "정상"},
    {"name": "압력", "result": "정상"}
  ]
}
```

#### 2.5.4 HTML 사용 예시

```html
<!-- Inspection form.html -->
<form id="inspection-form" 
      data-form-manager
      th:attr="data-action=${isNew ? '/api/inspections' : '/api/inspections/' + inspection.inspectionId},
               data-method=${isNew ? 'POST' : 'PUT'}"
      data-redirect="/inspection/detail/{id}">
  
  <!-- Hidden fields -->
  <input type="hidden" name="inspectionId" th:value="${inspection?.inspectionId}" />
  <input type="hidden" name="stage" th:value="${stage}" />
  <input type="hidden" name="status" value="DRAFT" />
  
  <!-- Items -->
  <input type="text" name="items[0].name" value="항목1" />
  <input type="text" name="items[0].method" value="방법1" />
  
  <button type="submit">저장</button>
</form>
```

**처리 흐름**:
1. 사용자가 "저장" 버튼 클릭
2. `handleSPAForms()`가 submit 이벤트 가로챔
3. 파일 업로드 (있으면)
4. FormData를 JSON으로 변환 (`items` 배열 포함)
5. `POST /api/inspections` 또는 `PUT /api/inspections/{id}` 호출
6. 응답 JSON에서 `id` 추출
7. `/inspection/detail/{id}` → `/inspection/detail/I250113001` 치환
8. SPA 네비게이션으로 detail 페이지 이동

### 2.6 결재 상신 (submitApproval)

#### 2.6.1 전역 함수 구조

**목적**: detail.html의 "결재 상신" 버튼에서 호출하여 API로 결재 요청

**구현 위치**: `pages/inspection.js` (모든 모듈에서 공통 사용)

```javascript
// pages/inspection.js
const InspectionModule = {
  init: function(container) {
    this.initApprovalButtons(container);
    // ... 기타 초기화
  },
  
  initApprovalButtons: function(root) {
    // 전역 함수 등록 (한 번만)
    if (!window.submitApproval) {
      window.submitApproval = async function(id, stage, module = 'inspections', detailPath = 'inspection') {
        // stage: PLN 또는 ACT
        // module: inspections, workorders, workpermits
        // detailPath: inspection, workorder, workpermit
        
        if (!confirm('결재를 상신하시겠습니까?')) {
          return;
        }
        
        try {
          // API URL 구성
          const apiUrl = stage === 'PLN' 
            ? `/api/${module}/${id}/submit-plan-approval`
            : `/api/${module}/${id}/submit-actual-approval`;
          
          // API 호출
          const response = await fetch(apiUrl, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'X-CSRF-TOKEN': getCSRFToken()
            },
            credentials: 'same-origin'
          });
          
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
          }
          
          const result = await response.json();
          
          // 성공 알림 및 리다이렉트
          if (window.cmms?.notification) {
            window.cmms.notification.success('결재가 상신되었습니다.');
          }
          
          // detail 페이지 새로고침
          if (window.cmms?.navigation) {
            window.cmms.navigation.navigate(`/${detailPath}/detail?id=${id}`);
          }
        } catch (error) {
          console.error('결재 상신 오류:', error);
          if (window.cmms?.notification) {
            window.cmms.notification.error('결재 상신 중 오류가 발생했습니다.');
          }
        }
      };
      
      function getCSRFToken() {
        const cookies = document.cookie.split('; ');
        for (const cookie of cookies) {
          if (cookie.startsWith('XSRF-TOKEN=')) {
            return decodeURIComponent(cookie.split('=')[1]);
          }
        }
        return '';
      }
    }
  }
};
```

#### 2.6.2 HTML 사용 예시

```html
<!-- inspection/detail.html -->
<button class="btn" 
        th:classappend="${inspection.status == 'DRAFT' ? 'primary' : 'disabled'}"
        th:disabled="${inspection.status != 'DRAFT'}"
        th:onclick="${inspection.status == 'DRAFT' ? 'submitApproval(\'' + inspection.inspectionId + '\', \'' + inspection.stage + '\', \'inspections\', \'inspection\')' : 'return false;'}">
  결재 상신
</button>

<!-- workorder/detail.html -->
<button class="btn" 
        th:classappend="${workOrder.status == 'DRAFT' ? 'primary' : 'disabled'}"
        th:disabled="${workOrder.status != 'DRAFT'}"
        th:onclick="${workOrder.status == 'DRAFT' ? 'submitApproval(\'' + workOrder.orderId + '\', \'' + workOrder.stage + '\', \'workorders\', \'workorder\')' : 'return false;'}">
  결재 상신
</button>
```

**처리 흐름**:
1. 사용자가 "결재 상신" 버튼 클릭
2. `submitApproval(id, stage, module, detailPath)` 호출
3. `POST /api/inspections/{id}/submit-plan-approval` 또는 `submit-actual-approval`
4. 서버에서 Approval 생성 및 원본 모듈 status → *_SUBMIT
5. 성공 알림 및 detail 페이지 새로고침

### 2.7 ES 모듈 시스템의 장점

**기존 방식 (app.js) vs 신규 방식 (main.js + ES 모듈)**:

| 항목 | 기존 (app.js) | 신규 (main.js + ES 모듈) |
|------|--------------|--------------------------|
| 로딩 | `<script src="app.js">` | `<script type="module" src="main.js">` |
| 스코프 | 전역 오염 | 모듈 스코프 격리 |
| 의존성 | 암묵적 (주석으로만 표시) | 명시적 (import/export) |
| 로딩 순서 | 수동 관리 필요 | 자동 의존성 해결 |
| 코드 분할 | 어려움 | 쉬움 (dynamic import) |
| 트리 셰이킹 | 불가능 | 가능 |
| 타입 지원 | 어려움 | TypeScript 쉽게 통합 가능 |

## 3. 파일 관리 시스템 (ES 모듈)

### 3.1 파일 모듈 구조

**ES 모듈 방식**:
```
ui/
├── file-upload.js   # 파일 업로드 위젯 (initFileUpload)
└── file-list.js     # 파일 목록 위젯 (initFileList)
```

**초기화 흐름**:
```javascript
// main.js (엔트리 포인트)
import { initUI } from './ui/index.js';
initUI();

// ui/index.js (UI 모듈 통합)
import { initFileUpload } from './file-upload.js';
import { initFileList } from './file-list.js';

export function initUI() {
  initNotification();  // 알림 시스템
  initFileUpload();    // window.cmms.fileUpload 등록
  initFileList();      // window.cmms.fileList 등록
  initTableManager();  // 테이블 관리
  initDataLoader();    // 데이터 로딩
  initConfirmDialog(); // 확인 다이얼로그
  initValidator();     // 폼 유효성 검사
  initPrintUtils();    // 인쇄 유틸리티
}
```

### 3.2 파일 업로드 모듈 (ui/file-upload.js)

```javascript
// ui/file-upload.js
export function initFileUpload() {
  window.cmms = window.cmms || {};
  window.cmms.fileUpload = {
    config: {
      isLoaded: false,
      uploadUrl: '/api/files/upload',
      maxFileSize: window.fileUploadConfig?.maxSize || 10 * 1024 * 1024,
      allowedExtensions: window.fileUploadConfig?.allowedExtensions || []
    },
    
    loadConfig: function() {
      // 서버 설정값은 window.fileUploadConfig에서 가져옴
      // (defaultLayout.html의 인라인 스크립트에서 주입)
      return Promise.resolve();
    },
    
    initializeContainers: function(root) {
      const containers = (root || document).querySelectorAll('[data-file-upload]');
      containers.forEach(container => {
        this.init(container);
      });
    },
    
    init: function(container) {
      if (container.dataset.initialized) return;
      
      const input = container.querySelector('#attachments-input');
      const addButton = container.querySelector('[data-attachments-add]');
      
      if (input && addButton) {
        addButton.addEventListener('click', () => input.click());
        input.addEventListener('change', (e) => this.handleFileSelect(e, container));
      }
      
      container.dataset.initialized = 'true';
    },
    
    handleFileSelect: function(event, container) {
      const files = Array.from(event.target.files);
      if (files.length === 0) return;
      
      this.uploadFiles(files, container);
    },
    
    uploadFiles: function(files, container) {
      const formData = new FormData();
      files.forEach(file => formData.append('files', file));
      
      fetch('/api/files', {
        method: 'POST',
        body: formData,
        headers: {
          'X-CSRF-TOKEN': this.getCSRFToken()
        }
      })
      .then(response => response.json())
      .then(data => {
        if (data.success) {
          const fileGroupIdInput = container.querySelector('input[name="fileGroupId"]');
          if (fileGroupIdInput && data.fileGroupId) {
            fileGroupIdInput.value = data.fileGroupId;
          }
          this.updateFileList(data.files, container);
        }
      })
      .catch(error => {
        console.error('업로드 오류:', error);
        if (window.cmms?.notification) {
          window.cmms.notification.error('파일 업로드 중 오류가 발생했습니다.');
        }
      });
    },
    
    getCSRFToken: function() {
      const cookies = document.cookie.split('; ');
      for (const cookie of cookies) {
        if (cookie.startsWith('XSRF-TOKEN=')) {
          return decodeURIComponent(cookie.split('=')[1]);
        }
      }
      return '';
    }
  };
}
```

### 3.3 파일 목록 모듈 (ui/file-list.js)

```javascript
// ui/file-list.js
export function initFileList() {
  window.cmms = window.cmms || {};
  window.cmms.fileList = {
    config: {
      isLoaded: false,
      listUrl: '/api/files/list',
      deleteUrl: '/api/files/delete'
    },
    
    initializeContainers: function(root) {
      const containers = (root || document).querySelectorAll('[data-file-list]');
      containers.forEach(container => {
        this.init(container);
      });
    },
    
    init: function(container) {
      if (container.dataset.initialized) return;
      
      const fileGroupId = container.dataset.fileGroupId;
      if (fileGroupId) {
        this.loadFiles(fileGroupId, container);
      }
      
      container.dataset.initialized = 'true';
    },
    
    loadFiles: function(fileGroupId, container) {
      fetch(`/api/files?groupId=${fileGroupId}`)
        .then(response => response.json())
        .then(data => {
          if (data.success && data.items) {
            this.renderFileList(data.items, container);
          }
        })
        .catch(error => {
          console.error('파일 목록 로드 오류:', error);
        });
    },
    
    renderFileList: function(files, container) {
      const listElement = container.querySelector('.file-list');
      if (!listElement) return;
      
      if (files.length === 0) {
        listElement.innerHTML = '<li class="empty">첨부된 파일이 없습니다.</li>';
        return;
      }
      
      listElement.innerHTML = files.map(file => `
        <li class="file-item">
          <a href="/api/files/${file.fileId}?groupId=${file.fileGroupId}" 
             download="${file.originalName}">
            ${file.originalName} (${this.formatFileSize(file.size)})
          </a>
        </li>
      `).join('');
    },
    
    formatFileSize: function(bytes) {
      if (bytes === 0) return '0 Bytes';
      const k = 1024;
      const sizes = ['Bytes', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    },
    
    deleteFile: function(fileId, fileGroupId) {
      fetch(`/api/files/${fileId}?groupId=${fileGroupId}`, {
        method: 'DELETE',
        headers: {
          'X-CSRF-TOKEN': this.getCSRFToken()
        }
      })
      .then(response => {
        if (response.ok) {
          if (window.cmms?.notification) {
            window.cmms.notification.success('파일이 삭제되었습니다.');
          }
        }
      })
      .catch(error => {
        console.error('파일 삭제 오류:', error);
        if (window.cmms?.notification) {
          window.cmms.notification.error('파일 삭제 중 오류가 발생했습니다.');
        }
      });
    },
    
    getCSRFToken: function() {
      const cookies = document.cookie.split('; ');
      for (const cookie of cookies) {
        if (cookie.startsWith('XSRF-TOKEN=')) {
          return decodeURIComponent(cookie.split('=')[1]);
        }
      }
      return '';
    }
  };
}
```

### 3.4 위젯 자동 초기화 (core/navigation.js)

**SPA 콘텐츠 로드 후 자동 초기화**:
```javascript
// core/navigation.js의 loadContent() 메서드 내부
loadContent(url, { push = true } = {}) {
  // ... (콘텐츠 로드 로직)
  
  fetch(url)
    .then(res => res.text())
    .then(html => {
      slot.innerHTML = html;
      
      // 히스토리 업데이트
      if (push) {
        history.pushState({ content: url }, '', fullUrl);
      }
      
      // 페이지 모듈 로드
      const moduleId = extractModuleId(url);
      if (moduleId) {
        loadModule(moduleId);
      }
      
      // SPA 폼 처리
      handleSPAForms();
      
      // 파일 위젯 자동 초기화
      setTimeout(() => {
        // 파일 업로드 위젯 (전체 문서 대상)
        if (window.cmms?.fileUpload?.initializeContainers) {
          window.cmms.fileUpload.initializeContainers(document);
        }
        
        // 파일 목록 위젯 (SPA 슬롯 대상)
        if (window.cmms?.fileList?.initializeContainers) {
          window.cmms.fileList.initializeContainers(slot);
        }
      }, 10);
    });
}
```

**초기화 범위 및 중복 방지**:
- ✅ **파일 업로드**: `document` 전체 (기존 페이지 + 새 콘텐츠)
- ✅ **파일 목록**: `slot` (SPA로 로드된 콘텐츠만)
- ✅ **중복 방지**: `dataset.initialized` 속성으로 재초기화 방지
- ✅ **타이밍**: `setTimeout(10ms)` - DOM 안정화 후 초기화

## 4. UI 컴포넌트 모듈 (ui/)

### 4.1 테이블 관리자 (ui/table-manager.js)

**테이블 행 클릭 및 액션 처리**:
```javascript
// ui/table-manager.js
export function initTableManager() {
  window.cmms = window.cmms || {};
  window.cmms.tableManager = {
    init: function() {
      this.bindRowClickEvents();
      this.bindActionButtons();
    },
    
    bindRowClickEvents: function() {
      document.addEventListener('click', function(e) {
        const row = e.target.closest('tr[data-row-link]');
        if (row && !e.target.closest('button, a')) {
          const url = row.dataset.rowLink;
          if (window.cmms?.navigation) {
            window.cmms.navigation.loadContent(url);
          }
        }
      });
    },
    
    bindActionButtons: function() {
      // 삭제 버튼 확인 다이얼로그는 initConfirmDialog에서 처리
    }
  };
  
  // 자동 초기화
  window.cmms.tableManager.init();
}
```

### 4.2 데이터 로더 (ui/data-loader.js)

**AJAX 데이터 로딩 유틸리티**:
```javascript
// ui/data-loader.js
export function initDataLoader() {
  window.cmms = window.cmms || {};
  window.cmms.dataLoader = {
    load: function(url, options = {}) {
      return fetch(url, {
        method: options.method || 'GET',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': this.getCSRFToken(),
          ...options.headers
        },
        body: options.body
      })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        return response.json();
      });
    },
    
    getCSRFToken: function() {
      const cookies = document.cookie.split('; ');
      for (const cookie of cookies) {
        if (cookie.startsWith('XSRF-TOKEN=')) {
          return decodeURIComponent(cookie.split('=')[1]);
        }
      }
      return '';
    }
  };
}
```

### 4.3 확인 다이얼로그 (ui/confirm-dialog.js)

**[data-confirm] 속성 기반 확인 다이얼로그**:
```javascript
// ui/confirm-dialog.js
export function initConfirmDialog() {
  // [data-confirm] 속성이 있는 요소에 이벤트 리스너 등록
  document.addEventListener('click', (e) => {
    const element = e.target.closest('[data-confirm]');
    if (!element) return;
    
    const message = element.getAttribute('data-confirm') || '확인하시겠습니까?';
    if (!confirm(message)) {
      e.preventDefault();
      e.stopPropagation();
    }
  }, { capture: true });
  
  console.log('  ✅ Confirm Dialog 초기화 완료');
}
```

### 4.4 폼 유효성 검사 (ui/validator.js)

**[data-validate] 속성 기반 HTML5 검증**:
```javascript
// ui/validator.js
export function initValidator() {
  // [data-validate] 속성이 있는 폼에 검증 로직 적용
  document.addEventListener('submit', (e) => {
    const form = e.target.closest('form[data-validate]');
    if (!form) return;
    
    if (!form.checkValidity()) {
      e.preventDefault();
      
      // 첫 번째 오류 필드로 포커스 이동
      const firstInvalid = form.querySelector(':invalid');
      if (firstInvalid) {
        firstInvalid.focus();
        firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      
      // 오류 메시지 표시
      if (window.cmms?.notification) {
        window.cmms.notification.error('필수 입력 항목을 확인해주세요.');
      }
    }
  }, { capture: true });
  
  console.log('  ✅ Validator 초기화 완료');
}
```

### 4.5 알림 시스템 (ui/notification.js)

**토스트 알림 표시**:
```javascript
// ui/notification.js
export function initNotification() {
  window.cmms = window.cmms || {};
  window.cmms.notification = {
    success: function(message) {
      this.show(message, 'success');
    },
    
    error: function(message) {
      this.show(message, 'error');
    },
    
    warning: function(message) {
      this.show(message, 'warning');
    },
    
    info: function(message) {
      this.show(message, 'info');
    },
    
    show: function(message, type = 'info') {
      const notification = document.createElement('div');
      notification.className = `notification notification-${type}`;
      notification.textContent = message;
      
      document.body.appendChild(notification);
      
      setTimeout(() => notification.classList.add('show'), 100);
      
      setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
          if (notification.parentNode) {
            document.body.removeChild(notification);
          }
        }, 300);
      }, 3000);
    }
  };
  
  console.log('  ✅ Notification 초기화 완료');
}
```

## 5. KPI 대시보드 (dashboard.js)

### 5.1 대시보드 모듈

#### 5.1.1 KPI 데이터 로드 및 렌더링
```javascript
window.cmms.modules = window.cmms.modules || {};

window.cmms.modules.dashboard = {
    init: function() {
        this.loadKPIData();
        this.initializeCharts();
        this.setupAutoRefresh();
    },
    
    loadKPIData: function() {
        const kpiCards = document.querySelectorAll('.kpi-card');
        
        kpiCards.forEach(card => {
            const kpiType = card.dataset.kpiType;
            if (kpiType) {
                this.loadKPI(kpiType, card);
            }
        });
    },
    
    loadKPI: function(kpiType, cardElement) {
        fetch(`/api/dashboard/kpi/${kpiType}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                this.renderKPICard(cardElement, data);
            }
        })
        .catch(error => {
            console.error(`KPI 로드 오류 (${kpiType}):`, error);
        });
    },
    
    renderKPICard: function(cardElement, data) {
        const valueElement = cardElement.querySelector('.kpi-value');
        const trendElement = cardElement.querySelector('.kpi-trend');
        const statusElement = cardElement.querySelector('.kpi-status');
        
        if (valueElement) {
            valueElement.textContent = formatKPINumber(data.value, data.unit);
        }
        
        if (trendElement) {
            trendElement.textContent = formatTrend(data.trend);
            trendElement.className = `kpi-trend ${data.trend > 0 ? 'positive' : 'negative'}`;
        }
        
        if (statusElement) {
            statusElement.className = `kpi-status ${data.status}`;
            statusElement.textContent = getStatusText(data.status);
        }
    },
    
    initializeCharts: function() {
        const chartElements = document.querySelectorAll('.chart-container');
        
        chartElements.forEach(element => {
            const chartType = element.dataset.chartType;
            const dataUrl = element.dataset.dataUrl;
            
            if (chartType && dataUrl) {
                this.loadChartData(chartType, dataUrl, element);
            }
        });
    },
    
    loadChartData: function(chartType, dataUrl, container) {
        fetch(dataUrl)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                this.renderChart(chartType, data.chartData, container);
            }
        })
        .catch(error => {
            console.error('차트 데이터 로드 오류:', error);
        });
    },
    
    renderChart: function(chartType, chartData, container) {
        // Chart.js 또는 다른 차트 라이브러리 사용
        if (window.Chart) {
            const ctx = container.querySelector('canvas').getContext('2d');
            new Chart(ctx, {
                type: chartType,
                data: chartData,
                options: {
                    responsive: true,
                    maintainAspectRatio: false
                }
            });
        }
    },
    
    setupAutoRefresh: function() {
        // 5분마다 KPI 데이터 자동 새로고침
        setInterval(() => {
            this.loadKPIData();
        }, 5 * 60 * 1000);
    }
};
```

## 6. 모듈 시스템 구조

### 6.1 페이지 모듈 시스템

#### 6.1.1 페이지 등록 방식
```javascript
// pages/memo.js 예시
window.cmms.pages.register('memo', function(container) {
    // 메모 페이지 초기화 로직
    console.log('메모 페이지 초기화됨');
    
    // 페이지별 기능 초기화
    initializeMemoList();
    initializeMemoForm();
});

function initializeMemoList() {
    // 메모 목록 초기화
}

function initializeMemoForm() {
    // 메모 폼 초기화
}
```

### 6.2 UI 모듈 (ES 모듈)

#### 6.2.1 UI 모듈 구조
```javascript
// ui/index.js - UI 모듈 통합
import { initNotification } from './notification.js';
import { initFileUpload } from './file-upload.js';
import { initFileList } from './file-list.js';
import { initTableManager } from './table-manager.js';
import { initDataLoader } from './data-loader.js';
import { initConfirmDialog } from './confirm-dialog.js';
import { initValidator } from './validator.js';
import { initPrintUtils } from './print-utils.js';

export function initUI() {
  // 각 모듈이 window.cmms에 자동 등록됨
  initNotification();   // window.cmms.notification
  initFileUpload();     // window.cmms.fileUpload
  initFileList();       // window.cmms.fileList
  initTableManager();   // window.cmms.tableManager
  initDataLoader();     // window.cmms.dataLoader, window.cmms.common.DataLoader
  initConfirmDialog();  // [data-confirm] 이벤트 리스너 등록
  initValidator();      // [data-validate] 이벤트 리스너 등록
  initPrintUtils();     // window.cmms.printUtils
}
```

#### 6.2.2 Core 모듈 구조
```javascript
// core/index.js - Core 모듈 통합
import { initCsrf } from './csrf.js';
import { initNavigation } from './navigation.js';
import { initModuleLoader } from './module-loader.js';
import { initPages } from './pages.js';
import { initUtils } from './utils.js';

export function initCore() {
  // 각 모듈이 window.cmms에 자동 등록됨
  initCsrf();           // window.cmms.csrf (fetch 인터셉터)
  initNavigation();     // window.cmms.navigation (SPA 시스템)
  initModuleLoader();   // window.cmms.moduleLoader (동적 로딩)
  initPages();          // window.cmms.pages (페이지 훅)
  initUtils();          // window.cmms.utils (유틸리티)
}
```

### 6.3 페이지 모듈 예시

#### 6.3.1 설비 관리 모듈 (pages/plant.js)
```javascript
// pages/plant.js
window.cmms.pages.register('plant', function(container) {
    // 설비 페이지 초기화
    initializePlantList();
    initializePlantForm();
});

function initializePlantList() {
    // 설비 목록 초기화
    const searchInput = container.querySelector('#search-input');
    if (searchInput) {
        searchInput.addEventListener('input', handleSearch);
    }
}

function initializePlantForm() {
    // 설비 폼 초기화
    const form = container.querySelector('#plant-form');
    if (form) {
        form.addEventListener('submit', handleFormSubmit);
    }
}
```

### 6.4 파일 관리 모듈

#### 6.4.1 파일 업로드 (ui/file-upload.js)
```javascript
// ui/file-upload.js - 파일 업로드 전용 모듈 (ES 모듈)
export function initFileUpload() {
  window.cmms = window.cmms || {};
  window.cmms.fileUpload = {
    config: {
        isLoaded: false,
        maxFileSize: 10 * 1024 * 1024, // 10MB
        allowedTypes: ['image/*', 'application/pdf', '.doc', '.docx']
    },
    
    loadConfig: function() {
        // 파일 업로드 설정 로드
        return Promise.resolve();
    },
    
    initializeContainers: function() {
        // 파일 업로드 컨테이너 초기화
        const containers = document.querySelectorAll('[data-attachments]');
        containers.forEach(container => {
            this.initializeContainer(container);
        });
    },
    
    initializeContainer: function(container) {
        // 개별 컨테이너 초기화
        if (container.dataset.initialized) return;
        
        const input = container.querySelector('#attachments-input');
        const addButton = container.querySelector('[data-attachments-add]');
        
        if (input && addButton) {
            addButton.addEventListener('click', () => input.click());
            input.addEventListener('change', (e) => this.handleFileSelect(e, container));
        }
        
        container.dataset.initialized = 'true';
    }
  };
}
```

## 7. 유틸리티 함수

### 7.1 공통 헬퍼 함수

#### 7.1.1 HTTP 요청 및 응답 처리
```javascript
// CSRF 토큰 가져오기
function getCSRFToken() {
    const token = document.querySelector('meta[name="_csrf"]');
    return token ? token.getAttribute('content') : '';
}

// 성공 메시지 표시
function showSuccess(message) {
    showNotification(message, 'success');
}

// 오류 메시지 표시
function showError(message) {
    showNotification(message, 'error');
}

// 알림 메시지 표시
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 3000);
}

// 파일 크기 포맷팅
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// KPI 숫자 포맷팅
function formatKPINumber(value, unit = '') {
    if (typeof value === 'number') {
        return value.toLocaleString() + (unit ? ' ' + unit : '');
    }
    return value + (unit ? ' ' + unit : '');
}

// 트렌드 포맷팅
function formatTrend(trend) {
    if (trend > 0) {
        return `+${trend}%`;
    } else if (trend < 0) {
        return `${trend}%`;
    }
    return '0%';
}

// 상태 텍스트 가져오기
function getStatusText(status) {
    const statusMap = {
        'good': '양호',
        'warning': '주의',
        'danger': '위험'
    };
    return statusMap[status] || status;
}
```

### 7.2 폼 유틸리티

#### 7.2.1 폼 데이터 처리
```javascript
// 폼 데이터를 JSON으로 변환
function formToJSON(form) {
    const formData = new FormData(form);
    const json = {};
    
    for (let [key, value] of formData.entries()) {
        if (json[key]) {
            if (Array.isArray(json[key])) {
                json[key].push(value);
            } else {
                json[key] = [json[key], value];
            }
        } else {
            json[key] = value;
        }
    }
    
    return json;
}

// JSON 데이터를 폼에 설정
function jsonToForm(json, form) {
    Object.keys(json).forEach(key => {
        const element = form.querySelector(`[name="${key}"]`);
        if (element) {
            if (element.type === 'checkbox' || element.type === 'radio') {
                element.checked = json[key] === element.value;
            } else {
                element.value = json[key];
            }
        }
    });
}

// 폼 초기화
function resetForm(form) {
    form.reset();
    
    // 커스텀 필드 초기화
    const customFields = form.querySelectorAll('.custom-field');
    customFields.forEach(field => {
        field.value = '';
        field.classList.remove('has-value');
    });
}
```

## 8. 에러 처리 및 디버깅

### 8.1 전역 에러 처리

#### 8.1.1 에러 캐치 및 로깅
```javascript
// 전역 에러 핸들러
window.addEventListener('error', function(e) {
    console.error('전역 에러:', e.error);
    logError(e.error, e.filename, e.lineno);
});

// Promise rejection 핸들러
window.addEventListener('unhandledrejection', function(e) {
    console.error('처리되지 않은 Promise rejection:', e.reason);
    logError(e.reason, 'Promise', 0);
});

// 에러 로깅 함수
function logError(error, filename, lineno) {
    const errorInfo = {
        message: error.message || error,
        filename: filename,
        lineno: lineno,
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        url: window.location.href
    };
    
    // 서버로 에러 정보 전송 (선택사항)
    fetch('/api/errors', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': getCSRFToken()
        },
        body: JSON.stringify(errorInfo)
    }).catch(err => {
        console.error('에러 로깅 실패:', err);
    });
}
```

### 8.2 디버깅 도구

#### 8.2.1 개발 모드 디버깅
```javascript
// 개발 모드에서만 활성화
if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
    window.cmms.debug = {
        log: function(message, data) {
            console.log(`[CMMS] ${message}`, data);
        },
        
        showState: function() {
            console.log('CMMS 상태:', {
                loadedModules: Array.from(window.cmms.moduleLoader.loadedModules),
                currentUrl: window.location.href,
                navigationState: history.state
            });
        },
        
        testAPI: function(endpoint) {
            fetch(endpoint)
            .then(response => response.json())
            .then(data => {
                console.log(`API 응답 (${endpoint}):`, data);
            })
            .catch(error => {
                console.error(`API 오류 (${endpoint}):`, error);
            });
        }
    };
}


## 9. 참조 문서

### 9.1 관련 문서
- **[CMMS_PRD.md](./CMMS_PRD.md)**: 제품 요구사항 정의서
- **[CMMS_STRUCTURES.md](./CMMS_STRUCTURES.md)**: 기술 아키텍처 가이드
- **[CMMS_CSS.md](./CMMS_CSS.md)**: CSS 스타일 가이드

### 9.2 현재 구조 요약 (ES 모듈)

#### 9.2.1 ES 모듈 시스템 구조
- **엔트리 포인트**: `main.js` - ES 모듈 시스템 초기화
- **모듈 구조**:
  - `core/` - 핵심 시스템 (csrf, navigation, module-loader, pages, utils)
  - `api/` - 데이터 계층 (auth, storage)
  - `ui/` - UI 컴포넌트 (notification, file-upload, file-list, table-manager, data-loader, confirm-dialog, validator, print-utils)
  - `pages/` - 페이지별 모듈
- **초기화 순서**: `main.js` → `initCore()` → `initApi()` → `initUI()` → `navigation.init()`
- **SPA 폼 처리**: `core/navigation.js`에서 `data-redirect` 속성 기반 통합 처리
- **위젯 자동 초기화**: SPA 콘텐츠 로드 후 파일 위젯 자동 초기화

#### 9.2.2 주요 특징
1. **모듈 격리**: ES 모듈 스코프로 전역 오염 방지
2. **명시적 의존성**: import/export로 모듈 간 의존성 명확화
3. **트리 셰이킹 가능**: 사용하지 않는 코드 제거 가능
4. **코드 분할**: 동적 import로 페이지별 모듈 지연 로딩
5. **하위 호환성**: `window.cmms.common.DataLoader` 등 기존 API 유지
6. **경량화**: Picker 페이지는 필요한 모듈만 선택적 로드

#### 9.2.3 폼 처리 통합
```html
<!-- 모든 폼은 data-redirect 속성 사용 -->
<form action="/plant/save" method="post" data-redirect="/plant/list">
  <!-- 폼 필드들 -->
  <button type="submit">저장</button>
</form>
```

#### 9.2.4 경량 모듈 로딩 (Picker 페이지 예시)
```html
<!-- 필요한 모듈만 직접 import (main.js 대신) -->
<script type="module">
  import { initNotification } from '/assets/js/ui/notification.js';
  import { initDataLoader } from '/assets/js/ui/data-loader.js';
  
  window.cmms = window.cmms || {};
  initNotification();
  initDataLoader();
</script>
```

#### 9.2.5 레거시 코드에서 ES 모듈로 마이그레이션 가이드

**이전 (app.js + common.js)**:
```html
<!-- 구 방식 -->
<script src="/assets/js/common/fileUpload.js"></script>
<script src="/assets/js/common/FileList.js"></script>
<script src="/assets/js/common.js"></script>
<script src="/assets/js/app.js"></script>

<script>
  // app.js가 로드된 후 사용
  window.cmms.fileUpload.init();
  window.cmms.notification.success('완료');
</script>
```

**현재 (ES 모듈)**:
```html
<!-- 신규 방식 (메인 페이지) -->
<script type="module" src="/assets/js/main.js"></script>

<script th:inline="javascript">
  // 인라인 스크립트에서 window.cmms 사용 (ES 모듈 로드 후 가능)
  window.addEventListener('DOMContentLoaded', () => {
    // window.cmms.notification.success('완료');
  });
</script>
```

**마이그레이션 체크리스트**:
- [ ] ~~`app.js`~~ → `main.js` + `core/`, `api/`, `ui/` 모듈
- [ ] ~~`common/fileUpload.js`~~ → `ui/file-upload.js`
- [ ] ~~`common/FileList.js`~~ → `ui/file-list.js`
- [ ] ~~`common.js`~~ → `ui/table-manager.js`, `ui/data-loader.js`, `ui/validator.js`
- [ ] 모든 `<script src="...">` → `<script type="module">`
- [ ] 전역 함수 → 모듈 export/import
- [ ] `window.cmms.common.DataLoader` → 하위 호환성 유지됨 (그대로 사용 가능)

### 9.3 외부 참조
- [MDN JavaScript Guide](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide)
- [Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)
- [History API](https://developer.mozilla.org/en-US/docs/Web/API/History_API)
