/**
 * 동적 모듈 로더 시스템
 * 
 * - 페이지별 모듈 매핑 및 로딩
 * - 스크립트 동적 주입
 * - 로딩 상태 관리
 */

/**
 * 모듈 로더 초기화 함수
 */
export function initModuleLoader() {
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
     * @param {string} path - URL 경로
     * @returns {string|null} 모듈 식별자 또는 null
     */
    extractModuleId: function(path) {
      if (!path) return null;
      
      // URL 경로에서 모듈명 추출
      const pathParts = path.split('/');
      const moduleName = pathParts[1];
      
      // 매핑 테이블에 있는지 확인
      return this.moduleMap[moduleName] ? moduleName : null;
    },
    
    /**
     * 스크립트 동적 주입 함수 (Dynamic import 우선, 폴백으로 script 태그)
     * @param {string} src - 스크립트 소스 URL
     * @returns {Promise} 로딩 Promise
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
        
        // Dynamic import 시도 (ES 모듈인 경우)
        if (src.includes('/pages/')) {
          try {
            // 페이지 모듈은 기존 방식으로 로딩 (window.cmms 구조 사용)
            const script = document.createElement('script');
            script.src = src;
            script.async = true;
            
            script.onload = () => {
              this.loadingStates[src] = 'loaded';
              console.log(`페이지 모듈 로드 완료: ${src}`);
              resolve();
            };
            
            script.onerror = () => {
              this.loadingStates[src] = 'error';
              console.error(`페이지 모듈 로드 실패: ${src}`);
              reject(new Error(`Failed to load page module: ${src}`));
            };
            
            document.head.appendChild(script);
            return;
          } catch (error) {
            console.warn('Dynamic import 실패, script 태그로 폴백:', error);
          }
        }
        
        // 폴백: 기존 script 태그 방식
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
     * @param {string} contentUrl - 콘텐츠 URL
     * @returns {Promise} 로딩 Promise
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
        // 간단하고 명확한 에러 로그
        console.error('🔴 [ModuleLoader] 모듈 로딩 실패');
        console.error('  모듈:', moduleId);
        console.error('  경로:', scriptSrc);
        console.error('  에러:', error.message);
        console.warn('  ⚠️  이 페이지의 일부 기능이 작동하지 않을 수 있습니다');
        console.info('  💡 해결: 페이지 새로고침(F5) 또는 캐시 삭제(Ctrl+F5)');
        
        // 로딩 실패해도 페이지 동작은 계속
      }
    }
  };
}
