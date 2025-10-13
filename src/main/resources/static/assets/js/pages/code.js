/**
 * Code 모듈 JavaScript
 * 
 * 공통코드 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 동적 테이블 행 추가/삭제 기능을 제공합니다.
 * 공통 유틸(notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  if (!window.cmms) window.cmms = {};
  if (!window.cmms.code) window.cmms.code = {};

  // 기존 객체를 보존하면서 메서드만 추가
  Object.assign(window.cmms.code, {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('Code list page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Code list already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      // 기본 목록 페이지는 공통 기능으로 충분
      console.log('Code list uses common functionality');
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Code form page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Code form already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initDynamicTable(root);
      this.initCancelButton(root);
      // this.initFormSubmit(root);  // Form Manager 제거로 인한 주석 처리
    },
    
    // 동적 테이블 초기화 (root 기반)
    initDynamicTable: function(root) {
      const section = root.querySelector('[data-code-items]');
      if (!section) return;
      
      // 중복 초기화 방지
      if (section.dataset.initialized === 'true') {
        console.log('Dynamic table already initialized');
        return;
      }
      
      const tbody = section.querySelector('#code-items-body');
      const addBtn = section.querySelector('[data-add-item]');
      
      if (!tbody || !addBtn) return;
      
      // 순서 재정렬 함수
      const renumber = () => {
        tbody.querySelectorAll('.code-item-row').forEach((tr, i) => {
          const lineNo = tr.querySelector('.line-no');
          if (lineNo) lineNo.textContent = i + 1;
          
          // input name 속성 재정렬
          tr.querySelectorAll('input').forEach((inp) => {
            const field = inp.name.split('.').pop();
            inp.name = `items[${i}].${field}`;
          });
          
          // 삭제 버튼 표시/숨김 (첫 번째 행은 숨김)
          const delBtn = tr.querySelector('[data-remove-item]');
          if (delBtn) {
            delBtn.style.display = i === 0 ? 'none' : 'inline-flex';
          }
        });
      };
      
      // 새 행 추가 함수
      const addRow = () => {
        const currentRowCount = tbody.querySelectorAll('.code-item-row').length;
        const tr = document.createElement('tr');
        tr.className = 'code-item-row';
        tr.innerHTML = `
          <td class="cell-center line-no">${currentRowCount + 1}</td>
          <td><input class="input" name="items[${currentRowCount}].code" required maxlength="5" placeholder="예: REPR" /></td>
          <td><input class="input" name="items[${currentRowCount}].name" required maxlength="100" placeholder="예: 수리" /></td>
          <td><input class="input" name="items[${currentRowCount}].note" maxlength="500" placeholder="" /></td>
          <td class="cell-center"><button type="button" class="btn sm danger" data-remove-item>삭제</button></td>
        `;
        
        tbody.appendChild(tr);
        attachRowHandlers(tr);
        renumber();
        
        // 새로 추가된 행의 첫 번째 입력 필드에 포커스
        const firstInput = tr.querySelector('input');
        if (firstInput) firstInput.focus();
      };
      
      // 행 이벤트 핸들러 연결 함수
      const attachRowHandlers = (tr) => {
        const delBtn = tr.querySelector('[data-remove-item]');
        if (delBtn) {
          delBtn.addEventListener('click', () => {
            tr.remove();
            renumber();
            
            // 행이 하나도 없으면 기본 행 추가
            if (tbody.querySelectorAll('.code-item-row').length === 0) {
              addRow();
            }
          });
        }
      };
      
      // 기존 행들에 이벤트 핸들러 연결
      tbody.querySelectorAll('.code-item-row').forEach(attachRowHandlers);
      
      // 추가 버튼 이벤트 연결
      addBtn.addEventListener('click', addRow);
      
      // 초기 순서 정렬
      renumber();
      
      section.dataset.initialized = 'true';
    },
    
    // 취소 버튼 초기화 (root 기반)
    initCancelButton: function(root) {
      const cancelBtn = root.querySelector('[data-cancel-btn]');
      if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
          window.cmms.navigation.navigate('/code/list');
        });
      }
    },
    
    // 폼 제출 초기화 (공통 FormManager 활용, root 기반)
    // initFormSubmit: function(root) {
    //   const form = root.querySelector('[data-form-manager]');
    //   if (!form) return;
    //   
    //   // 중복 초기화 방지
    //   if (form.dataset.initialized === 'true') {
    //     console.log('Form submit already initialized');
    //     return;
    //   }
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
    //   
    //   form.dataset.initialized = 'true';
    // },
    
    // 폼 성공 처리 (root 기반)
    // handleFormSuccess: function(result, root) {
    //   if (window.cmms?.notification) {
    //     window.cmms.notification.success('공통코드가 성공적으로 저장되었습니다.');
    //   } else {
    //     alert('공통코드가 성공적으로 저장되었습니다.');
    //   }
    //   
    //   // SPA 네비게이션으로 레이아웃 유지
    //   setTimeout(() => {
    //     window.cmms.navigation.navigate('/code/list');
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
    // },
    
    // [DEPRECATED] 초기화 상태 리셋 (페이지 전환 시 호출)
    // DOM 기반 초기화로 변경되어 더 이상 필요하지 않음
    // resetInitialization: function(pageType) {
    //   console.log('Code initialization state reset (deprecated):', pageType || 'all');
    // }
  });
  
  // 페이지별 초기화 등록 (root 기반 구조)
  window.cmms.pages.register('code-list', function(root) {
    window.cmms.code.initList(root);
  });
  
  window.cmms.pages.register('code-form', function(root) {
    window.cmms.code.initForm(root);
  });
  
})();
