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

## 2. JavaScript 로딩 순서 및 초기화

### 2.1 전체 로딩 순서
1. **로그인 페이지**: `/auth/login.html` (단독 페이지, SPA 아님)
2. **메인 레이아웃**: `/layout/defaultLayout.html?content=/dashboard/index.html`
3. **파일 모듈 로드**: `common/fileUpload.js`, `common/fileList.js` 먼저 로드
4. **공통 모듈 로드**: `common.js` (테이블 관리, 데이터 로더 등)
5. **핵심 JS 로드**: `app.js` (전역 네임스페이스 및 SPA 네비게이션)
6. **네비게이션 초기화**: `window.cmms.navigation.init()` 호출
7. **콘텐츠 슬롯 삽입**: fetch로 콘텐츠 로드 후 `#layout-slot`에 삽입
8. **페이지 모듈 로드**: URL 기반 `pages/*.js` 동적 로딩
9. **페이지 초기화**: `data-page` 속성 기반 `window.cmms.pages.register()` 호출
10. **위젯 초기화**: 파일 업로드 등 전용 위젯 자동 초기화

### 2.2 메인 애플리케이션 (app.js)

#### 2.2.1 초기화 및 이벤트 바인딩
```javascript
// app.js - DOMContentLoaded에서 최소한의 초기화만 수행
document.addEventListener('DOMContentLoaded', () => {
  // CSRF 토큰 동기화
  window.cmms.csrf.refreshForms();
  
  // [data-confirm] 확인 다이얼로그 처리
  const confirmables = document.querySelectorAll('[data-confirm]');
  confirmables.forEach((el) => {
    el.addEventListener('click', (e) => {
      const msg = el.getAttribute('data-confirm') || '확인하시겠습니까?';
      if (!confirm(msg)) {
        e.preventDefault();
        e.stopPropagation();
      }
    });
  });
});

// 네비게이션 초기화는 defaultLayout.html에서 호출
// <script>window.cmms.navigation.init();</script>
```

#### 2.2.2 네비게이션 시스템 (app.js)
```javascript
// app.js의 navigation 모듈
window.cmms.navigation = {
  init: function() {
    // 클릭 이벤트 리스너 등록 (SPA 네비게이션)
    document.addEventListener('click', (e) => {
      const anchor = e.target.closest('a[href]');
      if (anchor && anchor.getAttribute('href')) {
        const href = anchor.getAttribute('href');
        // 외부 링크, 다운로드, 새창 열기 등은 제외
        if (href.startsWith('http') || href.startsWith('mailto:') || 
            href.startsWith('#') || href.startsWith('/api/auth/logout') ||
            href.startsWith('/auth/') || href.startsWith('/api/files') ||
            anchor.hasAttribute('data-hard-nav') || anchor.target === '_blank') {
          return; // 브라우저 기본 동작 사용
        }
        e.preventDefault();
        this.navigate(href);
      }
    }, { capture: true });

    // 브라우저 뒤로가기/앞으로가기 처리
    window.addEventListener('popstate', (e) => {
      const content = e.state?.content || new URLSearchParams(window.location.search).get('content') || '../plant/list.html';
      this.loadContent(content, { push: false });
    });

    // 초기 콘텐츠 로드
    const initialContent = window.initialContent || new URLSearchParams(window.location.search).get('content') || '../plant/list.html';
    this.loadContent(initialContent, { push: false });
  }
};
```

#### 2.2.3 콘텐츠 로딩 및 위젯 초기화
```javascript
// app.js의 loadContent 메서드
loadContent: function(url, { push = true } = {}) {
  this.currentContentUrl = url;
  const slot = document.getElementById('layout-slot');
  
  // 로딩 상태 표시
  slot.innerHTML = '<div class="loading">로딩 중...</div>';
  
  // URL 정규화
  const normalizedUrl = url.startsWith('/') ? url : '/' + url;
  
  fetch(normalizedUrl)
    .then((res) => {
      if (res.status === 403) throw window.cmms.csrf.toCsrfError(res);
      if (!res.ok) throw new Error('Load failed: ' + res.status);
      return res.text();
    })
    .then((html) => {
      slot.innerHTML = html;
      this.slot = slot;
      
      // 히스토리 업데이트
      if (push) {
        const state = { content: normalizedUrl };
        history.pushState(state, '', normalizedUrl);
      }
      
      // 모듈 로드
      const moduleId = this.extractModuleId(normalizedUrl);
      if (moduleId) {
        this.loadModule(moduleId);
      }
      
      // SPA 폼 처리
      this.handleSPAForms();
      
      // 파일 위젯 초기화 (전체 문서 대상)
      setTimeout(() => {
        const uploadModule = (window.cmms && window.cmms.fileUpload) || null;
        if (uploadModule && typeof uploadModule.initializeContainers === 'function') {
          uploadModule.initializeContainers(document);
        }
        
        const fileListModule = (window.cmms && window.cmms.fileList) || null;
        if (fileListModule && typeof fileListModule.initializeContainers === 'function') {
          fileListModule.initializeContainers(this.slot);
        }
      }, 10);
    })
    .catch((err) => {
      console.error(err);
      slot.innerHTML = '<div class="notice danger">페이지를 불러올 수 없습니다. 다시 시도해주세요.</div>';
    });
}
```

