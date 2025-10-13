/**
 * Memo 모듈 JavaScript
 * 
 * 게시/메모 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 공통 유틸(TableManager, DataLoader, fileUpload, notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  window.cmms = window.cmms || {};
  window.cmms.memo = window.cmms.memo || {};

  Object.assign(window.cmms.memo, {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('Memo list page initialized', root);
      
      // ⭐ DOM 기반 중복 초기화 방지
      if (root.dataset.memoListInit === 'true') {
        console.log('Memo list already initialized, skipping');
        return;
      }
      root.dataset.memoListInit = 'true';
      
      this.initPagination(root);
      this.initSearch(root);
      this.initResetForm(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('Memo detail page initialized', root);
      
      // ⭐ DOM 기반 중복 초기화 방지
      if (root.dataset.memoDetailInit === 'true') {
        console.log('Memo detail already initialized, skipping');
        return;
      }
      root.dataset.memoDetailInit = 'true';
      
      this.initPrintButton(root);
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Memo form page initialized', root);
      
      // ⭐ DOM 기반 중복 초기화 방지
      if (root.dataset.memoFormInit === 'true') {
        console.log('Memo form already initialized, skipping');
        return;
      }
      root.dataset.memoFormInit = 'true';
      
      // textarea만 사용하므로 특별한 초기화 불필요
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('Memo pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-btn을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
    },
    
    // 검색 초기화 (root 기반)
    initSearch: function(root) {
      console.log('Memo search initialized - 기본 폼 제출로 처리됨');
      // 기본 폼 제출로 처리되므로 별도 초기화 불필요
    },
    
    // 폼 초기화 버튼 (root 기반)
    initResetForm: function(root) {
      const resetBtn = root.querySelector('[data-reset-form]');
      if (resetBtn) {
        resetBtn.addEventListener('click', () => {
          const form = resetBtn.closest('form');
          if (form) {
            form.reset();
            if (window.cmms?.notification) {
              window.cmms.notification.success('검색 조건이 초기화되었습니다.');
            }
          }
        });
      }
    },
    
    // 인쇄 버튼 초기화 (통합 모듈 사용)
    initPrintButton: function(root) {
      window.cmms.printUtils.initPrintButton(root);
    }
  });
  
  // 페이지별 초기화 등록 (root 기반 구조)
  window.cmms.pages.register('memo-list', function(root) {
    window.cmms.memo.initList(root);
  });
  
  window.cmms.pages.register('memo-detail', function(root) {
    window.cmms.memo.initDetail(root);
  });
  
  window.cmms.pages.register('memo-form', function(root) {
    window.cmms.memo.initForm(root);
  });
  
})();