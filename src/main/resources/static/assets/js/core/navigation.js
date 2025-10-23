/**
 * SPA 네비게이션 모듈
 * 
 * - SPA 콘텐츠 로딩
 * - 브라우저 히스토리 관리
 * - URL 해석 및 라우팅
 * - 동적 모듈 로딩 연동
 * 
 * @functions
 * - initNavigation() - 네비게이션 모듈 초기화
 * 
 * @methods (window.cmms.navigation)
 * - resolveUrl(href, basePath) - Thymeleaf 링크를 URL로 변환
 * - setState(contentUrl, push) - 브라우저 스택 리스트에 URL 데이터 추가
 * - setActive(contentUrl) - 현재 활성화된 메뉴 설정
 * - loadContent(contentUrl, opts) - AJAX 콘텐츠 가져오기와 출력
 * - navigate(targetHref) - URL 해석 및 네비게이션
 * - executePageScripts(doc) - 페이지별 스크립트 실행
 * - fetchAndInject(url, slotEl, opts) - HTML 부분을 가져와서 슬롯 요소에 주입
 * - bindDeleteHandler() - 삭제 핸들러 바인딩
 * - bindActionHandler() - 액션 버튼 이벤트 핸들러 바인딩
 * - initSidebarToggle() - 사이드바 토글 기능 초기화
 * - init() - 네비게이션 시스템 초기화
 */

/**
 * 네비게이션 모듈 초기화 함수
 */
