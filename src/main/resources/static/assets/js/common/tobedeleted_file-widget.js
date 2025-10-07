/**
 * 독립적인 파일 업로드 위젯
 * S3 기반 Presigned URL 업로드 지원
 */
class FileUploadWidget {
    constructor(container, options = {}) {
        this.container = container;
        this.options = {
            groupId: null,
            refEntity: null,
            refId: null,
            maxFiles: 10,
            maxFileSize: 10 * 1024 * 1024, // 10MB
            allowedTypes: ['image/*', 'application/pdf', 'text/*'],
            ...options
        };
        
        this.uploadSession = null;
        this.init();
    }

    init() {
        this.setupDOM();
        this.setupEventListeners();
        this.loadExistingFiles();
    }

    setupDOM() {
        // 기존 파일 목록 컨테이너 확인
        this.fileList = this.container.querySelector('.attachments-list');
        if (!this.fileList) {
            throw new Error('파일 목록 컨테이너(.attachments-list)를 찾을 수 없습니다.');
        }

        // 파일 입력 요소 확인
        this.fileInput = this.container.querySelector('input[type="file"]');
        if (!this.fileInput) {
            throw new Error('파일 입력 요소를 찾을 수 없습니다.');
        }

        // 파일 추가 버튼 확인
        this.addButton = this.container.querySelector('[data-attachments-add]');
        if (!this.addButton) {
            throw new Error('파일 추가 버튼([data-attachments-add])을 찾을 수 없습니다.');
        }

        // 그룹 ID 히든 필드 확인
        this.groupIdField = this.container.querySelector('input[name="file_group_id"]');
        if (this.groupIdField) {
            this.options.groupId = this.groupIdField.value;
        }
    }

