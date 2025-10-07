# JS Migration Plan

## 1. 목적 및 배경
- `src/main/resources/static/assets/js/app.js`에 집중된 SPA 코어를 책임 단위로 분리하고, 전역 네임스페이스(`window.cmms`) 의존을 단계적으로 축소한다.
- 기존 Thymeleaf 기반 HTML 템플릿 로딩 흐름을 유지하면서도, 모듈화 준비를 마친 뒤 향후 번들러 도입이 가능하도록 기반을 마련한다.
- 본 문서는 해당 작업의 개발 표준(Development Standard)으로 사용되며, 구현 전 팀 합의 없이 임의 변경하지 않는다.

## 2. 현재 구조 요약
- **코어 허브**: `app.js`(784라인)가 CSRF, 유틸리티, 알림, SPA 네비게이션, 모듈 로더, 페이지 훅을 모두 포함하여 단일 파일에 과도한 책임이 집중됨. `window.cmms` 네임스페이스로 분류는 되어 있으나 모듈 간 결합도가 높음.
- **공통 모듈**: `common.js`, `common/fileUpload.js`, `common/FileList.js`, `auth.js`가 별도 파일로 분리되어 있으나 전역 네임스페이스에 강하게 의존하며 ES 모듈 시스템을 사용하지 않음.
- **페이지 모듈**: `assets/js/pages/*.js`가 `window.cmms.plant.initList()` 같은 패턴으로 구조화되어 있으나 전역 네임스페이스 의존성과 명시적 의존관계 부재 문제가 있음.
- **템플릿**: `templates/layout/defaultLayout.html`에서 스크립트 로딩 순서(`fileUpload.js → FileList.js → common.js → auth.js → app.js`)와 Thymeleaf 변수 주입에 크게 의존하여 모듈 시스템 도입 시 복잡성 증가 예상.