### 2.3 모듈 로더 시스템

#### 2.3.1 모듈 매핑 및 로딩
```javascript
// app.js에 정의된 모듈 로더
window.cmms.moduleLoader = {
    moduleMap: {
        'workorder': '/assets/js/pages/workorder.js',
        'plant': '/assets/js/pages/plant.js',
        'member': '/assets/js/pages/member.js',
        'inventory': '/assets/js/pages/inventory.js',
        'inventory-tx': '/assets/js/pages/inventory-tx.js',
        'inspection': '/assets/js/pages/inspection.js',
        'workpermit': '/assets/js/pages/workpermit.js',
        'approval': '/assets/js/pages/approval.js',
        'memo': '/assets/js/pages/memo.js',
        'code': '/assets/js/pages/code.js',
        'domain': '/assets/js/pages/domain.js'
    },
    
    loadedModules: new Set(),
    
    loadModule: function(moduleId) {
        const modulePath = this.moduleMap[moduleId];
        
        if (modulePath && !this.loadedModules.has(moduleId)) {
            this.loadScript(modulePath).then(() => {
                this.loadedModules.add(moduleId);
                this.initializeModule(moduleId);
              }).catch((e) => {
                console.warn('Module load failed:', moduleId, e);
              });
        } else if (this.loadedModules.has(moduleId)) {
          this.initializeModule(moduleId);
        }
    },
    
    extractModuleId: function(url) {
        // /plant/list.html -> plant
        // /dashboard/index.html -> dashboard
        const match = url.match(/\/([^\/]+)\//);
        return match ? match[1] : null;
    },
    
    loadScript: function(src) {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = src;
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
    },
    
    initializeModule: function(moduleId) {
        const module = window.cmms.modules && window.cmms.modules[moduleId];
        if (module && typeof module.init === 'function') {
            module.init();
        }
    }
};

// 네비게이션에 모듈 로더 연결
window.cmms.navigation.loadModule = window.cmms.moduleLoader.loadModule.bind(window.cmms.moduleLoader);
window.cmms.navigation.extractModuleId = window.cmms.moduleLoader.extractModuleId.bind(window.cmms.moduleLoader);
```

## 3. 파일 관리 시스템

### 3.1 파일 업로드 모듈 연계 방식

#### 3.1.1 로딩 순서 최적화
```html
<!-- defaultLayout.html에서 올바른 로딩 순서 -->
<script th:src="@{/assets/js/common/fileUpload.js}" src="../../static/assets/js/common/fileUpload.js"></script>
<script th:src="@{/assets/js/common/FileList.js}" src="../../static/assets/js/common/FileList.js"></script>
<script th:src="@{/assets/js/common.js}" src="../../static/assets/js/common.js"></script>
<script th:src="@{/assets/js/app.js}" src="../../static/assets/js/app.js"></script>
```

#### 3.1.2 common/fileUpload.js에서의 등록
```javascript
// common/fileUpload.js
window.cmms = window.cmms || {};
window.cmms.fileUpload = {
  config: {
    isLoaded: false,
    uploadUrl: '/api/files/upload',
    maxFileSize: 10 * 1024 * 1024
  },
  
  loadConfig: function() {
    // 설정 로드 로직
    return Promise.resolve();
  },
  
  ensureConfigLoaded: function() {
    if (!this.config.isLoaded) {
      return this.loadConfig().then(() => {
        this.config.isLoaded = true;
      });
    }
    return Promise.resolve();
  },
  
  initializeContainers: function(root) {
    const containers = (root || document).querySelectorAll('[data-attachments]');
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
  }
};
```

### 3.2 파일 목록 모듈 연계 방식

