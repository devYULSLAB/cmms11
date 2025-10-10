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
      
      // 새로고침 버튼 이벤트 (현재 사용 안 함)
      // const refreshBtn = container.querySelector('[data-refresh-file-list]');
      // if (refreshBtn) {
      //   refreshBtn.addEventListener('click', () => {
      //     this.loadFileList(container);
      //   });
      // }
    },
    
    /**
     * 파일 목록 로드
     * @param {HTMLElement} container - 파일 목록 컨테이너
     */
    loadFileList: async function(container) {
      // data-file-group-id에서 endpoint 생성
      const fileGroupId = container.getAttribute('data-file-group-id');
      if (!fileGroupId) return;
      
      const endpoint = `/api/files?groupId=${fileGroupId}`;
      
      try {
        const response = await fetch(endpoint, {
          credentials: 'same-origin'
        });
        
        if (!response.ok) {
          throw new Error(`Failed to load file list: ${response.status}`);
        }
        
        const result = await response.json();
        // API 응답이 { items: [...] } 형태이므로 items 추출
        const files = result.items || [];
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
        fileListElement.innerHTML = '<p class="notice">등록된 파일이 없습니다.</p>';
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
      // FileItemResponse에는 fileId 필드 사용
      const fileId = file.fileId || file.id;
      
      return `
        <div class="attachment-item" data-file-id="${fileId}">
          <div class="file-name">
            <strong>${file.originalName}</strong>
            <div class="file-size" style="margin-top: 4px;">
              ${fileSize}
            </div>
            ${file.description ? `<div style="margin-top: 4px; color: var(--muted);">${file.description}</div>` : ''}
          </div>
          <div class="file-actions">
            ${this.createFileActions(file)}
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
      // FileItemResponse에는 fileId 필드 사용
      const fileId = file.fileId || file.id;
      // 다운로드 버튼만 제공
      return `
        <button class="btn btn-download" 
                data-action="download" 
                data-file-id="${fileId}"
                title="다운로드">
          📥 다운로드
        </button>
      `;
    },
    
    /**
     * 파일 항목 이벤트 바인딩
     * @param {HTMLElement} container - 파일 목록 컨테이너
     */
    bindFileItemEvents: function(container) {
      const fileItems = container.querySelectorAll('.attachment-item');
      
      fileItems.forEach(item => {
        const buttons = item.querySelectorAll('[data-action]');
        
        buttons.forEach(button => {
          button.addEventListener('click', (e) => {
            e.preventDefault();
            
            const action = button.getAttribute('data-action');
            const fileId = button.getAttribute('data-file-id');
            
            if (action === 'download') {
              this.downloadFile(fileId, container);
            }
          });
        });
      });
    },
    
    /**
     * 파일 다운로드
     * @param {string} fileId - 파일 ID
     * @param {HTMLElement} container - 파일 목록 컨테이너
     */
    downloadFile: function(fileId, container) {
      // container에서 fileGroupId 가져오기
      const fileGroupId = container.getAttribute('data-file-group-id');
      
      if (!fileGroupId) {
        console.error('fileGroupId not found');
        if (window.cmms?.notification) {
          window.cmms.notification.error('파일 그룹 ID를 찾을 수 없습니다.');
        }
        return;
      }
      
      const downloadUrl = `/api/files/${fileId}?groupId=${fileGroupId}`;
      
      // 새 창에서 다운로드
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = '';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    },
    
    
    /**
     * 파일 삭제 (현재 사용 안 함 - 리스트 페이지에서는 삭제 기능 불필요)
     * @param {string} fileId - 파일 ID
     * @param {HTMLElement} fileItem - 파일 항목 요소
     */
    /*
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
    */
    
    /**
     * 에러 표시
     * @param {HTMLElement} container - 컨테이너
     * @param {string} message - 에러 메시지
     */
    displayError: function(container, message) {
      const fileListElement = container.querySelector('.file-list');
      if (fileListElement) {
        fileListElement.innerHTML = `
          <div class="notice" style="color: var(--danger); border-color: var(--danger);">
            ${message}
          </div>
        `;
      }
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
