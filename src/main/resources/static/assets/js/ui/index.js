/**
 * UI 모듈 통합 및 초기화
 * 
 * 모든 UI 컴포넌트 모듈을 통합하여 초기화하는 모듈입니다.
 * - notification: 알림 시스템
 * - file-upload: 파일 업로드 위젯
 * - file-list: 파일 목록 관리
 * - table-manager: 테이블 관리
 * - data-loader: 데이터 로딩
 * - confirm-dialog: 확인 대화상자
 * - validator: 유효성 검사
 * - print-utils: 인쇄 유틸리티
 */

import { initNotification } from './notification.js';
import { initFileUpload } from './file-upload.js';
import { initFileList } from './file-list.js';
import { initTableManagerModule } from './table-manager.js';
import { initDataLoader } from './data-loader.js';
import { initConfirmDialog } from './confirm-dialog.js';
import { initValidator } from './validator.js';
import { initPrintUtils } from './print-utils.js';

/**
 * UI 모듈 초기화 함수
 * UI 컴포넌트 모듈들을 초기화합니다.
 */
export function initUI() {
  try {
    // 1. 알림 시스템 초기화
    initNotification();
    
    // 2. 파일 업로드 위젯 초기화
    initFileUpload();
    
    // 3. 파일 목록 관리 초기화
    initFileList();
    
    // 4. 테이블 관리 초기화
    initTableManagerModule();
    
    // 5. 데이터 로딩 초기화
    initDataLoader();
    
    // 6. 확인 대화상자 초기화
    initConfirmDialog();
    
    // 7. 유효성 검사 초기화
    initValidator();
    
    // 8. 인쇄 유틸리티 초기화
    initPrintUtils();
    
    // 9. 공통 이벤트 핸들러 초기화
    initCommonEventHandlers();
    
    console.log('UI modules initialized successfully');
    
  } catch (error) {
    console.error('UI modules initialization failed:', error);
    throw error;
  }
}

/**
 * 공통 이벤트 핸들러 초기화
 */
function initCommonEventHandlers() {
  // DOM이 로드된 후 실행
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupCommonEventHandlers);
  } else {
    setupCommonEventHandlers();
  }
}

/**
 * 공통 이벤트 핸들러 설정
 */
function setupCommonEventHandlers() {
  // data-cancel-btn 처리
  document.querySelectorAll('[data-cancel-btn]').forEach(btn => {
    if (btn.__cancelBound) return;
    btn.__cancelBound = true;
    btn.addEventListener('click', () => {
      if (window.cmms?.navigation?.navigate) {
        // 현재 경로에서 list로 이동
        const currentPath = window.location.pathname;
        const listPath = currentPath.replace(/\/form$/, '/list').replace(/\/edit\/[^/]+$/, '/list');
        window.cmms.navigation.navigate(listPath);
      } else {
        window.history.back();
      }
    });
  });

  // data-table-manager 자동 초기화
  document.querySelectorAll('[data-table-manager]').forEach(table => {
    if (table.__tableManagerInitialized) return;
    table.__tableManagerInitialized = true;
    
    const config = {};
    
    // data 속성에서 설정 읽기
    if (table.dataset.rowSelector) config.rowSelector = table.dataset.rowSelector;
    if (table.dataset.numberField) config.numberField = table.dataset.numberField;
    if (table.dataset.minRows) config.minRows = parseInt(table.dataset.minRows);
    
    // 테이블 매니저 초기화
    if (window.cmms?.common?.TableManager) {
      window.cmms.common.TableManager.init(table, config);
    }
  });

  // data-print-btn 처리
  document.querySelectorAll('[data-print-btn]').forEach(btn => {
    if (btn.__printBound) return;
    btn.__printBound = true;
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      if (window.cmms?.printUtils) {
        window.cmms.printUtils.printPage();
      } else {
        window.print();
      }
    });
  });
}

// 개별 모듈 초기화 함수들도 export
export { 
  initNotification, 
  initFileUpload, 
  initFileList, 
  initTableManagerModule, 
  initDataLoader, 
  initConfirmDialog, 
  initValidator, 
  initPrintUtils 
};
