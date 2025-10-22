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

      // 초기 상태 설정: 기존 파일 목록을 보관하고 UI를 동기화
      this.setSelectedFiles(container, Array.from(input.files || []));
      this.renderFileList(container, this.getSelectedFiles(container), input);
      
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
        this.handleFiles(files, input, container);
      });
      
      // 파일 선택 이벤트
      input.addEventListener('change', (e) => {
        const files = Array.from(e.target.files);
        this.handleFiles(files, input, container);
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
    handleFiles: function(files, input, container) {
      const validFiles = this.validateFiles(files);
      
      if (validFiles.length > 0) {
        // 기존 파일 목록과 새 파일들을 병합 후 중복 제거
        const existingFiles = this.getSelectedFiles(container);
        const mergedFiles = this.mergeFiles(existingFiles, validFiles);

        this.setSelectedFiles(container, mergedFiles);
        this.updateFileInput(mergedFiles, input);
        this.renderFileList(container, mergedFiles, input);
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
     * 컨테이너에 저장된 파일 목록을 반환
     * @param {HTMLElement} container - 파일 업로드 컨테이너
     * @returns {Array<File>} 선택된 파일 목록
     */
    getSelectedFiles: function(container) {
      if (!container.__selectedFiles) {
        container.__selectedFiles = [];
      }
      return container.__selectedFiles;
    },

    /**
     * 컨테이너에 파일 목록을 저장
     * @param {HTMLElement} container - 파일 업로드 컨테이너
     * @param {Array<File>} files - 저장할 파일 목록
     */
    setSelectedFiles: function(container, files) {
      container.__selectedFiles = files;
    },

    /**
     * 파일 목록 병합 및 중복 제거
     * @param {Array<File>} existingFiles - 기존 파일 목록
     * @param {Array<File>} newFiles - 새로 추가된 파일 목록
     * @returns {Array<File>} 병합된 파일 목록
     */
    mergeFiles: function(existingFiles, newFiles) {
      const fileMap = new Map();
      const addToMap = (file) => {
        fileMap.set(this.buildFileKey(file), file);
      };
      existingFiles.forEach(addToMap);
      newFiles.forEach(addToMap);
      return Array.from(fileMap.values());
    },

    /**
     * 파일 목록 렌더링
     * @param {HTMLElement} container - 파일 업로드 컨테이너
     * @param {Array<File>} files - 렌더링할 파일 목록
     * @param {HTMLInputElement} input - 파일 입력 요소
     */
    renderFileList: function(container, files, input) {
      let fileList = container.querySelector('.file-list');
      if (!fileList) {
        fileList = document.createElement('ul');
        fileList.className = 'file-list list-unstyled';
        container.appendChild(fileList);
      }

      fileList.innerHTML = '';

      if (!files.length) {
        const emptyItem = document.createElement('li');
        emptyItem.className = 'empty';
        emptyItem.textContent = '첨부된 파일이 없습니다.';
        fileList.appendChild(emptyItem);
        return;
      }

      files.forEach(file => {
        const listItem = this.createFileListItem(file, input, container);
        fileList.appendChild(listItem);
      });
    },
    
    /**
     * 파일 목록 항목 생성
     * @param {File} file - 파일 객체
     * @param {HTMLInputElement} input - 파일 입력 요소
     * @returns {HTMLElement} 파일 목록 항목 요소
     */
    createFileListItem: function(file, input, container) {
      const li = document.createElement('li');
      li.className = 'attachment-item';
      
      // 파일 객체 참조 저장
      li.__fileObject = file;
      
      // 파일명
      const fileName = document.createElement('div');
      fileName.className = 'file-name';
      fileName.textContent = file.name;
      
      // 파일 크기 (별도 요소로 분리)
      const fileSize = document.createElement('div');
      fileSize.className = 'file-size';
      fileSize.textContent = this.formatFileSize(file.size);
      
      // 삭제 버튼
      const removeBtn = document.createElement('button');
      removeBtn.type = 'button';
      removeBtn.className = 'btn btn-remove';
      removeBtn.innerHTML = '×';
      removeBtn.title = '삭제';
      removeBtn.addEventListener('click', () => {
        this.removeFileByKey(container, this.buildFileKey(file), input);
      });
      
      // Flexbox 레이아웃에 맞게 조립
      li.appendChild(fileName);
      li.appendChild(fileSize);
      li.appendChild(removeBtn);
      
      return li;
    },
    
    /**
     * 파일 제거
     * @param {HTMLElement} container - 파일 업로드 컨테이너
     * @param {string} fileKey - 제거할 파일 식별 키
     * @param {HTMLInputElement} input - 파일 입력 요소
     */
    removeFileByKey: function(container, fileKey, input) {
      const currentFiles = this.getSelectedFiles(container);
      const filteredFiles = currentFiles.filter(file => this.buildFileKey(file) !== fileKey);
      this.setSelectedFiles(container, filteredFiles);
      this.updateFileInput(filteredFiles, input);
      this.renderFileList(container, filteredFiles, input);
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
     * 파일 고유 키 생성
     * @param {File} file - 파일 객체
     * @returns {string} 고유 키
     */
    buildFileKey: function(file) {
      return [file.name, file.size, file.lastModified].join('::');
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
     * Form의 파일을 서버에 업로드
     * @param {HTMLFormElement} form - 대상 form
     * @returns {Promise<string|null>} fileGroupId 또는 null
     */
    uploadFormFiles: async function(form) {
      const fileUploadContainer = form.querySelector('[data-file-upload]');
      if (!fileUploadContainer) return null;
      
      const fileInput = fileUploadContainer.querySelector('input[type="file"]');
      if (!fileInput || fileInput.files.length === 0) return null;
      
      const formData = new FormData();
      Array.from(fileInput.files).forEach(file => {
        formData.append('files', file);
      });
      
      // refEntity, refId 추가 (있으면)
      const refEntityInput = form.querySelector('[name="refEntity"]');
      const refIdInput = form.querySelector('[name="refId"]');
      if (refEntityInput && refEntityInput.value) {
        formData.append('refEntity', refEntityInput.value);
      }
      if (refIdInput && refIdInput.value) {
        formData.append('refId', refIdInput.value);
      }
      
      return await this.uploadToServer(formData);
    },
    
    /**
     * 파일을 서버에 업로드
     * @param {FormData} formData - 업로드할 FormData
     * @returns {Promise<string>} fileGroupId
     */
    uploadToServer: async function(formData) {
      try {
        const response = await fetch('/api/files', {
          method: 'POST',
          body: formData,
          credentials: 'same-origin'
        });
        
        if (!response.ok) {
          throw new Error('File upload failed: ' + response.status);
        }
        
        const result = await response.json();
        return result.fileGroupId;
      } catch (error) {
        console.error('File upload error:', error);
        throw error;
      }
    }
  };
}
