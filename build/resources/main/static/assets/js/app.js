/**
 * CMMS JavaScript 모듈 시스템
 * 
 * 이 파일은 CMMS 애플리케이션의 모든 JavaScript 기능을 통합 관리합니다.
 * 기본 인프라 역할을 담당하며, 공통 모듈들과 SPA 네비게이션을 제공합니다.
 * 
 * 모듈 구조:
 * - window.cmms.csrf: CSRF 토큰 관리 (자동 동기화)
 * - window.cmms.utils: 유틸리티 함수들 (파일 크기 포맷팅 등)
 * - window.cmms.notification: 알림 시스템 (성공/에러/경고)
 * - window.cmms.navigation: SPA 네비게이션 (동적 콘텐츠 로드)
 * - window.cmms.moduleLoader: 페이지별 모듈 동적 로더
 * - window.cmms.pages: 페이지 초기화 훅 시스템
 * - window.cmms.user: 사용자 정보 관리
 * 
 * 외부 모듈 (별도 로드):
 * - window.cmms.fileUpload: 파일 업로드 위젯 (common/fileUpload.js)
 * - window.cmms.fileList: 파일 목록 위젯 (common/FileList.js)
 * 
 * 사용법:
 * - window.cmms.notification.success('성공 메시지');
 * - window.cmms.navigation.navigate('/plant/list');
 * - window.cmms.fileUpload.initializeContainers(container);
 * 
 * 초기화 순서:
 * 1. DOMContentLoaded: CSRF 토큰 동기화, 전역 이벤트 바인딩
 * 2. navigation.init(): SPA 네비게이션 시스템 초기화
 * 3. loadContent(): 파일 위젯 자동 초기화
 */

// =============================================================================
// CSRF 토큰 관리 함수
// =============================================================================

const CSRF_COOKIE_NAME = 'XSRF-TOKEN';

const CSRF_SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

/**
 * 쿠키에서 CSRF 토큰을 읽어오는 함수
 * @returns {string|null} CSRF 토큰은 null
 */
function readCsrfTokenFromCookie() {
  const cookies = document.cookie ? document.cookie.split('; ') : [];
  for (const entry of cookies) {
    if (entry.startsWith(`${CSRF_COOKIE_NAME}=`)) {
      return decodeURIComponent(entry.split('=').slice(1).join('='));
    }
  }
  return null;
}

/**
 * 모든 폼의 CSRF hidden 필드를 동기화하는 함수
 * POST 메서드에 대해서는 CSRF 토큰을 추가
 */
function syncCsrfHiddenFields() {
  const token = readCsrfTokenFromCookie();
  if (!token) return;
  const forms = document.querySelectorAll('form');
  forms.forEach((form) => {
    const method = (form.getAttribute('method') || 'get').toUpperCase();
    if (method !== 'POST') return;
    let csrfInput = form.querySelector('input[name="_csrf"]');
    if (!csrfInput) {
      csrfInput = document.createElement('input');
      csrfInput.type = 'hidden';
      csrfInput.name = '_csrf';
      form.appendChild(csrfInput);
    }
    csrfInput.value = token;
  });
}

/**
 * 요청에 CSRF 헤더를 첨부해야 하는지 결정하는 함수
 * @param {Request} request - Fetch API Request 객체
 * @returns {boolean} CSRF 헤더 첨부 여부
 */
function shouldAttachCsrfHeader(request) {
  try {
    const url = new URL(request.url);
    const isSameOrigin = url.origin === window.location.origin;
    const method = (request.method || 'GET').toUpperCase();
    return isSameOrigin && !CSRF_SAFE_METHODS.has(method);
  } catch (err) {
    // In case of relative URLs, new URL may throw; default to attaching
    const method = (request.method || 'GET').toUpperCase();
    return !CSRF_SAFE_METHODS.has(method);
  }
}

// =============================================================================
// Fetch API 핑 - CSRF 토큰 동기화 처리
// =============================================================================

/**
 * Fetch API 핑 처리와 CSRF 토큰 동기화를 수행하는 IIFE
 * 모든 fetch 요청에 X-CSRF-TOKEN 헤더를 추가
 */
(function wrapFetchWithCsrf() {
  if (typeof window === 'undefined' || typeof window.fetch !== 'function') {
    return;
  }

  const originalFetch = window.fetch.bind(window);

  window.fetch = function csrfAwareFetch(input, init) {
    const request = new Request(input, init);
    const token = readCsrfTokenFromCookie();

    let finalRequest = request;
    if (token && shouldAttachCsrfHeader(request)) {
      const headers = new Headers(request.headers || {});
      headers.set('X-CSRF-TOKEN', token);
      finalRequest = new Request(request, { headers });
    }

    const responsePromise = originalFetch(finalRequest);
    return responsePromise.finally(() => {
      try {
        window.cmms.csrf.refreshForms();
      } catch (e) {
        // no-op; syncing CSRF fields should not break application flow
      }
    });
  };
})();

