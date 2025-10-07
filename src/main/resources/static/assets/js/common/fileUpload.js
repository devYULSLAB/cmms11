/**
 * CMMS 파일 업로드 모듈
 * 
 * 이 파일은 CMMS 애플리케이션의 파일 업로드 기능을 독립적으로 관리합니다.
 * app.js에서 분리된 모듈로, 파일 업로드 위젯의 모든 기능을 담당합니다.
 * 
 * 주요 기능:
 * - 파일 업로드 위젯 초기화 및 관리
 * - 서버 설정 기반 파일 검증 (크기, 확장자)
 * - 기존 파일 목록 로드 및 표시
 * - 파일 삭제 기능
 * - SPA 환경에서의 동적 초기화 지원
 * 
 * 사용법:
 * - window.cmms.fileUpload.init(container);
 * - window.cmms.fileUpload.initializeContainers(root);
 * 
 * 의존성:
 * - window.cmms.utils.formatFileSize (파일 크기 포맷팅)
 * - window.cmms.notification (알림 시스템)
 * - window.cmms.csrf (CSRF 토큰 관리)
 * 
 * SPA 지원:
 * - DOMContentLoaded 및 SPA 전환 시 자동 초기화
 * - 중복 초기화 방지 메커니즘 포함
 */

(function() {
  'use strict';

  // CMMS 네임스페이스 확인 및 생성
  if (!window.cmms) {
    window.cmms = {};
  }

  /**
   * 파일 업로드 모듈 객체
   * 모든 파일 업로드 관련 기능을 포함합니다.
   */
  const fileUploadModule = {
    /**
     * 파일 업로드 설정 정보
     * 서버에서 동적으로 로드되며, 기본값을 제공합니다.
     */
    config: {
      maxSize: 10 * 1024 * 1024, // 기본 최대 파일 크기 (10MB)
      allowedExtensions: ['jpg', 'jpeg', 'png', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'hwp', 'hwpx', 'zip', 'txt'], // 허용된 파일 확장자
      maxSizeFormatted: '10MB', // 사용자에게 표시할 최대 크기 문자열
      profile: 'default' // 현재 프로파일 (서버에서 설정)
    },

    /**
     * 서버에서 직접 주입된 파일 업로드 설정을 로드하는 함수
     * window.fileUploadConfig 객체에서 설정을 읽어와 config에 적용합니다.
     * 설정이 없거나 로드에 실패하면 기본값을 사용합니다.
     */
    async loadConfig() {
      try {
        if (window.fileUploadConfig) {
          this.config.maxSize = window.fileUploadConfig.maxSize;
          this.config.allowedExtensions = window.fileUploadConfig.allowedExtensions;
          this.config.maxSizeFormatted = window.fileUploadConfig.maxSizeFormatted;
          this.config.profile = window.fileUploadConfig.profile;
          return;
        }
        console.warn('window.fileUploadConfig is missing. Falling back to defaults.');
      } catch (error) {
        console.warn('Failed to load file upload config, using defaults:', error);
      }
    },

    /**
     * 설정이 로드되었는지 확인하고 필요시 로드하는 함수
     * 이미 로드된 설정이 있는지 확인한 후, 없으면 loadConfig()를 호출합니다.
     * 파일 업로드 전에 반드시 호출되어야 합니다.
     */
    async ensureConfigLoaded() {
      if (this.config && this.config.profile !== 'default') {
        return;
      }
      await this.loadConfig();
    },

    /**
     * 파일 업로드 위젯을 초기화하는 메인 함수
     * 컨테이너 내의 파일 입력, 추가 버튼, 파일 목록을 찾아 이벤트를 바인딩합니다.
     * 기존 파일이 있는 경우 자동으로 로드합니다.
     * 
     * @param {HTMLElement} container - [data-attachments] 속성이 있는 컨테이너 요소
     */
    init(container) {
      // DOM 요소들 찾기
      const input = container.querySelector('#attachments-input');
      const addBtn = container.querySelector('[data-attachments-add]');
      const list = container.querySelector('.attachments-list');
      const form = container.closest('form');
      const hiddenField = container.querySelector('input[name="fileGroupId"]') || form?.querySelector('input[name="fileGroupId"]');

      // 필수 요소 확인
      if (!list) {
        return;
      }

      // 읽기 전용 모드 확인 및 처리
      const readonly = container.hasAttribute('data-readonly') || container.dataset.readonly === 'true';
      if (readonly) {
        if (addBtn) addBtn.style.display = 'none';
        if (input) input.style.display = 'none';
      }

      // 기존 파일 그룹 ID가 있는 경우 파일 목록 로드
      const groupId = hiddenField?.value?.trim();
      if (groupId) {
        this.loadExisting(groupId, list, { readonly }).catch((err) => {
          try { console.warn('Load existing attachments failed:', err); } catch (_) {}
        });
      }

      // 파일 입력이나 추가 버튼이 없으면 초기화 중단
      if (!input || !addBtn) {
        return;
      }

      // 파일 추가 버튼 클릭 이벤트 바인딩
      addBtn.addEventListener('click', () => input.click());

      // 파일 선택 변경 이벤트 바인딩
      input.addEventListener('change', async (event) => {
        const files = Array.from(event.target.files || []);
        if (files.length === 0) return;

        // 설정 로드 확인
        await this.ensureConfigLoaded();

        // 파일 검증
        const maxSize = this.config.maxSize;
        const allowedExts = this.config.allowedExtensions;

        for (const file of files) {
          const ext = file.name.split('.').pop()?.toLowerCase();
          if (!allowedExts.includes(ext)) {
            window.cmms?.notification?.error?.(`Unsupported file type: ${file.name}`);
            return;
          }
          if (file.size > maxSize) {
            window.cmms?.notification?.error?.(`File size exceeds ${this.config.maxSizeFormatted}: ${file.name}`);
            return;
          }
        }

        // 파일 업로드 실행
        await this.uploadFiles(files, hiddenField, list);
        event.target.value = '';
      });
    },

    /**
     * 기존 파일 목록을 서버에서 로드하는 함수
     * 파일 그룹 ID를 사용하여 해당 그룹의 모든 파일을 가져와 화면에 표시합니다.
     * 
     * @param {string} groupId - 파일 그룹 ID
     * @param {HTMLElement} list - 파일 목록을 표시할 DOM 요소
     * @param {Object} options - 옵션 객체
     * @param {boolean} options.readonly - 읽기 전용 모드 여부
     */
    async loadExisting(groupId, list, { readonly = false } = {}) {
      const response = await fetch(`/api/files?groupId=${encodeURIComponent(groupId)}`, { credentials: 'same-origin' });
      if (!response.ok) throw new Error('Failed to load existing attachments: ' + response.status);
      const result = await response.json();
      const items = Array.isArray(result?.items) ? result.items : [];
      this.renderFileList(items, list, { readonly, groupId });
    },

    /**
     * 선택된 파일들을 서버에 업로드하는 함수
     * FormData를 사용하여 파일들을 서버로 전송하고, 응답을 받아 파일 목록을 업데이트합니다.
     * 
     * @param {File[]} files - 업로드할 파일 배열
     * @param {HTMLElement} hiddenField - 파일 그룹 ID를 저장할 히든 필드
     * @param {HTMLElement} list - 파일 목록을 표시할 DOM 요소
     */
    async uploadFiles(files, hiddenField, list) {
      // FormData 생성 및 파일 추가
      const formData = new FormData();
      files.forEach(file => formData.append('files', file));

      // 기존 파일 그룹 ID가 있으면 추가
      if (hiddenField?.value) {
        formData.append('groupId', hiddenField.value);
      }

      try {
        // 서버로 파일 업로드 요청
        const response = await fetch('/api/files', {
          method: 'POST',
          body: formData
        });

        if (!response.ok) throw new Error('Upload failed');

        const result = await response.json();

        // 새로운 파일 그룹 ID가 있으면 히든 필드에 저장
        if (hiddenField && result.fileGroupId) {
          hiddenField.value = result.fileGroupId;
        }

        // 업로드된 파일 목록을 화면에 표시
        this.renderFileList(result.items || [], list, { groupId: hiddenField?.value });
        window.cmms?.notification?.success?.(`${files.length} file(s) uploaded.`);

      } catch (error) {
        console.error('Upload error:', error);
        window.cmms?.notification?.error?.('File upload failed.');
      }
    },

    /**
     * 파일 목록을 화면에 렌더링하는 함수
     * 파일 배열을 받아 HTML로 변환하여 목록 요소에 표시합니다.
     * 각 파일에 대해 다운로드 링크와 삭제 버튼을 생성합니다.
     * 
     * @param {Array} items - 파일 정보 배열
     * @param {HTMLElement} list - 파일 목록을 표시할 DOM 요소
     * @param {Object} options - 옵션 객체
     * @param {boolean} options.readonly - 읽기 전용 모드 여부
     * @param {string} options.groupId - 파일 그룹 ID (오버라이드용)
     */
    renderFileList(items, list, { readonly = false, groupId: groupIdOverride } = {}) {
      list.innerHTML = '';

      // 파일이 없는 경우 빈 상태 메시지 표시
      if (!Array.isArray(items) || items.length === 0) {
        list.innerHTML = '<li class="empty">No attachments.</li>';
        return;
      }

      // 그룹 ID 해결 (옵션에서 제공되거나 DOM에서 찾기)
      let resolvedGroupId = groupIdOverride;
      if (!resolvedGroupId) {
        try {
          const container = list.closest('[data-attachments]') || list.parentElement;
          const form = container?.closest('form');
          const hidden = container?.querySelector('input[name="fileGroupId"]') || form?.querySelector('input[name="fileGroupId"]');
          resolvedGroupId = hidden?.value || undefined;
        } catch (_) {}
      }

      // 각 파일에 대해 목록 아이템 생성
      items.forEach(item => {
        const li = document.createElement('li');
        li.className = 'attachment-item';
        
        // 파일 크기 포맷팅 (utils 모듈 사용)
        const size = window.cmms?.utils?.formatFileSize ? window.cmms.utils.formatFileSize(item.size) : `${item.size} bytes`;
        const groupIdForItem = item.fileGroupId || resolvedGroupId || '';
        
        // 다운로드 링크 생성
        const download = `<a href="/api/files/${item.fileId}?groupId=${groupIdForItem}" class="btn-download" data-hard-nav>Download</a>`;
        
        // 삭제 버튼 생성 (읽기 전용이 아닌 경우에만)
        const remove = readonly ? '' : `<button type="button" class="btn-remove" data-file-id="${item.fileId}" data-group-id="${groupIdForItem}">Delete</button>`;
        
        // 파일 아이템 HTML 생성
        li.innerHTML = `
          <span class="file-name">${item.originalName}</span>
          <span class="file-size">${size}</span>
          ${download}
          ${remove}
        `;
        list.appendChild(li);
      });

      // 삭제 버튼 이벤트 바인딩 (읽기 전용이 아닌 경우에만)
      if (!readonly) {
        list.querySelectorAll('.btn-remove').forEach(btn => {
          btn.addEventListener('click', () => this.deleteFile(btn, list));
        });
      }
    },

    /**
     * 파일을 삭제하는 함수
     * 사용자 확인 후 서버에 삭제 요청을 보내고, 성공 시 UI에서 해당 파일을 제거합니다.
     * 
     * @param {HTMLElement} btn - 삭제 버튼 요소
     * @param {HTMLElement} list - 파일 목록 요소
     */
    async deleteFile(btn, list) {
      const fileId = btn.dataset.fileId;
      let groupId = btn.dataset.groupId;
      
      // 그룹 ID가 없으면 DOM에서 찾기
      if (!groupId) {
        try {
          const container = list.closest('[data-attachments]') || list.parentElement;
          const form = container?.closest('form');
          const hidden = container?.querySelector('input[name="fileGroupId"]') || form?.querySelector('input[name="fileGroupId"]');
          groupId = hidden?.value;
        } catch (_) {}
      }

      // 사용자 확인
      if (!confirm('Delete this file?')) return;

      try {
        // 서버에 삭제 요청
        const response = await fetch(`/api/files/${fileId}?groupId=${encodeURIComponent(groupId || '')}`, {
          method: 'DELETE'
        });

        if (!response.ok) throw new Error('Delete failed');

        // UI에서 파일 아이템 제거
        btn.closest('li')?.remove();

        // 파일이 모두 삭제된 경우 빈 상태 메시지 표시
        if (!list.children.length) {
          list.innerHTML = '<li class="empty">No attachments.</li>';
        }

        window.cmms?.notification?.success?.('File deleted.');

      } catch (error) {
        console.error('Delete error:', error);
        window.cmms?.notification?.error?.('File delete failed.');
      }
    },

    /**
     * 지정된 루트 요소 내의 모든 파일 업로드 컨테이너를 초기화하는 함수
     * SPA 환경에서 동적으로 로드된 콘텐츠의 파일 업로드 위젯을 초기화할 때 사용됩니다.
     * 중복 초기화를 방지하기 위해 __fileUploadInitialized 플래그를 사용합니다.
     * 
     * @param {Element|Document} root - 초기화할 범위의 루트 요소 (기본값: document)
     */
    initializeContainers(root = document) {
      const scope = root instanceof Element ? root : document;
      const targets = scope.querySelectorAll('[data-attachments]');
      targets.forEach(container => {
        // 중복 초기화 방지
        if (container.__fileUploadInitialized) {
          return;
        }
        this.init(container);
        container.__fileUploadInitialized = true;
      });
    }
  };

  // CMMS 네임스페이스에 파일 업로드 모듈 등록
  window.cmms.fileUpload = fileUploadModule;
})();
