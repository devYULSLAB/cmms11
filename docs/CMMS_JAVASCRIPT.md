# CMMS JavaScript 개발 가이드

> **참조 문서**: [CMMS_PRD.md](./CMMS_PRD.md) - 제품 요구사항, [CMMS_STRUCTURES.md](./CMMS_STRUCTURES.md) - 기술 아키텍처

본 문서는 CMMS 시스템의 JavaScript 개발 가이드입니다. SPA 내비게이션, 모듈 시스템, 파일 업로드, KPI 대시보드 등의 프론트엔드 구현을 다룹니다.

## 1. 프로젝트 구조

### 1.1 JavaScript 파일 구조
```
src/main/resources/static/assets/js/
├── app.js              # 메인 애플리케이션 (전역 네임스페이스, SPA 네비게이션)
├── common/             # 공통 모듈
│   ├── fileUpload.js   # 파일 업로드 전용 모듈
│   └── fileList.js     # 파일 목록 관리 모듈
├── pages/              # 페이지별 모듈 (data-page 기반)
│   ├── plant.js        # 설비 관리
│   ├── inventory.js    # 재고 관리
│   ├── inspection.js   # 예방점검
│   ├── workorder.js    # 작업지시
│   ├── workpermit.js   # 작업허가
│   ├── approval.js     # 결재 관리
│   ├── memo.js         # 메모/게시판
│   ├── member.js       # 사용자 관리
│   ├── code.js         # 공통코드
│   └── domain.js       # 도메인 관리
└── vendor/             # 외부 라이브러리 (선택사항)
    └── chart.js        # 차트 라이브러리
```

### 1.2 모듈 시스템 아키텍처
```javascript
// app.js에 정의된 전역 네임스페이스
window.cmms = {
    csrf: {},           // CSRF 토큰 관리 (app.js)
    moduleLoader: {},   // 모듈 로더 (app.js)
    pages: {},          // 페이지 초기화 훅 (app.js)
    utils: {},          // 공통 유틸리티 (app.js)
    notification: {},   // 알림 시스템 (app.js)
    navigation: {},     // SPA 네비게이션 (app.js)
    user: {},           // 사용자 정보 관리 (app.js)
    // fileUpload: {}   // common/fileUpload.js에서 동적으로 등록
    // fileList: {}     // common/fileList.js에서 동적으로 등록
};
```

### 1.3 표준화된 모듈 패턴
- **핵심 모듈**: `app.js` - 전역 네임스페이스 및 핵심 기능 (CSRF, 네비게이션, 알림, SPA 폼 처리)
- **공통 모듈**: `common.js` - 테이블 관리, 데이터 로더, 확인 다이얼로그, 유효성 검사
- **파일 모듈**: `common/fileUpload.js`, `common/fileList.js` - 파일 업로드/목록 관리
- **페이지 모듈**: `pages/*.js` - `window.cmms.pages.register()` 방식으로 등록하는 페이지별 모듈
- **초기화 패턴**: 페이지 모듈은 `data-page` 속성 기반 자동 초기화
- **폼 처리**: 모든 폼은 `app.js`의 SPA 폼 처리로 통일 (`data-redirect` 속성 사용)

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

### 2.5 ES 모듈 시스템의 장점

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

**초기화**:
```javascript
// ui/index.js
import { initFileUpload } from './file-upload.js';
import { initFileList } from './file-list.js';

export function initUI() {
  initFileUpload();   // window.cmms.fileUpload 등록
  initFileList();     // window.cmms.fileList 등록
  // ... 기타 UI 모듈
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

### 6.2 공통 모듈

#### 6.2.1 공통 유틸리티 (common.js)
```javascript
// common.js - 공통 유틸리티 (FormManager 제거됨)
window.cmms.common = {
    TableManager: {
        init: function() {
            this.bindRowClickEvents();
            this.bindActionButtons();
        }
    },
    DataLoader: {
        // AJAX 데이터 로딩
    },
    ConfirmDialog: {
        // 확인 다이얼로그
    },
    Validator: {
        // 폼 유효성 검사 (수동 적용)
    }
};
```

#### 6.2.2 도메인 선택기 (domain.js)
```javascript
// domain.js - 도메인 선택기 (사이트, 부서, 사용자 등)
window.cmms.utils = window.cmms.utils || {};
window.cmms.utils.DomainPicker = {
    init: function() {
        this.initializeSiteSelector();
        this.initializeDeptSelector();
        this.initializeUserSelector();
    },
    
    initializeSiteSelector: function() {
        // 사이트 선택기 초기화
    },
    
    initializeDeptSelector: function() {
        // 부서 선택기 초기화
    },
    
    initializeUserSelector: function() {
        // 사용자 선택기 초기화
    }
};
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

#### 6.4.1 파일 업로드 (common/fileUpload.js)
```javascript
// common/fileUpload.js - 파일 업로드 전용 모듈
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

// 파일 업로드 모듈을 위젯 시스템에 연결
window.cmms.widgets.initializeFileUpload = function() {
    window.cmms.fileUpload.init();
};

---

## 9. 참조 문서

### 9.1 관련 문서
- **[CMMS_PRD.md](./CMMS_PRD.md)**: 제품 요구사항 정의서
- **[CMMS_STRUCTURES.md](./CMMS_STRUCTURES.md)**: 기술 아키텍처 가이드
- **[CMMS_CSS.md](./CMMS_CSS.md)**: CSS 스타일 가이드

### 9.2 최적화된 구조 요약

#### 9.2.1 현재 구현 상태
- **로딩 순서**: `fileUpload.js` → `FileList.js` → `common.js` → `app.js`
- **파일 모듈**: `window.cmms.fileUpload`, `window.cmms.fileList` 정상 등록
- **SPA 폼 처리**: `app.js`에서 `data-redirect` 속성 기반 통합 처리
- **위젯 초기화**: SPA 콘텐츠 로드 후 자동 위젯 초기화

#### 9.2.2 주요 개선사항
1. **FormManager 제거**: SPA 폼 처리로 통일
2. **중복 코드 제거**: `window.cmms` 선언, CSRF 토큰 동기화 등
3. **로딩 순서 최적화**: 파일 모듈을 먼저 로드
4. **위젯 초기화 범위**: 파일 업로드는 전체 문서, 파일 목록은 SPA 슬롯

#### 9.2.3 폼 처리 통합
```html
<!-- 모든 폼은 data-redirect 속성 사용 -->
<form action="/plant/save" method="post" data-redirect="/plant/list">
  <!-- 폼 필드들 -->
  <button type="submit">저장</button>
</form>
```

### 9.3 외부 참조
- [MDN JavaScript Guide](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide)
- [Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)
- [History API](https://developer.mozilla.org/en-US/docs/Web/API/History_API)
