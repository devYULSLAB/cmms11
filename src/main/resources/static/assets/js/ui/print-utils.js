/**
 * 인쇄 유틸리티 모듈
 * 
 * 인쇄 관련 기능을 제공하는 모듈입니다.
 * - 인쇄 기능
 * - 인쇄 스타일링
 * - 인쇄 최적화
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
 * 인쇄 미리보기
 * @param {HTMLElement} element - 미리보기할 요소
 * @param {Object} options - 미리보기 옵션
 */
export function printPreview(element, options = {}) {
  const config = Object.assign({
    title: '인쇄 미리보기',
    width: '80%',
    height: '80%'
  }, options);
  
  const previewWindow = window.open('', '_blank', 
    `width=${config.width},height=${config.height},scrollbars=yes,resizable=yes`
  );
  
  if (!previewWindow) {
    console.error('Failed to open preview window');
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
        ${config.styles ? config.styles.join('\n') : ''}
        body { margin: 20px; }
      </style>
    </head>
    <body>
      ${element ? element.outerHTML : document.body.innerHTML}
      <div style="margin-top: 20px; text-align: center;">
        <button onclick="window.print()" class="btn btn-primary">인쇄</button>
        <button onclick="window.close()" class="btn btn-secondary">닫기</button>
      </div>
    </body>
    </html>
  `;
  
  previewWindow.document.write(html);
  previewWindow.document.close();
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
    printPreview: printPreview
  };
}
