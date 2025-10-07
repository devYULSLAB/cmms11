/**
 * 테이블 관리 모듈
 * 
 * common.js에서 테이블 관리 기능을 추출한 모듈입니다.
 * - 동적 테이블 행 관리 (추가/삭제/재정렬)
 * - 데이터 바인딩
 * - 이벤트 처리
 */

/**
 * 테이블 매니저 초기화 함수
 * @param {HTMLElement} table - 대상 테이블 요소
 * @param {Object} options - 옵션 설정
 * @returns {Object} 테이블 매니저 객체
 */
export function initTableManager(table, options = {}) {
  const config = Object.assign({
    rowSelector: '.item-row',
    numberField: '.line-no',
    addButton: '[data-add-row]',
    removeButton: '[data-remove-row]',
    namePattern: /items\[(\d+)\]\.(\w+)/,
    minRows: 1
  }, options);

  if (!table) {
    console.warn('TableManager: 대상 테이블 요소를 찾을 수 없습니다.');
    return null;
  }

  const tbody = table.querySelector('tbody') || table;
  const manager = {
    table,
    tbody,
    config,
    
    /**
     * 순번 재정렬
     */
    renumber: function() {
      const rows = tbody.querySelectorAll(config.rowSelector);
      rows.forEach((row, index) => {
        // 순번 표시 업데이트
        const numberField = row.querySelector(config.numberField);
        if (numberField) {
          numberField.textContent = index + 1;
        }
        
        // name 속성 재정렬 (items[0].field -> items[index].field)
        const inputs = row.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
          const name = input.name;
          if (name && config.namePattern.test(name)) {
            input.name = name.replace(config.namePattern, `items[${index}].$2`);
          }
        });
        
        // 삭제 버튼 표시/숨김
        const removeBtn = row.querySelector(config.removeButton);
        if (removeBtn && rows.length <= config.minRows) {
          removeBtn.style.display = 'none';
        } else if (removeBtn) {
          removeBtn.style.display = '';
        }
      });
      
      // 콜백 실행
      if (typeof config.afterRenumber === 'function') {
        config.afterRenumber(rows.length);
      }
    },
    
    /**
     * 행 추가
     * @param {Object} data - 행에 추가할 데이터
     * @returns {HTMLElement} 생성된 행 요소
     */
    addRow: function(data = {}) {
      const currentRows = tbody.querySelectorAll(config.rowSelector);
      const index = currentRows.length;
      
      let rowElement;
      
      if (typeof config.rowTemplate === 'function') {
        rowElement = config.rowTemplate(index, data);
      } else {
        // 기본 템플릿 (<template> 태그 또는 숨겨진 행)
        const template = table.querySelector('[data-template]');
        if (template && template.tagName === 'TEMPLATE') {
          rowElement = template.content.firstElementChild.cloneNode(true);
        } else {
          // 숨겨진 행을 템플릿으로 사용 (기존 호환성)
          const hiddenRow = table.querySelector('tbody tr.hidden, tbody tr[style*="display: none"]');
          if (hiddenRow) {
            rowElement = hiddenRow.cloneNode(true);
            rowElement.style.display = '';
            rowElement.classList.remove('hidden');
          } else {
            console.warn('TableManager: 템플릿 요소를 찾을 수 없습니다. <template data-template> 또는 숨겨진 행을 추가하세요.');
            return null;
          }
        }
      }
      
      if (rowElement) {
        tbody.appendChild(rowElement);
        manager.attachRowHandlers(rowElement);
        manager.renumber();
        
        if (typeof config.onAdd === 'function') {
          config.onAdd(rowElement, index, data);
        }
      }
      
      return rowElement;
    },
    
    /**
     * 행 삭제
     * @param {HTMLElement} rowElement - 삭제할 행 요소
     * @returns {boolean} 삭제 성공 여부
     */
    removeRow: async function(rowElement) {
      if (!rowElement) return false;
      
      const currentRows = tbody.querySelectorAll(config.rowSelector);
      if (currentRows.length <= config.minRows) {
        if (window.cmms && window.cmms.notification) {
          window.cmms.notification.warning(`최소 ${config.minRows}개의 항목은 유지되어야 합니다.`);
        }
        return false;
      }
      
      // 삭제 확인
      if (config.confirmRemove !== false) {
        if (window.cmms && window.cmms.common && window.cmms.common.ConfirmDialog) {
          if (!await window.cmms.common.ConfirmDialog.delete()) {
            return false;
          }
        } else {
          // Fallback: 네이티브 confirm
          if (!confirm('정말로 삭제하시겠습니까?')) {
            return false;
          }
        }
      }
        
      rowElement.remove();
      manager.renumber();
      
      if (typeof config.onRemove === 'function') {
        config.onRemove(rowElement);
      }
      
      return true;
    },
    
    /**
     * 행 이벤트 바인딩
     * @param {HTMLElement} rowElement - 행 요소
     */
    attachRowHandlers: function(rowElement) {
      const removeBtn = rowElement.querySelector(config.removeButton);
      if (removeBtn && !removeBtn.__bound) {
        removeBtn.__bound = true;
        removeBtn.addEventListener('click', (e) => {
          e.preventDefault();
          manager.removeRow(rowElement);
        });
      }
    },
    
    /**
     * 기존 행들 이벤트 바인딩 (템플릿 행 제외)
     */
    initExistingRows: function() {
      // tbody 내의 실제 행들만 처리 (템플릿은 제외)
      tbody.querySelectorAll(config.rowSelector).forEach(row => {
        manager.attachRowHandlers(row);
      });
      manager.renumber();
    },
    
    /**
     * 서버 데이터로 행들 초기화
     * @param {Array} dataArray - 초기화할 데이터 배열
     */
    loadFromData: function(dataArray) {
      if (!Array.isArray(dataArray)) return;
      
      // tbody 내의 실제 행들만 제거 (템플릿은 tbody 밖에 있으므로 자동 제외)
      tbody.querySelectorAll(config.rowSelector).forEach(row => {
        row.remove();
      });
      
      // 새 행들 추가
      dataArray.forEach(data => {
        manager.addRow(data);
      });
    },
    
    /**
     * 폼 데이터 직렬화
     * @returns {Array} 직렬화된 행 데이터 배열
     */
    serialize: function() {
      const rows = [];
      tbody.querySelectorAll(config.rowSelector).forEach(row => {
        const rowData = {};
        const inputs = row.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
          const name = input.name;
          if (name) {
            const match = name.match(config.namePattern);
            if (match && match[2]) {
              rowData[match[2]] = input.value;
            }
          }
        });
        if (Object.keys(rowData).length > 0) {
          rows.push(rowData);
        }
      });
      return rows;
    },
    
    /**
     * 테이블 데이터 검증
     * @returns {Object} 검증 결과
     */
    validate: function() {
      const errors = [];
      const rows = tbody.querySelectorAll(config.rowSelector);
      
      rows.forEach((row, index) => {
        const requiredFields = row.querySelectorAll('[required]');
        requiredFields.forEach(field => {
          if (!field.value.trim()) {
            const label = row.querySelector(`label[for="${field.id}"]`)?.textContent || field.name;
            errors.push(`행 ${index + 1}: ${label}은(는) 필수 입력 항목입니다.`);
          }
        });
      });
      
      return {
        isValid: errors.length === 0,
        errors
      };
    }
  };
  
  // 추가 버튼 이벤트 바인딩
  const addBtn = table.querySelector(config.addButton);
  if (addBtn && !addBtn.__bound) {
    addBtn.__bound = true;
    addBtn.addEventListener('click', (e) => {
      e.preventDefault();
      manager.addRow();
    });
  }
  
  // 기존 행들 초기화
  manager.initExistingRows();
  
  return manager;
}

/**
 * 테이블 매니저 모듈 초기화 함수
 */
export function initTableManagerModule() {
  // 기존 window.cmms.common.TableManager 호환성 유지
  window.cmms = window.cmms || {};
  window.cmms.common = window.cmms.common || {};
  window.cmms.common.TableManager = {
    init: initTableManager
  };
  
  // 레거시 호환성을 위한 전역 함수
  window.cmms.initTable = function(selector, options = {}) {
    const table = document.querySelector(selector);
    return table ? initTableManager(table, options) : null;
  };
}
