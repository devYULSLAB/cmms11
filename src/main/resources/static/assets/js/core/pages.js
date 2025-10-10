/**
 * 페이지 초기화 훅 시스템
 * 
 * - 페이지별 초기화 함수 등록
 * - 페이지 생명주기 관리
 * - 컨테이너 기반 초기화 실행
 * 
 * @functions
 * - initPages() - 페이지 초기화 모듈 초기화
 * 
 * @methods (window.cmms.pages)
 * - register(name, initFn) - 페이지 초기화 함수 등록
 * - run(container, ctx) - 페이지 초기화 실행
 * - reset(container) - 페이지 초기화 상태 초기화
 * - list() - 등록된 페이지 목록 조회
 * - unregister(name) - 페이지 초기화 함수 제거
 */

/**
 * 페이지 초기화 모듈 초기화 함수
 */
export function initPages() {
  window.cmms = window.cmms || {};
  window.cmms.pages = {
    registry: {},
    
    /**
     * 페이지 초기화 함수 등록
     * @param {string} name - 페이지 이름
     * @param {Function} initFn - 초기화 함수
     */
    register: function(name, initFn) {
      if (typeof initFn !== 'function') return;
      this.registry[name] = initFn;
    },
    
    /**
     * 페이지 초기화 실행
     * @param {Element} container - 컨테이너 요소
     * @param {Object} ctx - 컨텍스트 객체
     */
    run: function(container, ctx) {
      try {
        if (!container) return;
        
        const pageRoot = container.querySelector('[data-page]') || container;
        const pageId = pageRoot.getAttribute && pageRoot.getAttribute('data-page');
        
        if (pageId && typeof this.registry[pageId] === 'function') {
          // 중복 초기화 방지
          if (pageRoot.__cmmsPageInited === pageId) return;
          pageRoot.__cmmsPageInited = pageId;
          
          // 초기화 함수 실행
          this.registry[pageId](pageRoot, ctx || {});
        }
      } catch (e) {
        console.warn('cmms.pages.run failed:', e);
      }
    },
    
    /**
     * 페이지 초기화 상태 초기화
     * @param {Element} container - 컨테이너 요소
     */
    reset: function(container) {
      if (!container) return;
      
      const pageRoot = container.querySelector('[data-page]') || container;
      if (pageRoot) {
        delete pageRoot.__cmmsPageInited;
      }
    },
    
    /**
     * 등록된 페이지 목록 조회
     * @returns {string[]} 등록된 페이지 이름 배열
     */
    list: function() {
      return Object.keys(this.registry);
    },
    
    /**
     * 페이지 초기화 함수 제거
     * @param {string} name - 페이지 이름
     */
    unregister: function(name) {
      delete this.registry[name];
    }
  };
}
