/**
 * 인증 API 모듈
 * 
 * - 로그인/로그아웃 처리
 * - 인증 상태 관리
 * - 세션 처리
 * 
 * @functions
 * - initAuth() - 인증 모듈 초기화
 * 
 * @methods (window.cmms.auth)
 * - logout(options) - 표준화된 로그아웃 처리
 * - logoutWithForm() - 폼 기반 로그아웃
 * - showLogoutLoading() - 로그아웃 로딩 표시
 * - showLogoutSuccess() - 로그아웃 성공 표시
 * - showLogoutError(error) - 로그아웃 에러 표시
 * - redirectToLogin() - 로그인 페이지로 리다이렉트
 * - isAuthenticated() - 현재 인증 상태 확인
 * - getUserInfo() - 사용자 정보 가져오기
 * - hasPermission(permission) - 권한 확인
 */

/**
 * 인증 모듈 초기화 함수
 */
export function initAuth() {
  window.cmms = window.cmms || {};
  window.cmms.auth = {
    
    /**
     * 표준화된 로그아웃 처리
     * @param {Object} options - 옵션 설정
     * @param {boolean} options.showLoading - 로딩 표시 여부
     * @param {boolean} options.redirectAfterLogout - 로그아웃 후 리다이렉트 여부
     * @param {Function} options.onSuccess - 성공 콜백
     * @param {Function} options.onError - 에러 콜백
     */
    logout: async function(options = {}) {
      const defaultOptions = {
        showLoading: true,
        redirectAfterLogout: true,
        onSuccess: null,
        onError: null
      };
      
      const opts = Object.assign(defaultOptions, options);
      
      try {
        // 로딩 표시
        if (opts.showLoading) {
          this.showLogoutLoading();
        }
        
        // CSRF 토큰 자동 처리됨 (app.js fetch 래핑)
        const response = await fetch('/api/auth/logout', {
          method: 'POST',
          credentials: 'same-origin',
          headers: {
            'Content-Type': 'application/json'
          }
        });
        
        if (!response.ok) {
          throw new Error(`Logout failed: ${response.status} ${response.statusText}`);
        }
        
        const result = await response.json();
        
        // 성공 콜백 실행
        if (typeof opts.onSuccess === 'function') {
          opts.onSuccess(result);
        }
        
        // 성공 알림
        this.showLogoutSuccess();
        
        // 리다이렉트
        if (opts.redirectAfterLogout) {
          this.redirectToLogin();
        }
        
      } catch (error) {
        console.error('Logout error:', error);
        
        // 에러 콜백 실행
        if (typeof opts.onError === 'function') {
          opts.onError(error);
        }
        
        // 에러 알림
        this.showLogoutError(error);
        
        // 에러 발생 시에도 로그인 페이지로 이동 (보안상 안전)
        if (opts.redirectAfterLogout) {
          setTimeout(() => {
            this.redirectToLogin();
          }, 2000);
        }
      }
    },
    
    /**
     * 폼 기반 로그아웃 (fallback 방식)
     * JavaScript가 비활성화된 환경에서 사용
     */
    logoutWithForm: function() {
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = '/api/auth/logout';
      form.style.display = 'none';
      
      // CSRF 토큰 추가
      const csrfToken = window.cmms.csrf?.readToken();
      if (csrfToken) {
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);
      }
      
      document.body.appendChild(form);
      form.submit();
    },
    
    /**
     * 로그아웃 로딩 표시
     */
    showLogoutLoading: function() {
      if (window.cmms.notification) {
        window.cmms.notification.info('로그아웃 중...');
      }
    },
    
    /**
     * 로그아웃 성공 표시
     */
    showLogoutSuccess: function() {
      if (window.cmms.notification) {
        window.cmms.notification.success('로그아웃되었습니다.');
      }
    },
    
    /**
     * 로그아웃 에러 표시
     */
    showLogoutError: function(error) {
      if (window.cmms.notification) {
        window.cmms.notification.error('로그아웃 중 오류가 발생했습니다.');
      }
    },
    
    /**
     * 로그인 페이지로 리다이렉트
     */
    redirectToLogin: function() {
      setTimeout(() => {
        window.location.href = '/auth/login.html';
      }, 1000);
    },
    
    /**
     * 현재 인증 상태 확인 (확장성 고려)
     */
    isAuthenticated: function() {
      // 세션 쿠키 존재 여부로 간단히 확인
      return document.cookie.includes('JSESSIONID');
    },
    
    /**
     * 사용자 정보 가져오기 (확장성 고려)
     */
    getUserInfo: function() {
      // 실제 구현에서는 서버에서 사용자 정보를 가져올 수 있음
      return {
        authenticated: this.isAuthenticated(),
        // 추가 사용자 정보 필드들
      };
    },
    
    /**
     * 권한 확인 (확장성 고려)
     * @param {string} permission - 확인할 권한
     * @returns {boolean} 권한 보유 여부
     */
    hasPermission: function(permission) {
      // 실제 구현에서는 사용자 권한을 확인할 수 있음
      return this.isAuthenticated();
    }
  };
}
