/**
 * 알림 시스템 모듈
 * 
 * - 성공/에러/경고 알림
 * - 토스트 메시지
 * - 확인 대화상자
 * - 로딩 대화상자
 * 
 * @functions
 * - showNotification(message, type, duration) - 기본 알림 표시
 * - showSuccess(message, duration) - 성공 알림 표시
 * - showError(message, duration) - 에러 알림 표시
 * - showWarning(message, duration) - 경고 알림 표시
 * - showInfo(message, duration) - 정보 알림 표시
 * - showToast(message, type, duration) - 토스트 메시지 표시
 * - createToastContainer() - 토스트 컨테이너 생성 (내부 함수)
 * - showConfirmDialog(message, title, confirmText, cancelText) - 확인 대화상자 표시
 * - showLoadingDialog(message) - 로딩 대화상자 표시
 * - initNotification() - 알림 모듈 초기화
 */

/**
 * 기본 알림 표시 함수
 * @param {string} message - 알림 메시지
 * @param {string} type - 알림 타입 (success, error, warning, info)
 * @param {number} duration - 표시 시간 (밀리초)
 * @returns {HTMLElement} 생성된 알림 요소
 */
export function showNotification(message, type = 'info', duration = 3000) {
  // 기존 알림 제거
  const existing = document.querySelector('.cmms-notification');
  if (existing) {
    existing.remove();
  }

  // 알림 생성
  const notification = document.createElement('div');
  notification.className = `cmms-notification ${type}`;
  notification.textContent = message;
  notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    z-index: 9999;
    max-width: 400px;
    padding: 12px 16px;
    border-radius: 4px;
    color: white;
    font-weight: 500;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  `;
  
  // 타입별 알림 배경색 설정
  if (type === 'success') {
    notification.style.backgroundColor = '#10b981';
  } else if (type === 'error') {
    notification.style.backgroundColor = '#ef4444';
  } else if (type === 'warning') {
    notification.style.backgroundColor = '#f59e0b';
  } else {
    notification.style.backgroundColor = '#3b82f6';
  }
  
  document.body.appendChild(notification);

  // 자동 삭제
  setTimeout(() => {
    if (notification.parentNode) {
      notification.remove();
    }
  }, duration);

  return notification;
}

/**
 * 성공 알림 표시
 * @param {string} message - 알림 메시지
 * @param {number} duration - 표시 시간 (밀리초)
 * @returns {HTMLElement} 생성된 알림 요소
 */
export function showSuccess(message, duration = 3000) {
  return showNotification(message, 'success', duration);
}

/**
 * 에러 알림 표시
 * @param {string} message - 알림 메시지
 * @param {number} duration - 표시 시간 (밀리초)
 * @returns {HTMLElement} 생성된 알림 요소
 */
export function showError(message, duration = 5000) {
  return showNotification(message, 'error', duration);
}

/**
 * 경고 알림 표시
 * @param {string} message - 알림 메시지
 * @param {number} duration - 표시 시간 (밀리초)
 * @returns {HTMLElement} 생성된 알림 요소
 */
export function showWarning(message, duration = 4000) {
  return showNotification(message, 'warning', duration);
}

/**
 * 정보 알림 표시
 * @param {string} message - 알림 메시지
 * @param {number} duration - 표시 시간 (밀리초)
 * @returns {HTMLElement} 생성된 알림 요소
 */
export function showInfo(message, duration = 3000) {
  return showNotification(message, 'info', duration);
}

/**
 * 토스트 메시지 표시
 * @param {string} message - 토스트 메시지
 * @param {string} type - 토스트 타입 (success, error, warning, info)
 * @param {number} duration - 표시 시간 (밀리초)
 * @returns {HTMLElement} 생성된 토스트 요소
 */
export function showToast(message, type = 'info', duration = 3000) {
  const toast = document.createElement('div');
  toast.className = `toast align-items-center text-white bg-${type === 'error' ? 'danger' : type} border-0`;
  toast.setAttribute('role', 'alert');
  toast.setAttribute('aria-live', 'assertive');
  toast.setAttribute('aria-atomic', 'true');
  
  toast.innerHTML = `
    <div class="d-flex">
      <div class="toast-body">
        ${message}
      </div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
    </div>
  `;
  
  const toastContainer = document.querySelector('.toast-container') || createToastContainer();
  toastContainer.appendChild(toast);
  
  // Bootstrap Toast 사용 (있는 경우)
  if (window.bootstrap && window.bootstrap.Toast) {
    const bsToast = new window.bootstrap.Toast(toast, { delay: duration });
    bsToast.show();
    
    toast.addEventListener('hidden.bs.toast', () => {
      toast.remove();
    });
  } else {
    // Fallback: 자동 제거
    setTimeout(() => {
      if (toast.parentNode) {
        toast.remove();
      }
    }, duration);
  }
  
  return toast;
}

/**
 * 토스트 컨테이너 생성
 * @returns {HTMLElement} 생성된 토스트 컨테이너
 */
function createToastContainer() {
  const container = document.createElement('div');
  container.className = 'toast-container position-fixed top-0 end-0 p-3';
  container.style.zIndex = '9999';
  document.body.appendChild(container);
  return container;
}

/**
 * 확인 대화상자 표시
 * @param {string} message - 확인 메시지
 * @param {string} title - 대화상자 제목
 * @param {string} confirmText - 확인 버튼 텍스트
 * @param {string} cancelText - 취소 버튼 텍스트
 * @returns {Promise<boolean>} 확인 결과
 */
export function showConfirmDialog(message, title = '확인', confirmText = '확인', cancelText = '취소') {
  return new Promise((resolve) => {
    // Bootstrap Modal 사용 (있는 경우)
    if (window.bootstrap && window.bootstrap.Modal) {
      const modal = document.createElement('div');
      modal.className = 'modal fade';
      modal.setAttribute('tabindex', '-1');
      modal.innerHTML = `
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">${title}</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
              ${message}
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">${cancelText}</button>
              <button type="button" class="btn btn-primary" id="confirm-btn">${confirmText}</button>
            </div>
          </div>
        </div>
      `;
      
      document.body.appendChild(modal);
      
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
      // Fallback: 네이티브 confirm 사용
      const result = confirm(`${title}\n\n${message}`);
      resolve(result);
    }
  });
}

/**
 * 로딩 대화상자 표시
 * @param {string} message - 로딩 메시지
 * @returns {Object} 로딩 대화상자 객체 (hide 메서드 포함)
 */
export function showLoadingDialog(message = '처리 중입니다...') {
  const modal = document.createElement('div');
  modal.className = 'modal fade';
  modal.setAttribute('tabindex', '-1');
  modal.setAttribute('data-bs-backdrop', 'static');
  modal.setAttribute('data-bs-keyboard', 'false');
  modal.innerHTML = `
    <div class="modal-dialog modal-sm modal-dialog-centered">
      <div class="modal-content">
        <div class="modal-body text-center">
          <div class="spinner-border text-primary mb-3" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p class="mb-0">${message}</p>
        </div>
      </div>
    </div>
  `;
  
  document.body.appendChild(modal);
  
  let bsModal = null;
  if (window.bootstrap && window.bootstrap.Modal) {
    bsModal = new window.bootstrap.Modal(modal);
    bsModal.show();
  } else {
    // Fallback: 모달 표시
    modal.style.display = 'block';
    modal.classList.add('show');
  }
  
  return {
    hide: () => {
      if (bsModal) {
        bsModal.hide();
        modal.addEventListener('hidden.bs.modal', () => {
          modal.remove();
        });
      } else {
        modal.remove();
      }
    }
  };
}

/**
 * 알림 모듈 초기화 함수
 */
export function initNotification() {
  // 기존 window.cmms.notification 호환성 유지
  window.cmms = window.cmms || {};
  window.cmms.notification = {
    show: showNotification,
    success: showSuccess,
    error: showError,
    warning: showWarning,
    info: showInfo,
    toast: showToast,
    confirm: showConfirmDialog,
    loading: showLoadingDialog
  };
}
