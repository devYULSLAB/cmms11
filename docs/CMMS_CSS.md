# CMMS CSS 스타일 가이드

> **참조 문서**: [CMMS_PRD.md](./CMMS_PRD.md) - 제품 요구사항, [CMMS_STRUCTURES.md](./CMMS_STRUCTURES.md) - 기술 아키텍처

본 문서는 CMMS 시스템의 CSS 스타일 가이드입니다. 반응형 디자인, 컴포넌트 스타일, 모바일 최적화 등을 다룹니다.

## 1. CSS 파일 구조

### 1.1 스타일시트 구성
```
src/main/resources/static/assets/css/
├── base.css          # 기본 스타일 (반응형, 공통 컴포넌트)
├── print.css         # 인쇄용 스타일
├── themes/           # 테마별 스타일 (선택사항)
│   ├── light.css
│   └── dark.css
└── vendor/           # 외부 라이브러리 (선택사항)
    └── chart.css
```

### 1.2 CSS 변수 시스템
```css
/* base.css - CSS 변수 정의 */
:root {
  /* 색상 팔레트 */
  --color-primary: #2563eb;
  --color-primary-dark: #1d4ed8;
  --color-primary-light: #3b82f6;
  
  --color-secondary: #64748b;
  --color-success: #059669;
  --color-warning: #d97706;
  --color-danger: #dc2626;
  --color-info: #0891b2;
  
  /* 중성 색상 */
  --color-white: #ffffff;
  --color-gray-50: #f8fafc;
  --color-gray-100: #f1f5f9;
  --color-gray-200: #e2e8f0;
  --color-gray-300: #cbd5e1;
  --color-gray-400: #94a3b8;
  --color-gray-500: #64748b;
  --color-gray-600: #475569;
  --color-gray-700: #334155;
  --color-gray-800: #1e293b;
  --color-gray-900: #0f172a;
  
  /* 타이포그래피 */
  --font-family-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --font-family-mono: 'SF Mono', Monaco, 'Cascadia Code', monospace;
  
  --font-size-xs: 0.75rem;    /* 12px */
  --font-size-sm: 0.875rem;   /* 14px */
  --font-size-base: 1rem;     /* 16px */
  --font-size-lg: 1.125rem;   /* 18px */
  --font-size-xl: 1.25rem;    /* 20px */
  --font-size-2xl: 1.5rem;    /* 24px */
  --font-size-3xl: 1.875rem;  /* 30px */
  
  /* 간격 */
  --spacing-1: 0.25rem;   /* 4px */
  --spacing-2: 0.5rem;    /* 8px */
  --spacing-3: 0.75rem;   /* 12px */
  --spacing-4: 1rem;      /* 16px */
  --spacing-5: 1.25rem;   /* 20px */
  --spacing-6: 1.5rem;    /* 24px */
  --spacing-8: 2rem;      /* 32px */
  --spacing-10: 2.5rem;   /* 40px */
  --spacing-12: 3rem;     /* 48px */
  
  /* 브레이크포인트 */
  --breakpoint-sm: 640px;
  --breakpoint-md: 768px;
  --breakpoint-lg: 1024px;
  --breakpoint-xl: 1280px;
  
  /* 그림자 */
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1);
  
  /* 테두리 반경 */
  --radius-sm: 0.25rem;
  --radius-md: 0.375rem;
  --radius-lg: 0.5rem;
  --radius-xl: 0.75rem;
}
```

## 2. 기본 레이아웃

### 2.1 SPA 레이아웃 구조
```css
/* 기본 레이아웃 */
body {
  margin: 0;
  font-family: var(--font-family-sans);
  font-size: var(--font-size-base);
  line-height: 1.6;
  color: var(--color-gray-800);
  background-color: var(--color-gray-50);
}

/* 앱바 */
.appbar {
  background-color: var(--color-white);
  border-bottom: 1px solid var(--color-gray-200);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 100;
}

.appbar-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 var(--spacing-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

/* 브레드크럼 */
.breadcrumbs {
  background-color: var(--color-white);
  border-bottom: 1px solid var(--color-gray-200);
  padding: var(--spacing-2) var(--spacing-4);
}

.breadcrumbs .sep {
  color: var(--color-gray-400);
  margin: 0 var(--spacing-2);
}

/* 메인 컨테이너 */
.container {
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--spacing-6) var(--spacing-4);
}

/* 푸터 */
footer {
  background-color: var(--color-gray-800);
  color: var(--color-white);
  padding: var(--spacing-8) 0;
  margin-top: var(--spacing-12);
}

footer .container {
  text-align: center;
}
```

