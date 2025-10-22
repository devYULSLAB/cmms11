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
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Inventory list already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initPagination(root);
      this.initSearch(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('Inventory detail page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Inventory detail already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initPrintButton(root);
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Inventory form page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Inventory form already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initCancelButton(root);
    },
    
    // 업로드 페이지 초기화 (root 기반)
    initUpload: function(root) {
      console.log('Inventory upload page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Inventory upload already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initUploadForm(root);
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('Inventory pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-btn을 자동으로 처리
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
      this.initUploadSubmit(root);
    },
    
    // 업로드 폼 제출 초기화 (root 기반)
    initUploadSubmit: function(root) {
      const form = root.querySelector('#uploadForm');
      const previewSection = root.querySelector('#previewSection');
      const validDataBody = root.querySelector('#validDataBody');
      const errorSection = root.querySelector('#errorSection');
      const errorBody = root.querySelector('#errorBody');
      const previewSummary = root.querySelector('#previewSummary');
      const saveBtn = root.querySelector('#saveBtn');
      const cancelBtn = root.querySelector('#cancelBtn');
      
      if (!form) return;
      
      let validatedData = null; // 검증된 데이터 저장
      
      // 1단계: 업로드 및 검증
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        
        const fileInput = root.querySelector('#csvFile');
        const file = fileInput?.files[0];
        
        if (!file) {
          this.showNotification('파일을 선택하세요', true);
          return;
        }
        
        const formData = new FormData();
        formData.append('file', file);
        
        try {
          // 검증 API 호출 (저장 안 함)
          const response = await fetch(form.action, {
            method: 'POST',
            body: formData
          });
          
          if (!response.ok) {
            this.showNotification('검증 중 오류가 발생했습니다', true);
            return;
          }
          
          const result = await response.json();
          validatedData = result.validItems;
          
          // 유효한 데이터 테이블 렌더링
          this.renderValidData(validDataBody, result.validItems);
          
          // 오류 렌더링
          this.renderErrors(errorBody, result.errors);
          
          // 섹션 표시
          if (previewSection) previewSection.hidden = false;
          if (errorSection) {
            errorSection.hidden = result.errors.length === 0;
          }
          
          // 요약 표시
          if (previewSummary) {
            previewSummary.textContent = `(성공: ${result.successCount}건, 실패: ${result.failureCount}건)`;
            previewSummary.className = result.failureCount > 0 ? 'badge warning' : 'badge success';
          }
          
        } catch (err) {
          this.showNotification('검증 중 오류 발생', true);
        }
      });
      
      // 2단계: 저장 버튼
      if (saveBtn) {
        saveBtn.addEventListener('click', async () => {
          if (!validatedData || validatedData.length === 0) {
            this.showNotification('저장할 데이터가 없습니다', true);
            return;
          }
          
          try {
            const response = await fetch('/api/inventories/upload/confirm', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(validatedData)
            });
            
            if (!response.ok) {
              this.showNotification('저장 중 오류가 발생했습니다', true);
              return;
            }
            
            const result = await response.json();
            
            this.showNotification(`${result.successCount}건 저장되었습니다`, false);
            
            // 페이지 이동
            setTimeout(() => {
              window.cmms.navigation.navigate('/inventory/list');
            }, 1500);
            
          } catch (err) {
            this.showNotification('저장 중 오류 발생', true);
          }
        });
      }
      
      // 취소 버튼
      if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
          if (previewSection) previewSection.hidden = true;
          if (errorSection) errorSection.hidden = true;
          validatedData = null;
          form.reset();
        });
      }
    },
    
    // 유효한 데이터 렌더링
    renderValidData: function(tbody, items) {
      if (!tbody) return;
      tbody.innerHTML = '';
      
      items.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${item.inventoryId || '-'}</td>
          <td>${item.name || '-'}</td>
          <td>${item.unit || '-'}</td>
          <td>${item.makerName || '-'}</td>
          <td>${item.model || '-'}</td>
          <td>${item.serial || '-'}</td>
          <td>${item.spec || '-'}</td>
          <td>${item.note || '-'}</td>
        `;
        tbody.appendChild(row);
      });
    },
    
    // 오류 렌더링
    renderErrors: function(tbody, errors) {
      if (!tbody) return;
      tbody.innerHTML = '';
      
      errors.forEach(error => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${error.rowNumber}</td>
          <td>${error.message}</td>
        `;
        tbody.appendChild(row);
      });
    },
    
    // 알림 표시
    showNotification: function(message, isError) {
      if (window.cmms?.notification) {
        if (isError) {
          window.cmms.notification.error(message);
        } else {
          window.cmms.notification.success(message);
        }
      } else {
        alert(message);
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
