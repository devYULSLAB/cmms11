/**
 * 확인 대화상자 모듈
 * 
 * - 표준화된 확인 대화상자
 * - 삭제 확인 대화상자
 * - 저장 확인 대화상자
 * 
 * @functions
 * - showConfirmDialog(message, options) - 표준화된 확인 다이얼로그
 * - confirmDelete(message) - 삭제 확인 대화상자
 * - confirmSave(message) - 저장 확인 대화상자
 * - confirmWarning(message) - 경고 확인 대화상자
 * - confirmInfo(message) - 정보 확인 대화상자
 * - showSelectDialog(message, options, config) - 선택 대화상자
 * - initConfirmDialog() - 확인 대화상자 모듈 초기화
 */

/**
 * 표준화된 확인 다이얼로그
 * @param {string} message - 확인 메시지
 * @param {Object} options - 옵션 설정
 * @returns {Promise<boolean>} 확인 결과
 */
export function showConfirmDialog(message, options = {}) {
  const config = Object.assign({
    title: '확인',
    confirmText: '확인',
    cancelText: '취소',
    type: 'warning' // success, warning, error
  }, options);
  
  return new Promise((resolve) => {
    if (config.useNative !== false && typeof confirm === 'function') {
      // 네이티브 확인창 사용
      const result = confirm(message);
      resolve(result);
    } else {
      // 커스텀 모달 구현
      const modal = document.createElement('div');
      modal.className = 'modal fade';
      modal.setAttribute('tabindex', '-1');
      modal.innerHTML = `
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">${config.title}</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
              ${message}
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">${config.cancelText}</button>
              <button type="button" class="btn btn-primary" id="confirm-btn">${config.confirmText}</button>
            </div>
          </div>
        </div>
      `;
      
      document.body.appendChild(modal);
      
      // Bootstrap Modal 사용 (있는 경우)
      if (window.bootstrap && window.bootstrap.Modal) {
        const bsModal = new window.bootstrap.Modal(modal);
        bsModal.show();
        
        const confirmBtn = modal.querySelector('#confirm-btn');
        const cancelBtn = modal.querySelector('.btn-secondary');
        
        confirmBtn.addEventListener('click', () => {
          bsModal.hide();
          resolve(true);
        });
        
        cancelBtn.addEventListener('click', () => {
          bsModal.hide();
          resolve(false);
        });
        
        modal.addEventListener('hidden.bs.modal', () => {
          modal.remove();
        });
      } else {
        // Fallback: 직접 표시
        modal.style.display = 'block';
        modal.classList.add('show');
        document.body.classList.add('modal-open');
        
        const confirmBtn = modal.querySelector('#confirm-btn');
        const cancelBtn = modal.querySelector('.btn-secondary');
        const closeBtn = modal.querySelector('.btn-close');
        
        const hideModal = () => {
          modal.style.display = 'none';
          modal.classList.remove('show');
          document.body.classList.remove('modal-open');
          modal.remove();
        };
        
        confirmBtn.addEventListener('click', () => {
          hideModal();
          resolve(true);
        });
        
        cancelBtn.addEventListener('click', () => {
          hideModal();
          resolve(false);
        });
        
        closeBtn.addEventListener('click', () => {
          hideModal();
          resolve(false);
        });
        
        // 배경 클릭으로 닫기
        modal.addEventListener('click', (e) => {
          if (e.target === modal) {
            hideModal();
            resolve(false);
          }
        });
      }
    }
  });
}

/**
 * 삭제 확인 대화상자
 * @param {string} message - 삭제 확인 메시지
 * @returns {Promise<boolean>} 삭제 확인 결과
 */
export function confirmDelete(message = '정말로 삭제하시겠습니까?') {
  return showConfirmDialog(message, {
    title: '삭제 확인',
    confirmText: '삭제',
    cancelText: '취소',
    type: 'error'
  });
}

/**
 * 저장 확인 대화상자
 * @param {string} message - 저장 확인 메시지
 * @returns {Promise<boolean>} 저장 확인 결과
 */
export function confirmSave(message = '저장하시겠습니까?') {
  return showConfirmDialog(message, {
    title: '저장 확인',
    confirmText: '저장',
    cancelText: '취소',
    type: 'success'
  });
}

/**
 * 경고 확인 대화상자
 * @param {string} message - 경고 메시지
 * @returns {Promise<boolean>} 확인 결과
 */
export function confirmWarning(message) {
  return showConfirmDialog(message, {
    title: '경고',
    confirmText: '확인',
    cancelText: '취소',
    type: 'warning'
  });
}

/**
 * 정보 확인 대화상자
 * @param {string} message - 정보 메시지
 * @returns {Promise<boolean>} 확인 결과
 */
export function confirmInfo(message) {
  return showConfirmDialog(message, {
    title: '정보',
    confirmText: '확인',
    cancelText: '취소',
    type: 'info'
  });
}

/**
 * 선택 대화상자 (여러 옵션 중 선택)
 * @param {string} message - 선택 메시지
 * @param {Array} options - 선택 옵션 배열
 * @param {Object} config - 설정
 * @returns {Promise<string|null>} 선택된 옵션 또는 null
 */
export function showSelectDialog(message, options = [], config = {}) {
  const defaultConfig = Object.assign({
    title: '선택',
    cancelText: '취소'
  }, config);
  
  return new Promise((resolve) => {
    const optionButtons = options.map(option => 
      `<button type="button" class="btn btn-outline-primary me-2 mb-2 option-btn" data-value="${option.value || option}">${option.label || option}</button>`
    ).join('');
    
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.setAttribute('tabindex', '-1');
    modal.innerHTML = `
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">${defaultConfig.title}</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <p>${message}</p>
            <div class="options-container">
              ${optionButtons}
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">${defaultConfig.cancelText}</button>
          </div>
        </div>
      </div>
    `;
    
    document.body.appendChild(modal);
    
    // Bootstrap Modal 사용 (있는 경우)
    if (window.bootstrap && window.bootstrap.Modal) {
      const bsModal = new window.bootstrap.Modal(modal);
      bsModal.show();
      
      const cancelBtn = modal.querySelector('.btn-secondary');
      
      // 옵션 버튼 이벤트
      modal.querySelectorAll('.option-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const value = btn.getAttribute('data-value');
          bsModal.hide();
          resolve(value);
        });
      });
      
      cancelBtn.addEventListener('click', () => {
        bsModal.hide();
        resolve(null);
      });
      
      modal.addEventListener('hidden.bs.modal', () => {
        modal.remove();
      });
    } else {
      // Fallback 구현
      modal.style.display = 'block';
      modal.classList.add('show');
      document.body.classList.add('modal-open');
      
      const hideModal = () => {
        modal.style.display = 'none';
        modal.classList.remove('show');
        document.body.classList.remove('modal-open');
        modal.remove();
      };
      
      // 옵션 버튼 이벤트
      modal.querySelectorAll('.option-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const value = btn.getAttribute('data-value');
          hideModal();
          resolve(value);
        });
      });
      
      modal.querySelector('.btn-secondary').addEventListener('click', () => {
        hideModal();
        resolve(null);
      });
    }
  });
}

/**
 * 확인 대화상자 모듈 초기화 함수
 */
export function initConfirmDialog() {
  // 기존 window.cmms.common.ConfirmDialog 호환성 유지
  window.cmms = window.cmms || {};
  window.cmms.common = window.cmms.common || {};
  window.cmms.common.ConfirmDialog = {
    show: showConfirmDialog,
    delete: confirmDelete,
    save: confirmSave,
    warning: confirmWarning,
    info: confirmInfo,
    select: showSelectDialog
  };
}
