/**
 * WorkOrder 모듈 JavaScript
 * 
 * 작업지시 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 공통 유틸(TableManager, DataLoader, fileUpload, notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  window.cmms = window.cmms || {};
  window.cmms.workorder = window.cmms.workorder || {};

  Object.assign(window.cmms.workorder, {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('WorkOrder list page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('WorkOrder list already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initPagination(root);
      this.initSearch(root);
      this.initResetForm(root);
      this.initNavButtons(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('WorkOrder detail page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('WorkOrder detail already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initPrintButton(root);
      this.initApprovalButtons(root);
    },
    
    // 결재 상신 버튼 초기화 (공통 submitApproval 함수 사용)
    initApprovalButtons: function(root) {
      // inspection.js에서 이미 window.submitApproval 전역 함수 등록됨
      // 모든 모듈에서 공통 사용
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('WorkOrder form page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('WorkOrder form already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initTableManager(root);
      // this.initFormSubmit(root);  // Form Manager 제거로 인한 주석 처리
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('WorkOrder pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-btn을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
    },
    
    // 검색 초기화 (root 기반)
    initSearch: function(root) {
      console.log('WorkOrder search initialized - 기본 폼 제출로 처리됨');
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
    
    // 네비게이션 버튼 초기화 (root 기반)
    initNavButtons: function(root) {
      root.querySelectorAll('[data-nav-btn]').forEach(btn => {
        btn.addEventListener('click', () => {
          const url = btn.dataset.url;
          if (url) {
            window.cmms.navigation.navigate(url);
          }
        });
      });
    },
    
    // 인쇄 버튼 초기화 (통합 모듈 사용)
    initPrintButton: function(root) {
      window.cmms.printUtils.initPrintButton(root);
    },
    
    // 테이블 매니저 초기화 (공통 TableManager 활용, root 기반)
    initTableManager: function(root) {
      const table = root.querySelector('[data-table-manager]');
      if (!table) return;
      
      // 공통 TableManager 초기화 (root 범위 내에서만)
      if (window.cmms?.TableManager) {
        window.cmms.TableManager.init(table);
      }
    },
    
    // 폼 제출 초기화 (공통 FormManager 활용, root 기반)
    // initFormSubmit: function(root) {
    //   const form = root.querySelector('[data-form-manager]');
    //   if (!form) return;
    //   
    //   // 공통 FormManager를 활용한 폼 제출 처리
    //   if (window.cmms?.formManager) {
    //     window.cmms.formManager.init(form, {
    //       onSuccess: (result) => {
    //         this.handleFormSuccess(result, root);
    //       },
    //       onError: (error) => {
    //         this.handleFormError(error, root);
    //       }
    //     });
    //   } else {
    //     // FormManager가 없는 경우 직접 처리
    //     form.addEventListener('submit', async (event) => {
    //       event.preventDefault();
    //       await this.handleDirectForm(form, root);
    //     });
    //   }
    // },
    
    // 폼 성공 처리 (root 기반)
    // handleFormSuccess: function(result, root) {
    //   if (window.cmms?.notification) {
    //     window.cmms.notification.success('작업지시가 성공적으로 저장되었습니다.');
    //   } else {
    //     alert('작업지시가 성공적으로 저장되었습니다.');
    //   }
    //   
    //   // SPA 네비게이션으로 레이아웃 유지
    //   setTimeout(() => {
    //     window.cmms.navigation.navigate('/workorder/list');
    //   }, 1000);
    // },
    
    // 폼 에러 처리 (root 기반)
    // handleFormError: function(error, root) {
    //   console.error('Form submit error:', error);
    //   if (window.cmms?.notification) {
    //     window.cmms.notification.error('저장 중 오류가 발생했습니다.');
    //   } else {
    //     alert('저장 중 오류가 발생했습니다.');
    //   }
    // },
    
    // 직접 폼 처리 (FormManager 없을 때)
    // handleDirectForm: async function(form, root) {
    //   try {
    //     // 테이블 데이터 수집
    //     const table = root.querySelector('[data-table-manager]');
    //     if (table && window.cmms?.TableManager) {
    //       window.cmms.TableManager.collectData(table);
    //     }
    //     
    //     const formData = new FormData(form);
    //     
    //     const response = await fetch(form.action, {
    //       method: 'POST',
    //       body: formData,
    //     });
    //     
    //     if (!response.ok) {
    //       throw new Error('저장 실패');
    //     }
    //     
    //     const result = await response.json();
    //     this.handleFormSuccess(result, root);
    //     
    //   } catch (error) {
    //     this.handleFormError(error, root);
    //   }
    // }
  });
  
  // 페이지별 초기화 등록 (root 기반 구조)
  window.cmms.pages.register('workorder-list', function(root) {
    window.cmms.workorder.initList(root);
  });
  
  window.cmms.pages.register('workorder-detail', function(root) {
    window.cmms.workorder.initDetail(root);
  });
  
  window.cmms.pages.register('workorder-form', function(root) {
    window.cmms.workorder.initForm(root);
  });
  
})();