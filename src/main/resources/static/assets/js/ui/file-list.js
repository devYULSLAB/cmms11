/**
 * 파일 목록 관리 모듈
 * 
 * 기존 common/FileList.js에서 파일 목록 기능을 추출한 모듈입니다.
 * - 파일 목록 표시
 * - 파일 관리
 * - 다운로드/삭제
 */

/**
 * 파일 목록 모듈 초기화 함수
 */
export function initFileList() {
  // 기존 window.cmms.fileList 호환성 유지
  window.cmms = window.cmms || {};
  window.cmms.fileList = {
    
    /**
     * 파일 목록 컨테이너 초기화
     * @param {HTMLElement|Document} container - 초기화할 컨테이너
     */
    initializeContainers: function(container) {
      const containers = container.querySelectorAll('[data-file-list]');
      containers.forEach(container => {
        this.initFileListContainer(container);
      });
    },
    
    /**
     * 개별 파일 목록 컨테이너 초기화
     * @param {HTMLElement} container - 파일 목록 컨테이너
     */
    initFileListContainer: function(container) {
      if (container.__fileListInitialized) return;
      container.__fileListInitialized = true;
      
      // 파일 목록 로드
      this.loadFileList(container);
      
      // 새로고침 버튼 이벤트
      const refreshBtn = container.querySelector('[data-refresh-file-list]');
      if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
          this.loadFileList(container);
        });
      }
    },
    
    /**
     * 파일 목록 로드
     * @param {HTMLElement} container - 파일 목록 컨테이너
     */
    loadFileList: async function(container) {
      const endpoint = container.getAttribute('data-file-list-endpoint');
      if (!endpoint) return;
      
      try {
        const response = await fetch(endpoint, {
          credentials: 'same-origin'
        });
        
        if (!response.ok) {
          throw new Error(`Failed to load file list: ${response.status}`);
        }
        
        const files = await response.json();
        this.displayFileList(container, files);
        
      } catch (error) {
        console.error('File list load error:', error);
        this.displayError(container, '파일 목록을 불러오는데 실패했습니다.');
      }
    },
    
    /**
     * 파일 목록 표시
     * @param {HTMLElement} container - 파일 목록 컨테이너
     * @param {Array} files - 파일 배열
     */
    displayFileList: function(container, files) {
      const fileListElement = container.querySelector('.file-list');
      if (!fileListElement) return;
      
      if (files.length === 0) {
        fileListElement.innerHTML = '<p class="text-muted">등록된 파일이 없습니다.</p>';
        return;
      }
      
      const fileItems = files.map(file => this.createFileItem(file)).join('');
      fileListElement.innerHTML = fileItems;
      
      // 파일 항목 이벤트 바인딩
      this.bindFileItemEvents(container);
    },
    
    /**
     * 파일 항목 생성
     * @param {Object} file - 파일 정보
     * @returns {string} 파일 항목 HTML
     */
    createFileItem: function(file) {
      const fileSize = this.formatFileSize(file.size);
      const uploadDate = this.formatDate(file.uploadDate);
      
      return `
        <div class="file-item border rounded p-3 mb-2" data-file-id="${file.id}">
          <div class="d-flex justify-content-between align-items-start">
            <div class="file-info flex-grow-1">
              <div class="file-name fw-bold">${file.originalName}</div>
              <div class="file-meta text-muted small">
                <span class="file-size">${fileSize}</span>
                <span class="mx-2">•</span>
                <span class="upload-date">${uploadDate}</span>
                ${file.description ? `<div class="file-description mt-1">${file.description}</div>` : ''}
              </div>
            </div>
            <div class="file-actions">
              ${this.createFileActions(file)}
            </div>
          </div>
        </div>
      `;
    },
    
    /**
     * 파일 액션 버튼 생성
     * @param {Object} file - 파일 정보
     * @returns {string} 액션 버튼 HTML
     */
    createFileActions: function(file) {
      const actions = [];
      
      // 다운로드 버튼
      actions.push(`
        <button class="btn btn-sm btn-outline-primary" 
                data-action="download" 
                data-file-id="${file.id}"
                title="다운로드">
          📥
        </button>
      `);
      
      // 미리보기 버튼 (이미지 파일인 경우)
      if (this.isImageFile(file.originalName)) {
        actions.push(`
          <button class="btn btn-sm btn-outline-info" 
                  data-action="preview" 
                  data-file-id="${file.id}"
                  data-file-url="${file.url}"
                  title="미리보기">
            👁️
          </button>
        `);
      }
      
      // 삭제 버튼
      actions.push(`
        <button class="btn btn-sm btn-outline-danger" 
                data-action="delete" 
                data-file-id="${file.id}"
                title="삭제">
          🗑️
        </button>
      `);
      
      return actions.join(' ');
    },
    
    /**
     * 파일 항목 이벤트 바인딩
     * @param {HTMLElement} container - 파일 목록 컨테이너
     */
    bindFileItemEvents: function(container) {
      const fileItems = container.querySelectorAll('.file-item');
      
      fileItems.forEach(item => {
        const buttons = item.querySelectorAll('[data-action]');
        
        buttons.forEach(button => {
          button.addEventListener('click', (e) => {
            e.preventDefault();
            
            const action = button.getAttribute('data-action');
            const fileId = button.getAttribute('data-file-id');
            
            switch (action) {
              case 'download':
                this.downloadFile(fileId);
                break;
              case 'preview':
                const fileUrl = button.getAttribute('data-file-url');
                this.previewFile(fileUrl);
                break;
              case 'delete':
                this.deleteFile(fileId, item);
                break;
            }
          });
        });
      });
    },
    
    /**
     * 파일 다운로드
     * @param {string} fileId - 파일 ID
     */
    downloadFile: function(fileId) {
      const downloadUrl = `/api/files/download/${fileId}`;
      
      // 새 창에서 다운로드
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = '';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    },
    
    /**
     * 파일 미리보기
     * @param {string} fileUrl - 파일 URL
     */
    previewFile: function(fileUrl) {
      // 모달로 이미지 미리보기
      const modal = document.createElement('div');
      modal.className = 'modal fade';
      modal.setAttribute('tabindex', '-1');
      modal.innerHTML = `
        <div class="modal-dialog modal-lg">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">파일 미리보기</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body text-center">
              <img src="${fileUrl}" class="img-fluid" alt="파일 미리보기">
            </div>
          </div>
        </div>
      `;
      
      document.body.appendChild(modal);
      
      if (window.bootstrap && window.bootstrap.Modal) {
        const bsModal = new window.bootstrap.Modal(modal);
        bsModal.show();
        
        modal.addEventListener('hidden.bs.modal', () => {
          modal.remove();
        });
      } else {
        // Fallback: 직접 표시
        modal.style.display = 'block';
        modal.classList.add('show');
        
        const closeBtn = modal.querySelector('.btn-close');
        closeBtn.addEventListener('click', () => {
          modal.remove();
        });
        
        modal.addEventListener('click', (e) => {
          if (e.target === modal) {
            modal.remove();
          }
        });
      }
    },
    
    /**
     * 파일 삭제
     * @param {string} fileId - 파일 ID
     * @param {HTMLElement} fileItem - 파일 항목 요소
     */
    deleteFile: async function(fileId, fileItem) {
      // 삭제 확인
      if (window.cmms && window.cmms.common && window.cmms.common.ConfirmDialog) {
        const confirmed = await window.cmms.common.ConfirmDialog.delete('정말로 이 파일을 삭제하시겠습니까?');
        if (!confirmed) return;
      } else {
        if (!confirm('정말로 이 파일을 삭제하시겠습니까?')) {
          return;
        }
      }
      
      try {
        const response = await fetch(`/api/files/delete/${fileId}`, {
          method: 'DELETE',
          credentials: 'same-origin'
        });
        
        if (!response.ok) {
          throw new Error(`Delete failed: ${response.status}`);
        }
        
        // 파일 항목 제거
        fileItem.remove();
        
        // 성공 알림
        if (window.cmms && window.cmms.notification) {
          window.cmms.notification.success('파일이 삭제되었습니다.');
        }
        
      } catch (error) {
        console.error('File delete error:', error);
        
        // 에러 알림
        if (window.cmms && window.cmms.notification) {
          window.cmms.notification.error('파일 삭제에 실패했습니다.');
        }
      }
    },
    
    /**
     * 에러 표시
     * @param {HTMLElement} container - 컨테이너
     * @param {string} message - 에러 메시지
     */
    displayError: function(container, message) {
      const fileListElement = container.querySelector('.file-list');
      if (fileListElement) {
        fileListElement.innerHTML = `
          <div class="alert alert-danger">
            <i class="fas fa-exclamation-triangle"></i>
            ${message}
          </div>
        `;
      }
    },
    
    /**
     * 이미지 파일 여부 확인
     * @param {string} filename - 파일명
     * @returns {boolean} 이미지 파일 여부
     */
    isImageFile: function(filename) {
      const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'];
      const extension = filename.split('.').pop().toLowerCase();
      return imageExtensions.includes(extension);
    },
    
    /**
     * 파일 크기 포맷팅
     * @param {number} bytes - 바이트 크기
     * @returns {string} 포맷된 파일 크기
     */
    formatFileSize: function(bytes) {
      if (window.cmms && window.cmms.utils && window.cmms.utils.formatFileSize) {
        return window.cmms.utils.formatFileSize(bytes);
      }
      
      if (!bytes) return '0 Bytes';
      const k = 1024;
      const sizes = ['Bytes', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      const size = bytes / Math.pow(k, i);
      return `${parseFloat(size.toFixed(2))} ${sizes[i]}`;
    },
    
    /**
     * 날짜 포맷팅
     * @param {string|Date} date - 날짜
     * @returns {string} 포맷된 날짜
     */
    formatDate: function(date) {
      if (!date) return '';
      
      const d = new Date(date);
      return d.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    }
  };
}
