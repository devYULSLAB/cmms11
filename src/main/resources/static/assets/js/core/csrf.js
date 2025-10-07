/**
 * CSRF 토큰 관리 모듈
 * 
 * app.js에서 CSRF 관련 기능을 추출한 모듈입니다.
 * - CSRF 토큰 읽기/동기화
 * - Fetch API 래핑으로 자동 토큰 첨부
 * - 폼 필드 자동 동기화
 */

const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

/**
 * 쿠키에서 CSRF 토큰을 읽어오는 함수
 * @returns {string|null} CSRF 토큰 또는 null
 */
export function readCsrfTokenFromCookie() {
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
export function syncCsrfHiddenFields() {
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
export function shouldAttachCsrfHeader(request) {
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

/**
 * CSRF 에러 객체 생성 함수
 * @param {Response} response - HTTP 응답 객체
 * @returns {Error} CSRF 에러 객체
 */
export function createCsrfForbiddenError(response) {
  const error = new Error('Forbidden');
  error.name = 'CsrfForbiddenError';
  error.response = response;
  return error;
}

/**
 * CSRF 에러 처리 함수
 * @param {Response} response - HTTP 응답 객체
 * @returns {boolean} 에러 처리 여부
 */
export function handleCsrfError(response) {
  if (response.status === 403) {
    // 무한 새로고침 방지
    const lastReloadKey = 'lastCsrfReload';
    const lastReloadTime = sessionStorage.getItem(lastReloadKey);
    const now = Date.now();
    
    // 5초 이내 재시도 시 로그인 페이지로 이동
    if (lastReloadTime && (now - parseInt(lastReloadTime)) < 5000) {
      console.error('CSRF 무한 새로고침 감지, 로그인 페이지로 이동');
      
      if (window.cmms?.notification) {
        window.cmms.notification.error('인증 오류가 발생했습니다. 다시 로그인해주세요.');
      }
      
      setTimeout(() => {
        sessionStorage.removeItem(lastReloadKey);
        window.location.href = '/auth/login.html';
      }, 2000);
      
      return true;
    }
    
    // 정상적인 CSRF 에러 처리
    sessionStorage.setItem(lastReloadKey, now.toString());
    
    if (window.cmms?.notification) {
      window.cmms.notification.error('세션이 만료되었습니다. 페이지를 새로고침합니다.');
    }
    
    setTimeout(() => {
      window.location.reload();
    }, 2000);
    
    return true;
  }
  return false;
}

/**
 * Fetch API를 CSRF 토큰으로 래핑하는 함수
 */
export function wrapFetchWithCsrf() {
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
        syncCsrfHiddenFields();
      } catch (e) {
        // no-op; syncing CSRF fields should not break application flow
      }
    });
  };
}

/**
 * CSRF 모듈 초기화 함수
 */
export function initCsrf() {
  // Fetch API 래핑
  wrapFetchWithCsrf();
  
  // 기존 window.cmms.csrf 구조 유지
  window.cmms = window.cmms || {};
  window.cmms.csrf = {
    refreshForms: syncCsrfHiddenFields,
    readToken: readCsrfTokenFromCookie,
    shouldAttachHeader: shouldAttachCsrfHeader,
    toCsrfError: createCsrfForbiddenError,
    handleError: handleCsrfError
  };
  
  // 초기화 시 자동으로 폼 동기화
  syncCsrfHiddenFields();
}