## 3. 범위 및 원칙 (Development Standards)
- **범위**: `app.js`와 그 소비자(`common.js`, `auth.js`, `pages/*.js`, 관련 템플릿`).
- **비범위**: 백엔드 API, 스타일시트, 번들러/빌드 도입(후속 단계에서 다룸).
- **원칙**:
  1. 브릿지 계층을 두고 전역 API를 점진적으로 축소한다.
  2. 각 단계별로 테스트 가능한 최소 단위로 변경하며, 회귀 테스트 체계를 반드시 유지한다.
  3. 템플릿(HTML) 변경은 JS 변경과 묶어 배포하여 삽입 순서 의존성을 관리한다.
  4. 모든 변경은 코드 리뷰와 QA 체크리스트를 거치며, 문서(`docs/JS_migration_plan.md`)에 결과를 기록한다.

## 4. 단계별 마이그레이션 (안전한 점진적 전환 전략)

### 단계 0: 준비 (1주)
- **Deliverables**: 의존성 다이어그램, 스크립트 로딩 순서 분석, 회귀 체크리스트 초안
- **표준 절차**:
  - `docs/CMMS_JAVASCRIPT.md`에 현재 구조 다이어그램 추가
  - `docs/JS_regression_checklist.md` 생성 및 팀 승인
  - 브랜치 정책: `feature/js-modularization` 하위에 단계별 브랜치 생성

### 단계 1: 안전한 모듈 추출 (핵심 전략) (2주)
- **목표**: app.js에서 기능별로 모듈을 추출하되 기존 파일들은 그대로 유지하여 완벽한 롤백 가능
- **작업 표준**:
  - app.js에서 기능별 블록을 새로운 디렉터리로 추출
  - **기존 파일명 그대로 유지** (app.js, common.js, auth.js 등)
  - 새로운 모듈 구조 생성:
    - `core/` (csrf, navigation, module-loader, pages, utils)
    - `api/` (auth, storage)
    - `ui/` (notification, file-upload, file-list, table-manager, data-loader, confirm-dialog, validator, print-utils)
  - main.js 엔트리 포인트 생성
  - 기존 window.cmms 구조를 그대로 복사하여 호환성 유지
- **완료 기준**: 
  - [ ] core/ 디렉터리에 5개 모듈 추출 완료
  - [ ] api/ 디렉터리에 2개 모듈 추출 완료
  - [ ] ui/ 디렉터리에 8개 모듈 추출 완료
  - [ ] main.js 엔트리 포인트 생성 완료
  - [ ] 기존 파일들 (app.js, common.js, auth.js) 그대로 유지
  - [ ] 회귀 테스트 체크리스트 100% 통과

### 단계 2: 점진적 테스트 및 전환 (1주)
- **목표**: 새로운 ES 모듈 방식과 기존 방식의 병행 테스트
- **작업 표준**:
  - 템플릿에서 새로운 방식과 기존 방식 선택적 사용 가능하도록 구성
  - main.js와 app.js 병행 로딩 테스트
  - 기능 동일성 확인 및 성능 비교
  - 브라우저 호환성 테스트
- **완료 기준**:
  - [ ] 새로운 방식으로 모든 기능 정상 동작 확인
  - [ ] 기존 방식과 기능 동일성 100% 확인
  - [ ] 성능 비교 테스트 완료
  - [ ] 브라우저 호환성 확인 완료

### 단계 3: 완전 전환 (1주)
- **목표**: 새로운 ES 모듈 방식으로 완전 전환
- **작업 표준**:
  - defaultLayout.html에서 main.js 사용으로 전환
  - 기존 스크립트 태그 주석 처리 (롤백용으로 유지)
  - 동적 모듈 로딩 시스템을 Dynamic import로 전환
  - 페이지별 모듈 로딩 최적화
- **완료 기준**:
  - [ ] 템플릿에서 main.js 사용으로 전환 완료
  - [ ] Dynamic import 기반 동적 로딩 구현 완료
  - [ ] 페이지별 모듈 로딩 정상 동작 확인
  - [ ] 회귀 테스트 체크리스트 100% 통과

### 단계 4: 최적화 및 정리 (1주)
- **목표**: ES 모듈 시스템 최적화 및 레거시 정리
- **작업 표준**:
  - 기존 파일들을 archive/ 디렉터리로 이동 (완전 제거 전 백업)
  - ES 모듈 트리 쉐이킹 최적화
  - 모듈별 독립적 테스트 구축
  - 문서 업데이트 및 사용법 가이드 작성
- **완료 기준**:
  - [ ] 기존 파일들 백업 완료
  - [ ] ES 모듈 최적화 완료
  - [ ] 모듈별 테스트 구축 완료
  - [ ] 문서 업데이트 완료
## 4.1 새로운 모듈 구조 및 역할

### 📁 **모듈별 역할 및 책임**

#### **🏠 main.js (엔트리 포인트)**
- **역할**: 애플리케이션의 진입점, 모든 모듈을 통합하여 초기화
- **책임**: 모듈 로딩 순서 관리, 전역 초기화, Thymeleaf 설정 연동

#### **🔧 core/ (핵심 시스템 모듈)**
- **역할**: 애플리케이션의 핵심 인프라스트럭처 제공
- **책임**: 기본 시스템 기능, 네비게이션, 모듈 관리

**포함 내역:**
- `index.js`: 코어 모듈 통합 및 초기화 순서 관리
- `csrf.js`: CSRF 토큰 관리, Fetch API 래핑, 자동 토큰 동기화
- `navigation.js`: SPA 네비게이션, 콘텐츠 로딩, 브라우저 히스토리 관리
- `module-loader.js`: 동적 스크립트 로딩, 페이지별 모듈 매핑 및 로딩 관리
- `pages.js`: 페이지 초기화 훅, 레지스트리 관리, 생명주기 관리
- `utils.js`: 공통 유틸리티 함수 (파일 크기 포맷팅, DOM 유틸리티, 문자열 처리 등)

#### **🌐 api/ (데이터 계층 모듈)**
- **역할**: 서버와의 데이터 통신 및 로컬 저장소 관리
- **책임**: API 호출, 인증, 데이터 저장/조회

**포함 내역:**
- `index.js`: API 모듈 통합 및 초기화
- `auth.js`: 로그인/로그아웃, 인증 상태 관리, 세션 처리
- `storage.js`: 로컬 스토리지, 세션 스토리지, 캐시 관리

#### **🎨 ui/ (사용자 인터페이스 모듈)**
- **역할**: 재사용 가능한 UI 컴포넌트 및 인터랙션 제공
- **책임**: 사용자 경험, 폼 처리, 알림, 파일 관리

**포함 내역:**
- `index.js`: UI 모듈 통합 및 초기화
- `notification.js`: 성공/에러/경고 알림 시스템, 토스트 메시지
- `file-upload.js`: 파일 업로드 위젯, 드래그 앤 드롭, 파일 검증
- `file-list.js`: 파일 목록 표시, 파일 관리, 다운로드/삭제
- `table-manager.js`: 동적 테이블 관리, 행 추가/삭제/재정렬, 데이터 바인딩
- `data-loader.js`: API 호출 및 데이터 로딩, 로딩 상태 관리, 에러 처리
- `confirm-dialog.js`: 확인 대화상자, 사용자 확인 처리
- `validator.js`: 폼 유효성 검사, 입력 검증, 에러 메시지 표시
- `print-utils.js`: 인쇄 기능, 인쇄 스타일링, 인쇄 최적화

#### **📄 pages/ (페이지별 비즈니스 로직)**
- **역할**: 각 페이지별 특화된 비즈니스 로직 및 초기화
- **책임**: 페이지별 기능 구현, 이벤트 처리, 데이터 바인딩

**포함 내역:**
- `plant.js`: 설비 관리 페이지 (목록, 상세, 등록/수정, 이력, 업로드)
- `workorder.js`: 작업지시 관리 페이지 (목록, 상세, 등록/수정, 상태 관리)
- `inventory.js`: 재고 관리 페이지 (목록, 상세, 등록/수정, 재고 조정)
- `inventory-tx.js`: 재고 트랜잭션 페이지 (입고, 출고, 조정, 마감)
- `inspection.js`: 점검 관리 페이지 (계획, 실행, 결과, 이력)
- `workpermit.js`: 작업허가서 관리 페이지 (신청, 승인, 발급, 관리)
- `approval.js`: 전자결재 페이지 (상신, 승인, 반려, 이력)
- `memo.js`: 메모 관리 페이지 (작성, 조회, 수정, 삭제)
- `code.js`: 공통코드 관리 페이지 (코드 그룹, 코드값 관리)
- `domain.js`: 도메인 관리 페이지 (회사, 부서, 사이트, 저장소 관리)
- `member.js`: 사용자 관리 페이지 (등록, 수정, 권한 관리)

#### **📦 common/ (레거시 모듈 - 점진적 제거 예정)**
- **역할**: 기존 공통 모듈 (호환성 유지용)
- **책임**: 기존 기능 보존, 점진적 마이그레이션 대상

**포함 내역:**
- `FileList.js`: 기존 파일 목록 위젯 (ui/file-list.js로 마이그레이션 예정)
- `fileUpload.js`: 기존 파일 업로드 위젯 (ui/file-upload.js로 마이그레이션 예정)
- `print-utils.js`: 기존 인쇄 유틸리티 (ui/print-utils.js로 마이그레이션 예정)
- `tobedeleted_file-widget.js`: 삭제 예정 위젯 (마이그레이션 후 제거)

### 📊 **모듈 간 의존성 관계**

```
main.js
├── core/ (핵심 인프라)
│   ├── csrf.js → 모든 모듈에서 사용
│   ├── navigation.js → pages/ 모듈들과 연동
│   ├── module-loader.js → pages/ 모듈 동적 로딩
│   ├── pages.js → pages/ 모듈 등록/실행
│   └── utils.js → 모든 모듈에서 사용
├── api/ (데이터 계층)
│   ├── auth.js → 모든 모듈에서 사용
│   └── storage.js → ui/ 모듈들에서 사용
└── ui/ (UI 컴포넌트)
    ├── notification.js → 모든 모듈에서 사용
    ├── file-upload.js → pages/ 모듈들에서 사용
    ├── file-list.js → pages/ 모듈들에서 사용
    ├── table-manager.js → pages/ 모듈들에서 사용
    ├── data-loader.js → pages/ 모듈들에서 사용
    ├── confirm-dialog.js → 모든 모듈에서 사용
    ├── validator.js → pages/ 모듈들에서 사용
    └── print-utils.js → pages/ 모듈들에서 사용
```

### 🔄 **마이그레이션 우선순위**

1. **1순위**: core/ (핵심 인프라) - 모든 모듈의 기반
2. **2순위**: api/ (데이터 계층) - 인증 및 저장소 관리
3. **3순위**: ui/ (UI 컴포넌트) - 재사용 가능한 컴포넌트
4. **4순위**: pages/ (페이지 로직) - 비즈니스 로직
5. **5순위**: common/ (레거시) - 점진적 제거

## 4.2 샘플 코드 (구현 표준)

### main.js (엔트리 포인트)
```javascript
// main.js - ES 모듈 엔트리 포인트
import { initCore } from './core/';
import { initApi } from './api/';
import { initUI } from './ui/';

// ES 모듈은 HTML 파싱 완료 후 실행되므로 DOMContentLoaded 불필요
// 하지만 기존 방식과 호환성을 위해 유지
document.addEventListener('DOMContentLoaded', () => {
  // 1. 핵심 시스템 초기화 (순서 중요)
  initCore();
  
  // 2. API 계층 초기화
  initApi();
  
  // 3. UI 컴포넌트 초기화
  initUI();
  
  // 4. 기존 방식과 동일한 초기화 순서 보장
  if (window.cmms && window.cmms.navigation) {
    window.cmms.navigation.init();
  }
  
  // 5. 초기 콘텐츠 로드 (기존 방식과 동일)
  if (window.initialContent) {
    window.cmms.navigation.loadContent(window.initialContent);
  }
});
```

### core/utils.js (공통 유틸리티 - 상세 기능)
```javascript
// core/utils.js - app.js에서 유틸리티 부분 추출
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export function showLoading(element) {
  if (element) {
    element.innerHTML = '<div class="spinner-border spinner-border-sm me-2"></div>로딩 중...';
  }
}

export function hideLoading(element) {
  if (element) {
    element.innerHTML = '';
  }
}

export function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

export function throttle(func, limit) {
  let inThrottle;
  return function() {
    const args = arguments;
    const context = this;
    if (!inThrottle) {
      func.apply(context, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
}

export function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

export function sanitizeHtml(html) {
  const div = document.createElement('div');
  div.textContent = html;
  return div.innerHTML;
}

export function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

export function setCookie(name, value, days) {
  const expires = new Date();
  expires.setTime(expires.getTime() + (days * 24 * 60 * 60 * 1000));
  document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/`;
}

export function removeCookie(name) {
  document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;`;
}

export function copyToClipboard(text) {
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(text);
  } else {
    // Fallback for older browsers
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
      document.execCommand('copy');
      return Promise.resolve();
    } catch (err) {
      return Promise.reject(err);
    } finally {
      document.body.removeChild(textArea);
    }
  }
}

export function downloadFile(url, filename) {
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return '';
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds);
}

export function parseDate(dateString, format = 'YYYY-MM-DD') {
  if (!dateString) return null;
  
  const parts = dateString.split(/[-/]/);
  if (parts.length !== 3) return null;
  
  const year = parseInt(parts[0]);
  const month = parseInt(parts[1]) - 1;
  const day = parseInt(parts[2]);
  
  return new Date(year, month, day);
}

export function isDateValid(date) {
  return date instanceof Date && !isNaN(date.getTime());
}

export function getRandomId(prefix = 'id') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}

