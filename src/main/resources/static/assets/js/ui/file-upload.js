/**
 * 파일 업로드 모듈
 * 
 * 기존 common/fileUpload.js에서 파일 업로드 기능을 추출한 모듈입니다.
 * - 파일 업로드 위젯
 * - 드래그 앤 드롭
 * - 파일 검증
 */

/**
 * 파일 업로드 모듈 초기화 함수
 */
export function initFileUpload() {
  // 기존 window.cmms.fileUpload 호환성 유지
  window.cmms = window.cmms || {};
  window.cmms.fileUpload = {
    
    /**
     * 파일 업로드 컨테이너 초기화
     * @param {HTMLElement|Document} container - 초기화할 컨테이너
     */
    initializeContainers: function(container) {
      // data-file-upload 속성을 가진 컨테이너 처리
      const containers = container.querySelectorAll('[data-file-upload]');
      containers.forEach(container => {
        this.initFileUploadContainer(container);
      });
    },
    
    /**
     * 개별 파일 업로드 컨테이너 초기화
     * @param {HTMLElement} container - 파일 업로드 컨테이너
     */
    initFileUploadContainer: function(container) {
      if (container.__fileUploadInitialized) return;
      container.__fileUploadInitialized = true;
      
      const input = container.querySelector('input[type="file"]');
      if (!input) return;
      
      // 드래그 앤 드롭 이벤트 설정
      container.addEventListener('dragover', (e) => {
        e.preventDefault();
        container.classList.add('dragover');
      });
      
      container.addEventListener('dragleave', (e) => {
        e.preventDefault();
        container.classList.remove('dragover');
      });
      
      container.addEventListener('drop', (e) => {
        e.preventDefault();
        container.classList.remove('dragover');
        
        const files = Array.from(e.dataTransfer.files);
        this.handleFiles(files, input);
      });
      
      // 파일 선택 이벤트
      input.addEventListener('change', (e) => {
        const files = Array.from(e.target.files);
        this.handleFiles(files, input);
      });

      // data-attachments-add 버튼 처리 (기존 호환성)
      const addButton = container.querySelector('[data-attachments-add]');
      if (addButton) {
        addButton.addEventListener('click', () => {
          input.click();
        });
      }
    },
    
    /**
     * 파일 처리
     * @param {Array} files - 처리할 파일 배열
     * @param {HTMLInputElement} input - 파일 입력 요소
     */
    handleFiles: function(files, input) {
      const validFiles = this.validateFiles(files);
      
      if (validFiles.length > 0) {
        this.displaySelectedFiles(validFiles, input);
        this.updateFileInput(validFiles, input);
      }
    },
    
    /**
     * 파일 유효성 검사
     * @param {Array} files - 검사할 파일 배열
     * @returns {Array} 유효한 파일 배열
     */
    validateFiles: function(files) {
      const config = this.getUploadConfig();
      const validFiles = [];
      
      files.forEach(file => {
        // 파일 크기 검사
        if (file.size > config.maxSize) {
          if (window.cmms && window.cmms.notification) {
            window.cmms.notification.error(`파일 크기가 너무 큽니다. 최대 ${config.maxSizeFormatted}까지 업로드 가능합니다.`);
          }
          return;
        }
        
        // 파일 확장자 검사
        const extension = file.name.split('.').pop().toLowerCase();
        if (!config.allowedExtensions.includes(extension)) {
          if (window.cmms && window.cmms.notification) {
            window.cmms.notification.error(`지원하지 않는 파일 형식입니다. 허용된 형식: ${config.allowedExtensions.join(', ')}`);
          }
          return;
        }
        
        validFiles.push(file);
      });
      
      return validFiles;
    },
    
    /**
     * 선택된 파일 목록 표시
     * @param {Array} files - 표시할 파일 배열
     * @param {HTMLInputElement} input - 파일 입력 요소
     */
    displaySelectedFiles: function(files, input) {
      // data-file-upload 컨테이너 찾기
      const container = input.closest('[data-file-upload]');
      if (!container) {
        console.warn('파일 업로드 컨테이너를 찾을 수 없습니다');
        return;
      }
      
      // .file-list 목록 찾기
      let fileList = container.querySelector('.file-list');
      
      if (!fileList) {
        fileList = document.createElement('ul');
        fileList.className = 'file-list list-unstyled';
        container.appendChild(fileList);
      }
      
      files.forEach(file => {
        const listItem = this.createFileListItem(file);
        fileList.appendChild(listItem);
      });
    },
    
    /**
     * 파일 목록 항목 생성
     * @param {File} file - 파일 객체
     * @returns {HTMLElement} 파일 목록 항목 요소
     */
    createFileListItem: function(file) {
      const li = document.createElement('li');
      li.className = 'file-item d-flex justify-content-between align-items-center mb-2 p-2 border rounded';
      
      const fileInfo = document.createElement('div');
      fileInfo.innerHTML = `
        <strong>${file.name}</strong>
        <small class="text-muted d-block">${this.formatFileSize(file.size)}</small>
      `;
      
      const removeBtn = document.createElement('button');
      removeBtn.type = 'button';
      removeBtn.className = 'btn btn-sm btn-outline-danger';
      removeBtn.innerHTML = '×';
      removeBtn.addEventListener('click', () => {
        li.remove();
      });
      
      li.appendChild(fileInfo);
      li.appendChild(removeBtn);
      
      return li;
    },
    
    /**
     * 파일 입력 요소 업데이트
     * @param {Array} files - 업데이트할 파일 배열
     * @param {HTMLInputElement} input - 파일 입력 요소
     */
    updateFileInput: function(files, input) {
      // DataTransfer 객체를 사용하여 파일 목록 업데이트
      const dataTransfer = new DataTransfer();
      files.forEach(file => {
        dataTransfer.items.add(file);
      });
      input.files = dataTransfer.files;
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
     * 업로드 설정 가져오기
     * @returns {Object} 업로드 설정
     */
    getUploadConfig: function() {
      return window.fileUploadConfig || {
        maxSize: 10485760, // 10MB
        allowedExtensions: ['jpg', 'jpeg', 'png', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'hwp', 'hwpx', 'zip', 'txt'],
        maxSizeFormatted: '10MB',
        profile: 'default'
      };
    },
    
    /**
     * 파일 업로드
     * @param {Array} files - 업로드할 파일 배열
     * @param {string} endpoint - 업로드 엔드포인트
     * @param {Object} options - 업로드 옵션
     * @returns {Promise} 업로드 Promise
     */
    uploadFiles: async function(files, endpoint, options = {}) {
      const config = Object.assign({
        showProgress: true,
        onProgress: null,
        onSuccess: null,
        onError: null
      }, options);
      
      try {
        const formData = new FormData();
        
        // 파일 추가
        files.forEach(file => {
          formData.append('files', file);
        });
        
        // 추가 데이터 추가
        if (options.data) {
          Object.keys(options.data).forEach(key => {
            formData.append(key, options.data[key]);
          });
        }
        
        // 업로드 진행률 표시
        if (config.showProgress) {
          if (window.cmms && window.cmms.notification) {
            window.cmms.notification.info('파일을 업로드하는 중...');
          }
        }
        
        const response = await fetch(endpoint, {
          method: 'POST',
          body: formData,
          credentials: 'same-origin'
        });
        
        if (!response.ok) {
          throw new Error(`Upload failed: ${response.status}`);
        }
        
        const result = await response.json();
        
        // 성공 콜백
        if (typeof config.onSuccess === 'function') {
          config.onSuccess(result);
        }
        
        // 성공 알림
        if (window.cmms && window.cmms.notification) {
          window.cmms.notification.success('파일 업로드가 완료되었습니다.');
        }
        
        return result;
        
      } catch (error) {
        console.error('File upload error:', error);
        
        // 에러 콜백
        if (typeof config.onError === 'function') {
          config.onError(error);
        }
        
        // 에러 알림
        if (window.cmms && window.cmms.notification) {
          window.cmms.notification.error('파일 업로드에 실패했습니다.');
        }
        
        throw error;
      }
    }
  };
}
