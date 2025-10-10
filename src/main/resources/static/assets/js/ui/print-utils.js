/**
 * 인쇄 유틸리티 모듈
 * 
 * - 인쇄 기능
 * - 인쇄 스타일링
 * - 인쇄 최적화
 * 
 * @functions
 * - printPage(options) - 현재 페이지 인쇄
 * - printElement(element, options) - 특정 요소 인쇄
 * - createPrintStyles(config) - 인쇄용 스타일 생성 (내부 함수)
 * - addPageBreak(element) - 페이지 나누기 요소 추가
 * - optimizeForPrint(element) - 인쇄 최적화 클래스 추가
 * - createPrintButton(options) - 인쇄 버튼 생성
 * - initPrintButton(root) - 인쇄 버튼 초기화
 * - initPrintUtils() - 인쇄 유틸리티 모듈 초기화
 */

/**
 * 현재 페이지 인쇄
 * @param {Object} options - 인쇄 옵션
 */
export function printPage(options = {}) {
  const config = Object.assign({
    title: document.title,
    styles: [],
    hideElements: ['.no-print', '.sidebar', '.navbar', '.footer'],
    showElements: []
  }, options);
  
  // 인쇄용 스타일 생성
  const printStyles = createPrintStyles(config);
  
  // 인쇄용 스타일을 head에 추가
  const styleElement = document.createElement('style');
  styleElement.id = 'print-styles';
  styleElement.textContent = printStyles;
  document.head.appendChild(styleElement);
  
  // 인쇄 전 이벤트
  if (typeof config.beforePrint === 'function') {
    config.beforePrint();
  }
  
  // 인쇄 실행
  window.print();
  
  // 인쇄 후 정리
  setTimeout(() => {
    const printStyleElement = document.getElementById('print-styles');
    if (printStyleElement) {
      printStyleElement.remove();
    }
    
    // 인쇄 후 이벤트
    if (typeof config.afterPrint === 'function') {
      config.afterPrint();
    }
  }, 1000);
}

/**
 * 특정 요소 인쇄
 * @param {HTMLElement} element - 인쇄할 요소
 * @param {Object} options - 인쇄 옵션
 */
export function printElement(element, options = {}) {
  const config = Object.assign({
    title: document.title,
    styles: [],
    newWindow: true
  }, options);
  
  if (!element) {
    console.error('Print element is required');
    return;
  }
  
  const printWindow = config.newWindow ? window.open('', '_blank') : window;
  
  if (!printWindow) {
    console.error('Failed to open print window');
    return;
  }
  
  const html = `
    <!DOCTYPE html>
    <html>
    <head>
      <title>${config.title}</title>
      <meta charset="utf-8">
      <style>
        ${createPrintStyles(config)}
        ${config.styles.join('\n')}
      </style>
    </head>
    <body>
      ${element.outerHTML}
    </body>
    </html>
  `;
  
  printWindow.document.write(html);
  printWindow.document.close();
  
  // 인쇄 전 이벤트
  if (typeof config.beforePrint === 'function') {
    config.beforePrint(printWindow);
  }
  
  printWindow.focus();
  printWindow.print();
  
  if (config.newWindow) {
    printWindow.close();
  }
  
  // 인쇄 후 이벤트
  if (typeof config.afterPrint === 'function') {
    config.afterPrint(printWindow);
  }
}

/**
 * 인쇄용 스타일 생성
 * @param {Object} config - 스타일 설정
 * @returns {string} 인쇄용 CSS
 */
function createPrintStyles(config) {
  const hideSelectors = config.hideElements.map(selector => 
    `${selector} { display: none !important; }`
  ).join('\n');
  
  const showSelectors = config.showElements.map(selector => 
    `${selector} { display: block !important; }`
  ).join('\n');
  
  return `
    @media print {
      * {
        -webkit-print-color-adjust: exact !important;
        color-adjust: exact !important;
      }
      
      body {
        font-size: 12pt;
        line-height: 1.4;
        color: #000;
        background: #fff;
      }
      
      .container {
        max-width: none !important;
        width: 100% !important;
      }
      
      .row {
        margin: 0 !important;
      }
      
      .col, .col-* {
        padding: 5px !important;
      }
      
      table {
        width: 100% !important;
        border-collapse: collapse;
        page-break-inside: avoid;
      }
      
      table th, table td {
        border: 1px solid #000 !important;
        padding: 4px !important;
        font-size: 11pt;
      }
      
      table th {
        background-color: #f5f5f5 !important;
        font-weight: bold;
      }
      
      .btn {
        display: none !important;
      }
      
      .card {
        border: 1px solid #000 !important;
        box-shadow: none !important;
      }
      
      .card-header {
        background-color: #f5f5f5 !important;
        border-bottom: 1px solid #000 !important;
      }
      
      ${hideSelectors}
      ${showSelectors}
      
      .page-break {
        page-break-before: always;
      }
      
      .no-page-break {
        page-break-inside: avoid;
      }
      
      .print-only {
        display: block !important;
      }
      
      .no-print {
        display: none !important;
      }
    }
  `;
}