#### 3.2.1 common/fileList.js에서의 등록
```javascript
// common/fileList.js
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
    
    // 파일 목록 컨테이너 초기화
    // 파일 목록 로드, 다운로드, 삭제 등
    
    container.dataset.initialized = 'true';
  },
  
  loadFiles: function(fileGroupId) {
    // 파일 목록 로드
  },
  
  deleteFile: function(fileId) {
    // 파일 삭제
  }
};
```

### 3.3 실제 연계 방식

#### 3.3.1 SPA 콘텐츠 로드 후 위젯 초기화
```javascript
// app.js의 loadContent 메서드에서 위젯 초기화
// 파일 위젯 초기화 (전체 문서 대상)
setTimeout(() => {
  const uploadModule = (window.cmms && window.cmms.fileUpload) || null;
  if (uploadModule && typeof uploadModule.initializeContainers === 'function') {
    uploadModule.initializeContainers(document); // 전체 문서 대상
  }
  
  const fileListModule = (window.cmms && window.cmms.fileList) || null;
  if (fileListModule && typeof fileListModule.initializeContainers === 'function') {
    fileListModule.initializeContainers(this.slot); // SPA 슬롯 대상
  }
}, 10);
```

#### 3.3.2 위젯 초기화 범위
- **파일 업로드**: `document` 전체 대상 (기존 페이지와 새로 로드된 페이지 모두)
- **파일 목록**: `this.slot` 대상 (SPA로 로드된 콘텐츠만)
- **초기화 중복 방지**: `dataset.initialized` 속성으로 중복 초기화 방지

### 3.4 파일 업로드 기능 구현

#### 3.4.1 파일 업로드 처리
```javascript
// common/fileUpload.js의 파일 업로드 로직
handleFileSelect: function(event, container) {
  const files = Array.from(event.target.files);
  if (files.length === 0) return;
  
  this.ensureConfigLoaded().then(() => {
    this.uploadFiles(files, container);
  });
},

uploadFiles: function(files, container) {
  const formData = new FormData();
  files.forEach(file => formData.append('files', file));
  
  const fileGroupIdInput = container.querySelector('input[name="fileGroupId"]');
  if (fileGroupIdInput && fileGroupIdInput.value) {
    formData.append('groupId', fileGroupIdInput.value);
  }
  
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
      if (fileGroupIdInput && data.fileGroupId) {
        fileGroupIdInput.value = data.fileGroupId;
      }
      this.updateFileList(data.files, container);
    } else {
      this.showError('파일 업로드 실패: ' + data.message);
    }
  })
  .catch(error => {
    console.error('업로드 오류:', error);
    this.showError('파일 업로드 중 오류가 발생했습니다.');
  });
}
```

## 4. 공통 유틸리티 (common.js)

### 4.1 테이블 관리자

#### 4.1.1 테이블 행 클릭 및 액션 처리
```javascript
// common.js의 TableManager
window.cmms.common = window.cmms.common || {};

window.cmms.common.TableManager = {
  init: function() {
    this.bindRowClickEvents();
    this.bindActionButtons();
  },
  
  bindRowClickEvents: function() {
    document.addEventListener('click', function(e) {
      const row = e.target.closest('tr[data-row-link]');
      if (row && !e.target.closest('button, a')) {
        const url = row.dataset.rowLink;
        window.cmms.navigation.loadContent(url);
      }
    });
  },
  
  bindActionButtons: function() {
    // 삭제 버튼 확인 다이얼로그는 app.js에서 처리
  }
};
```

### 4.2 데이터 로더

#### 4.2.1 AJAX 데이터 로딩
```javascript
window.cmms.common.DataLoader = {
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
    const token = document.querySelector('meta[name="_csrf"]');
    return token ? token.getAttribute('content') : '';
  }
};
```

### 4.3 확인 다이얼로그

#### 4.3.1 확인 다이얼로그 처리
```javascript
window.cmms.common.ConfirmDialog = {
  show: function(message, callback) {
    if (confirm(message)) {
      callback();
    }
  }
};
```

### 4.4 폼 유효성 검사

#### 4.4.1 수동 유효성 검사
```javascript
window.cmms.common.Validator = {
  validate: function(form) {
    const errors = [];
    const requiredFields = form.querySelectorAll('[required]');
    
    requiredFields.forEach(field => {
      const value = field.value.trim();
      const label = form.querySelector(`label[for="${field.id}"]`)?.textContent || field.name;
      
      if (!value) {
        errors.push(`${label}은(는) 필수 입력 항목입니다.`);
        field.classList.add('error');
      } else {
        field.classList.remove('error');
      }
    });
    
    return {
      isValid: errors.length === 0,
      errors
    };
  }
};
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