### 2.2 반응형 그리드 시스템
```css
/* 12열 그리드 시스템 */
.grid {
  display: grid;
  gap: var(--spacing-4);
}

.grid.cols-12 {
  grid-template-columns: repeat(12, 1fr);
}

/* 컬럼 스팬 */
.col-span-1 { grid-column: span 1; }
.col-span-2 { grid-column: span 2; }
.col-span-3 { grid-column: span 3; }
.col-span-4 { grid-column: span 4; }
.col-span-5 { grid-column: span 5; }
.col-span-6 { grid-column: span 6; }
.col-span-7 { grid-column: span 7; }
.col-span-8 { grid-column: span 8; }
.col-span-9 { grid-column: span 9; }
.col-span-10 { grid-column: span 10; }
.col-span-11 { grid-column: span 11; }
.col-span-12 { grid-column: span 12; }

/* 반응형 컬럼 */
@media (max-width: 768px) {
  .col-span-6 {
    grid-column: span 12;
  }
  
  .col-span-4 {
    grid-column: span 12;
  }
  
  .col-span-3 {
    grid-column: span 6;
  }
}

/* 스택 레이아웃 (모바일 대응) */
.stack {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.stack-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1);
}

.stack-item label {
  font-weight: 600;
  color: var(--color-gray-700);
  font-size: var(--font-size-sm);
}

/* 행 레이아웃 */
.row {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
}

.spacer {
  flex: 1;
}
```

## 3. 컴포넌트 스타일

### 3.1 카드 컴포넌트
```css
/* 카드 기본 스타일 */
.card {
  background-color: var(--color-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  margin-bottom: var(--spacing-6);
}

.card-header {
  padding: var(--spacing-6);
  border-bottom: 1px solid var(--color-gray-200);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-gray-900);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.card-body {
  padding: var(--spacing-6);
}

.section {
  margin-bottom: var(--spacing-6);
}

.section:last-child {
  margin-bottom: 0;
}

.section-title {
  margin: 0 0 var(--spacing-4) 0;
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-gray-800);
  border-bottom: 1px solid var(--color-gray-200);
  padding-bottom: var(--spacing-2);
}
```

### 3.2 폼 컴포넌트
```css
/* 폼 행 */
.form-row {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-4);
}

@media (min-width: 768px) {
  .form-row {
    flex-direction: row;
    align-items: flex-start;
  }
  
  .form-row .label {
    flex: 0 0 200px;
    padding-top: var(--spacing-2);
  }
  
  .form-row .input,
  .form-row .select,
  .form-row .textarea {
    flex: 1;
  }
}

/* 라벨 */
.label {
  font-weight: 500;
  color: var(--color-gray-700);
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-1);
}

.label.required::after {
  content: ' *';
  color: var(--color-danger);
}

/* 입력 필드 */
.input,
.select,
.textarea {
  padding: var(--spacing-3);
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input:focus,
.select:focus,
.textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgb(37 99 235 / 0.1);
}

.input:invalid,
.select:invalid,
.textarea:invalid {
  border-color: var(--color-danger);
}

/* 텍스트 영역 */
.textarea {
  resize: vertical;
  min-height: 100px;
}

/* 체크박스와 라디오 */
.checkbox,
.radio {
  margin-right: var(--spacing-2);
}

/* 파일 입력 */
.file-input {
  position: relative;
  display: inline-block;
}

.file-input input[type="file"] {
  position: absolute;
  opacity: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.file-input-label {
  display: inline-block;
  padding: var(--spacing-3) var(--spacing-4);
  background-color: var(--color-gray-100);
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color 0.2s;
}

.file-input-label:hover {
  background-color: var(--color-gray-200);
}
```

