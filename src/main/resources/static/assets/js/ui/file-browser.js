/**
 * 파일 브라우저 UI 컨트롤러 (목업 모드)
 */
class FileBrowser {
    constructor() {
        this.currentPath = '/';
        this.mockData = this.generateMockData();
    }
    
    /**
     * 목업 데이터 생성
     */
    generateMockData() {
        return {
            '/': {
                folders: [
                    { name: '매뉴얼', path: '/매뉴얼', modified: '2025-10-15 10:30' },
                    { name: '도면', path: '/도면', modified: '2025-10-12 14:20' },
                    { name: '사진', path: '/사진', modified: '2025-10-10 09:15' }
                ],
                files: []
            },
            '/매뉴얼': {
                folders: [
                    { name: '설비', path: '/매뉴얼/설비', modified: '2025-10-14 11:00' },
                    { name: '전기', path: '/매뉴얼/전기', modified: '2025-10-13 16:45' },
                    { name: '배관', path: '/매뉴얼/배관', modified: '2025-10-11 10:20' }
                ],
                files: []
            },
            '/매뉴얼/설비': {
                folders: [],
                files: [
                    { name: '압축기_매뉴얼.pdf', size: 2457600, modified: '2025-10-14 11:00', type: 'pdf' },
                    { name: '모터_매뉴얼.pdf', size: 1843200, modified: '2025-10-13 15:30', type: 'pdf' },
                    { name: '펌프_매뉴얼.hwp', size: 921600, modified: '2025-10-12 09:15', type: 'hwp' }
                ]
            },
            '/매뉴얼/전기': {
                folders: [],
                files: [
                    { name: '전기설비_매뉴얼.pdf', size: 3276800, modified: '2025-10-13 16:45', type: 'pdf' },
                    { name: '배선도.dwg', size: 512000, modified: '2025-10-10 14:20', type: 'default' }
                ]
            },
            '/매뉴얼/배관': {
                folders: [],
                files: [
                    { name: '배관_시방서.pdf', size: 1638400, modified: '2025-10-11 10:20', type: 'pdf' }
                ]
            },
            '/도면': {
                folders: [],
                files: [
                    { name: '공장배치도.pdf', size: 5242880, modified: '2025-10-12 14:20', type: 'pdf' },
                    { name: '설비배치도.dwg', size: 2097152, modified: '2025-10-10 11:30', type: 'default' }
                ]
            },
            '/사진': {
                folders: [],
                files: [
                    { name: '설비사진_001.jpg', size: 1048576, modified: '2025-10-10 09:15', type: 'image' },
                    { name: '설비사진_002.jpg', size: 983040, modified: '2025-10-10 09:20', type: 'image' },
                    { name: '작업현장.jpg', size: 1310720, modified: '2025-10-09 16:00', type: 'image' }
                ]
            }
        };
    }
    
    /**
     * 초기화
     */
    init() {
        this.loadFiles(this.currentPath);
        this.renderFolderTree();
        this.initEventListeners();
    }
    
    /**
     * 폴더 트리 렌더링
     */
    renderFolderTree() {
        const root = document.getElementById('folderTreeRoot');
        if (!root) return;
        
        root.innerHTML = `
            <div class="file-tree-node ${this.currentPath === '/' ? 'selected' : ''}" data-path="/">
                <span class="file-tree-icon">📁</span>
                <span>Home</span>
            </div>
            ${this.renderTreeChildren('/', 0)}
        `;
    }
    
    /**
     * 트리 자식 노드 렌더링 (재귀)
     */
    renderTreeChildren(path, level) {
        const data = this.mockData[path];
        if (!data || !data.folders || data.folders.length === 0) {
            return '';
        }
        
        return `
            <div class="file-tree-children">
                ${data.folders.map(folder => `
                    <div class="file-tree-node ${this.currentPath === folder.path ? 'selected' : ''}" 
                         data-path="${folder.path}">
                        <span class="file-tree-icon">📁</span>
                        <span>${folder.name}</span>
                    </div>
                    ${this.renderTreeChildren(folder.path, level + 1)}
                `).join('')}
            </div>
        `;
    }
    