// =============================================================================
// SPA 모듈 동적 로더 시스템
// =============================================================================
window.cmms = window.cmms || {};
window.cmms.moduleLoader = {
  // 경로→모듈 매핑 테이블
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
  
  // 로딩 상태 테이블
  loadingStates: {},
  
  /**
   * 경로에서 모듈 식별자 추출
   */
  extractModuleId: function(path) {
    if (!path) return null;
    
    // URL 경로에서 모듈명 추출
    const pathParts = path.split('/');
    const moduleName = pathParts[1];
    
    // 매핑 테이블에 있는지 확인 (원본 그대로 우선)
    if (this.moduleMap[moduleName]) {
      return moduleName;
    }
    
    // 카멜케이스를 케밥케이스로 변환해서 재시도
    // 예: inventoryTx -> inventory-tx
    const kebabCase = moduleName.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
    if (this.moduleMap[kebabCase]) {
      return kebabCase;
    }
    
    return null;
  },
  
  /**
   * 스크립트 동적 주입 함수
   */
  injectScript: function(src) {
    return new Promise((resolve, reject) => {
      // 이미 로드된 스크립트인지 확인
      if (this.loadingStates[src] === 'loaded') {
        resolve();
        return;
      }
      
      // 로딩 중인 스크립트인지 확인
      if (this.loadingStates[src] === 'loading') {
        // 로딩 완료까지 대기
        const checkLoaded = () => {
          if (this.loadingStates[src] === 'loaded') {
            resolve();
          } else if (this.loadingStates[src] === 'error') {
            reject(new Error(`Module loading failed: ${src}`));
          } else {
            setTimeout(checkLoaded, 50);
          }
        };
        checkLoaded();
        return;
      }
      
      // 새로운 스크립트 로딩 시작
      this.loadingStates[src] = 'loading';
      
      const script = document.createElement('script');
      script.src = src;
      script.async = true;
      
      script.onload = () => {
        this.loadingStates[src] = 'loaded';
        console.log(`모듈 로드 완료: ${src}`);
        resolve();
      };
      
      script.onerror = () => {
        this.loadingStates[src] = 'error';
        console.error(`모듈 로드 실패: ${src}`);
        reject(new Error(`Failed to load module: ${src}`));
      };
      
      document.head.appendChild(script);
    });
  },
  
  /**
   * 모듈 동적 로딩
   */
  loadModule: async function(contentUrl) {
    const moduleId = this.extractModuleId(contentUrl);
    if (!moduleId) {
      console.log(`모듈 로딩 건너뜀: ${contentUrl} (매핑 없음)`);
      return;
    }
    
    const scriptSrc = this.moduleMap[moduleId];
    if (!scriptSrc) {
      console.warn(`모듈 스크립트 경로 없음: ${moduleId}`);
      return;
    }
    
    try {
      await this.injectScript(scriptSrc);
    } catch (error) {
      console.error(`모듈 로딩 실패: ${moduleId}`, error);
      // 로딩 실패해도 페이지 동작은 계속
    }
  }
};

// =============================================================================
// Page Init Hooks
// =============================================================================
window.cmms.pages = window.cmms.pages || {
  registry: {},
  register: function(name, initFn) {
    if (typeof initFn !== 'function') return;
    this.registry[name] = initFn;
  },
  run: function(container, ctx) {
    try {
      if (!container) return;
      const pageRoot = container.querySelector('[data-page]') || container;
      const pageId = pageRoot.getAttribute && pageRoot.getAttribute('data-page');
      if (pageId && typeof this.registry[pageId] === 'function') {
        if (pageRoot.__cmmsPageInited === pageId) return;
        pageRoot.__cmmsPageInited = pageId;
        this.registry[pageId](pageRoot, ctx || {});
      }
    } catch (e) {
      console.warn('cmms.pages.run failed:', e);
    }
  }
};

// =============================================================================
// DOM 로드 이벤트 처리와 UI 호출 처리
// =============================================================================

/**
 * DOM 로드 이벤트 발생 시 실행되는 초기화 함수
 * 이벤트 리스너, 인터랙티브 요소, 파일 업로드 위젯 초기화
 */