export function initNavigation() {
  window.cmms = window.cmms || {};
  window.cmms.navigation = {
    slot: null,
    currentContentUrl: '../memo/list.html',

    /**
     * Thymeleaf 링크를 URL로 변환하는 함수
     * @param {string} href - 변환할 URL
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
        this.navigate('../memo/list.html');
        return;
      }
      
      // 못 찾을 URL 검증
      if (contentUrl.includes('..') && contentUrl.split('..').length > 2) {
        console.warn('Invalid URL pattern detected:', contentUrl);
        this.slot.innerHTML = `
          <div class="notice danger">
            <h3>못 찾을 URL이 있습니다.</h3>
            <p>보안의 유효한 URL이 아닙니다.</p>
            <a class="btn primary" href="/memo/list">목록으로 이동</a>
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

        // ⭐ data-form-manager 폼 처리 (업무 모듈용 - API 호출)
        this.handleSPAForms();
        
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
      const scripts = doc.querySelectorAll('script');
      scripts.forEach(script => {
        try {
          if (script.textContent.trim()) {
            console.log('✅ 페이지 스크립트 실행:', script.textContent.substring(0, 50) + '...');
            
            // DOMContentLoaded 이벤트 리스너를 즉시 실행 함수로 변환
            const scriptContent = script.textContent;
            const modifiedScript = scriptContent.replace(
              /document\.addEventListener\('DOMContentLoaded',\s*\(\)\s*=>\s*\{/g,
              '(function() {'
            );
            
            // eval() 대신 Function 생성자 사용 (더 안전)
            const func = new Function(modifiedScript);
            func();
          }
        } catch (error) {
          console.warn('⚠️ 페이지 스크립트 실행 실패:', error);
        }
      });
    },

    /**
     * HTML 부분을 가져와서 슬롯 요소에 주입하는 SPA 기능
     * @param {string} url - 가져올 HTML의 URL
     * @param {Element} slotEl - 콘텐츠를 주입할 슬롯 요소
     * @param {Object} opts - 옵션 객체
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

      // ⚠️ data-validate 폼 처리 제거됨 (data-form-manager로 대체)
      // 도메인/코드/마스터 모듈은 순수 HTML form (서버 직접 처리)
      // 업무 모듈은 data-form-manager (handleSPAForms에서 처리)

      // CSRF 토큰은 fetch wrapper에서 자동 처리됨

      return { doc, root };
    },

    /**
     * [DEPRECATED - 사용 안 함] 사용자 정보 로드 함수
     * @deprecated Thymeleaf 템플릿에서 사용자 정보를 직접 주입하는 경우 사용
     * 추후 완전히 제거 예정
     */
    // loadUserInfo: function loadUserInfo() {
    //   // Thymeleaf 템플릿에서 사용자 정보를 직접 주입하는 경우
    //   // JavaScript에서 별도로 로드하는 경우 사용
    //   console.log('사용자 정보 로드 함수 (Thymeleaf 템플릿에서 직접 주입된 경우)');
    // },

    /**
     * data-form-manager 폼 자동 바인딩 (업무 모듈용)
     */
    handleSPAForms: function handleSPAForms() {
      const forms = this.slot.querySelectorAll('form[data-form-manager]');
      
      forms.forEach((form) => {
        if (form.__cmmsFormManagerHandled) return;
        form.__cmmsFormManagerHandled = true;
        
        form.addEventListener('submit', async (e) => {
          e.preventDefault();
          
          try {
            const action = form.getAttribute('data-action');
            const method = form.getAttribute('data-method') || 'POST';
            const redirectTemplate = form.getAttribute('data-redirect');
            
            if (!action) {
              console.error('data-action is required for data-form-manager');
              return;
            }
            
            // 파일 업로드 처리
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
            
            // FormData를 JSON으로 변환 (다중값 지원)
            const formData = new FormData(form);
            const jsonData = this.formDataToJSON(formData);
            
            // CSRF 토큰 추출 (core/csrf.js 사용)
            const csrfToken = window.cmms?.csrf?.readToken() || '';
            
            // API 호출
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
              throw new Error('API 요청 실패: ' + response.status + ' - ' + errorText);
            }
            
            const result = await response.json();
            
            // {id} 치환 로직
            if (redirectTemplate) {
              let redirectUrl = redirectTemplate;
              
              // {id} 패턴을 실제 ID로 치환
              const idMatches = redirectTemplate.match(/\{(\w+)\}/g);
              if (idMatches) {
                idMatches.forEach((placeholder) => {
                  const fieldName = placeholder.slice(1, -1); // {id} → id

                  let fieldValue =
                    result[fieldName] ??
                    result.plantId ??
                    result.inventoryId ??
                    result.inspectionId ??
                    result.orderId ??
                    result.permitId ??
                    result.memoId ??
                    result.approvalId;

                  if (!fieldValue) {
                    const fallbackKey = Object.keys(result || {}).find(
                      (key) => key.toLowerCase().endsWith('id') && result[key]
                    );
                    if (fallbackKey) {
                      fieldValue = result[fallbackKey];
                    }
                  }

                  if (fieldValue) {
                    redirectUrl = redirectUrl.replace(placeholder, fieldValue);
                  }
                });
              }
              
              this.navigate(redirectUrl);
            } else {
              // 성공 알림
              if (window.cmms?.notification) {
                window.cmms.notification.success('저장되었습니다.');
              }
            }
            
          } catch (err) {
            if (err && err.name === 'CsrfForbiddenError') {
              // CSRF 에러는 이미 csrf.js에서 처리 (새로고침/로그인 페이지 이동 등)
              return;
            }
            console.error('Form submission error:', err);
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
      const skipPrefixes = ['common_', 'fire_', 'confined_', 'electric_', 'high_', 'excavation_'];
      for (let [key, values] of multiValueKeys.entries()) {
        const shouldSkip =
          key === 'workTypes' ||
          skipPrefixes.some(prefix => key.startsWith(prefix));
        if (shouldSkip) {
          continue;
        }
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
        if (!confirm('정말 삭제하시겠습니까?')) return;
        
        // ⭐ REST API 표준에 맞춰 DELETE 메서드 사용 + CSRF 토큰 추가
        const csrfToken = window.cmms?.csrf?.readToken() || '';
        fetch(url, { 
          method: 'DELETE', 
          credentials: 'same-origin',
          headers: {
            'X-CSRF-TOKEN': csrfToken
          }
        })
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
        
        // ⭐ CSRF 토큰 추가
        const csrfToken = window.cmms?.csrf?.readToken() || '';
        fetch(url, { 
          method: 'POST', 
          credentials: 'same-origin',
          headers: {
            'X-CSRF-TOKEN': csrfToken
          }
        })
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
     * [DEPRECATED - 사용 안 함] preloadRelatedPages 더미 함수
     * 추후 완전히 제거 예정 또는 실제 구현 필요 시 활성화
     */
    // preloadRelatedPages: function(moduleName) {
    //   console.log(`preloadRelatedPages 호출됨: ${moduleName} (더미 구현)`);
    //   // 실제 구현이 필요한 경우 여기에 로직 추가
    // },

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

      // data-confirm 요소들 처리
      document.addEventListener('click', (e) => {
        const confirmable = e.target.closest('[data-confirm]');
        if (confirmable) {
          const msg = confirmable.getAttribute('data-confirm') || '진행하시겠습니까?';
          if (!confirm(msg)) {
            e.preventDefault();
            e.stopPropagation();
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
}