    /**
     * 파일 목록 로드
     */
    loadFiles(path) {
        this.currentPath = path;
        
        const data = this.mockData[path];
        const tbody = document.getElementById('fileListBody');
        
        if (!data) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4">
                        <div class="file-list-empty">
                            <div class="file-list-empty-icon">📁</div>
                            <div>폴더가 비어있습니다</div>
                        </div>
                    </td>
                </tr>
            `;
            this.updateBreadcrumb(path);
            this.renderFolderTree();
            return;
        }
        
        const items = [
            ...data.folders.map(f => ({ ...f, isFolder: true })),
            ...data.files.map(f => ({ ...f, isFolder: false }))
        ];
        
        if (items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4">
                        <div class="file-list-empty">
                            <div class="file-list-empty-icon">📁</div>
                            <div>폴더가 비어있습니다</div>
                        </div>
                    </td>
                </tr>
            `;
        } else {
            tbody.innerHTML = items.map(item => this.renderFileRow(item)).join('');
        }
        
        this.updateBreadcrumb(path);
        this.renderFolderTree();
    }
    
    /**
     * 파일 행 렌더링
     */
    renderFileRow(item) {
        const icon = this.getFileIcon(item);
        const size = item.isFolder ? '-' : this.formatSize(item.size);
        const path = item.isFolder ? item.path : this.currentPath + '/' + item.name;
        
        return `
            <tr data-path="${path}" data-is-folder="${item.isFolder}">
                <td>
                    <div class="file-item">
                        <span class="file-icon ${item.type || 'folder'}">${icon}</span>
                        <span>${item.name}</span>
                    </div>
                </td>
                <td class="file-size">${size}</td>
                <td class="file-date">${item.modified}</td>
                <td>
                    <div class="file-actions">
                        ${!item.isFolder ? `
                            <button class="btn sm primary" onclick="alert('다운로드 기능 (목업)')">
                                다운로드
                            </button>
                        ` : ''}
                        <button class="btn sm danger" onclick="alert('삭제 기능 (목업)')">
                            삭제
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }
    
    /**
     * 파일 아이콘 가져오기
     */
    getFileIcon(item) {
        if (item.isFolder) return '📁';
        
        switch (item.type) {
            case 'pdf': return '📄';
            case 'image': return '🖼️';
            case 'word': return '📘';
            case 'excel': return '📗';
            case 'hwp': return '📝';
            default: return '📄';
        }
    }
    
    /**
     * 파일 크기 포맷팅
     */
    formatSize(bytes) {
        if (!bytes) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`;
    }
    
    /**
     * Breadcrumb 업데이트
     */
    updateBreadcrumb(path) {
        const breadcrumb = document.getElementById('breadcrumb');
        if (!breadcrumb) return;
        
        if (path === '/') {
            breadcrumb.innerHTML = '<a href="#" data-path="/">Home</a>';
            return;
        }
        
        const parts = path.split('/').filter(p => p);
        let html = '<a href="#" data-path="/">Home</a>';
        let currentPath = '';
        
        parts.forEach((part, idx) => {
            currentPath += '/' + part;
            html += ` <span class="file-breadcrumb-separator">/</span> `;
            html += `<a href="#" data-path="${currentPath}">${part}</a>`;
        });
        
        breadcrumb.innerHTML = html;
    }
    
    /**
     * 이벤트 리스너 초기화
     */
    initEventListeners() {
        // 파일 목록 클릭 (폴더 이동)
        document.getElementById('fileListBody')?.addEventListener('click', (e) => {
            const row = e.target.closest('tr[data-path]');
            if (row && !e.target.closest('button')) {
                const isFolder = row.dataset.isFolder === 'true';
                if (isFolder) {
                    this.loadFiles(row.dataset.path);
                }
            }
        });
        
        // 트리 노드 클릭
        document.getElementById('folderTreeRoot')?.addEventListener('click', (e) => {
            const node = e.target.closest('.file-tree-node[data-path]');
            if (node) {
                this.loadFiles(node.dataset.path);
            }
        });
        
        // Breadcrumb 클릭
        document.getElementById('breadcrumb')?.addEventListener('click', (e) => {
            e.preventDefault();
            const link = e.target.closest('a[data-path]');
            if (link) {
                this.loadFiles(link.dataset.path);
            }
        });
        
        // 툴바 버튼들
        document.getElementById('btn-upload')?.addEventListener('click', () => {
            alert('업로드 기능 (목업 모드)\n\n실제 구현 시 파일 선택 대화상자가 표시됩니다.');
        });
        
        document.getElementById('btn-create-folder')?.addEventListener('click', () => {
            alert('폴더 생성 기능 (목업 모드)\n\n실제 구현 시 폴더 이름 입력 대화상자가 표시됩니다.');
        });
        
        document.getElementById('btn-refresh')?.addEventListener('click', () => {
            this.loadFiles(this.currentPath);
        });
    }
}

// 전역으로 export
window.FileBrowser = FileBrowser;

