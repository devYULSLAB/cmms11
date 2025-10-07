/**
 * CMMS ES 모듈 엔트리 포인트
 * 
 * ES 모듈 시스템의 진입점입니다.
 * <script type="module">은 defer 속성을 가지므로 HTML 파싱 완료 후 실행됩니다.
 * 따라서 DOMContentLoaded 이벤트 없이 즉시 초기화할 수 있습니다.
 * 
 * 모듈 구조:
 * - core/: 핵심 시스템 (csrf, navigation, module-loader, pages, utils)
 * - api/: 데이터 계층 (auth, storage)
 * - ui/: UI 컴포넌트 (notification, file-upload, file-list, table-manager, data-loader, confirm-dialog, validator, print-utils)
 * 
 * 초기화 순서:
 * 1. 전역 객체 초기화 (window.cmms)
 * 2. core/ 모듈 (핵심 인프라)
 * 3. api/ 모듈 (데이터 계층)
 * 4. ui/ 모듈 (UI 컴포넌트)
 * 5. navigation 초기화 및 초기 콘텐츠 로드
 * 
 * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules
 */

// ES 모듈 import
import { initCore } from './core/index.js';
import { initApi } from './api/index.js';
import { initUI } from './ui/index.js';

// 전역 객체 초기화 (최우선)
window.cmms = window.cmms || {};
window.cmms.moduleSystem = {
  version: '1.0.0',
  type: 'ES Modules',
  initialized: false,
  startTime: Date.now()
};

/**
 * CMMS 시스템 초기화 함수
 * ES 모듈은 DOM 준비 완료 후 실행되므로 즉시 호출 가능
 */
function initialize() {
  const startTime = performance.now();
  
  try {
    console.log('🚀 CMMS 시스템 초기화 시작');
    
    // 1. 핵심 시스템 초기화 (순서 중요)
    initCore();
    console.log('  ✅ Core 모듈 초기화 완료');
    
    // 2. API 계층 초기화
    initApi();
    console.log('  ✅ API 모듈 초기화 완료');
    
    // 3. UI 컴포넌트 초기화
    initUI();
    console.log('  ✅ UI 모듈 초기화 완료');
    
    // 4. 네비게이션 시스템 초기화
    if (window.cmms?.navigation) {
      window.cmms.navigation.init();
      console.log('  ✅ Navigation 초기화 완료');
    } else {
      console.warn('  ⚠️ Navigation 모듈을 찾을 수 없습니다');
    }
    
    // 5. 초기 콘텐츠 로드
    if (window.initialContent) {
      console.log('  📄 초기 콘텐츠 로드:', window.initialContent);
      window.cmms.navigation.loadContent(window.initialContent);
    } else {
      console.warn('  ⚠️ initialContent가 설정되지 않았습니다');
    }
    
    // 6. 초기화 완료 플래그 및 성능 측정
    window.cmms.moduleSystem.initialized = true;
    window.cmms.moduleSystem.loadTime = Math.round(performance.now() - startTime);
    
    console.log(`🎉 CMMS 시스템 초기화 완료 (${window.cmms.moduleSystem.loadTime}ms)`);
    
    // 7. 오프라인 감지
    window.addEventListener('online', () => {
      console.log('🌐 네트워크 연결 복구');
      if (window.cmms?.notification) {
        window.cmms.notification.success('네트워크 연결이 복구되었습니다.');
      }
    });
    
    window.addEventListener('offline', () => {
      console.log('📴 네트워크 연결 끊어짐');
      if (window.cmms?.notification) {
        window.cmms.notification.warning('네트워크 연결이 끊어졌습니다. 캐시된 데이터를 사용합니다.');
      }
    });
    
  } catch (error) {
    console.error('❌ CMMS 시스템 초기화 실패:', error);
    console.error('   스택 트레이스:', error.stack);
    
    // 초기화 실패 플래그
    window.cmms.moduleSystem.initialized = false;
    window.cmms.moduleSystem.error = error.message;
    
    // 사용자에게 알림 (notification이 실패했을 수 있으므로 fallback)
    const errorMessage = '시스템 초기화에 실패했습니다. 페이지를 새로고침해주세요.';
    
    if (window.cmms?.notification) {
      window.cmms.notification.error(errorMessage);
    } else {
      // Fallback: 네이티브 alert
      alert(errorMessage);
    }
    
    // 에러 전파 (선택적)
    // throw error;
  }
}

// ES 모듈은 defer로 동작하므로 DOM이 준비된 상태
// 따라서 DOMContentLoaded 없이 즉시 초기화 가능
initialize();