### 3.3 버튼 컴포넌트
```css
/* 버튼 기본 스타일 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-3) var(--spacing-4);
  font-size: var(--font-size-sm);
  font-weight: 500;
  text-decoration: none;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

/* 버튼 크기 */
.btn.sm {
  padding: var(--spacing-2) var(--spacing-3);
  font-size: var(--font-size-xs);
}

.btn.lg {
  padding: var(--spacing-4) var(--spacing-6);
  font-size: var(--font-size-lg);
}

/* 버튼 색상 */
.btn.primary {
  background-color: var(--color-primary);
  color: var(--color-white);
  border-color: var(--color-primary);
}

.btn.primary:hover {
  background-color: var(--color-primary-dark);
  border-color: var(--color-primary-dark);
}

.btn.secondary {
  background-color: var(--color-gray-100);
  color: var(--color-gray-700);
  border-color: var(--color-gray-300);
}

.btn.secondary:hover {
  background-color: var(--color-gray-200);
}

.btn.success {
  background-color: var(--color-success);
  color: var(--color-white);
  border-color: var(--color-success);
}

.btn.warning {
  background-color: var(--color-warning);
  color: var(--color-white);
  border-color: var(--color-warning);
}

.btn.danger {
  background-color: var(--color-danger);
  color: var(--color-white);
  border-color: var(--color-danger);
}

.btn.danger:hover {
  background-color: #b91c1c;
  border-color: #b91c1c;
}

/* 버튼 비활성화 */
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 버튼 그룹 */
.btn-group {
  display: flex;
  gap: var(--spacing-2);
}

.btn-group .btn {
  border-radius: 0;
}

.btn-group .btn:first-child {
  border-radius: var(--radius-md) 0 0 var(--radius-md);
}

.btn-group .btn:last-child {
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
}

.btn-group .btn:only-child {
  border-radius: var(--radius-md);
}
```

### 3.4 배지 컴포넌트
```css
/* 배지 기본 스타일 */
.badge {
  display: inline-flex;
  align-items: center;
  padding: var(--spacing-1) var(--spacing-2);
  font-size: var(--font-size-xs);
  font-weight: 500;
  border-radius: var(--radius-sm);
  background-color: var(--color-gray-100);
  color: var(--color-gray-700);
}

/* 배지 색상 */
.badge.primary {
  background-color: var(--color-primary);
  color: var(--color-white);
}

.badge.success {
  background-color: var(--color-success);
  color: var(--color-white);
}

.badge.warning {
  background-color: var(--color-warning);
  color: var(--color-white);
}

.badge.danger {
  background-color: var(--color-danger);
  color: var(--color-white);
}

.badge.info {
  background-color: var(--color-info);
  color: var(--color-white);
}

/* 상태별 배지 */
.badge.status-plan {
  background-color: var(--color-gray-100);
  color: var(--color-gray-700);
}

.badge.status-asgn {
  background-color: #fef3c7;
  color: #92400e;
}

.badge.status-proc {
  background-color: #fed7aa;
  color: #ea580c;
}

.badge.status-done {
  background-color: #d1fae5;
  color: #065f46;
}
```

## 4. 테이블 스타일

### 4.1 기본 테이블
```css
/* 테이블 기본 스타일 */
.table {
  width: 100%;
  border-collapse: collapse;
  background-color: var(--color-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.table th,
.table td {
  padding: var(--spacing-4);
  text-align: left;
  border-bottom: 1px solid var(--color-gray-200);
}

.table th {
  background-color: var(--color-gray-50);
  font-weight: 600;
  color: var(--color-gray-700);
  font-size: var(--font-size-sm);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.table tbody tr:hover {
  background-color: var(--color-gray-50);
}

.table tbody tr[data-row-link] {
  cursor: pointer;
}

/* 액션 컬럼 */
.table .actions {
  width: 120px;
  text-align: center;
}

.table .actions .btn {
  margin: 0 var(--spacing-1);
}

/* 반응형 테이블 */
@media (max-width: 768px) {
  .table {
    font-size: var(--font-size-sm);
  }
  
  .table th,
  .table td {
    padding: var(--spacing-2);
  }
  
  .table .actions {
    width: 80px;
  }
  
  .table .actions .btn {
    padding: var(--spacing-1);
    font-size: var(--font-size-xs);
  }
}
```

### 4.2 테이블 확장 기능
```css
/* 정렬 가능한 헤더 */
.table th.sortable {
  cursor: pointer;
  user-select: none;
  position: relative;
}

.table th.sortable:hover {
  background-color: var(--color-gray-100);
}

.table th.sortable::after {
  content: '↕';
  position: absolute;
  right: var(--spacing-2);
  opacity: 0.5;
}

.table th.sortable.asc::after {
  content: '↑';
  opacity: 1;
}

.table th.sortable.desc::after {
  content: '↓';
  opacity: 1;
}

/* 선택 가능한 행 */
.table tbody tr.selectable {
  cursor: pointer;
}

.table tbody tr.selected {
  background-color: #dbeafe;
}

.table tbody tr.selected:hover {
  background-color: #bfdbfe;
}

/* 로딩 상태 */
.table.loading {
  opacity: 0.6;
  pointer-events: none;
}

.table.loading::after {
  content: '로딩 중...';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: var(--color-white);
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
}
```

## 5. 파일 업로드 컴포넌트

