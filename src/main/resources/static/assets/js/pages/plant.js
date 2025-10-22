/**
 * Plant 모듈 JavaScript
 * 
 * 설비 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 공통 유틸(TableManager, DataLoader, fileUpload, notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  window.cmms = window.cmms || {};
  window.cmms.plant = window.cmms.plant || {};

  Object.assign(window.cmms.plant, {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('Plant list page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Plant list already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initPagination(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('Plant detail page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Plant detail already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initPrintButton(root);
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Plant form page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Plant form already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initCancelButton(root);
    },
    
    // 이력 페이지 초기화 (root 기반)
    initHistory: function(root) {
      console.log('Plant history page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Plant history already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initSearch(root);
      this.initHistoryLoad(root);
    },
    
    // 업로드 페이지 초기화 (root 기반)
    initUpload: function(root) {
      console.log('Plant upload page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Plant upload already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initUploadForm(root);
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('Plant pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-btn을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
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
          window.cmms.navigation.navigate('/plant/list');
        });
      }
    },
    
    // 검색 초기화 (root 기반)
    initSearch: function(root) {
      const searchBtn = root.querySelector('[data-plant-search]');
      const clearBtn = root.querySelector('[data-clear-search]');
      
      if (searchBtn) {
        searchBtn.addEventListener('click', () => {
          this.searchPlant(root);
        });
      }
      
      if (clearBtn) {
        clearBtn.addEventListener('click', () => {
          this.clearSearch(root);
        });
      }
    },
    
    // 검색 초기화 (root 기반)
    clearSearch: function(root) {
      const searchInput = root.querySelector('#plant-search');
      const resultsSection = root.querySelector('#plant-search-results');
      const infoSection = root.querySelector('#plant-info-section');
      const inspectionSection = root.querySelector('#inspection-history-section');
      const workorderSection = root.querySelector('#workorder-history-section');
      const workpermitSection = root.querySelector('#workpermit-history-section');
      
      if (searchInput) searchInput.value = '';
      if (resultsSection) resultsSection.classList.add('hidden');
      if (infoSection) infoSection.classList.add('hidden');
      if (inspectionSection) inspectionSection.classList.add('hidden');
      if (workorderSection) workorderSection.classList.add('hidden');
      if (workpermitSection) workpermitSection.classList.add('hidden');
      
      this.selectedPlantId = null;
    },
    
    // 설비 검색 (공통 DataLoader 사용, root 기반)
    searchPlant: async function(root) {
      const searchInput = root.querySelector('#plant-search');
      if (!searchInput) return;
      
      const plantId = searchInput.value.trim();
      if (!plantId) {
        if (window.cmms?.notification) {
          window.cmms.notification.warning('설비번호를 입력해주세요.');
        } else {
          alert('설비번호를 입력해주세요.');
        }
        return;
      }

      try {
        // 공통 DataLoader 사용
        const data = await window.cmms.common.DataLoader.load('/api/plants', {
          params: { plantId: plantId, size: 10 }
        });
        
        const resultsSection = root.querySelector('#plant-search-results');
        const resultsBody = root.querySelector('#plant-results-body');
        
        if (data.content && data.content.length > 0) {
          resultsBody.innerHTML = '';
          data.content.forEach(plant => {
            const row = document.createElement('tr');
            row.innerHTML = `
              <td>${plant.plantId}</td>
              <td>${plant.name || '-'}</td>
              <td>${plant.siteId || '-'}</td>
              <td>${plant.funcId || '-'}</td>
              <td>${plant.deptId || '-'}</td>
              <td class="actions">
                <button class="btn btn-sm" onclick="window.cmms.plant.selectPlant('${plant.plantId}', arguments[0].closest('[data-slot-root]'))">선택</button>
              </td>
            `;
            resultsBody.appendChild(row);
          });
          resultsSection.classList.remove('hidden');
        } else {
          resultsSection.classList.add('hidden');
          if (window.cmms?.notification) {
            window.cmms.notification.warning('해당 설비를 찾을 수 없습니다.');
          } else {
            alert('해당 설비를 찾을 수 없습니다.');
          }
        }
      } catch (error) {
        console.error('설비 검색 오류:', error);
        if (window.cmms?.notification) {
          window.cmms.notification.error('설비 검색 중 오류가 발생했습니다.');
        } else {
          alert('설비 검색 중 오류가 발생했습니다.');
        }
      }
    },
    
    // 설비 선택 (root 기반)
    selectPlant: async function(plantId, root) {
      this.selectedPlantId = plantId;
      
      try {
        // 설비 정보 로드 (공통 DataLoader 사용)
        const plant = await window.cmms.common.DataLoader.load(`/api/plants/${plantId}`);
        
        // 설비 정보 표시
        const plantIdEl = root.querySelector('#info-plant-id');
        const plantNameEl = root.querySelector('#info-plant-name');
        const assetTypeEl = root.querySelector('#info-asset-type');
        const siteEl = root.querySelector('#info-site');
        const deptEl = root.querySelector('#info-dept');
        const funcEl = root.querySelector('#info-func');
        const makerEl = root.querySelector('#info-maker');
        const modelEl = root.querySelector('#info-model');
        const installDateEl = root.querySelector('#info-install-date');
        
        if (plantIdEl) plantIdEl.textContent = plant.plantId;
        if (plantNameEl) plantNameEl.textContent = plant.name || '-';
        if (assetTypeEl) assetTypeEl.textContent = plant.assetId || '-';
        if (siteEl) siteEl.textContent = plant.siteId || '-';
        if (deptEl) deptEl.textContent = plant.deptId || '-';
        if (funcEl) funcEl.textContent = plant.funcId || '-';
        if (makerEl) makerEl.textContent = plant.makerName || '-';
        if (modelEl) modelEl.textContent = plant.model || '-';
        if (installDateEl) installDateEl.textContent = plant.installDate || '-';
        
        // 링크 업데이트
        const detailLink = root.querySelector('#plant-detail-link');
        const editLink = root.querySelector('#plant-edit-link');
        if (detailLink) detailLink.href = `/plant/detail/${plantId}`;
        if (editLink) editLink.href = `/plant/edit/${plantId}`;
        
        // 설비 정보 섹션 표시
        const infoSection = root.querySelector('#plant-info-section');
        if (infoSection) infoSection.classList.remove('hidden');
        
        // 이력 데이터 로드
        await this.loadInspectionHistory(plantId, root);
        await this.loadWorkorderHistory(plantId, root);
        await this.loadWorkpermitHistory(plantId, root);
        
      } catch (error) {
        console.error('설비 정보 로드 오류:', error);
        if (window.cmms?.notification) {
          window.cmms.notification.error('설비 정보를 불러오는 중 오류가 발생했습니다.');
        } else {
          alert('설비 정보를 불러오는 중 오류가 발생했습니다.');
        }
      }
    },
    
    // 이력 로드 초기화 (root 기반)
    initHistoryLoad: function(root) {
      // Enter 키로 검색
      const searchInput = root.querySelector('#plant-search');
      if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
          if (e.key === 'Enter') {
            this.searchPlant(root);
          }
        });
      }
    },
    
    // 예방점검 이력 로드 (공통 DataLoader 사용, root 기반)
    loadInspectionHistory: async function(plantId, root) {
      try {
        const data = await window.cmms.common.DataLoader.load('/api/inspections', {
          params: { plantId: plantId, size: 5 }
        });
        
        const historyBody = root.querySelector('#inspection-history-body');
        if (!historyBody) return;
        
        historyBody.innerHTML = '';
        
        if (data.content && data.content.length > 0) {
          data.content.forEach(inspection => {
            const row = document.createElement('tr');
            const statusText = inspection.status === 'PLAN' ? '계획' : 
                             inspection.status === 'PROC' ? '진행' : '완료';
            const statusClass = inspection.status === 'PLAN' ? 'badge' : 
                              inspection.status === 'PROC' ? 'badge warning' : 'badge success';
            
            row.innerHTML = `
              <td><a href="/inspection/edit/${inspection.inspectionId}">${inspection.inspectionId}</a></td>
              <td>${inspection.name || '-'}</td>
              <td>${inspection.jobId || '-'}</td>
              <td>${inspection.plannedDate || '-'}</td>
              <td>${inspection.actualDate || '-'}</td>
              <td><span class="${statusClass}">${statusText}</span></td>
              <td class="actions">
                <a class="btn btn-sm" href="/inspection/edit/${inspection.inspectionId}">수정</a>
              </td>
            `;
            historyBody.appendChild(row);
          });
          const section = root.querySelector('#inspection-history-section');
          if (section) section.classList.remove('hidden');
        } else {
          const section = root.querySelector('#inspection-history-section');
          if (section) section.classList.add('hidden');
        }
      } catch (error) {
        console.error('점검 이력 로드 오류:', error);
      }
    },
    
    // 작업오더 이력 로드 (공통 DataLoader 사용, root 기반)
    loadWorkorderHistory: async function(plantId, root) {
      try {
        const data = await window.cmms.common.DataLoader.load('/api/workorders', {
          params: { plantId: plantId, size: 5 }
        });
        
        const historyBody = root.querySelector('#workorder-history-body');
        if (!historyBody) return;
        
        historyBody.innerHTML = '';
        
        if (data.content && data.content.length > 0) {
          data.content.forEach(workorder => {
            const row = document.createElement('tr');
            const statusText = workorder.status === 'PLAN' ? '계획' : 
                             workorder.status === 'PROC' ? '진행' : '완료';
            const statusClass = workorder.status === 'PLAN' ? 'badge' : 
                              workorder.status === 'PROC' ? 'badge warning' : 'badge success';
            
            row.innerHTML = `
              <td><a href="/workorder/edit/${workorder.orderId}">${workorder.orderId}</a></td>
              <td>${workorder.name || '-'}</td>
              <td>${workorder.jobId || '-'}</td>
              <td>${workorder.plannedDate || '-'}</td>
              <td>${workorder.actualDate || '-'}</td>
              <td><span class="${statusClass}">${statusText}</span></td>
              <td class="actions">
                <a class="btn btn-sm" href="/workorder/edit/${workorder.orderId}">수정</a>
              </td>
            `;
            historyBody.appendChild(row);
          });
          const section = root.querySelector('#workorder-history-section');
          if (section) section.classList.remove('hidden');
        } else {
          const section = root.querySelector('#workorder-history-section');
          if (section) section.classList.add('hidden');
        }
      } catch (error) {
        console.error('작업오더 이력 로드 오류:', error);
      }
    },
    
    // 작업허가 이력 로드 (공통 DataLoader 사용, root 기반)
    loadWorkpermitHistory: async function(plantId, root) {
      try {
        const data = await window.cmms.common.DataLoader.load('/api/workpermits', {
          params: { plantId: plantId, size: 5 }
        });
        
        const historyBody = root.querySelector('#workpermit-history-body');
        if (!historyBody) return;
        
        historyBody.innerHTML = '';
        
        if (data.content && data.content.length > 0) {
          data.content.forEach(workpermit => {
            const row = document.createElement('tr');
            const statusText = workpermit.status === 'REQUEST' ? '신청' : 
                             workpermit.status === 'APPROVED' ? '승인' : '완료';
            const statusClass = workpermit.status === 'REQUEST' ? 'badge' : 
                              workpermit.status === 'APPROVED' ? 'badge warning' : 'badge success';
            
            row.innerHTML = `
              <td><a href="/workpermit/edit/${workpermit.workpermitId}">${workpermit.workpermitId}</a></td>
              <td>${workpermit.name || '-'}</td>
              <td>${workpermit.jobId || '-'}</td>
              <td>${workpermit.requestDate || '-'}</td>
              <td>${workpermit.approvedDate || '-'}</td>
              <td><span class="${statusClass}">${statusText}</span></td>
              <td class="actions">
                <a class="btn btn-sm" href="/workpermit/edit/${workpermit.workpermitId}">수정</a>
              </td>
            `;
            historyBody.appendChild(row);
          });
          const section = root.querySelector('#workpermit-history-section');
          if (section) section.classList.remove('hidden');
        } else {
          const section = root.querySelector('#workpermit-history-section');
          if (section) section.classList.add('hidden');
        }
      } catch (error) {
        console.error('작업허가 이력 로드 오류:', error);
      }
    },
    
    // 업로드 폼 초기화 (root 기반)
    initUploadForm: function(root) {
      console.log('Plant upload form initialized');
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
            const response = await fetch('/api/plants/upload/confirm', {
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
              window.cmms.navigation.navigate('/plant/list');
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
          <td>${item.plantId || '-'}</td>
          <td>${item.name || '-'}</td>
          <td>${item.assetId || '-'}</td>
          <td>${item.siteId || '-'}</td>
          <td>${item.deptId || '-'}</td>
          <td>${item.funcId || '-'}</td>
          <td>${item.makerName || '-'}</td>
          <td>${item.model || '-'}</td>
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
  window.cmms.pages.register('plant-list', function(root) {
    window.cmms.plant.initList(root);
  });
  
  window.cmms.pages.register('plant-detail', function(root) {
    window.cmms.plant.initDetail(root);
  });
  
  window.cmms.pages.register('plant-form', function(root) {
    window.cmms.plant.initForm(root);
  });
  
  window.cmms.pages.register('plant-history', function(root) {
    window.cmms.plant.initHistory(root);
  });
  
  window.cmms.pages.register('plant-upload', function(root) {
    window.cmms.plant.initUpload(root);
  });
  
})();