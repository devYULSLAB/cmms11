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
      
      // ⭐ DOM 기반 중복 초기화 방지
      if (root.dataset.approvalListInit === 'true') {
        console.log('Approval list already initialized, skipping');
        return;
      }
      root.dataset.approvalListInit = 'true';
      
      this.initPagination(root);
      this.initSearch(root);
      this.initResetForm(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('Approval detail page initialized', root);
      
      // ⭐ DOM 기반 중복 초기화 방지
      if (root.dataset.approvalDetailInit === 'true') {
        console.log('Approval detail already initialized, skipping');
        return;
      }
      root.dataset.approvalDetailInit = 'true';
      
      this.initApprovalActions(root);
      this.initPrintButton(root);
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Approval form page initialized', root);
      
      // ⭐ DOM 기반 중복 초기화 방지
      if (root.dataset.approvalFormInit === 'true') {
        console.log('Approval form already initialized, skipping');
        return;
      }
      root.dataset.approvalFormInit = 'true';
      
      this.initEditor(root);
      this.initApproverManagement(root);
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
      const memberIdInput = root.querySelector('#member-id-input');
      const decisionSelect = root.querySelector('#decision-select');
      const addBtn = root.querySelector('#add-approver');
      const orgPickerBtn = root.querySelector('#btn-org-picker');
      
      if (!approverItems || !memberIdInput || !decisionSelect || !addBtn) return;
      
      // 중복 초기화 방지
      if (approverItems.dataset.initialized === 'true') {
        console.log('Approver management already initialized');
        return;
      }
      
      let approverCount = 0;
      
      // 사용자 정보 조회 (API 호출)
      const fetchMemberInfo = async (memberId) => {
        try {
          const result = await window.cmms.common.DataLoader.load(`/api/members/${memberId}`);
          return {
            name: result.name || '알 수 없음',
            deptId: result.deptId || 'N/A',
            position: result.position || '',
            title: result.title || ''
          };
        } catch (error) {
          console.error('Member fetch error:', error);
          return { name: '알 수 없음', deptId: 'N/A', position: '', title: '' };
        }
      };
      
      // 순서 재정렬
      const renumber = () => {
        const items = Array.from(approverItems.querySelectorAll('.approver-item'));
        
        items.forEach((item, idx) => {
          // 1. 화면 번호 업데이트
          const orderElement = item.querySelector('.order');
          if (orderElement) orderElement.textContent = String(idx + 1);
          
          // 2. ⭐ hidden 필드 이름 재정렬
          const hiddenInputs = item.querySelectorAll('input[type="hidden"]');
          hiddenInputs.forEach(input => {
            const name = input.name;
            // steps[X].field → steps[idx].field
            input.name = name.replace(/steps\[\d+\]/, `steps[${idx}]`);
          });
          
          // 3. stepNo 값도 업데이트
          const stepNoInput = item.querySelector('input[name*=".stepNo"]');
          if (stepNoInput) stepNoInput.value = idx + 1;
        });
        
        // 4. ⭐ approverCount 재계산
        approverCount = items.length;
      };
      
      // 빈 메시지 표시/숨김 관리
      const toggleEmptyMessage = (show) => {
        const emptyMessage = approverItems.querySelector('.empty-message');
        if (emptyMessage) {
          emptyMessage.style.display = show ? 'block' : 'none';
        }
      };
      
      // 결재자 추가
      const addApprover = async () => {
        const memberId = memberIdInput.value.trim();
        const decision = decisionSelect.value;
        
        if (!memberId) {
          if (window.cmms?.notification) {
            window.cmms.notification.warning('결재자 ID를 입력하세요.');
          } else {
            alert('결재자 ID를 입력하세요.');
          }
          return;
        }
        
        // 중복 방지 (같은 사용자+구분)
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
        
        // 사용자 정보 조회
        const memberInfo = await fetchMemberInfo(memberId);
        
        toggleEmptyMessage(false); // 빈 메시지 숨기기
        // root 기반 DOM 생성
        const item = root.createElement ? root.createElement('div') : document.createElement('div');
        item.className = 'approver-item';
        item.dataset.memberId = memberId;
        item.dataset.decision = decision;
        
        const decisionText = decision === 'APPROVAL' ? '결재' : decision === 'AGREE' ? '합의' : '통보';
        const decisionClass = decision === 'APPROVAL' ? 'approval' : decision === 'AGREE' ? 'agree' : 'inform';
        
        item.innerHTML = `
          <div class="order">${approverCount + 1}</div>
          <div class="member-id">${memberId}</div>
          <div class="member-name">${memberInfo.name || '-'}</div>
          <div class="member-dept">${memberInfo.deptId || '-'}</div>
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
        memberIdInput.value = ''; // 입력 필드 초기화
      };
      
      // 결재자 추가 버튼
      addBtn.addEventListener('click', addApprover);
      
      // Enter 키로 추가
      memberIdInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          addApprover();
        }
      });
      
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
          // 결재자가 없으면 빈 메시지 표시
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
      
      // 조직도 팝업 열기
      if (orgPickerBtn) {
        orgPickerBtn.addEventListener('click', () => {
          const width = 900;
          const height = 600;
          const left = (screen.width - width) / 2;
          const top = (screen.height - height) / 2;
          
          window.open(
            '/common/org-picker',
            'orgPicker',
            `width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=yes`
          );
        });
      }
      
      // 팝업에서 조직원 선택 시 호출되는 콜백 (네임스페이스 사용)
      window.cmms.approval.onMemberSelected = async function(member) {
        // 멤버 ID를 입력 필드에 설정
        memberIdInput.value = member.memberId;
        
        // 자동으로 결재선에 추가
        const decision = decisionSelect.value;
        
        // 중복 방지
        const existing = Array.from(approverItems.querySelectorAll('.approver-item')).some(item => 
          item.dataset.memberId === member.memberId && item.dataset.decision === decision
        );
        if (existing) {
          if (window.cmms?.notification) {
            window.cmms.notification.warning('이미 추가된 결재자입니다.');
          } else {
            alert('이미 추가된 결재자입니다.');
          }
          return;
        }
        
        toggleEmptyMessage(false); // 빈 메시지 숨기기
        // root 기반 DOM 생성
        const item = root.createElement ? root.createElement('div') : document.createElement('div');
        item.className = 'approver-item';
        item.dataset.memberId = member.memberId;
        item.dataset.decision = decision;
        
        const decisionText = decision === 'APPROVAL' ? '결재' : decision === 'AGREE' ? '합의' : '통보';
        const decisionClass = decision === 'APPROVAL' ? 'approval' : decision === 'AGREE' ? 'agree' : 'inform';
        
        item.innerHTML = `
          <div class="order">${approverCount + 1}</div>
          <div class="member-id">${member.memberId}</div>
          <div class="member-name">${member.name || '-'}</div>
          <div class="member-dept">${member.deptName || '-'}</div>
          <div class="decision">
            <span class="decision-badge ${decisionClass}">${decisionText}</span>
          </div>
          <div class="actions">
            <button class="btn sm" type="button" data-move="up" title="위로">▲</button>
            <button class="btn sm" type="button" data-move="down" title="아래로">▼</button>
            <button class="btn sm danger" type="button" data-remove title="삭제">×</button>
          </div>
          <input type="hidden" name="steps[${approverCount}].memberId" value="${member.memberId}" />
          <input type="hidden" name="steps[${approverCount}].decision" value="${decision}" />
          <input type="hidden" name="steps[${approverCount}].stepNo" value="${approverCount + 1}" />
        `;
        
        approverItems.appendChild(item);
        approverCount++;
        memberIdInput.value = ''; // 입력 필드 초기화
      };
      
      // HTML에 이미 빈 메시지가 있으므로 별도 초기화 불필요
      
      approverItems.dataset.initialized = 'true';
    },
    
    // 인쇄 버튼 초기화 (통합 모듈 사용)
    initPrintButton: function(root) {
      window.cmms.printUtils.initPrintButton(root);
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