### 5.1 파일 첨부 위젯
```css
/* 파일 첨부 컨테이너 */
.attachments {
  border: 2px dashed var(--color-gray-300);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  text-align: center;
  transition: border-color 0.2s;
}

.attachments:hover {
  border-color: var(--color-primary);
}

.attachments.dragover {
  border-color: var(--color-primary);
  background-color: #dbeafe;
}

/* 파일 입력 */
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* 파일 목록 */
.attachments-list {
  list-style: none;
  padding: 0;
  margin: var(--spacing-4) 0 0 0;
  text-align: left;
}

.attachments-list .empty {
  color: var(--color-gray-500);
  font-style: italic;
  text-align: center;
  padding: var(--spacing-4);
}

.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-3);
  background-color: var(--color-gray-50);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-2);
}

.attachment-item a {
  color: var(--color-primary);
  text-decoration: none;
  flex: 1;
  margin-right: var(--spacing-2);
}

.attachment-item a:hover {
  text-decoration: underline;
}

.file-size {
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
  margin-right: var(--spacing-2);
}

.btn-remove {
  background: none;
  border: none;
  color: var(--color-danger);
  cursor: pointer;
  font-size: var(--font-size-lg);
  padding: var(--spacing-1);
  border-radius: var(--radius-sm);
  transition: background-color 0.2s;
}

.btn-remove:hover {
  background-color: var(--color-danger);
  color: var(--color-white);
}
```

## 6. KPI 대시보드 스타일

### 6.1 KPI 카드
```css
/* KPI 카드 */
.kpi-card {
  background-color: var(--color-white);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-md);
  border-left: 4px solid var(--color-primary);
}

.kpi-card.success {
  border-left-color: var(--color-success);
}

.kpi-card.warning {
  border-left-color: var(--color-warning);
}

.kpi-card.danger {
  border-left-color: var(--color-danger);
}

.kpi-title {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-gray-600);
  margin: 0 0 var(--spacing-2) 0;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kpi-value {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-gray-900);
  margin: 0 0 var(--spacing-2) 0;
}

.kpi-unit {
  font-size: var(--font-size-lg);
  font-weight: 400;
  color: var(--color-gray-600);
}

.kpi-trend {
  font-size: var(--font-size-sm);
  font-weight: 500;
  margin-top: var(--spacing-2);
}

.kpi-trend.positive {
  color: var(--color-success);
}

.kpi-trend.negative {
  color: var(--color-danger);
}

.kpi-status {
  display: inline-flex;
  align-items: center;
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 500;
  margin-top: var(--spacing-2);
}

.kpi-status.good {
  background-color: #d1fae5;
  color: #065f46;
}

.kpi-status.warning {
  background-color: #fef3c7;
  color: #92400e;
}

.kpi-status.danger {
  background-color: #fee2e2;
  color: #991b1b;
}
```

### 6.2 차트 컨테이너
```css
/* 차트 컨테이너 */
.chart-container {
  background-color: var(--color-white);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-md);
  margin-bottom: var(--spacing-6);
}

.chart-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-gray-900);
  margin: 0 0 var(--spacing-4) 0;
}

.chart-wrapper {
  position: relative;
  height: 300px;
  width: 100%;
}

@media (max-width: 768px) {
  .chart-wrapper {
    height: 200px;
  }
}
```

## 7. 알림 및 메시지

### 7.1 알림 시스템
```css
/* 알림 컨테이너 */
.notifications {
  position: fixed;
  top: var(--spacing-4);
  right: var(--spacing-4);
  z-index: 1000;
  max-width: 400px;
}

/* 알림 메시지 */
.notification {
  background-color: var(--color-white);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-2);
  box-shadow: var(--shadow-lg);
  border-left: 4px solid var(--color-primary);
  transform: translateX(100%);
  transition: transform 0.3s ease;
}

.notification.show {
  transform: translateX(0);
}

.notification.success {
  border-left-color: var(--color-success);
}

.notification.warning {
  border-left-color: var(--color-warning);
}

.notification.error {
  border-left-color: var(--color-danger);
}

.notification.info {
  border-left-color: var(--color-info);
}

/* 알림 내용 */
.notification-content {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-3);
}

.notification-icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  margin-top: 2px;
}

.notification-message {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--color-gray-800);
}

.notification-close {
  flex-shrink: 0;
  background: none;
  border: none;
  color: var(--color-gray-400);
  cursor: pointer;
  padding: var(--spacing-1);
  border-radius: var(--radius-sm);
  transition: color 0.2s;
}

.notification-close:hover {
  color: var(--color-gray-600);
}
```