/**
 * 페이지 나누기 요소 추가
 * @param {HTMLElement} element - 요소 앞에 페이지 나누기 추가
 */
export function addPageBreak(element) {
  if (!element) return;
  
  const pageBreak = document.createElement('div');
  pageBreak.className = 'page-break';
  element.parentNode.insertBefore(pageBreak, element);
}

/**
 * 인쇄 최적화 클래스 추가
 * @param {HTMLElement} element - 최적화할 요소
 */
export function optimizeForPrint(element) {
  if (!element) return;
  
  element.classList.add('no-page-break');
  
  // 테이블인 경우 인쇄 최적화
  if (element.tagName === 'TABLE') {
    const rows = element.querySelectorAll('tr');
    rows.forEach(row => {
      if (row.offsetHeight > 100) { // 높은 행은 페이지 나누기 방지
        row.classList.add('no-page-break');
      }
    });
  }
}

/**
 * 인쇄 버튼 생성
 * @param {Object} options - 버튼 옵션
 * @returns {HTMLElement} 인쇄 버튼 요소
 */
export function createPrintButton(options = {}) {
  const config = Object.assign({
    text: '인쇄',
    className: 'btn btn-primary',
    icon: '🖨️',
    target: null // 인쇄할 요소 (null이면 전체 페이지)
  }, options);
  
  const button = document.createElement('button');
  button.className = config.className;
  button.innerHTML = `${config.icon} ${config.text}`;
  
  button.addEventListener('click', () => {
    if (config.target) {
      printElement(config.target, config);
    } else {
      printPage(config);
    }
  });
  
  return button;
}

/**
 * 인쇄 메타데이터 채우기 (출력일시, 인쇄자)
 * @param {HTMLElement} root - 루트 요소
 */
function fillPrintMetadata(root) {
  // 출력일시 채우기
  const printDateElement = root.querySelector('#print-date');
  if (printDateElement) {
    const now = new Date();
    const dateStr = now.getFullYear() + '-' + 
                    String(now.getMonth() + 1).padStart(2, '0') + '-' + 
                    String(now.getDate()).padStart(2, '0') + ' ' +
                    String(now.getHours()).padStart(2, '0') + ':' + 
                    String(now.getMinutes()).padStart(2, '0');
    printDateElement.textContent = dateStr;
  }
  
  // 인쇄자 정보 채우기
  const printUserElement = root.querySelector('#print-user');
  if (printUserElement) {
    // 로그인 사용자 정보 가져오기
    // 1. appbar의 .meta > .badge에서 memberId 가져오기
    const memberBadge = document.querySelector('.appbar .meta .badge');
    const currentUser = memberBadge?.textContent?.trim() || 
                       window.cmms?.auth?.currentUser || 
                       localStorage.getItem('currentUser') || 
                       'Unknown';
    printUserElement.textContent = currentUser;
  }
}

/**
 * 인쇄 버튼 초기화 (root 기반)
 * @param {HTMLElement} root - 루트 요소
 */
export function initPrintButton(root) {
  const printBtn = root.querySelector('[data-print-btn]');
  if (!printBtn) {
    console.log('Print button not found in root');
    return;
  }
  
  // 중복 초기화 방지
  if (printBtn.dataset.initialized === 'true') {
    console.log('Print button already initialized');
    return;
  }
  
  printBtn.addEventListener('click', () => {
    // 인쇄 전에 출력일시와 인쇄자 정보 채우기
    fillPrintMetadata(root);
    
    // data-print-target 속성이 있으면 해당 요소만 인쇄
    const targetSelector = printBtn.dataset.printTarget;
    if (targetSelector) {
      const targetElement = root.querySelector(targetSelector);
      if (targetElement) {
        printElement(targetElement, {
          title: document.title,
          styles: []
        });
      } else {
        console.error('Print target element not found:', targetSelector);
      }
    } else {
      // 전체 페이지 인쇄
      printPage({
        title: document.title,
        hideElements: ['.no-print', '.sidebar', '.navbar', '.footer', '.btn']
      });
    }
  });
  
  printBtn.dataset.initialized = 'true';
  console.log('Print button initialized');
}

/**
 * 인쇄 유틸리티 모듈 초기화 함수
 */
export function initPrintUtils() {
  // 기존 window.cmms.printUtils 호환성 유지
  window.cmms = window.cmms || {};
  window.cmms.printUtils = {
    printPage: printPage,
    printElement: printElement,
    addPageBreak: addPageBreak,
    optimizeForPrint: optimizeForPrint,
    createPrintButton: createPrintButton,
    initPrintButton: initPrintButton
  };
}
