/**
 * API 모듈 통합 및 초기화
 * 
 * 모든 데이터 계층 모듈을 통합하여 초기화하는 모듈입니다.
 * - auth: 인증 및 사용자 관리
 * - storage: 로컬 저장소 관리
 */

import { initAuth } from './auth.js';
import { initStorage } from './storage.js';

/**
 * API 모듈 초기화 함수
 * 데이터 계층 모듈들을 초기화합니다.
 */
export function initApi() {
  try {
    // 1. 인증 모듈 초기화
    initAuth();
    
    // 2. 저장소 모듈 초기화
    initStorage();
    
    console.log('API modules initialized successfully');
    
  } catch (error) {
    console.error('API modules initialization failed:', error);
    throw error;
  }
}

// 개별 모듈 초기화 함수들도 export
export { initAuth, initStorage };