### 7.2 로딩 상태
```css
/* 로딩 스피너 */
.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-8);
  color: var(--color-gray-500);
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-gray-200);
  border-top: 2px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: var(--spacing-2);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 스켈레톤 로딩 */
.skeleton {
  background: linear-gradient(90deg, var(--color-gray-200) 25%, var(--color-gray-100) 50%, var(--color-gray-200) 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: var(--radius-md);
}

@keyframes loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.skeleton-text {
  height: 1em;
  margin-bottom: var(--spacing-2);
}

.skeleton-title {
  height: 1.5em;
  width: 60%;
  margin-bottom: var(--spacing-4);
}

.skeleton-button {
  height: 2.5em;
  width: 100px;
}
```

## 8. 반응형 디자인

### 8.1 모바일 최적화
```css
/* 모바일 네비게이션 */
@media (max-width: 768px) {
  .appbar-inner {
    padding: 0 var(--spacing-3);
  }
  
  .container {
    padding: var(--spacing-4) var(--spacing-3);
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-3);
  }
  
  .toolbar {
    width: 100%;
    justify-content: flex-end;
  }
  
  .btn-group {
    flex-direction: column;
  }
  
  .btn-group .btn {
    border-radius: var(--radius-md);
  }
}

/* 태블릿 최적화 */
@media (min-width: 769px) and (max-width: 1023px) {
  .container {
    padding: var(--spacing-6) var(--spacing-4);
  }
  
  .grid.cols-12 .col-span-6 {
    grid-column: span 12;
  }
}

/* 터치 친화적 UI */
@media (hover: none) and (pointer: coarse) {
  .btn {
    min-height: 44px;
    min-width: 44px;
  }
  
  .table th,
  .table td {
    padding: var(--spacing-3);
  }
  
  .input,
  .select,
  .textarea {
    min-height: 44px;
  }
}
```

### 8.2 접근성 개선
```css
/* 포커스 스타일 */
*:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* 스크린 리더 전용 텍스트 */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* 고대비 모드 지원 */
@media (prefers-contrast: high) {
  :root {
    --color-gray-300: #000000;
    --color-gray-400: #000000;
    --color-gray-500: #000000;
  }
}

/* 애니메이션 감소 설정 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

## 9. 인쇄 스타일

### 9.1 인쇄 최적화
```css
/* print.css */
@media print {
  /* 기본 인쇄 설정 */
  * {
    -webkit-print-color-adjust: exact;
    color-adjust: exact;
  }
  
  body {
    font-size: 12pt;
    line-height: 1.4;
    color: #000;
    background: #fff;
  }
  
  /* 인쇄하지 않을 요소 숨김 */
  .appbar,
  .breadcrumbs,
  .toolbar,
  .btn,
  .notifications,
  nav,
  footer {
    display: none !important;
  }
  
  /* 인쇄 전용 폼 */
  .print-form {
    display: block !important;
  }
  
  .print-form .doc {
    max-width: none;
    margin: 0;
    padding: 0;
    box-shadow: none;
    border: none;
  }
  
  /* 페이지 나누기 */
  .page-break {
    page-break-before: always;
  }
  
  .no-break {
    page-break-inside: avoid;
  }
  
  /* 테이블 인쇄 최적화 */
  .table {
    border-collapse: collapse;
    width: 100%;
  }
  
  .table th,
  .table td {
    border: 1px solid #000;
    padding: 4pt;
    font-size: 10pt;
  }
  
  .table th {
    background-color: #f0f0f0 !important;
    font-weight: bold;
  }
  
  /* 인쇄 날짜 */
  #print-date::before {
    content: "인쇄일시: " attr(data-date);
  }
}
```

---

## 10. 참조 문서

### 10.1 관련 문서
- **[CMMS_PRD.md](./CMMS_PRD.md)**: 제품 요구사항 정의서
- **[CMMS_STRUCTURES.md](./CMMS_STRUCTURES.md)**: 기술 아키텍처 가이드
- **[CMMS_JAVASCRIPT.md](./CMMS_JAVASCRIPT.md)**: JavaScript 개발 가이드

### 10.2 외부 참조
- [CSS Grid Layout](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Grid_Layout)
- [CSS Flexbox](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Flexible_Box_Layout)
- [CSS Custom Properties](https://developer.mozilla.org/en-US/docs/Web/CSS/Using_CSS_custom_properties)
- [Responsive Web Design](https://developer.mozilla.org/en-US/docs/Learn/CSS/CSS_layout/Responsive_Design)