// 파일 업로드 위젯 초기화는 common/fileUpload.js에서 처리됩니다.
document.addEventListener('DOMContentLoaded', () => {
  window.cmms.csrf.refreshForms();
  

  // 파일 업로드 설정은 fileUpload.js에서 필요시 자동 로드됨 (ensureConfigLoaded)


  // [data-row-link] 이벤트는 navigation.init()에서 통합 처리됨

  const confirmables = document.querySelectorAll('[data-confirm]');
  confirmables.forEach((el) => {
    el.addEventListener('click', (e) => {
      const msg = el.getAttribute('data-confirm') || '인식시겠습니까?';
      if (!confirm(msg)) {
        e.preventDefault();
        e.stopPropagation();
      }
    });
  });

  // 파일 업로드 위젯 초기화는 navigation.loadContent()에서 통합 처리
  // DOMContentLoaded에서는 설정만 로드
  
  // 로그아웃 버튼 이벤트 리스너 추가
  document.addEventListener('click', function(e) {
    const logoutElement = e.target.closest('[data-logout]');
    if (logoutElement) {
      e.preventDefault();
      
      // 확인 대화상자 (사용자 실수 방지)
      if (confirm('로그아웃하시겠습니까?')) {
        if (window.cmms.auth) {
          window.cmms.auth.logout({
            onSuccess: function(result) {
              console.log('Logout successful:', result);
            },
            onError: function(error) {
              console.error('Logout failed:', error);
            }
          });
        } else {
          // auth 모듈이 로드되지 않은 경우 fallback
          window.location.href = '/api/auth/logout';
        }
      }
    }
  });

});

// =============================================================================
// 유틸리티 함수들(파일 업로드 위젯에서 사용)
// =============================================================================

/**
 * 루트 요소에서 지정된 선택자에 해당하는 모든 요소를 찾는 함수
 * @param {Element} root - 검색할 루트 요소
 * @param {string} selector - CSS 선택자
 * @returns {Element[]} 찾은 요소의 배열
 */
  function findAll(root, selector) {
    if (!root) return [];
    const nodes = [];
    if (root instanceof Element && root.matches(selector)) {
      nodes.push(root);
    }
    const scoped = root.querySelectorAll ? root.querySelectorAll(selector) : [];
    return nodes.concat(Array.from(scoped));
  }

/**
 * 바이트 크기를 사람이 읽기 쉬운 형식으로 변환하는 함수
 * @param {number} bytes - 바이트 크기
 * @returns {string} 변환된 파일 크기 문자열
 */
  function formatFileSize(bytes) {
    if (!bytes) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    const size = bytes / Math.pow(k, i);
    return `${parseFloat(size.toFixed(2))} ${sizes[i]}`;
  }

/**
 * 첨부된 파일 목록을 생성하는 함수
 * @param {File} file - 첨부된 파일 객체
 * @param {Document} doc - 문서 객체 (기본 document)
 * @returns {HTMLLIElement} 생성된 리스트 아이템 요소
 */
  function createAttachmentListItem(file, doc) {
    const documentRef = doc || document;
    const li = documentRef.createElement('li');
    li.className = 'attachment-item';

    const fileName = documentRef.createElement('span');
    fileName.className = 'file-name';
    fileName.textContent = file.name;

    const fileSize = documentRef.createElement('span');
    fileSize.className = 'file-size';
    fileSize.textContent = formatFileSize(file.size);

    const removeButton = documentRef.createElement('button');
    removeButton.type = 'button';
    removeButton.className = 'btn-remove';
    removeButton.textContent = '삭제';
    removeButton.setAttribute('aria-label', `${file.name} 파일 삭제`);

    removeButton.addEventListener('click', () => {
      const list = li.parentElement;
      li.remove();
      if (!list) return;
      const remainingItems = list.querySelectorAll('.attachment-item');
      if (remainingItems.length === 1) {
        const emptyMessage = documentRef.createElement('li');
        emptyMessage.className = 'empty';
        emptyMessage.textContent = '첨부된 파일이 없습니다.';
        list.appendChild(emptyMessage);
      }
    });

    li.appendChild(fileName);
    li.appendChild(fileSize);
    li.appendChild(removeButton);

    return li;
  }
  
// =============================================================================
// CMMS 역할 관리 기능 (SPA 기능)
// =============================================================================

// window.cmms는 이미 122라인에서 선언됨

// =============================================================================
// CSRF 관리 모듈
// =============================================================================
window.cmms.csrf = {
  refreshForms: syncCsrfHiddenFields,
  readToken: readCsrfTokenFromCookie,
  getToken: readCsrfTokenFromCookie, // getToken 별칭 추가 (auth.js 호환)
  shouldAttachHeader: shouldAttachCsrfHeader,
  toCsrfError: function(response) {
    if (typeof createCsrfForbiddenError === 'function') {
      return createCsrfForbiddenError(response);
    }
    const error = new Error('Forbidden');
    error.name = 'CsrfForbiddenError';
    error.response = response;
    return error;
  },
  handleError: function(response) {
    if (response.status === 403) {
      window.cmms.notification.error('세션이 만료되었습니다. 다시 로그인해주세요.');
      setTimeout(() => {
        window.location.reload();
      }, 2000);
      return true;
    }
    return false;
  }
};