export function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj;
  if (obj instanceof Date) return new Date(obj.getTime());
  if (obj instanceof Array) return obj.map(item => deepClone(item));
  if (typeof obj === 'object') {
    const clonedObj = {};
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        clonedObj[key] = deepClone(obj[key]);
      }
    }
    return clonedObj;
  }
}

export function isEmpty(value) {
  if (value === null || value === undefined) return true;
  if (typeof value === 'string') return value.trim().length === 0;
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === 'object') return Object.keys(value).length === 0;
  return false;
}

export function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// 기존 window.cmms.utils 호환성 유지
window.cmms = window.cmms || {};
window.cmms.utils = {
  formatFileSize,
  showLoading,
  hideLoading,
  debounce,
  throttle,
  isValidEmail,
  sanitizeHtml,
  getCookie,
  setCookie,
  removeCookie,
  copyToClipboard,
  downloadFile,
  formatDate,
  parseDate,
  isDateValid,
  getRandomId,
  deepClone,
  isEmpty,
  generateUUID
};
```

### ui/notification.js (알림 시스템 - 상세 기능)
```javascript
// ui/notification.js - app.js에서 알림 부분 추출
export function showNotification(message, type = 'info', duration = 3000) {
  const notification = document.createElement('div');
  notification.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show`;
  notification.style.position = 'fixed';
  notification.style.top = '20px';
  notification.style.right = '20px';
  notification.style.zIndex = '9999';
  notification.style.minWidth = '300px';
  
  notification.innerHTML = `
    <strong>${type === 'error' ? '오류' : type === 'success' ? '성공' : '알림'}</strong> ${message}
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
  `;
  
  document.body.appendChild(notification);
  
  // 자동 제거
  setTimeout(() => {
    if (notification.parentNode) {
      notification.remove();
    }
  }, duration);
  
  return notification;
}

export function showError(message, duration = 5000) {
  return showNotification(message, 'error', duration);
}

export function showSuccess(message, duration = 3000) {
  return showNotification(message, 'success', duration);
}

export function showWarning(message, duration = 4000) {
  return showNotification(message, 'warning', duration);
}

export function showInfo(message, duration = 3000) {
  return showNotification(message, 'info', duration);
}

export function showToast(message, type = 'info', duration = 3000) {
  const toast = document.createElement('div');
  toast.className = `toast align-items-center text-white bg-${type === 'error' ? 'danger' : type} border-0`;
  toast.setAttribute('role', 'alert');
  toast.setAttribute('aria-live', 'assertive');
  toast.setAttribute('aria-atomic', 'true');
  
  toast.innerHTML = `
    <div class="d-flex">
      <div class="toast-body">
        ${message}
      </div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
    </div>
  `;
  
  const toastContainer = document.querySelector('.toast-container') || createToastContainer();
  toastContainer.appendChild(toast);
  
  const bsToast = new bootstrap.Toast(toast, { delay: duration });
  bsToast.show();
  
  toast.addEventListener('hidden.bs.toast', () => {
    toast.remove();
  });
  
  return toast;
}

function createToastContainer() {
  const container = document.createElement('div');
  container.className = 'toast-container position-fixed top-0 end-0 p-3';
  container.style.zIndex = '9999';
  document.body.appendChild(container);
  return container;
}

export function showConfirmDialog(message, title = '확인', confirmText = '확인', cancelText = '취소') {
  return new Promise((resolve) => {
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.setAttribute('tabindex', '-1');
    modal.innerHTML = `
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">${title}</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            ${message}
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">${cancelText}</button>
            <button type="button" class="btn btn-primary" id="confirm-btn">${confirmText}</button>
          </div>
        </div>
      </div>
    `;
    
    document.body.appendChild(modal);
    
    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
    
    const confirmBtn = modal.querySelector('#confirm-btn');
    const cancelBtn = modal.querySelector('.btn-secondary');
    
    confirmBtn.addEventListener('click', () => {
      bsModal.hide();
      resolve(true);
    });
    
    cancelBtn.addEventListener('click', () => {
      bsModal.hide();
      resolve(false);
    });
    
    modal.addEventListener('hidden.bs.modal', () => {
      modal.remove();
    });
  });
}

export function showLoadingDialog(message = '처리 중입니다...') {
  const modal = document.createElement('div');
  modal.className = 'modal fade';
  modal.setAttribute('tabindex', '-1');
  modal.setAttribute('data-bs-backdrop', 'static');
  modal.setAttribute('data-bs-keyboard', 'false');
  modal.innerHTML = `
    <div class="modal-dialog modal-sm modal-dialog-centered">
      <div class="modal-content">
        <div class="modal-body text-center">
          <div class="spinner-border text-primary mb-3" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p class="mb-0">${message}</p>
        </div>
      </div>
    </div>
  `;
  
  document.body.appendChild(modal);
  
  const bsModal = new bootstrap.Modal(modal);
  bsModal.show();
  
  return {
    hide: () => {
      bsModal.hide();
      modal.addEventListener('hidden.bs.modal', () => {
        modal.remove();
      });
    }
  };
}

// 기존 window.cmms.notification 호환성 유지
window.cmms = window.cmms || {};
window.cmms.notification = {
  show: showNotification,
  error: showError,
  success: showSuccess,
  warning: showWarning,
  info: showInfo,
  toast: showToast,
  confirm: showConfirmDialog,
  loading: showLoadingDialog
};
```

### core/index.js (코어 모듈 통합)
```javascript
// core/index.js
export { initCsrf } from './csrf.js';
export { initNavigation } from './navigation.js';
export { initModuleLoader } from './module-loader.js';
export { initPages } from './pages.js';
export { initUtils } from './utils.js';

export function initCore() {
  // 핵심 모듈들을 올바른 순서로 초기화
  initCsrf();
  initUtils();
  initModuleLoader();
  initPages();
  initNavigation();
}
```

### core/navigation.js (동적 모듈 로딩)
```javascript
// core/navigation.js - app.js에서 navigation 부분 추출
export function initNavigation() {
  // 기존 window.cmms.navigation 구조 유지
  window.cmms = window.cmms || {};
  window.cmms.navigation = {
    currentContentUrl: '',
    slot: null,
    
    init: function() { /* 기존 로직 */ },
    navigate: function(targetHref) { /* 기존 로직 */ },
    loadContent: async function(contentUrl, opts) { 
      // HTML 로딩
      const response = await fetch(contentUrl);
      const html = await response.text();
      this.slot.innerHTML = html;
      
      // Dynamic import로 페이지별 모듈 로딩
      const moduleName = extractModuleName(contentUrl);
      if (moduleName) {
        const module = await import(`../pages/${moduleName}.js`);
        if (module.init) {
          module.init(this.slot);
        }
      }
    }
  };
}
```

### defaultLayout.html (롤백 가능한 템플릿)
```html
<!-- defaultLayout.html -->
<!-- 기존 방식 (롤백용, 주석 처리) -->
<!-- 
<script th:src="@{/assets/js/common/fileUpload.js}" src="../../static/assets/js/common/fileUpload.js"></script>
<script th:src="@{/assets/js/common/FileList.js}" src="../../static/assets/js/common/FileList.js"></script>
<script th:src="@{/assets/js/common.js}" src="../../static/assets/js/common.js"></script>
<script th:src="@{/assets/js/auth.js}" src="../../static/assets/js/auth.js"></script>
<script th:src="@{/assets/js/app.js}" src="../../static/assets/js/app.js"></script>
-->

<!-- 새로운 ES 모듈 방식 -->
<script type="module" src="/assets/js/main.js"></script>

<!-- Thymeleaf 설정은 그대로 유지 -->
<script th:inline="javascript">
  // 초기 콘텐츠 설정
  window.initialContent = /*[[${content}]]*/ '/plant/list.html';
  
  // 파일 업로드 설정 로드
  window.fileUploadConfig = {
    maxSize: /*[[${fileUploadConfig.maxSize}]]*/ 10485760,
    allowedExtensions: /*[[${fileUploadConfig.allowedExtensions}]]*/ ['jpg', 'jpeg', 'png', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'hwp', 'hwpx', 'zip', 'txt'],
    maxSizeFormatted: /*[[${fileUploadConfig.maxSizeFormatted}]]*/ '10MB',
    profile: /*[[${fileUploadConfig.profile}]]*/ 'default'
  };
</script>
```

## 4.3 ES 모듈 로딩 방식

### 초기 로딩 (선택적)
```javascript
// main.js에서 import하는 모듈들만 로딩됨
import { initCore } from './core/';        // core/index.js와 그 의존성만
import { initApi } from './api/';          // api/index.js와 그 의존성만  
import { initUI } from './ui/';            // ui/index.js와 그 의존성만

// pages/*.js 모듈들은 로딩되지 않음!
// memo.js, plant.js 등은 사용자가 해당 페이지를 방문할 때만 로딩
```

### 동적 로딩 (페이지별)
```javascript
// memo 링크 클릭 시
// 1. 서버에서 HTML 콘텐츠 가져오기
const response = await fetch('/memo/form');
const html = await response.text();

// 2. HTML을 DOM에 삽입
this.slot.innerHTML = html;

// 3. Dynamic import로 memo.js 모듈 로딩
const module = await import('../pages/memo.js');
if (module.init) {
  module.init(this.slot);  // 4. 페이지 초기화
}
```

## 4.4 롤백 전략

### 완벽한 롤백 가능성
```html
<!-- 문제 발생 시 즉시 롤백 -->
<!-- 새로운 방식 주석 처리 -->
<!-- <script type="module" src="/assets/js/main.js"></script> -->

<!-- 기존 방식 활성화 -->
<script src="/assets/js/app.js"></script>
<script src="/assets/js/common.js"></script>
<script src="/assets/js/auth.js"></script>
```

### 점진적 롤백
```html
<!-- 일부만 롤백 (혼합 사용) -->
<script type="module" src="/assets/js/main.js"></script>

<!-- 문제 있는 모듈만 기존 방식 사용 -->
<script src="/assets/js/common/fileUpload.js"></script>
```

## 5. 위험 및 대응

### 5.1 주요 위험 요소 및 대응 프로세스

#### **위험 1: ES 모듈 브라우저 호환성**
**문제**: ES 모듈을 지원하지 않는 구형 브라우저에서 동작 불가
**감지 기준**: 브라우저 콘솔에서 "Uncaught SyntaxError: Unexpected token 'import'" 에러 발생
**대응 담당**: 프론트엔드 개발자
**대응 절차**:
1. 즉시 기존 방식으로 롤백 (defaultLayout.html에서 주석 해제)
2. 브라우저 지원 범위 재검토 (IE11, Safari 10 등)
3. 필요시 번들러 도입 검토 (Vite, Webpack 등)
4. 폴백 스크립트 추가 검토

#### **위험 2: Dynamic Import 호환성**
**문제**: Dynamic import를 지원하지 않는 브라우저에서 페이지별 모듈 로딩 실패
**감지 기준**: 페이지 네비게이션 시 "TypeError: Failed to resolve module specifier" 에러
**대응 담당**: 프론트엔드 개발자
**대응 절차**:
1. 동적 로딩 실패 시 기존 moduleLoader 방식으로 폴백
2. 모듈 로딩 상태 모니터링 로직 추가
3. 지원하지 않는 브라우저 감지 및 경고 메시지 표시
4. 정적 import로 대체 가능한 모듈 식별

#### **위험 3: 모듈 간 순환 의존성**
**문제**: ES 모듈에서 순환 import로 인한 초기화 실패
**감지 기준**: "Cannot access before initialization" 또는 "Circular dependency" 에러
**대응 담당**: 프론트엔드 개발자
**대응 절차**:
1. 의존성 그래프 분석 도구 사용 (madge 등)
2. 순환 의존성 제거 (의존성 역전, 이벤트 기반 통신 등)
3. 모듈 구조 재설계
4. 단위 테스트로 의존성 검증

#### **위험 4: 기존 전역 네임스페이스 의존성**
**문제**: 기존 window.cmms 호출이 ES 모듈 전환 후 동작하지 않음
**감지 기준**: "TypeError: Cannot read property of undefined" 에러
**대응 담당**: 프론트엔드 개발자
**대응 절차**:
1. grep 검색으로 window.cmms 직접 호출 패턴 검색
2. 누락된 API를 기존 구조에서 복사하여 window.cmms에 추가
3. 해당 호출을 ES 모듈 import로 점진적 전환
4. 브릿지 API 사용 현황 모니터링

#### **위험 5: Thymeleaf 변수 주입 의존성**
**문제**: window.fileUploadConfig 등 서버 변수가 ES 모듈에서 접근 불가
**감지 기준**: 파일 업로드 기능 오작동 또는 설정값 undefined 에러
**대응 담당**: 프론트엔드 개발자 + 백엔드 개발자
**대응 절차**:
1. 서버 변수 주입 확인 (브라우저 개발자 도구)
2. 설정값 fallback 로직 추가
3. 서버-클라이언트 설정 동기화 검증
4. ES 모듈에서 전역 변수 접근 방식 수정

#### **위험 6: 성능 저하**
**문제**: ES 모듈 전환 후 초기 로딩 시간 증가
**감지 기준**: 페이지 로딩 시간 20% 이상 증가 또는 사용자 체감 성능 저하
**대응 담당**: 프론트엔드 개발자
**대응 절차**:
1. 성능 측정 도구로 로딩 시간 분석 (Lighthouse, WebPageTest)
2. 모듈 번들 크기 분석 및 최적화
3. 불필요한 모듈 제거 및 트리 쉐이킹 확인
4. 필요시 번들러 도입으로 최적화

### 5.2 구체적 회귀 테스트 체크리스트
**인증 및 네비게이션**
- [ ] 로그인/로그아웃 정상 동작
- [ ] SPA 네비게이션 (메뉴 클릭 시 콘텐츠 영역 변경)
- [ ] 브라우저 뒤로가기/앞으로가기 정상 동작
- [ ] 직접 URL 접근 시 정상 로드

**파일 업로드/관리**
- [ ] 파일 업로드 위젯 초기화 및 동작
- [ ] 파일 목록 표시 및 삭제
- [ ] 파일 크기 제한 및 확장자 검증
- [ ] 첨부파일이 있는 폼 제출

**주요 CRUD 기능**
- [ ] 설비 관리: 목록 조회, 상세보기, 등록/수정/삭제
- [ ] 작업지시: 목록 조회, 상세보기, 등록/수정/삭제
- [ ] 재고 관리: 목록 조회, 트랜잭션 처리
- [ ] 전자결재: 승인/반려 처리
- [ ] 점검 관리: 점검 계획 및 실행

**에러 처리**
- [ ] CSRF 토큰 만료 시 자동 갱신
- [ ] 네트워크 오류 시 적절한 에러 메시지 표시
- [ ] 404/403 에러 시 적절한 페이지 표시

**브라우저 호환성**
- [ ] Chrome (최신 2개 버전)
- [ ] Firefox (최신 2개 버전)
- [ ] Safari (최신 2개 버전)
- [ ] Edge (최신 2개 버전)

## 6. 테스트 & 검증 전략

### 6.1 테스트 도구 및 환경
- **수동 테스트**: 기존 수동 테스트 시나리오 활용
- **자동화 도구**: npm test (존재 시), 브라우저 콘솔 에러 모니터링
- **성능 측정**: Lighthouse, WebPageTest를 통한 로딩 시간 분석
- **브라우저 테스트**: BrowserStack 또는 로컬 멀티 브라우저 환경
- **배포 전 QA**: 스테이징 환경에서 단계별 기능 점검 및 로그 모니터링

### 6.2 단계별 검증 포인트
**단계 1 (모듈 추출) 검증**
- [ ] 새로운 모듈 구조 생성 완료
- [ ] 기존 파일들 그대로 유지 확인
- [ ] main.js 엔트리 포인트 정상 동작

**단계 2 (점진적 테스트) 검증**
- [ ] 새로운 방식과 기존 방식 기능 동일성 100% 확인
- [ ] 성능 비교 테스트 완료 (로딩 시간 10% 이내 차이)
- [ ] 브라우저 호환성 확인 완료

**단계 3 (완전 전환) 검증**
- [ ] Dynamic import 기반 동적 로딩 정상 동작
- [ ] 페이지별 모듈 로딩 정상 동작
- [ ] 메모리 사용량 최적화 확인

**단계 4 (최적화) 검증**
- [ ] ES 모듈 트리 쉐이킹 최적화 완료
- [ ] 모듈별 독립적 테스트 구축 완료
- [ ] 문서 업데이트 완료

## 7. 커뮤니케이션 계획
- 단계 착수 전 변경 범위와 예상 영향을 주간 공유
- 템플릿 담당자와 인라인 핸들러 제거 일정 조율
- QA/운영 팀에 회귀 테스트 시나리오와 체크리스트 전달
- 브라우저 호환성 이슈 발생 시 즉시 공유 및 롤백 계획 수립

## 8. 향후 과제 및 연동 계획

### 8.1 단계별 최적화 트리거
**단계 2 완료 후 (점진적 테스트 완료)**
- **성능 최적화**: 로딩 시간 분석 결과를 바탕으로 모듈 최적화 검토
- **브라우저 지원**: 호환성 테스트 결과를 바탕으로 지원 범위 조정
- **트리거 조건**: 성능 지표 목표 달성 및 브라우저 호환성 100% 확인 시

**단계 3 완료 후 (완전 전환 완료)**
- **번들러 도입**: Dynamic import 최적화를 위한 Vite/Webpack 도입 검토
- **타입 시스템**: JSDoc/TypeScript 도입을 통한 타입 안정성 확보
- **트리거 조건**: ES 모듈 전환 완료 및 성능 지표 안정화 시

**단계 4 완료 후 (최적화 완료)**
- **고급 최적화**: 코드 스플리팅, 지연 로딩, 캐싱 전략 수립
- **모니터링**: 성능 모니터링 시스템 구축
- **트리거 조건**: 모든 최적화 완료 및 모니터링 시스템 구축 완료 시

### 8.2 최적화 검토 프로세스
1. **각 단계 완료 후 1주 내**: 해당 단계의 최적화 트리거 조건 평가
2. **조건 달성 시**: 기술 리드와 함께 최적화 필요성 및 우선순위 검토
3. **검토 결과**: 다음 단계 진행 또는 최적화 작업 병행 결정
4. **문서화**: 최적화 검토 결과를 별도 문서로 기록

## 9. 마이그레이션 일정 및 리소스 계획

### 9.1 단계별 일정
- **단계 0 (준비)**: 1주 - 기술 리드 + 프론트엔드 개발자 1명
- **단계 1 (모듈 추출)**: 2주 - 프론트엔드 개발자 1명
- **단계 2 (점진적 테스트)**: 1주 - 프론트엔드 개발자 1명 + QA 담당자 0.5명
- **단계 3 (완전 전환)**: 1주 - 프론트엔드 개발자 1명
- **단계 4 (최적화)**: 1주 - 프론트엔드 개발자 1명 + 기술 리드 0.5명
- **총 예상 기간**: 6주

### 9.2 리소스 요구사항
**필수 인력**:
- 프론트엔드 개발자 1명 (풀타임)
- 기술 리드 0.5명 (파트타임, 단계 0, 4에서 참여)
- QA 담당자 0.5명 (파트타임, 단계 2에서 집중 참여)

**필수 도구**:
- 브라우저 개발자 도구 (Chrome DevTools)
- Git 버전 관리 시스템
- 로컬 개발 환경 (Spring Boot + Thymeleaf)
- 스테이징 환경 (배포 전 테스트용)
- 성능 측정 도구 (Lighthouse, WebPageTest)

### 9.3 마일스톤 및 검토점
- **단계 0 완료**: 의존성 분석 완료 및 계획 승인
- **단계 1 완료**: 모듈 추출 완료 및 기존 기능 동작 확인
- **단계 2 완료**: 점진적 테스트 완료 및 성능 비교 완료
- **단계 3 완료**: 완전 전환 완료 및 동적 로딩 정상 동작 확인
- **단계 4 완료**: 최적화 완료 및 문서화 완료

*각 단계별로 충분한 테스트 및 검증 기간을 포함하여 안정성을 확보합니다.

