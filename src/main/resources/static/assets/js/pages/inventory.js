/**
 * Inventory 모듈 JavaScript
 * 
 * 자재/재고 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 공통 유틸(TableManager, DataLoader, fileUpload, notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  window.cmms = window.cmms || {};
  window.cmms.inventory = window.cmms.inventory || {};

  Object.assign(window.cmms.inventory, {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('Inventory list page initialized', root);
      this.initPagination(root);
      this.initSearch(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('Inventory detail page initialized', root);
      this.initPrintButton(root);
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Inventory form page initialized', root);
      this.initCancelButton(root);
    },
    
    // 업로드 페이지 초기화 (root 기반)
    initUpload: function(root) {
      console.log('Inventory upload page initialized', root);
      this.initUploadForm(root);
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('Inventory pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-button을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
    },
    
    // 검색 초기화 (root 기반)
    initSearch: function(root) {
      console.log('Inventory search initialized - 기본 폼 제출로 처리됨');
      // 기본 폼 제출로 처리되므로 별도 초기화 불필요
    },
    
    // 인쇄 버튼 초기화 (통합 모듈 사용)
    initPrintButton: function(root) {
      window.cmms.printUtils.initPrintButton(root);
    },
    
    // 취소 버튼 초기화 (root 기반)
    initCancelButton: function(root) {
      const cancelBtn = root.querySelector('[data-cancel-btn]');
      if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
          window.cmms.navigation.navigate('/inventory/list');
        });
      }
    },
    
    // 업로드 폼 초기화 (root 기반)
    initUploadForm: function(root) {
      console.log('Inventory upload form initialized');
      // this.initUploadSubmit(root); // FormManager 제거됨
    },
    
    // 업로드 폼 제출 초기화 (FormManager 제거됨 - app.js SPA 폼 처리 활용)
    // initUploadSubmit: function(root) {
    //   const form = root.querySelector('#uploadForm');
    //   if (!form) return;
    //   
    //   // 공통 FormManager를 활용한 폼 제출 처리
    //   if (window.cmms?.formManager) {
    //     // FormManager가 처리하도록 위임
    //     window.cmms.formManager.init(form, {
    //       onSuccess: (result) => {
    //         this.handleUploadSuccess(result, root);
    //       },
    //       onError: (error) => {
    //         this.handleUploadError(error, root);
    //       }
    //     });
    //   } else {
    //     // FormManager가 없는 경우 직접 처리
    //     form.addEventListener('submit', async (event) => {
    //       event.preventDefault();
    //       await this.handleDirectUpload(form, root);
    //     });
    //   }
    // },
    
    // 업로드 성공 처리 (root 기반)
    handleUploadSuccess: function(result, root) {
      const summaryText = `성공 ${result.successCount}건 · 실패 ${result.failureCount}건`;
      this.showUploadSummary(summaryText, result.failureCount > 0, root);
      
      // 파일 입력 초기화
      const fileInput = root.querySelector('#csvFile');
      if (fileInput) fileInput.value = '';
      
      // 에러 표시
      if (Array.isArray(result.errors) && result.errors.length) {
        this.displayUploadErrors(result.errors, root);
      }
      
      // 공통 notification으로도 표시
      if (window.cmms?.notification) {
        if (result.failureCount > 0) {
          window.cmms.notification.warning(summaryText);
        } else {
          window.cmms.notification.success(summaryText);
        }
      }
    },
    
    // 업로드 에러 처리 (root 기반)
    handleUploadError: function(error, root) {
      console.error('Upload error:', error);
      this.showUploadSummary('파일 업로드 중 오류가 발생했습니다.', true, root);
      
      if (window.cmms?.notification) {
        window.cmms.notification.error('파일 업로드 중 오류가 발생했습니다.');
      }
    },
    
    // 직접 업로드 처리 (FormManager 없을 때)
    handleDirectUpload: async function(form, root) {
      const fileInput = root.querySelector('#csvFile');
      const file = fileInput?.files[0];
      
      if (!file) {
        this.showUploadSummary('먼저 업로드할 CSV 파일을 선택하세요.', true, root);
        return;
      }
      
      this.showUploadSummary('업로드 중입니다. 잠시만 기다려 주세요...', false, root);
      this.clearUploadErrors(root);
      
      try {
        // 공통 DataLoader 사용 (FormData 전송)
        const formData = new FormData();
        formData.append('file', file);
        
        const result = await window.cmms.common.DataLoader.load(form.action, {
          method: 'POST',
          body: formData,
          headers: {
            'X-Requested-With': 'XMLHttpRequest'
          }
        });
        
        this.handleUploadSuccess(result, root);
        
      } catch (error) {
        this.handleUploadError(error, root);
      }
    },
    
    // 업로드 에러 표시 (root 기반)
    displayUploadErrors: function(errors, root) {
      const errorSection = root.querySelector('#errorSection');
      const errorRows = errorSection?.querySelector('[data-error-rows]');
      
      if (!errorSection || !errorRows) return;
      
      errorSection.hidden = false;
      errorRows.innerHTML = '';
      
      errors.forEach((error) => {
        const row = document.createElement('tr');
        const rowCell = document.createElement('td');
        rowCell.textContent = error.rowNumber;
        rowCell.className = 'cell-center';
        const messageCell = document.createElement('td');
        messageCell.textContent = error.message;
        row.appendChild(rowCell);
        row.appendChild(messageCell);
        errorRows.appendChild(row);
      });
    },
    
    // 업로드 에러 초기화 (root 기반)
    clearUploadErrors: function(root) {
      const errorSection = root.querySelector('#errorSection');
      const errorRows = errorSection?.querySelector('[data-error-rows]');
      
      if (errorSection) errorSection.hidden = true;
      if (errorRows) errorRows.innerHTML = '';
    },
    
    // 업로드 요약 표시 (공통 notification 사용, root 기반)
    showUploadSummary: function(message, isError, root) {
      const summary = root.querySelector('#uploadSummary');
      if (!summary) return;
      
      summary.hidden = false;
      summary.textContent = message;
      if (isError) {
        summary.classList.add('danger-text');
      } else {
        summary.classList.remove('danger-text');
      }
    }
  });
  
  // 페이지별 초기화 등록 (root 기반 구조)
  window.cmms.pages.register('inventory-list', function(root) {
    window.cmms.inventory.initList(root);
  });
  
  window.cmms.pages.register('inventory-detail', function(root) {
    window.cmms.inventory.initDetail(root);
  });
  
  window.cmms.pages.register('inventory-form', function(root) {
    window.cmms.inventory.initForm(root);
  });
  
  window.cmms.pages.register('inventory-upload', function(root) {
    window.cmms.inventory.initUpload(root);
  });
  
})();