    setupEventListeners() {
        // 파일 추가 버튼 클릭
        this.addButton.addEventListener('click', () => {
            this.fileInput.click();
        });

        // 파일 선택 변경
        this.fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileSelection(Array.from(e.target.files));
            }
        });

        // 드래그 앤 드롭 지원
        this.container.addEventListener('dragover', (e) => {
            e.preventDefault();
            this.container.classList.add('drag-over');
        });

        this.container.addEventListener('dragleave', (e) => {
            e.preventDefault();
            this.container.classList.remove('drag-over');
        });

        this.container.addEventListener('drop', (e) => {
            e.preventDefault();
            this.container.classList.remove('drag-over');
            
            if (e.dataTransfer.files.length > 0) {
                this.handleFileSelection(Array.from(e.dataTransfer.files));
            }
        });
    }

    async loadExistingFiles() {
        if (!this.options.groupId) return;

        try {
            const response = await fetch(`/api/files?groupId=${this.options.groupId}`);
            if (response.ok) {
                const fileGroup = await response.json();
                this.displayExistingFiles(fileGroup.items);
            }
        } catch (error) {
            console.error('기존 파일 로드 실패:', error);
            this.showError('기존 파일을 불러오는데 실패했습니다.');
        }
    }

    async handleFileSelection(files) {
        // 파일 검증
        const validFiles = this.validateFiles(files);
        if (validFiles.length === 0) return;

        // 업로드 세션 생성
        try {
            await this.createUploadSession(validFiles);
        } catch (error) {
            console.error('업로드 세션 생성 실패:', error);
            this.showError('파일 업로드를 시작할 수 없습니다.');
        }
    }

    validateFiles(files) {
        const validFiles = [];
        
        for (const file of files) {
            // 파일 크기 검증
            if (file.size > this.options.maxFileSize) {
                this.showError(`파일이 너무 큽니다: ${file.name} (최대 ${this.formatFileSize(this.options.maxFileSize)})`);
                continue;
            }

            // 파일 타입 검증
            if (!this.isAllowedFileType(file)) {
                this.showError(`허용되지 않은 파일 형식입니다: ${file.name}`);
                continue;
            }

            validFiles.push(file);
        }

        return validFiles;
    }

    isAllowedFileType(file) {
        if (this.options.allowedTypes.length === 0) return true;
        
        return this.options.allowedTypes.some(type => {
            if (type.endsWith('/*')) {
                return file.type.startsWith(type.slice(0, -1));
            }
            return file.type === type;
        });
    }

    async createUploadSession(files) {
        const fileNames = files.map(file => file.name);
        
        const response = await fetch('/api/files/init', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                groupId: this.options.groupId,
                refEntity: this.options.refEntity,
                refId: this.options.refId,
                fileNames: fileNames
            })
        });

        if (!response.ok) {
            throw new Error(`업로드 세션 생성 실패: ${response.status}`);
        }

        this.uploadSession = await response.json();
        
        // 그룹 ID 업데이트
        if (!this.options.groupId) {
            this.options.groupId = this.uploadSession.groupId;
            if (this.groupIdField) {
                this.groupIdField.value = this.uploadSession.groupId;
            }
        }

        // 파일들을 S3에 업로드
        await this.uploadFilesToS3(files);
    }

    async uploadFilesToS3(files) {
        const uploadPromises = files.map(async (file, index) => {
            const uploadInfo = this.uploadSession.uploadInfos[index];
            
            try {
                // S3에 직접 업로드
                const uploadResponse = await fetch(uploadInfo.presignedUrl, {
                    method: 'PUT',
                    body: file,
                    headers: {
                        'Content-Type': file.type
                    }
                });

                if (!uploadResponse.ok) {
                    throw new Error(`S3 업로드 실패: ${uploadResponse.status}`);
                }

                // 업로드 성공 표시
                this.showUploadSuccess(file.name);
                
                return {
                    fileId: uploadInfo.fileId,
                    originalName: file.name,
                    s3Key: uploadInfo.s3Key,
                    contentType: file.type,
                    size: file.size,
                    checksum: await this.calculateChecksum(file)
                };
            } catch (error) {
                console.error(`파일 업로드 실패: ${file.name}`, error);
                this.showUploadError(file.name, error.message);
                return null;
            }
        });

        const uploadResults = await Promise.all(uploadPromises);
        const successfulUploads = uploadResults.filter(result => result !== null);

        if (successfulUploads.length > 0) {
            // 메타데이터 저장
            await this.saveFileMetadata(successfulUploads);
        }
    }

    async saveFileMetadata(fileMetadatas) {
        try {
            const response = await fetch(`/api/files/${this.uploadSession.groupId}/complete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(fileMetadatas)
            });

            if (!response.ok) {
                throw new Error(`메타데이터 저장 실패: ${response.status}`);
            }

            const fileGroup = await response.json();
            this.displayExistingFiles(fileGroup.items);
            this.showSuccess(`${fileMetadatas.length}개 파일이 성공적으로 업로드되었습니다.`);
            
        } catch (error) {
            console.error('메타데이터 저장 실패:', error);
            this.showError('파일 정보를 저장하는데 실패했습니다.');
        }
    }

    displayExistingFiles(items) {
        this.fileList.innerHTML = '';
        
        if (items.length === 0) {
            const emptyItem = document.createElement('li');
            emptyItem.className = 'empty';
            emptyItem.textContent = '첨부된 파일이 없습니다.';
            this.fileList.appendChild(emptyItem);
            return;
        }

        items.forEach(item => {
            const listItem = this.createFileListItem(item);
            this.fileList.appendChild(listItem);
        });
    }

    createFileListItem(item) {
        const li = document.createElement('li');
        li.className = 'attachment-item';
        li.setAttribute('data-file-id', item.fileId);

        const fileName = document.createElement('span');
        fileName.className = 'file-name';
        fileName.textContent = item.originalName;

        const fileSize = document.createElement('span');
        fileSize.className = 'file-size';
        fileSize.textContent = this.formatFileSize(item.size);

        const downloadButton = document.createElement('a');
        downloadButton.className = 'btn btn-sm';
        downloadButton.textContent = '다운로드';
        downloadButton.href = `/api/files/${item.fileId}/download?groupId=${this.options.groupId}`;
        downloadButton.target = '_blank';

        const deleteButton = document.createElement('button');
        deleteButton.type = 'button';
        deleteButton.className = 'btn btn-sm btn-danger';
        deleteButton.textContent = '삭제';
        deleteButton.setAttribute('data-confirm', `${item.originalName} 파일을 삭제하시겠습니까?`);
        deleteButton.addEventListener('click', () => this.deleteFile(item.fileId, item.originalName));

        li.appendChild(fileName);
        li.appendChild(fileSize);
        li.appendChild(downloadButton);
        li.appendChild(deleteButton);

        return li;
    }

    async deleteFile(fileId, fileName) {
        try {
            const response = await fetch(`/api/files/${fileId}?groupId=${this.options.groupId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error(`파일 삭제 실패: ${response.status}`);
            }

            // UI에서 제거
            const listItem = this.fileList.querySelector(`[data-file-id="${fileId}"]`);
            if (listItem) {
                listItem.remove();
            }

            this.showSuccess(`${fileName} 파일이 삭제되었습니다.`);
            
        } catch (error) {
            console.error('파일 삭제 실패:', error);
            this.showError('파일 삭제에 실패했습니다.');
        }
    }

    async calculateChecksum(file) {
        // 간단한 체크섬 계산 (실제로는 SHA-256 등 사용)
        return `${file.name}_${file.size}_${file.lastModified}`;
    }

    formatFileSize(bytes) {
        return window.cmms.utils.formatFileSize(bytes);
    }

    showSuccess(message) {
        window.cmms.notification.success(message);
    }

    showError(message) {
        window.cmms.notification.error(message);
    }

    showUploadSuccess(fileName) {
        window.cmms.notification.success(`${fileName} 업로드 완료`);
    }

    showUploadError(fileName, error) {
        window.cmms.notification.error(`${fileName} 업로드 실패: ${error}`);
    }
}

// CMMS 네임스페이스에 파일 업로드 모듈 추가
window.cmms = window.cmms || {};
window.cmms.fileUpload = {
    init: function(container, options = {}) {
        return new FileUploadWidget(container, options);
    },
    FileUploadWidget: FileUploadWidget
};

// DOM 로드 시 자동 초기화
document.addEventListener('DOMContentLoaded', () => {
    const attachmentContainers = document.querySelectorAll('[data-attachments]');
    attachmentContainers.forEach(container => {
        try {
            window.cmms.fileUpload.init(container);
        } catch (error) {
            console.error('파일 업로드 위젯 초기화 실패:', error);
        }
    });
});
