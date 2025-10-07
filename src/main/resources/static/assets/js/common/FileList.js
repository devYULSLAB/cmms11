/**
 * FileList Widget - 첨부파일 목록 표시 위젯
 * 
 * 사용법:
 * <div data-file-list 
 *      data-file-group-id="F250927001"
 *      data-empty-text="첨부된 파일이 없습니다."
 *      data-loading-text="첨부 파일을 불러오는 중..."
 *      data-error-text="첨부 파일을 불러올 수 없습니다.">
 * </div>
 */

window.cmms.fileList = {
  /**
   * 파일 리스트 위젯 초기화
   * @param {HTMLElement} element - data-file-list 속성이 있는 DOM 요소
   */
  init: function(element) {
    if (!element || !element.dataset.fileGroupId) {
      console.warn('FileList: fileGroupId가 없습니다.');
      return;
    }

    const fileGroupId = element.dataset.fileGroupId;
    const emptyText = element.dataset.emptyText || '첨부된 파일이 없습니다.';
    const loadingText = element.dataset.loadingText || '첨부 파일을 불러오는 중...';
    const errorText = element.dataset.errorText || '첨부 파일을 불러올 수 없습니다.';

    // 로딩 상태 표시
    this.showLoading(element, loadingText);

    // 파일 목록 로드
    this.loadFiles(fileGroupId, element, emptyText, errorText);
  },

  /**
   * 로딩 상태 표시
   * @param {HTMLElement} element - DOM 요소
   * @param {string} loadingText - 로딩 메시지
   */
  showLoading: function(element, loadingText) {
    element.innerHTML = `<div class="loading">${loadingText}</div>`;
  },

  /**
   * 파일 목록 로드
   * @param {string} fileGroupId - 파일 그룹 ID
   * @param {HTMLElement} element - DOM 요소
   * @param {string} emptyText - 빈 상태 메시지
   * @param {string} errorText - 에러 메시지
   */
  loadFiles: async function(fileGroupId, element, emptyText, errorText) {
    try {
      const response = await fetch(`/api/files?groupId=${encodeURIComponent(fileGroupId)}`, { 
        credentials: 'same-origin' 
      });
      
      if (!response.ok) {
        throw new Error(`Failed to load files: ${response.status}`);
      }
      
      const result = await response.json();
      const files = Array.isArray(result?.items) ? result.items : [];

      if (files.length > 0) {
        this.renderFiles(files, element, fileGroupId);
      } else {
        this.renderEmpty(element, emptyText);
      }
    } catch (error) {
      console.error('FileList: 파일 로드 오류:', error);
      this.renderError(element, errorText);
    }
  },

  /**
   * 파일 목록 렌더링
   * @param {Array} files - 파일 목록
   * @param {HTMLElement} element - DOM 요소
   */
  renderFiles: function(files, element, fileGroupId) {
    const filesHtml = files.map(file => {
      const size = this.formatFileSize((file.size ?? file.fileSize) || 0);
      const groupParam = fileGroupId ? `?groupId=${encodeURIComponent(fileGroupId)}` : '';
      const downloadHref = `/api/files/${file.fileId}${groupParam}`;
      return `
      <li class="attachment-item">
        <div class="attachment-info">
          <span class="file-name">${file.originalName}</span>
          <span class="file-size">${size}</span>
        </div>
        <div class="actions">
          <a class="btn-download" href="${downloadHref}" download="${file.originalName}" data-hard-nav>다운로드</a>
        </div>
      </li>
      `;
    }).join('');

    element.innerHTML = `
      <ul class="attachments-list">
        ${filesHtml}
      </ul>
    `;
  },

  /**
   * 빈 상태 렌더링
   * @param {HTMLElement} element - DOM 요소
   * @param {string} emptyText - 빈 상태 메시지
   */
  renderEmpty: function(element, emptyText) {
    element.innerHTML = `<div class="notice">${emptyText}</div>`;
  },

  /**
   * 에러 상태 렌더링
   * @param {HTMLElement} element - DOM 요소
   * @param {string} errorText - 에러 메시지
   */
  renderError: function(element, errorText) {
    element.innerHTML = `<div class="notice danger">${errorText}</div>`;
  },

  /**
   * 지정된 루트 요소 내의 모든 파일 목록 요소를 초기화하는 함수
   * SPA 환경에서 동적으로 로드된 콘텐츠의 파일 목록을 초기화할 때 사용됩니다.
   * 중복 초기화를 방지하기 위해 __fileListInitialized 플래그를 사용합니다.
   * 
   * @param {Element|Document} root - 초기화할 범위의 루트 요소 (기본값: document)
   */
  initializeContainers: function(root = document) {
    const scope = root instanceof Element ? root : document;
    const fileListElements = scope.querySelectorAll('[data-file-list]');
    fileListElements.forEach(element => {
      // 중복 초기화 방지
      if (element.__fileListInitialized) {
        return;
      }
      this.init(element);
      element.__fileListInitialized = true;
    });
  },

  /**
   * 파일 크기 포맷팅
   * @param {number} bytes - 바이트 크기
   * @returns {string} - 포맷된 크기 문자열
   */
  formatFileSize: function(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
};

// DOM 로드 시 자동 초기화
document.addEventListener('DOMContentLoaded', () => {
  const fileListElements = document.querySelectorAll('[data-file-list]');
  fileListElements.forEach(element => {
    try {
      window.cmms.fileList.init(element);
    } catch (error) {
      console.error('FileList 초기화 실패:', error);
    }
  });
});
