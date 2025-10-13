/**
 * Workpermit 모듈 JavaScript
 * 
 * 작업허가 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 공통 유틸(TableManager, DataLoader, fileUpload, notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  window.cmms = window.cmms || {};
  window.cmms.workpermit = window.cmms.workpermit || {};

  Object.assign(window.cmms.workpermit, {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('Workpermit list page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Workpermit list already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initPagination(root);
      this.initSearch(root);
      this.initResetForm(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('Workpermit detail page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Workpermit detail already initialized, skipping');
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
      console.log('Workpermit form page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Workpermit form already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initCancelButton(root);
      // this.initSignatureCanvas(root); // 주석 처리 - initSigners에서 처리됨
      this.initSigners(root);
      // this.initFormSubmit(root);  // Form Manager 제거로 인한 주석 처리
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('Workpermit pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-btn을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
    },
    
    // 검색 초기화 (root 기반)
    initSearch: function(root) {
      console.log('Workpermit search initialized - 기본 폼 제출로 처리됨');
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
    },
    
    // 취소 버튼 초기화 (root 기반)
    initCancelButton: function(root) {
      const cancelBtn = root.querySelector('[data-cancel-btn]');
      if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
          window.cmms.navigation.navigate('/workpermit/list');
        });
      }
    },
    
    // 서명자 관리 초기화 (root 기반)
    initSigners: function(root) {
      const section = root.querySelector('[data-signers]');
      if (!section) return;
      
      const list = section.querySelector('.signers');
      const addBtn = section.querySelector('[data-add-signer]');
      
      if (!list || !addBtn) return;
      
      // 기존 서명자들 초기화
      this.initExistingSigners(list, root);
      
      // 서명자 추가 버튼
      addBtn.addEventListener('click', () => {
        this.addSigner(list, root);
      });
    },
    
    // 기존 서명자들 초기화 (root 기반)
    initExistingSigners: function(list, root) {
      // 서버에서 제공된 기존 서명자 데이터 로드
      try {
        const serverItems = window.serverData?.items || [];
        if (Array.isArray(serverItems) && serverItems.length > 0) {
          list.innerHTML = '';
          serverItems.forEach((item, idx) => {
            this.addSignerRow(list, idx, item, root);
          });
        } else {
          // 기본 서명자 행이 있으면 초기화
          const existingRows = list.querySelectorAll('.signer-row');
          existingRows.forEach((row, idx) => {
            this.ensureSignatureUI(row, idx, root);
          });
        }
      } catch (error) {
        console.error('Init existing signers error:', error);
        // 에러 시 기본 행들만 초기화
        const existingRows = list.querySelectorAll('.signer-row');
        existingRows.forEach((row, idx) => {
          this.ensureSignatureUI(row, idx, root);
        });
      }
    },
    
    // 서명자 추가 (root 기반)
    addSigner: function(list, root) {
      const idx = list.querySelectorAll('.signer-row').length;
      this.addSignerRow(list, idx, {}, root);
    },
    
    // 서명자 행 추가 (root 기반)
    addSignerRow: function(list, idx, data, root) {
      const row = document.createElement('div');
      row.className = 'signer-row';
      row.innerHTML = `
        <div class="form-row">
          <label class="label required" for="signer_${idx}_name">이름</label>
          <input id="signer_${idx}_name" name="items[${idx}].name" class="input" required maxlength="100" value="${data.name || ''}" />
        </div>
        <div class="form-row">
          <label class="label">서명</label>
          <div class="signature-box" aria-label="서명(펜으로 기입)"></div>
        </div>
        <div class="signer-actions">
          <button type="button" class="btn sm danger" data-remove-signer ${idx === 0 ? 'style="display:none"' : ''}>삭제</button>
        </div>`;
      
      list.appendChild(row);
      
      // 삭제 버튼 이벤트
      const removeBtn = row.querySelector('[data-remove-signer]');
      if (removeBtn) {
        removeBtn.addEventListener('click', () => {
          row.remove();
          this.updateSignerNumbers(list, root);
        });
      }
      
      // 서명 UI 초기화
      this.ensureSignatureUI(row, idx, root);
      
      // 서명자 번호 업데이트
      this.updateSignerNumbers(list, root);
    },
    
    // 서명자 번호 업데이트 (root 기반)
    updateSignerNumbers: function(list, root) {
      const rows = list.querySelectorAll('.signer-row');
      rows.forEach((row, idx) => {
        // 이름 입력 필드 name 속성 업데이트
        const nameInput = row.querySelector('input[name*="].name"]');
        if (nameInput) {
          nameInput.name = `items[${idx}].name`;
          nameInput.id = `signer_${idx}_name`;
          const label = row.querySelector('label[for*="signer_"]');
          if (label) label.setAttribute('for', `signer_${idx}_name`);
        }
        
        // 삭제 버튼 표시/숨김 (첫 번째는 숨김)
        const removeBtn = row.querySelector('[data-remove-signer]');
        if (removeBtn) {
          removeBtn.style.display = idx === 0 ? 'none' : 'inline-flex';
        }
        
        // 서명 캔버스 재초기화
        this.ensureSignatureUI(row, idx, root);
      });
    },
    
    // 서명 UI 초기화 (root 기반)
    ensureSignatureUI: function(row, idx, root) {
      // 캔버스 생성
      let canvas = row.querySelector('.signature-canvas');
      const box = row.querySelector('.signature-box');
      
      if (box && !canvas) {
        canvas = document.createElement('canvas');
        canvas.className = 'signature-canvas';
        box.appendChild(canvas);
      }
      
      // 숨겨진 입력 필드 생성
      let hidden = row.querySelector('input[name$=".signature"]');
      if (!hidden) {
        hidden = document.createElement('input');
        hidden.type = 'hidden';
        hidden.name = `items[${idx}].signature`;
        hidden.id = `signer_${idx}_signature`;
        row.appendChild(hidden);
      }
      
      // 지우기 버튼 생성
      const actions = row.querySelector('.signer-actions');
      if (actions && !actions.querySelector('[data-clear-signature]')) {
        const clearBtn = document.createElement('button');
        clearBtn.type = 'button';
        clearBtn.className = 'btn sm';
        clearBtn.textContent = '지우기';
        clearBtn.setAttribute('data-clear-signature', '');
        actions.insertBefore(clearBtn, actions.firstChild);
      }
      
      if (!canvas || !hidden) return;
      
      // 캔버스 초기화
      this.initSignatureCanvas(canvas, hidden, root);
    },
    
    // 서명 캔버스 초기화 (root 기반)
    initSignatureCanvas: function(canvas, hidden, root) {
      const ctx = canvas.getContext('2d');
      let drawing = false;
      canvas.dataset.dirty = 'false';
      
      // 캔버스 크기 설정
      const resize = () => {
        const ratio = Math.max(window.devicePixelRatio || 1, 1);
        const box = canvas.parentElement;
        const w = box?.clientWidth || 400;
        const h = box?.clientHeight || 100;
        
        canvas.width = Math.floor(w * ratio);
        canvas.height = Math.floor(h * ratio);
        canvas.style.width = w + 'px';
        canvas.style.height = h + 'px';
        
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        ctx.lineWidth = 2;
        ctx.lineCap = 'round';
        ctx.strokeStyle = '#111827';
      };
      
      resize();
      window.addEventListener('resize', resize);
      
      // 포인터 이벤트
      const getPosition = (e) => {
        const rect = canvas.getBoundingClientRect();
        return {
          x: e.clientX - rect.left,
          y: e.clientY - rect.top
        };
      };
      
      canvas.addEventListener('pointerdown', (e) => {
        canvas.setPointerCapture(e.pointerId);
        const pos = getPosition(e);
        drawing = true;
        ctx.beginPath();
        ctx.moveTo(pos.x, pos.y);
        canvas.dataset.dirty = 'true';
      });
      
      canvas.addEventListener('pointermove', (e) => {
        if (!drawing) return;
        const pos = getPosition(e);
        ctx.lineTo(pos.x, pos.y);
        ctx.stroke();
      });
      
      const endStroke = (e) => {
        if (!drawing) return;
        drawing = false;
        try {
          hidden.value = canvas.toDataURL('image/png');
        } catch (error) {
          console.error('Save signature error:', error);
        }
      };
      
      canvas.addEventListener('pointerup', endStroke);
      canvas.addEventListener('pointercancel', endStroke);
      
      // 지우기 버튼
      const clearBtn = canvas.closest('.signer-row').querySelector('[data-clear-signature]');
      if (clearBtn) {
        clearBtn.addEventListener('click', () => {
          ctx.clearRect(0, 0, canvas.width, canvas.height);
          canvas.dataset.dirty = 'false';
          hidden.value = '';
        });
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
    //     window.cmms.notification.success('작업허가가 성공적으로 저장되었습니다.');
    //   } else {
    //     alert('작업허가가 성공적으로 저장되었습니다.');
    //   }
    //   
    //   // SPA 네비게이션으로 레이아웃 유지
    //   setTimeout(() => {
    //     window.cmms.navigation.navigate('/workpermit/list');
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
    //     // 서명 캔버스 데이터 저장
    //     this.saveSignatures(form, root);
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
    // },
    
    // 서명 데이터 저장 (root 기반)
    saveSignatures: function(form, root) {
      const section = root.querySelector('[data-signers]');
      if (!section) return;
      
      const list = section.querySelector('.signers');
      if (!list) return;
      
      list.querySelectorAll('.signer-row').forEach((row) => {
        const nameInput = row.querySelector('input[name*="].name"]');
        const canvas = row.querySelector('.signature-canvas');
        const hidden = row.querySelector('input[name$=".signature"]');
        
        if (nameInput && hidden) {
          const name = nameInput.value || '';
          let signature = hidden.value || '';
          
          if (canvas && canvas.dataset.dirty === 'true') {
            try {
              signature = canvas.toDataURL('image/png');
            } catch (error) {
              console.error('Save signature error:', error);
            }
          }
          
          hidden.value = signature;
        }
      });
    }
  });
  
  // 페이지별 초기화 등록 (root 기반 구조)
  window.cmms.pages.register('workpermit-list', function(root) {
    window.cmms.workpermit.initList(root);
  });
  
  window.cmms.pages.register('workpermit-detail', function(root) {
    window.cmms.workpermit.initDetail(root);
  });
  
  window.cmms.pages.register('workpermit-form', function(root) {
    window.cmms.workpermit.initForm(root);
  });
  
})();