// =============================================================================
// 유틸리티 모듈
// =============================================================================
window.cmms.utils = {
  formatFileSize: formatFileSize,
  findAll: findAll,
  createAttachmentListItem: createAttachmentListItem
};

// =============================================================================
// 알림 시스템 모듈
// =============================================================================
window.cmms.notification = {
  show: function(message, type = 'info', duration = 3000) {
    // 기존 알림 제거
    const existing = document.querySelector('.cmms-notification');
    if (existing) {
      existing.remove();
    }

    // 알림 생성
    const notification = document.createElement('div');
    notification.className = `cmms-notification ${type}`;
    notification.textContent = message;
    notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 9999;
      max-width: 400px;
      padding: 12px 16px;
      border-radius: 4px;
      color: white;
      font-weight: 500;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    `;
    
    // 타입별 알림 배경색 설정
    if (type === 'success') {
      notification.style.backgroundColor = '#10b981';
    } else if (type === 'error') {
      notification.style.backgroundColor = '#ef4444';
    } else if (type === 'warning') {
      notification.style.backgroundColor = '#f59e0b';
    } else {
      notification.style.backgroundColor = '#3b82f6';
    }
    
    document.body.appendChild(notification);

    // 자동 삭제
    setTimeout(() => {
      if (notification.parentNode) {
        notification.remove();
      }
    }, duration);
  },
  
  success: function(message) {
    this.show(message, 'success');
  },
  
  error: function(message) {
    this.show(message, 'error');
  },
  
  warning: function(message) {
    this.show(message, 'warning');
  }
};

// =============================================================================
// 네비게이션 모듈 (SPA 기능)
// =============================================================================
window.cmms.navigation = {
  slot: null,
  currentContentUrl: '../plant/list.html',

  /**
   * Thymeleaf 링크를 URL로 변환하는 함수
   * @param {string} href - 변할 URL
   * @param {string} basePath - 기본 경로
   * @returns {string} 변환된 URL
   */
  resolveUrl: function resolveUrl(href, basePath) {
    try {
      // Handle unprocessed Thymeleaf-style links like '@{/domain/company/list}'
      if (href && href.startsWith('@{') && href.endsWith('}')) {
        href = href.slice(2, -1); // strip '@{' and '}'
      }
      // Absolute or external URLs: return as-is
      if (href.startsWith('http://') || href.startsWith('https://') || href.startsWith('/')) {
        return href;
      }

      // Treat non-HTML app routes (e.g., domain/company/list, api/...) as root-relative
      const isHtmlFile = /\.html($|[?#])/.test(href);
      if (!isHtmlFile) {
        return '/' + href.replace(/^\/+/, '');
      }

      // For HTML partials, resolve relative to the current content directory
      const baseDir = (() => {
        if (!basePath) return '/';
        const idx = basePath.lastIndexOf('/');
        if (idx <= 0) return '/';
        return basePath.slice(0, idx + 1);
      })();
      const u = new URL(href, window.location.origin + baseDir);
      return u.pathname + u.search;
    } catch (_) {
      return href;
    }
  },

  /**
   * 브라우저 스택 리스트에 URL 데이터를 추가하는 함수
   * @param {string} contentUrl - 콘텐츠 URL
   * @param {boolean} push - 스택 리스트에 추가하는지 여부
   */
  setState: function setState(contentUrl, push) {
    const url = '/layout/defaultLayout.html?content=' + encodeURIComponent(contentUrl);
    const state = { content: contentUrl };
    if (push) history.pushState(state, '', url);
    else history.replaceState(state, '', url);
  },

  /**
   * 현재 활성화된 메뉴를 설정하는 함수
   * @param {string} contentUrl - 콘텐츠 URL
   */
  setActive: function setActive(contentUrl) {
    try {
      const links = document.querySelectorAll('.sidebar .menu-item');
      const targetPath = new URL(contentUrl, window.location.origin).pathname;
      links.forEach((a) => {
        a.classList.remove('active');
        const href = a.getAttribute('href') || '';
        if (!href.startsWith('/layout/')) return;
        const u = new URL(href, window.location.origin);
        const c = u.searchParams.get('content') || '';
        if (!c) return;
        if (new URL(c, window.location.origin).pathname === targetPath) {
          a.classList.add('active');
          const grp = a.closest('.menu-group');
          if (grp && !grp.classList.contains('open')) {
            grp.classList.add('open');
            const btn = grp.querySelector('.menu-title');
            if (btn) btn.setAttribute('aria-expanded', 'true');
          }
        }
      });
    } catch (_) { /* noop */ }
  },

  /**
   * AJAX 콘텐츠 가져오기와 출력 트리거를 수행하는 함수
   * @param {string} contentUrl - 콘텐츠 URL
   * @param {Object} opts - 옵션 객체
   */
  loadContent: async function loadContent(contentUrl, opts = { push: false }) {
    this.currentContentUrl = contentUrl;
    if (opts.push === true) this.setState(contentUrl, true);
    
    // URL 효과적으로 검증
    if (!contentUrl || contentUrl.trim() === '') {
      console.warn('Empty content URL, redirecting to default');
      this.navigate('../plant/list.html');
      return;
    }
    
    // 못 찾을 URL 검증
    if (contentUrl.includes('..') && contentUrl.split('..').length > 2) {
      console.warn('Invalid URL pattern detected:', contentUrl);
      this.slot.innerHTML = `
        <div class="notice danger">
          <h3>못 찾을 URL이 있습니다.</h3>
          <p>보안의 유효한 URL이 아닙니다.</p>
          <a class="btn primary" href="/domain/company/list">목록으로 이동</a>
        </div>
      `;
      return;
    }
    
    try {
      const r = await fetch(contentUrl, { credentials: 'same-origin' });
      if (r.status === 403) {
        throw window.cmms.csrf.toCsrfError(r);
      }
      const html = await r.text();
      const parser = new DOMParser();
      const doc = parser.parseFromString(html, 'text/html');
      
      // data-slot-root 우선, 그다음 data-page, 마지막으로 main 요소 순으로 선택
      const slotRoot = doc.querySelector('[data-slot-root]');
      const pageRoot = doc.querySelector('[data-page]');
      const main = doc.querySelector('main');
      
      // 우선순위: data-slot-root > data-page > main > body
      let contentElement = null;
      if (slotRoot) {
        contentElement = slotRoot;
      } else if (pageRoot) {
        contentElement = pageRoot;
      } else if (main) {
        contentElement = main;
      } else {
        contentElement = doc.body;
      }
      
      // 요소 자체를 슬롯에 주입하여 래퍼 구조 보존
      this.slot.innerHTML = '';
      if (contentElement) {
        this.slot.appendChild(contentElement);
      } else {
        this.slot.innerHTML = html;
      }
      
      // CSRF 토큰은 이미 fetch wrapper에서 자동 처리됨

      // 모듈 동적 로딩 (첫 방문 시에만)
      await window.cmms.moduleLoader.loadModule(this.currentContentUrl);
      
      // SPA 콘텐츠 로드 이벤트 트리거 실행 (DOM 로드 직후)
      this.executePageScripts(doc);
      // Standardized page init hook (after scripts)
      if (window.cmms?.pages?.run) {
        window.cmms.pages.run(this.slot, { url: this.currentContentUrl, doc });
      }
      
      // 파일 위젯 초기화 (전체 document 대상)
      setTimeout(() => {
        try {
          // 파일 업로드 위젯 초기화 (전체 document 대상)
          const uploadModule = (window.cmms && window.cmms.fileUpload) || null;
          if (uploadModule && typeof uploadModule.initializeContainers === 'function') {
            uploadModule.initializeContainers(document);
          }
          
          // 파일 목록 위젯 초기화 (SPA 콘텐츠 로드 직후)
          const fileListModule = (window.cmms && window.cmms.fileList) || null;
          if (fileListModule && typeof fileListModule.initializeContainers === 'function') {
            fileListModule.initializeContainers(this.slot);
          }
        } catch (error) {
          console.warn('File widget initialization failed:', error);
        }
      }, 10);

      // Intercept SPA-friendly form submissions inside slot
      const forms = this.slot.querySelectorAll('form[data-redirect]');
      forms.forEach((form) => {
        if (form.__cmmsHandled) return;
        form.__cmmsHandled = true;
        form.addEventListener('submit', (e) => {
          try {
            e.preventDefault();
            const action = form.getAttribute('action') || '';
            const method = (form.getAttribute('method') || 'post').toUpperCase();
            const redirectTo = form.getAttribute('data-redirect') || this.currentContentUrl;
            const formData = new FormData(form);
            fetch(action, {
              method,
              body: formData,
              credentials: 'same-origin'
            }).then((res) => {
              if (res.status === 403) throw window.cmms.csrf.toCsrfError(res);
              if (!res.ok) throw new Error('Submit failed: ' + res.status);
              this.navigate(redirectTo);
            }).catch((err) => {
              console.error(err);
              const notice = document.createElement('div');
              notice.className = 'notice danger';
              notice.textContent = '요청에 실패했습니다. 다시 시도해주세요.';
              form.prepend(notice);
            });
          } catch (err) {
            console.error(err);
          }
        });
      });
      
      this.setActive(this.currentContentUrl);
      const title = doc.querySelector('title');
      if (title && title.textContent) document.title = title.textContent + ' · CMMS';
      
    } catch (err) {
      if (err && err.name === 'CsrfForbiddenError') {
        return;
      }
      console.error('Content load error:', err);
      
      // URL 못 찾을 경우 기본 페이지로
      if (err.message.includes('404') || err.message.includes('Not Found')) {
        this.slot.innerHTML = `
          <div class="notice danger">
            <h3>페이지 찾을 수 없습니다</h3>
            <p>요청한 페이지가 존재하지 않습니다.</p>
            <a class="btn primary" href="/domain/company/list">목록으로 이동</a>
          </div>
        `;
        // 3초 후 기본 페이지로 이동
        setTimeout(() => {
          this.navigate('../plant/list.html');
        }, 3000);
      } else if (err.message.includes('403') || err.message.includes('Forbidden')) {
        this.slot.innerHTML = `
          <div class="notice danger">
            <h3>접근 권한이 없습니다</h3>
            <p>이 페이지에 접근할 권한이 없습니다.</p>
            <a class="btn primary" href="/domain/company/list">목록으로 이동</a>
          </div>
        `;
      } else {
        this.slot.innerHTML = `
          <div class="notice danger">
            <h3>콘텐츠 불러오기 실패</h3>
            <p>네트워크 오류 또는 서버 문제가 발생했습니다.</p>
            <button class="btn primary" onclick="location.reload()">새로고침</button>
            <a class="btn" href="/domain/company/list">목록으로 이동</a>
          </div>
        `;
      }
    }
  },

  /**
   * URL을 해석하고 적절한 콘텐츠로 네비게이션하는 함수
   * @param {string} targetHref - 대상 URL
   */
  navigate: function navigate(targetHref) {
    const contentUrl = targetHref.startsWith('/layout/')
      ? (new URLSearchParams(new URL(targetHref, window.location.origin).search).get('content') || '../memo/list.html')
      : this.resolveUrl(targetHref, this.currentContentUrl);
    try { console.debug('[cmms-nav]', { targetHref, base: this.currentContentUrl, contentUrl }); } catch (_) {}
    this.setState(contentUrl, true);
    this.loadContent(contentUrl, { push: false });
  },

  /**
   * SPA 콘텐츠 로드 후 페이지별 스크립트 실행
   * @param {Document} doc - 파싱된 HTML 문서
   */
  executePageScripts: function(doc) {
    // 이지지리다렉 크립트 실행
    const scripts = doc.querySelectorAll('script');
    scripts.forEach(script => {
      try {
        // 라이브러리 크립트 실행
        if (script.textContent.trim()) {
          console.log('Executing page script:', script.textContent.substring(0, 100) + '...');
          
          // DOMContentLoaded 이벤트 리스너 실행
          const scriptContent = script.textContent;
          const modifiedScript = scriptContent.replace(
            /document\.addEventListener\('DOMContentLoaded',\s*\(\)\s*=>\s*\{/g,
            '(function() {'
          );
          
          eval(modifiedScript);
        }
      } catch (error) {
        console.warn('Page script execution failed:', error);
      }
    });
  },

  /**
   * HTML 부분을 가져와서 슬롯 요소에 주입하는 SPA 기능
   * [data-slot-root] 요소를 우선적으로 찾고, 없으면 <main> 또는 <body>를 사용
   * 
   * @param {string} url - 가져올 HTML의 URL
   * @param {Element} slotEl - 콘텐츠를 주입할 슬롯 요소
   * @param {Object} opts - 옵션 객체
   * @param {boolean} opts.pushState - 브라우저 히스토리 푸시 여부
   * @param {Function} opts.onAfterInject - 주입 후 콜백 함수
   * @returns {Promise<Object>} 문서 객체와 루트 요소를 포함한 객체
   */
  fetchAndInject: async function fetchAndInject(url, slotEl, opts) {
  const options = Object.assign({ pushState: false, onAfterInject: null }, opts);
  if (!slotEl) throw new Error('fetchAndInject: slot element is required');
  const res = await fetch(url, { credentials: 'same-origin' });
  if (!res.ok) throw new Error('Failed to fetch: ' + res.status);
  const html = await res.text();

  const parser = new DOMParser();
  const doc = parser.parseFromString(html, 'text/html');

  // Prefer explicit slot root, then a main, then body.
  const root =
    doc.querySelector('[data-slot-root]') ||
    doc.querySelector('main') ||
    doc.body;

  // 콘텐츠 주입
  slotEl.innerHTML = root.innerHTML;

 // 문서 제목 동기화 (제공된 경우)
  const title = doc.querySelector('title');
  if (title && title.textContent) {
    try { document.title = title.textContent; } catch (_) {}
  }

  // SPA 스타일 제출을 위한 [data-validate] 폼 연결
  const forms = slotEl.querySelectorAll('form[data-validate]');
  forms.forEach((form) => {
    if (form.__cmmsBound) return;
    form.__cmmsBound = true;
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const fd = new FormData(form);
      const action = form.getAttribute('action') || window.location.href;
      const method = (form.getAttribute('method') || 'post').toUpperCase();
      const redirectTo = form.getAttribute('data-redirect');
      fetch(action, {
        method,
        credentials: 'same-origin',
        body: fd,
      })
        .then((res) => {
          if (!res.ok) throw new Error('Submit failed: ' + res.status);
          if (redirectTo) {
            window.location.href = redirectTo;
          } else if (typeof options.onAfterInject === 'function') {
            options.onAfterInject({ form, res });
          }
        })
        .catch((err) => {
          console.error(err);
          const notice = document.createElement('div');
          notice.className = 'notice danger';
          notice.textContent = '요청이 실패했습니다. 잠시 후 다시 시도하세요';
          form.prepend(notice);
        });
    });
  });

  // CSRF 토큰은 fetch wrapper에서 자동 처리됨

  return { doc, root };
  },

  /**
   * 사용자 정보 로드 함수 (Thymeleaf 버전에서 주입된 경우)
   * @deprecated Thymeleaf 템플릿에서 사용자 정보를 직접 주입하는 경우 사용
   */
  loadUserInfo: function loadUserInfo() {
    // Thymeleaf 템플릿에서 사용자 정보를 직접 주입하는 경우
    // JavaScript에서 별도로 로드하는 경우 사용
    console.log('사용자 정보 로드 함수 (Thymeleaf 템플릿에서 직접 주입된 경우)');
  },

  /**
   * 삭제 핸들러 바인딩 함수
   */
  bindDeleteHandler: function bindDeleteHandler() {
    const slot = this.slot;
    if (!slot) return;
    
    slot.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-delete-url]');
      if (!btn) return;
      e.preventDefault();
      const url = btn.getAttribute('data-delete-url');
      const redirectTo = btn.getAttribute('data-redirect') || this.currentContentUrl;
      const confirmMsg = btn.getAttribute('data-confirm');
      if (confirmMsg && !confirm(confirmMsg)) return;
      fetch(url, { method: 'POST', credentials: 'same-origin' })
        .then((res) => {
          if (res.status === 403) throw window.cmms.csrf.toCsrfError(res);
          if (!res.ok) throw new Error('Delete failed: ' + res.status);
          this.navigate(redirectTo);
        })
        .catch((err) => {
          console.error(err);
          window.cmms.notification.error('요청이 실패했습니다. 잠시 후 다시 시도하세요');
        });
    }, { capture: true });
  },

  /**
   * 액션 버튼 이벤트 핸들러 바인딩 함수 (POST 요청용:data-delete-url과 동일 기능이나, 속성값 구분을 위해 유지함)
   */
  bindActionHandler: function bindActionHandler() {
    const slot = this.slot;
    if (!slot) return;
    
    slot.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action-url]');
      if (!btn) return;
      e.preventDefault();
      const url = btn.getAttribute('data-action-url');
      const redirectTo = btn.getAttribute('data-redirect') || this.currentContentUrl;
      const confirmMsg = btn.getAttribute('data-confirm');
      if (confirmMsg && !confirm(confirmMsg)) return;
      fetch(url, { method: 'POST', credentials: 'same-origin' })
        .then((res) => {
          if (res.status === 403) throw window.cmms.csrf.toCsrfError(res);
          if (!res.ok) throw new Error('Action failed: ' + res.status);
          this.navigate(redirectTo);
        })
        .catch((err) => {
          console.error(err);
          window.cmms.notification.error('요청이 실패했습니다. 잠시 후 다시 시도하세요');
        });
    }, { capture: true });
  },

  /**
   * 사이드바 토글 기능 초기화 함수
   */
  initSidebarToggle: function initSidebarToggle() {
    document.querySelectorAll('.sidebar .menu-title').forEach((btn) => {
      btn.addEventListener('click', () => {
        const grp = btn.closest('.menu-group');
        if (!grp) return;
        const willOpen = !grp.classList.contains('open');
        grp.classList.toggle('open', willOpen);
        btn.setAttribute('aria-expanded', willOpen ? 'true' : 'false');
      });
    });
  },

  /**
   * preloadRelatedPages 더미 함수 (안전한 구현)
   */
  preloadRelatedPages: function(moduleName) {
    console.log(`preloadRelatedPages 호출됨: ${moduleName} (더미 구현)`);
    // 실제 구현이 필요한 경우 여기에 로직 추가
  },

  /**
   * 네비게이션 시스템 초기화 함수
   */
  init: function init() {
    // 레이아웃 슬롯 요소 찾기
    this.slot = document.getElementById('layout-slot');
    if (!this.slot) {
      console.warn('layout-slot element not found');
      return;
    }

    // 클릭 이벤트 리스너 등록
    document.addEventListener('click', (e) => {
      const anchor = e.target.closest('a[href]');
      if (anchor && anchor.getAttribute('href')) {
        const href = anchor.getAttribute('href');
        // Bypass SPA for external, auth, or explicit hard-nav links
        if (
          href.startsWith('http') ||
          href.startsWith('mailto:') ||
          href.startsWith('#') ||
          href.startsWith('/api/auth/logout') ||
          href.startsWith('/auth/') ||
          href.startsWith('/api/files') ||
          anchor.hasAttribute('data-hard-nav') ||
          anchor.target === '_blank'
        ) {
          return; // let browser handle
        }
        e.preventDefault();
        this.navigate(href);
        return;
      }
      const row = e.target.closest('[data-row-link]');
      if (row) {
        const link = row.getAttribute('data-row-link');
        if (link) {
          e.preventDefault();
          this.navigate(link);
        }
      }
    }, { capture: true });

    // 뒤로가기 버튼 처리
    window.addEventListener('popstate', (e) => {
      const content = e.state?.content || new URLSearchParams(window.location.search).get('content') || '../plant/list.html';
      try {
        this.loadContent(content, { push: false });
      } catch (err) {
        console.error('Navigation error:', err);
        // 에러 발생 시 기본 페이지로 이동
        this.navigate('../plant/list.html');
      }
    });

    // 초기 로드 - Thymeleaf 서 전달된 콘텐츠 URL 사용
    const initialContent = window.initialContent || new URLSearchParams(window.location.search).get('content') || '../plant/list.html';
    this.setState(initialContent, false);
    this.loadContent(initialContent, { push: false });

    // 삭제 핸들러 바인딩
    this.bindDeleteHandler();
    
    // 액션 핸들러 바인딩
    this.bindActionHandler();

    // 사용자 정보 로드 (Thymeleaf 템플릿에서 직접 주입된 경우)
    // this.loadUserInfo();

    // 사이드바 토글 기능 초기화
    this.initSidebarToggle();
  }
};

// =============================================================================
// 사용자 정보 관리 모듈
// =============================================================================
window.cmms.user = {
  /**
   * 재 사용자 정보서 사코드전게 추출하는 함수
   * @returns {string} 사코드
   */
  getCurrentCompanyId: function() {
    // 실제로는 백엔드에서 인증된 사용자 정보를 통해 추출
    // 프론트엔드에서는 회사코드를 직접 조작할 수 없음
    
    // 사용자명: "C0001:admin" 형식인 경우
    const username = "C0001:admin"; // 실제로는 인증 정보에서 가져옴
    
    if (username && username.includes(":")) {
      const [companyId, memberId] = username.split(":", 2);
      return companyId; // "C0001"
    }
    
    return "C0001"; // 기본회사코드
  },

  /**
   * 파일 업로드 시 회사코드는 백엔드에서 자동으로 결정됨
   * @param {FileList} files - 업로드할 파일들
   * @param {Object} options - 업로드 옵션
   * @returns {Promise} 업로드 응답
   */
  uploadFiles: async function(files, options = {}) {
    // 실제 파일 업로드는 window.cmms.fileUpload.uploadFiles()에서 처리됨
    // 이 메서드는 예시용이며 실제로는 사용되지 않음
    console.warn('uploadFiles method is deprecated. Use window.cmms.fileUpload.uploadFiles() instead.');
    return Promise.resolve();
  },

  /**
   * 보안 강화 정보 출력
   */
  showSecurityInfo: function() {
    console.log(`
보안 강화 전:
- 프론트엔드에서 회사코드 전달: companyId: "C0002" (조작 가능)
- S3 경로: C0002/plant/F250119001/파일명.jpg (다른 회사 데이터 접근)

보안 강화 후:
- 백엔드에서 사용자 정보로 회사코드 추출: "C0001:admin" -> "C0001"
- S3 경로: C0001/plant/F250119001/파일명.jpg (본인 회사 데이터만 접근)
    `);
  }
};

