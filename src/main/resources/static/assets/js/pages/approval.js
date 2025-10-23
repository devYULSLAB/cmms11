/**
 * Approval 모듈 JavaScript
 * 
 * 전자결재 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 공통 유틸(TableManager, DataLoader, fileUpload, notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  if (!window.cmms) window.cmms = {};
  if (!window.cmms.approval) window.cmms.approval = {};

  // 기존 객체를 보존하면서 메서드만 추가
  Object.assign(window.cmms.approval, {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('Approval list page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Approval list already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initPagination(root);
      this.initSearch(root);
      this.initResetForm(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('Approval detail page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Approval detail already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initApprovalActions(root);
      this.initPrintButton(root);
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Approval form page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Approval form already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initEditor(root);
      this.initApproverManagement(root);
      this.initSaveButtons(root);
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('Approval pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-btn을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
    },
    
    // 검색 초기화 (root 기반)
    initSearch: function(root) {
      console.log('Approval search initialized - 기본 폼 제출로 처리됨');
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
    
    // 결재 액션 초기화 (root 기반)
    initApprovalActions: function(root) {
      console.log('Approval actions initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 삭제 로직이 data-delete-url을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
    },
    
    // 에디터 초기화 (root 기반)
    initEditor: function(root) {
      const toolbar = root.querySelector('.editor-toolbar');
      const editor = root.querySelector('#editor');
      const contentField = root.querySelector('#content');
      
      if (!toolbar || !editor || !contentField) return;
      
      // 중복 초기화 방지
      if (toolbar.dataset.initialized === 'true') {
        console.log('Editor already initialized');
        return;
      }
      
      // 에디터 툴바 버튼들
      const buttons = toolbar.querySelectorAll('[data-cmd]');
      buttons.forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.preventDefault();
          const cmd = btn.getAttribute('data-cmd');
          document.execCommand(cmd, false, null);
          editor.focus();
        });
      });
      
      // 폼 제출 시 에디터 내용을 hidden field에 동기화
      const form = root.querySelector('form');
      if (form) {
        form.addEventListener('submit', () => {
          contentField.value = editor.innerHTML;
        });
      }
      
      toolbar.dataset.initialized = 'true';
    },
    
    // 결재선 관리 초기화 (root 기반)
    initApproverManagement: function(root) {
      const approverItems = root.querySelector('#approver-items');
      const autocompleteWrapper = root.querySelector('[data-approver-autocomplete]');
      const displayInput = autocompleteWrapper?.querySelector('[data-approver-display]');
      const hiddenInput = autocompleteWrapper?.querySelector('[data-approver-id]');
      const suggestionsEl = autocompleteWrapper?.querySelector('[data-approver-suggestions]');
      const decisionSelect = root.querySelector('#decision-select');
      const addBtn = root.querySelector('#add-approver');
      
      if (!approverItems || !autocompleteWrapper || !displayInput || !hiddenInput || !suggestionsEl || !decisionSelect || !addBtn) {
        return;
      }
      
      if (approverItems.dataset.initialized === 'true') {
        console.log('Approver management already initialized');
        return;
      }
      
      const AUTOCOMPLETE_DEBOUNCE = 200;
      const suggestionCache = new Map();
      
      let approverCount = 0;
      
      const formatDisplayLabel = (member) => {
        if (!member) return '';
        const { name, memberId, deptName } = member;
        if (name && memberId) {
          return deptName ? `${name} (${memberId}) · ${deptName}` : `${name} (${memberId})`;
        }
        return memberId || name || '';
      };
      
      const resetAutocomplete = () => {
        displayInput.value = '';
        hiddenInput.value = '';
        delete hiddenInput.dataset.name;
        delete hiddenInput.dataset.deptName;
        delete hiddenInput.dataset.position;
        delete hiddenInput.dataset.title;
        hideSuggestions();
      };
      
      const hideSuggestions = () => {
        suggestionsEl.hidden = true;
        suggestionsEl.innerHTML = '';
        delete suggestionsEl.dataset.items;
        delete suggestionsEl.dataset.activeIndex;
      };
      
      const renderSuggestions = (items, keyword) => {
        let html = '';
        if (items.length === 0) {
          html = `<div class="approval-member-empty">'${keyword}'에 대한 검색 결과가 없습니다.</div>`;
        } else {
          html = items.map(({ memberId, name, deptName, position }) => {
            return `
              <button type="button" class="approval-member-suggestion" data-approval-suggestion
                data-member-id="${memberId}"
                data-member-name="${name ?? ''}"
                data-dept-name="${deptName ?? ''}"
                data-position="${position ?? ''}"
              >
                <div><strong>${name ?? memberId}</strong><small>${memberId}</small></div>
                <div>${deptName ? `<small>${deptName}</small>` : ''}${position ? ` <small>${position}</small>` : ''}</div>
              </button>
            `;
          }).join('');
        }
        suggestionsEl.innerHTML = html;
        suggestionsEl.hidden = false;
        suggestionsEl.dataset.items = JSON.stringify(items);
        suggestionsEl.dataset.activeIndex = '-1';
      };
      
      const getCachedSuggestions = (keyword) => {
        const cacheKey = keyword.toLowerCase();
        const cached = suggestionCache.get(cacheKey);
        if (cached && Date.now() - cached.timestamp < 60_000) {
          return cached.data;
        }
        return null;
      };
      
      const fetchSuggestions = async (keyword, options = {}) => {
        const trimmed = keyword.trim();
        if (!trimmed) return [];
        const cached = getCachedSuggestions(trimmed);
        if (cached) return cached;
        
        const params = new URLSearchParams({ q: trimmed, size: '7' });
        const response = await fetch(`/api/members/approval-candidates?${params.toString()}`, {
          credentials: 'same-origin',
          signal: options.signal
        });
        if (!response.ok) {
          throw new Error(`Search failed: ${response.status}`);
        }
        const json = await response.json();
        const list = Array.isArray(json?.content) ? json.content : [];
        suggestionCache.set(trimmed.toLowerCase(), { data: list, timestamp: Date.now() });
        return list;
      };
      
      let debounceTimer = null;
      let activeFetch = null;
      
      const scheduleSearch = (value) => {
        if (debounceTimer) {
          window.clearTimeout(debounceTimer);
        }
        const trimmed = value.trim();
        if (!trimmed) {
          hideSuggestions();
          hiddenInput.value = '';
          return;
        }
        
        debounceTimer = window.setTimeout(async () => {
          if (activeFetch) {
            activeFetch.abort();
          }
          const controller = new AbortController();
          activeFetch = controller;
          try {
            const suggestions = await fetchSuggestions(trimmed, { signal: controller.signal });
            renderSuggestions(suggestions, trimmed);
          } catch (error) {
            if (error.name !== 'AbortError') {
              console.error('Member search error', error);
              hideSuggestions();
            }
          } finally {
            if (activeFetch === controller) {
              activeFetch = null;
            }
          }
        }, AUTOCOMPLETE_DEBOUNCE);
      };
      
      const selectCandidate = (candidate) => {
        hiddenInput.value = candidate.memberId;
        hiddenInput.dataset.name = candidate.name ?? '';
        hiddenInput.dataset.deptName = candidate.deptName ?? '';
        hiddenInput.dataset.position = candidate.position ?? '';
        displayInput.value = formatDisplayLabel(candidate);
        hideSuggestions();
      };
      
      const activateNextSuggestion = (direction) => {
        if (suggestionsEl.hidden) return;
        const items = JSON.parse(suggestionsEl.dataset.items ?? '[]');
        if (!items.length) return;
        let activeIndex = Number.parseInt(suggestionsEl.dataset.activeIndex ?? '-1', 10);
        activeIndex += direction;
        if (activeIndex < 0) activeIndex = items.length - 1;
        if (activeIndex >= items.length) activeIndex = 0;
        suggestionsEl.dataset.activeIndex = String(activeIndex);
        suggestionsEl.querySelectorAll('[data-approval-suggestion]').forEach((btn, idx) => {
          if (idx === activeIndex) {
            btn.classList.add('is-active');
            btn.focus();
          } else {
            btn.classList.remove('is-active');
          }
        });
      };
      
      displayInput.addEventListener('input', (event) => {
        hiddenInput.value = '';
        delete hiddenInput.dataset.name;
        delete hiddenInput.dataset.deptName;
        delete hiddenInput.dataset.position;
        scheduleSearch(event.target.value);
      });
      
      displayInput.addEventListener('focus', (event) => {
        const value = event.target.value.trim();
        if (value) {
          scheduleSearch(value);
        }
      });
      
      displayInput.addEventListener('blur', () => {
        window.setTimeout(() => hideSuggestions(), 150);
      });
      
      displayInput.addEventListener('keydown', (event) => {
        if (event.key === 'ArrowDown') {
          event.preventDefault();
          activateNextSuggestion(1);
        } else if (event.key === 'ArrowUp') {
          event.preventDefault();
          activateNextSuggestion(-1);
        } else if (event.key === 'Enter') {
          const items = JSON.parse(suggestionsEl.dataset.items ?? '[]');
          const activeIndex = Number.parseInt(suggestionsEl.dataset.activeIndex ?? '-1', 10);
          if (!suggestionsEl.hidden && items.length && activeIndex >= 0) {
            event.preventDefault();
            selectCandidate(items[activeIndex]);
          } else if (hiddenInput.value) {
            event.preventDefault();
            addApprover();
          }
        } else if (event.key === 'Escape') {
          hideSuggestions();
        }
      });
      
      suggestionsEl.addEventListener('mousedown', (event) => {
        const button = event.target.closest('[data-approval-suggestion]');
        if (!button) return;
        const candidate = {
          memberId: button.dataset.memberId,
          name: button.dataset.memberName,
          deptName: button.dataset.deptName,
          position: button.dataset.position
        };
        selectCandidate(candidate);
      });
      
      const renumber = () => {
        const items = Array.from(approverItems.querySelectorAll('.approver-item'));
        items.forEach((item, idx) => {
          const orderElement = item.querySelector('.order');
          if (orderElement) orderElement.textContent = String(idx + 1);
          item.querySelectorAll('input[type="hidden"]').forEach((input) => {
            input.name = input.name.replace(/steps\[\d+\]/, `steps[${idx}]`);
          });
          const stepNoInput = item.querySelector('input[name*=".stepNo"]');
          if (stepNoInput) stepNoInput.value = idx + 1;
        });
        approverCount = items.length;
      };
      
      const toggleEmptyMessage = (show) => {
        const emptyMessage = approverItems.querySelector('.empty-message');
        if (emptyMessage) {
          emptyMessage.style.display = show ? 'block' : 'none';
        }
      };
      
      const addApprover = () => {
        const memberId = hiddenInput.value.trim();
        const decision = decisionSelect.value;
        if (!memberId) {
          if (window.cmms?.notification) {
            window.cmms.notification.warning('결재자를 선택하세요.');
          } else {
            alert('결재자를 선택하세요.');
          }
          return;
        }
        
        const existing = Array.from(approverItems.querySelectorAll('.approver-item')).some(item =>
          item.dataset.memberId === memberId && item.dataset.decision === decision
        );
        if (existing) {
          if (window.cmms?.notification) {
            window.cmms.notification.warning('이미 추가된 결재자입니다.');
          } else {
            alert('이미 추가된 결재자입니다.');
          }
          return;
        }
        
        toggleEmptyMessage(false);
        const item = document.createElement('div');
        item.className = 'approver-item';
        item.dataset.memberId = memberId;
        item.dataset.decision = decision;

        const decisionText = decision === 'APPROVAL' ? '결재' : decision === 'AGREE' ? '합의' : '통보';
        const decisionClass = decision === 'APPROVAL' ? 'approval' : decision === 'AGREE' ? 'agree' : 'inform';
        const memberName = hiddenInput.dataset.name || memberId;
        const deptName = hiddenInput.dataset.deptName || '-';
        
        item.innerHTML = `
          <div class="order">${approverCount + 1}</div>
          <div class="member-id">${memberId}</div>
          <div class="member-name">${memberName}</div>
          <div class="member-dept">${deptName}</div>
          <div class="decision">
            <span class="decision-badge ${decisionClass}">${decisionText}</span>
          </div>
          <div class="actions">
            <button class="btn sm" type="button" data-move="up" title="위로">▲</button>
            <button class="btn sm" type="button" data-move="down" title="아래로">▼</button>
            <button class="btn sm danger" type="button" data-remove title="삭제">×</button>
          </div>
          <input type="hidden" name="steps[${approverCount}].memberId" value="${memberId}" />
          <input type="hidden" name="steps[${approverCount}].decision" value="${decision}" />
          <input type="hidden" name="steps[${approverCount}].stepNo" value="${approverCount + 1}" />
        `;
        
        approverItems.appendChild(item);
        approverCount++;
        resetAutocomplete();
      };
      
      addBtn.addEventListener('click', addApprover);
      
      // 결재선 관리 (삭제, 순서 변경)
      approverItems.addEventListener('click', (e) => {
        const btn = e.target.closest('button');
        if (!btn) return;
        const item = btn.closest('.approver-item');
        if (!item) return;
        
        if (btn.hasAttribute('data-remove')) {
          item.remove();
          approverCount--;
          renumber();
          if (!approverItems.querySelector('.approver-item')) {
            toggleEmptyMessage(true);
          }
          return;
        }
        
        if (btn.dataset.move === 'up' && item.previousElementSibling) {
          approverItems.insertBefore(item, item.previousElementSibling);
          renumber();
          return;
        }
        
        if (btn.dataset.move === 'down' && item.nextElementSibling) {
          approverItems.insertBefore(item.nextElementSibling, item);
          renumber();
          return;
        }
      });
      
      renumber();
      toggleEmptyMessage(!approverItems.querySelector('.approver-item'));
      approverItems.dataset.initialized = 'true';
    },
    
    // 인쇄 버튼 초기화 (통합 모듈 사용)
    initPrintButton: function(root) {
      window.cmms.printUtils.initPrintButton(root);
    },
    
    // 저장 버튼 초기화 (root 기반)
    initSaveButtons: function(root) {
      const form = root.querySelector('[data-form-manager]');
      if (!form) return;
      
      // 임시저장 버튼
      const draftBtn = root.querySelector('[data-save-draft]');
      if (draftBtn) {
        draftBtn.addEventListener('click', (e) => {
          e.preventDefault();
          this.submitForm(form, 'DRAFT');
        });
      }
      
      // 상신 버튼
      const submitBtn = root.querySelector('[data-save-submit]');
      if (submitBtn) {
        submitBtn.addEventListener('click', (e) => {
          e.preventDefault();
          this.submitForm(form, 'SUBMIT');
        });
      }
    },
    
    // 폼 제출 처리 함수
    submitForm: async function(form, status) {
      try {
        // Status 필드 설정
        let statusInput = form.querySelector('[name="status"]');
        if (!statusInput) {
          statusInput = document.createElement('input');
          statusInput.type = 'hidden';
          statusInput.name = 'status';
          form.appendChild(statusInput);
        }
        statusInput.value = status;
        
        // 기존 Form Manager 로직과 동일하게 처리
        const action = form.getAttribute('data-action');
        const method = form.getAttribute('data-method') || 'POST';
        
        // 파일 업로드 처리
        if (window.cmms?.fileUpload) {
          try {
            const fileGroupId = await window.cmms.fileUpload.uploadFormFiles(form);
            if (fileGroupId) {
              let fileGroupIdInput = form.querySelector('[name="fileGroupId"]');
              if (!fileGroupIdInput) {
                fileGroupIdInput = document.createElement('input');
                fileGroupIdInput.type = 'hidden';
                fileGroupIdInput.name = 'fileGroupId';
                form.appendChild(fileGroupIdInput);
              }
              fileGroupIdInput.value = fileGroupId;
            }
          } catch (uploadError) {
            console.error('File upload failed:', uploadError);
            if (window.cmms?.notification) {
              window.cmms.notification.error('파일 업로드에 실패했습니다.');
            }
            return;
          }
        }
        
        // FormData를 JSON으로 변환
        const formData = new FormData(form);
        const jsonData = this.formDataToJSON(formData);
        
        // API 호출
        const response = await fetch(action, {
          method: method,
          headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': window.cmms?.csrf?.readToken() || ''
          },
          body: JSON.stringify(jsonData)
        });
        
        if (!response.ok) {
          throw new Error('저장 실패');
        }
        
        const result = await response.json();
        
        // 성공 메시지
        if (window.cmms?.notification) {
          const message = status === 'DRAFT' ? '임시저장되었습니다.' : '상신되었습니다.';
          window.cmms.notification.success(message);
        }
        
        // 리다이렉트
        setTimeout(() => {
          window.cmms.navigation.navigate('/approval/list');
        }, 1000);
        
      } catch (error) {
        console.error('Form submit error:', error);
        if (window.cmms?.notification) {
          window.cmms.notification.error('저장 중 오류가 발생했습니다.');
        }
      }
    },
    
    // FormData를 JSON으로 변환하는 유틸리티 함수
    formDataToJSON: function(formData) {
      const json = {};
      for (let [key, value] of formData.entries()) {
        if (json[key]) {
          if (Array.isArray(json[key])) {
            json[key].push(value);
          } else {
            json[key] = [json[key], value];
          }
        } else {
          json[key] = value;
        }
      }
      return json;
    }
  });
  
  // 페이지별 초기화 등록 (root 기반 구조)
  window.cmms.pages.register('approval-list', function(root) {
    window.cmms.approval.initList(root);
  });
  
  window.cmms.pages.register('approval-detail', function(root) {
    window.cmms.approval.initDetail(root);
  });
  
  window.cmms.pages.register('approval-form', function(root) {
    window.cmms.approval.initForm(root);
  });
  
  // 참조 문서 팝업 함수 (네임스페이스)
  window.cmms.approval.openRefDocPopup = function(module, id) {
    // form.html에서 호출 시 파라미터 없이 호출될 수 있음
    if (!module || !id) {
      module = document.getElementById('refEntity')?.value;
      id = document.getElementById('refId')?.value;
    }
    
    if (!module || !id) {
      alert('참조 모듈과 ID를 입력하세요.');
      return;
    }
    
    const urls = {
      'INSP': `/inspection/detail/${id}?popup=true`,
      'WORK': `/workorder/detail/${id}?popup=true`,
      'WPER': `/workpermit/detail/${id}?popup=true`
    };
    
    if (!urls[module]) {
      alert('지원하지 않는 문서 타입입니다: ' + module);
      return;
    }
    
    const width = 1000;
    const height = 800;
    const left = (screen.width - width) / 2;
    const top = (screen.height - height) / 2;
    
    window.open(
      urls[module],
      '원본문서',
      `width=${width},height=${height},left=${left},top=${top},scrollbars=yes,resizable=yes`
    );
  };
  
  // 결재 승인 함수 (네임스페이스)
  window.cmms.approval.approve = function(approvalId, comment) {
    if (!approvalId) {
      alert('결재 ID를 찾을 수 없습니다.');
      return;
    }
    
    if (!confirm('승인하시겠습니까?')) {
      return;
    }
    
    // CSRF 토큰
    const csrfToken = document.querySelector('[name="_csrf"]')?.value;
    
    fetch(`/api/approvals/${approvalId}/approve`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
      },
      body: JSON.stringify({ comment: comment })
    })
    .then(response => {
      if (!response.ok) {
        throw new Error('승인 처리 중 오류가 발생했습니다.');
      }
      return response.json();
    })
    .then(data => {
      alert('승인되었습니다.');
      window.location.href = '/approval/list';
    })
    .catch(error => {
      console.error('승인 오류:', error);
      alert(error.message || '승인 처리 중 오류가 발생했습니다.');
    });
  };
  
  // 결재 반려 함수 (네임스페이스)
  window.cmms.approval.reject = function(approvalId, comment) {
    if (!approvalId) {
      alert('결재 ID를 찾을 수 없습니다.');
      return;
    }
    
    if (!comment) {
      alert('반려 사유를 입력해주세요.');
      const commentInput = document.getElementById('approval-comment');
      commentInput?.focus();
      return;
    }
    
    if (!confirm('반려하시겠습니까?')) {
      return;
    }
    
    // CSRF 토큰
    const csrfToken = document.querySelector('[name="_csrf"]')?.value;
    
    fetch(`/api/approvals/${approvalId}/reject`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
      },
      body: JSON.stringify({ comment: comment })
    })
    .then(response => {
      if (!response.ok) {
        throw new Error('반려 처리 중 오류가 발생했습니다.');
      }
      return response.json();
    })
    .then(data => {
      alert('반려되었습니다.');
      window.location.href = '/approval/list';
    })
    .catch(error => {
      console.error('반려 오류:', error);
      alert(error.message || '반려 처리 중 오류가 발생했습니다.');
    });
  };
  
  // ⭐ 하위 호환성을 위한 전역 함수 래퍼
  window.openRefDocPopup = window.cmms.approval.openRefDocPopup;
  window.approveWithComment = function() {
    const approvalId = event?.target?.dataset?.approvalId;
    const comment = document.getElementById('approval-comment')?.value?.trim() || '';
    window.cmms.approval.approve(approvalId, comment);
  };
  window.rejectWithComment = function() {
    const approvalId = event?.target?.dataset?.approvalId;
    const comment = document.getElementById('approval-comment')?.value?.trim() || '';
    window.cmms.approval.reject(approvalId, comment);
  };
  
})();
