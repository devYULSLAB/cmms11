/**
 * 코어 모듈 통합 및 초기화
 * 
 * 모든 핵심 시스템 모듈을 통합하여 초기화하는 모듈입니다.
 * - csrf: CSRF 토큰 관리
 * - utils: 공통 유틸리티 함수들
 * - module-loader: 동적 모듈 로딩
 * - pages: 페이지 초기화 훅 시스템
 * - navigation: SPA 네비게이션
 */

import { initCsrf } from './csrf.js';
import { initUtils } from './utils.js';
import { initModuleLoader } from './module-loader.js';
import { initPages } from './pages.js';
import { initNavigation } from './navigation.js';

/**
 * 코어 모듈 초기화 함수
 * 핵심 모듈들을 올바른 순서로 초기화합니다.
 */
export function initCore() {
  try {
    // 1. CSRF 토큰 관리 (가장 먼저 초기화)
    initCsrf();
    
    // 2. 유틸리티 함수들
    initUtils();
    
    // 3. 모듈 로더 시스템
    initModuleLoader();
    
    // 4. 페이지 초기화 훅 시스템
    initPages();
    
    // 5. 네비게이션 시스템 (마지막에 초기화)
    initNavigation();
    
    console.log('Core modules initialized successfully');
    
  } catch (error) {
    console.error('Core modules initialization failed:', error);
    throw error;
  }
}

// 개별 모듈 초기화 함수들도 export
export { initCsrf, initUtils, initModuleLoader, initPages, initNavigation };
