/**
 * Inspection 모듈 JavaScript
 * 
 * 점검 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 공통 유틸(TableManager, FormManager, DataLoader, fileUpload, notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  if (!window.cmms) window.cmms = {};
  if (!window.cmms.inspection) window.cmms.inspection = {};

  window.cmms.inspection = {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('Inspection list page initialized', root);
      this.initPagination(root);
      this.initSearch(root);
      this.initResetForm(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('Inspection detail page initialized', root);
      this.initAttachments(root);
      this.initPrintPreview(root);
      this.initPrintButtons(root);
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Inspection form page initialized', root);
      this.initFileUpload(root);
      this.initCancelButton(root);
      this.initPlantPicker(root);
      this.initInspectionItems(root);
      this.initFormSubmit(root);
    },
    
    // 계획 페이지 초기화 (root 기반)
    initPlan: function(root) {
      console.log('Inspection plan page initialized', root);
      this.initPlanItems(root);
      this.initCancelButton(root);
      this.initPlanSubmit(root);
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('Inspection pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-button을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
    },
    
    // 검색 초기화 (root 기반)
    initSearch: function(root) {
      console.log('Inspection search initialized - 기본 폼 제출로 처리됨');
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
    
    // 첨부파일 초기화 (root 기반)
    initAttachments: function(root) {
      const container = root.querySelector('#attachments-container');
      if (!container) return;
      
      const fileGroupId = container.dataset.fileGroupId;
      if (fileGroupId) {
        this.loadAttachments(fileGroupId, container);
      }
    },
    
    // 첨부파일 로드 (공통 DataLoader 사용)
    loadAttachments: async function(fileGroupId, container) {
      try {
        // 공통 DataLoader를 사용하여 첨부파일 로드
        const result = await window.cmms.common.DataLoader.load('/api/files', {
          params: { groupId: fileGroupId }
        });
        
        if (result.items.length === 0) {
          container.innerHTML = '<div class="notice">첨부된 파일이 없습니다.</div>';
          return;
        }
        
        // root 기반으로 DOM 요소 생성
        const list = root.createElement ? root.createElement('ul') : document.createElement('ul');
        list.className = 'attachments-list';
        
        result.items.forEach(item => {
          const li = root.createElement ? root.createElement('li') : document.createElement('li');
          li.className = 'attachment-item';
          li.innerHTML = `
            <span class="file-name">${item.originalName}</span>
            <span class="file-size">${window.cmms.utils.formatFileSize(item.size)}</span>
            <a href="/api/files/${item.fileId}?groupId=${fileGroupId}" class="btn-download" target="_blank">다운로드</a>
          `;
          list.appendChild(li);
        });
        
        container.innerHTML = '';
        container.appendChild(list);
        
      } catch (error) {
        console.error('Load attachments error:', error);
        container.innerHTML = '<div class="notice danger">첨부 파일을 불러올 수 없습니다.</div>';
      }
    },
    
    // 출력 미리보기 초기화 (root 기반)
    initPrintPreview: function(root) {
      root.querySelector('[data-print-preview]')?.addEventListener('click', (e) => {
        e.preventDefault();
        this.showPrintPreview(root);
      });
    },
    
    // 인쇄 버튼 초기화 (root 기반)
    initPrintButtons: function(root) {
      root.querySelector('[data-print]')?.addEventListener('click', (e) => {
        e.preventDefault();
        window.print();
      });
    },
    
    // 출력 미리보기 표시 (root 기반)
    showPrintPreview: function(root) {
      // 현재 날짜 업데이트
      const printDateEl = root.querySelector('#print-date');
      if (printDateEl) {
        const now = new Date();
        const formatDate = (d) => {
          const z = (n) => n < 10 ? '0' + n : n;
          return d.getFullYear() + '-' + z(d.getMonth() + 1) + '-' + z(d.getDate()) + ' ' + z(d.getHours()) + ':' + z(d.getMinutes());
        };
        printDateEl.textContent = formatDate(now);
      }
      
      // 출력 미리보기 모드 활성화 (전역 document 사용 - 인쇄 기능이므로 허용)
      document.body.classList.add('print-preview');
      
      // ESC 키로 미리보기 종료
      const handleKeydown = (e) => {
        if (e.key === 'Escape') {
          document.body.classList.remove('print-preview');
          document.removeEventListener('keydown', handleKeydown);
        }
      };
      document.addEventListener('keydown', handleKeydown);
      
      // 인쇄 후 미리보기 종료
      window.addEventListener('afterprint', () => {
        document.body.classList.remove('print-preview');
      }, { once: true });
    },
    
    // 파일 업로드 초기화 (공통 fileUpload 위임, root 기반)
    initFileUpload: function(root) {
      console.log('Inspection file upload initialized');
      // 공통 fileUpload 위젯 초기화 (root 범위 내에서만)
      const containers = root.querySelectorAll('[data-attachments]');
      if (containers.length > 0 && window.cmms?.fileUpload?.init) {
        containers.forEach(container => {
          window.cmms.fileUpload.init(container);
        });
      }
    },
    
    // 취소 버튼 초기화 (root 기반)
    initCancelButton: function(root) {
      const cancelBtn = root.querySelector('[data-cancel-btn]');
      if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
          window.cmms.navigation.navigate('/inspection/list');
        });
      }
    },
    
    // 설비 선택 팝업 초기화 (root 기반)
    initPlantPicker: function(root) {
      const plantPickerBtn = root.querySelector('#btn-plant-picker');
      if (plantPickerBtn) {
        plantPickerBtn.addEventListener('click', () => {
          const width = 1000;
          const height = 600;
          const left = (screen.width - width) / 2;
          const top = (screen.height - height) / 2;
          
          window.open(
            '/common/plant-picker',
            'plantPicker',
            `width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=yes`
          );
        });
      }
      
      // 팝업에서 설비 선택 시 호출되는 콜백 (전역 함수 대신 이벤트 기반으로 처리)
      // window.onPlantSelected 대신 공통 plant picker 이벤트 활용
    },
    
    // 점검 항목 관리 초기화 (root 기반)
    initInspectionItems: function(root) {
      const section = root.querySelector('[data-insp-items]');
      if (!section) return;
      
      const tbody = section.querySelector('#insp-items-body');
      const addBtn = section.querySelector('[data-add-item]');
      const copyBtn = section.querySelector('#btn-copy-previous');
      
      if (!tbody || !addBtn) return;
      
      // 항목 추가 버튼
      addBtn.addEventListener('click', () => {
        this.addInspectionItem(tbody);
      });
      
      // 이전 점검 복사 버튼
      if (copyBtn) {
        copyBtn.addEventListener('click', () => {
          this.copyPreviousInspection(root, tbody);
        });
      }
      
      // 기존 항목들에 이벤트 핸들러 연결
      this.attachItemHandlers(tbody);
      this.renumberItems(tbody);
    },
    
    // 점검 항목 추가 (root 기반)
    addInspectionItem: function(tbody) {
      const i = tbody.querySelectorAll('.insp-item-row').length;
      
      // 첫 번째 행 (데이터 입력) - root 기반 DOM 생성
      const tr1 = root.createElement ? root.createElement('tr') : document.createElement('tr');
      tr1.className = 'insp-item-row';
      tr1.innerHTML = `
        <td class="cell-center line-no" rowspan="2">${i + 1}</td>
        <td><input class="input" name="items[${i}].name" required maxlength="100" placeholder="항목" /></td>
        <td><input class="input" name="items[${i}].unit" maxlength="20" placeholder="단위" /></td>
        <td><input class="input" name="items[${i}].method" maxlength="100" placeholder="방법" /></td>
        <td><input class="input" name="items[${i}].minVal" maxlength="50" placeholder="최소" /></td>
        <td><input class="input" name="items[${i}].maxVal" maxlength="50" placeholder="최대" /></td>
        <td><input class="input" name="items[${i}].stdVal" maxlength="50" placeholder="기준" /></td>
        <td><input class="input" name="items[${i}].result" maxlength="50" placeholder="결과" /></td>
        <td class="cell-center" rowspan="2"><button type="button" class="btn sm danger" data-remove-item>삭제</button></td>`;
      
      // 두 번째 행 (비고) - root 기반 DOM 생성
      const tr2 = root.createElement ? root.createElement('tr') : document.createElement('tr');
      tr2.className = 'insp-item-note-row';
      tr2.innerHTML = `
        <td colspan="7">
          <input class="input" name="items[${i}].note" maxlength="500" placeholder="비고" />
        </td>`;
      
      tbody.appendChild(tr1);
      tbody.appendChild(tr2);
      this.attachRowHandlers(tr1);
      this.renumberItems(tbody);
    },
    
    // 항목 삭제 핸들러 연결 (root 기반)
    attachRowHandlers: function(tr) {
      const delBtn = tr.querySelector('[data-remove-item]');
      if (delBtn) {
        delBtn.addEventListener('click', () => {
          // 두 행 모두 삭제 (데이터 행 + 비고 행)
          const nextRow = tr.nextElementSibling;
          if (nextRow && nextRow.classList.contains('insp-item-note-row')) {
            nextRow.remove();
          }
          tr.remove();
          
          // 부모 tbody를 찾아서 번호 재정렬
          const tbody = tr.closest('tbody');
          if (tbody) {
            this.renumberItems(tbody);
          }
        });
      }
    },
    
    // 모든 항목에 핸들러 연결 (root 기반)
    attachItemHandlers: function(tbody) {
      tbody.querySelectorAll('.insp-item-row').forEach(tr => {
        this.attachRowHandlers(tr);
      });
    },
    
    // 항목 번호 재정렬 (root 기반)
    renumberItems: function(tbody) {
      tbody.querySelectorAll('.insp-item-row').forEach((tr, i) => {
        tr.querySelector('.line-no').textContent = i + 1;
        tr.querySelectorAll('input').forEach((inp) => {
          const field = inp.name.split('.').pop();
          inp.name = `items[${i}].${field}`;
        });
        const delBtn = tr.querySelector('[data-remove-item]');
        if (delBtn) delBtn.style.display = i === 0 ? 'none' : 'inline-flex';
      });
    },
    
    // 이전 점검 복사 (공통 DataLoader 사용, root 기반)
    copyPreviousInspection: async function(root, tbody) {
      const plantIdField = root.querySelector('#plant_id');
      if (!plantIdField || !plantIdField.value.trim()) {
        if (window.cmms?.notification) {
          window.cmms.notification.warning('설비 번호를 먼저 입력하세요.');
        } else {
          alert('설비 번호를 먼저 입력하세요.');
        }
        return;
      }
      
      try {
        // 공통 DataLoader 사용
        const data = await window.cmms.common.DataLoader.load('/api/inspections', {
          params: { plantId: plantIdField.value.trim(), size: 1 }
        });
        
        if (data.content && data.content.length > 0) {
          const prevInspection = data.content[0];
          
          const confirmMessage = `이전 점검(${prevInspection.inspectionId})의 항목을 복사하시겠습니까?\n(기존 항목은 모두 삭제됩니다)`;
          
          if (confirm(confirmMessage)) {
            this.loadItemsFromData(prevInspection.items || [], tbody);
            
            if (window.cmms?.notification) {
              window.cmms.notification.success('이전 점검 항목이 복사되었습니다.');
            }
          }
        } else {
          if (window.cmms?.notification) {
            window.cmms.notification.warning('해당 설비의 이전 점검 기록이 없습니다.');
          } else {
            alert('해당 설비의 이전 점검 기록이 없습니다.');
          }
        }
      } catch (error) {
        console.error('Copy previous error:', error);
        if (window.cmms?.notification) {
          window.cmms.notification.error('이전 점검을 복사하는 중 오류가 발생했습니다.');
        } else {
          alert('이전 점검을 복사하는 중 오류가 발생했습니다.');
        }
      }
    },
    
    // 데이터로부터 항목 로드 (root 기반)
    loadItemsFromData: function(items, tbody) {
      // 기존 항목 모두 삭제
      tbody.innerHTML = '';
      
      // 새 항목 추가
      items.forEach((item, idx) => {
        // root 기반 DOM 생성
        const tr1 = root.createElement ? root.createElement('tr') : document.createElement('tr');
        tr1.className = 'insp-item-row';
        tr1.innerHTML = `
          <td class="cell-center line-no" rowspan="2">${idx + 1}</td>
          <td><input class="input" name="items[${idx}].name" required maxlength="100" placeholder="항목" value="${item.name || ''}" /></td>
          <td><input class="input" name="items[${idx}].unit" maxlength="20" placeholder="단위" value="${item.unit || ''}" /></td>
          <td><input class="input" name="items[${idx}].method" maxlength="100" placeholder="방법" value="${item.method || ''}" /></td>
          <td><input class="input" name="items[${idx}].minVal" maxlength="50" placeholder="최소" value="${item.minVal || ''}" /></td>
          <td><input class="input" name="items[${idx}].maxVal" maxlength="50" placeholder="최대" value="${item.maxVal || ''}" /></td>
          <td><input class="input" name="items[${idx}].stdVal" maxlength="50" placeholder="기준" value="${item.stdVal || ''}" /></td>
          <td><input class="input" name="items[${idx}].result" maxlength="50" placeholder="결과" value="${item.result || ''}" /></td>
          <td class="cell-center" rowspan="2"><button type="button" class="btn sm danger ${idx === 0 ? 'hidden' : ''}" data-remove-item>삭제</button></td>`;
        
        const tr2 = root.createElement ? root.createElement('tr') : document.createElement('tr');
        tr2.className = 'insp-item-note-row';
        tr2.innerHTML = `
          <td colspan="7">
            <input class="input" name="items[${idx}].note" maxlength="500" placeholder="비고" value="${item.note || ''}" />
          </td>`;
        
        tbody.appendChild(tr1);
        tbody.appendChild(tr2);
        this.attachRowHandlers(tr1);
      });
      
      this.renumberItems(tbody);
    },
    
    // 계획 항목 관리 초기화 (root 기반)
    initPlanItems: function(root) {
      const section = root.querySelector('[data-plan-items]');
      if (!section) return;
      
      const tbody = section.querySelector('#plan-items-body');
      const addBtn = section.querySelector('[data-add-item]');
      
      if (!tbody || !addBtn) return;
      
      // 항목 추가 버튼
      addBtn.addEventListener('click', () => {
        this.addPlanItem(tbody);
      });
      
      // 기존 항목들에 이벤트 핸들러 연결
      this.attachPlanHandlers(tbody);
      this.renumberPlanItems(tbody);
    },
    
    // 계획 항목 추가 (root 기반)
    addPlanItem: function(tbody) {
      const i = tbody.querySelectorAll('.plan-item-row').length;
      const firstRow = tbody.querySelector('.plan-item-row');
      if (!firstRow) return;
      
      const tr = firstRow.cloneNode(true);
      tr.className = 'plan-item-row';
      
      // 모든 input/select 필드 초기화
      tr.querySelectorAll('input[type="text"], input[type="date"]').forEach(inp => inp.value = '');
      tr.querySelectorAll('select').forEach(sel => sel.selectedIndex = 0);
      
      // name 속성 업데이트
      tr.querySelectorAll('input, select').forEach(el => {
        const field = el.name.split('.').pop();
        el.name = `inspections[${i}].${field}`;
      });
      
      // 삭제 버튼 표시
      const delBtn = tr.querySelector('[data-remove-item]');
      if (delBtn) delBtn.style.display = 'inline-flex';
      
      tbody.appendChild(tr);
      this.attachPlanRowHandlers(tr);
      this.renumberPlanItems(tbody);
    },
    
    // 계획 항목 삭제 핸들러 연결 (root 기반)
    attachPlanRowHandlers: function(tr) {
      const delBtn = tr.querySelector('[data-remove-item]');
      if (delBtn) {
        delBtn.addEventListener('click', () => {
          tr.remove();
          const tbody = tr.closest('tbody');
          if (tbody) {
            this.renumberPlanItems(tbody);
          }
        });
      }
    },
    
    // 모든 계획 항목에 핸들러 연결 (root 기반)
    attachPlanHandlers: function(tbody) {
      tbody.querySelectorAll('.plan-item-row').forEach(tr => {
        this.attachPlanRowHandlers(tr);
      });
    },
    
    // 계획 항목 번호 재정렬 (root 기반)
    renumberPlanItems: function(tbody) {
      tbody.querySelectorAll('.plan-item-row').forEach((tr, i) => {
        tr.querySelector('.line-no').textContent = i + 1;
        const inputs = tr.querySelectorAll('input');
        inputs.forEach((inp) => {
          const field = inp.name.split('.').pop();
          inp.name = `inspections[${i}].${field}`;
          if (field === 'status') inp.value = 'PLAN';
        });
        const delBtn = tr.querySelector('[data-remove-item]');
        if (delBtn) delBtn.style.display = i === 0 ? 'none' : 'inline-flex';
      });
    },
    
    // 폼 제출 초기화 (공통 FormManager 활용, root 기반)
    initFormSubmit: function(root) {
      const form = root.querySelector('[data-form-manager]');
      if (!form) return;
      
      // 공통 FormManager를 활용한 폼 제출 처리
      if (window.cmms?.formManager) {
        window.cmms.formManager.init(form, {
          onSuccess: (result) => {
            this.handleFormSuccess(result, root);
          },
          onError: (error) => {
            this.handleFormError(error, root);
          }
        });
      } else {
        // FormManager가 없는 경우 직접 처리
        form.addEventListener('submit', async (event) => {
          event.preventDefault();
          await this.handleDirectForm(form, root);
        });
      }
    },
    
    // 계획 제출 초기화 (공통 FormManager 활용, root 기반)
    initPlanSubmit: function(root) {
      const form = root.querySelector('[data-form-manager]');
      if (!form) return;
      
      // 공통 FormManager를 활용한 폼 제출 처리
      if (window.cmms?.formManager) {
        window.cmms.formManager.init(form, {
          onSuccess: (result) => {
            this.handlePlanSuccess(result, root);
          },
          onError: (error) => {
            this.handlePlanError(error, root);
          }
        });
      } else {
        // FormManager가 없는 경우 직접 처리
        form.addEventListener('submit', async (event) => {
          event.preventDefault();
          await this.handleDirectPlan(form, root);
        });
      }
    },
    
    // 폼 성공 처리 (root 기반)
    handleFormSuccess: function(result, root) {
      if (window.cmms?.notification) {
        window.cmms.notification.success('점검 정보가 성공적으로 저장되었습니다.');
      } else {
        alert('점검 정보가 성공적으로 저장되었습니다.');
      }
      
      // SPA 네비게이션으로 레이아웃 유지
      setTimeout(() => {
        window.cmms.navigation.navigate('/inspection/list');
      }, 1000);
    },
    
    // 계획 성공 처리 (root 기반)
    handlePlanSuccess: function(result, root) {
      if (window.cmms?.notification) {
        window.cmms.notification.success('점검 계획이 성공적으로 수립되었습니다.');
      } else {
        alert('점검 계획이 성공적으로 수립되었습니다.');
      }
      
      // SPA 네비게이션으로 레이아웃 유지
      setTimeout(() => {
        window.cmms.navigation.navigate('/inspection/list');
      }, 1000);
    },
    
    // 폼 에러 처리 (root 기반)
    handleFormError: function(error, root) {
      console.error('Form submit error:', error);
      if (window.cmms?.notification) {
        window.cmms.notification.error('저장 중 오류가 발생했습니다.');
      } else {
        alert('저장 중 오류가 발생했습니다.');
      }
    },
    
    // 계획 에러 처리 (root 기반)
    handlePlanError: function(error, root) {
      console.error('Plan submit error:', error);
      if (window.cmms?.notification) {
        window.cmms.notification.error('계획 수립 중 오류가 발생했습니다.');
      } else {
        alert('계획 수립 중 오류가 발생했습니다.');
      }
    },
    
    // 직접 폼 처리 (FormManager 없을 때)
    handleDirectForm: async function(form, root) {
      try {
        const formData = new FormData(form);
        
        const response = await fetch(form.action, {
          method: 'POST',
          body: formData,
        });
        
        if (!response.ok) {
          throw new Error('저장 실패');
        }
        
        const result = await response.json();
        this.handleFormSuccess(result, root);
        
      } catch (error) {
        this.handleFormError(error, root);
      }
    },
    
    // 직접 계획 처리 (FormManager 없을 때)
    handleDirectPlan: async function(form, root) {
      try {
        const formData = new FormData(form);
        
        const response = await fetch(form.action, {
          method: 'POST',
          body: formData,
        });
        
        if (!response.ok) {
          throw new Error('계획 수립 실패');
        }
        
        const result = await response.json();
        this.handlePlanSuccess(result, root);
        
      } catch (error) {
        this.handlePlanError(error, root);
      }
    }
  };
  
  // 페이지별 초기화 등록 (root 기반 구조)
  window.cmms.pages.register('inspection-list', function(root) {
    window.cmms.inspection.initList(root);
  });
  
  window.cmms.pages.register('inspection-detail', function(root) {
    window.cmms.inspection.initDetail(root);
  });
  
  window.cmms.pages.register('inspection-form', function(root) {
    window.cmms.inspection.initForm(root);
  });
  
  window.cmms.pages.register('inspection-plan', function(root) {
    window.cmms.inspection.initPlan(root);
  });
  
})();