/**
 * CMMS 공통 JavaScript 모듈 (app.js 확장)
 * 
 * SPA 아키텍처를 위한 공통 유틸리티들을 제공합니다.
 * 모든 모듈에서 재사용 가능한 컴포넌트와 이벤트 핸들러를 포함합니다.
 * 
 * 주요 기능:
 * - TableManager: 동적 테이블 행 관리 (추가/삭제/재정렬)
 * - FormManager: 폼 제출, 검증, CSRF 토큰 처리
 * - DataLoader: API 호출 및 데이터 로딩 (로딩/에러 상태 관리)
 * - ConfirmDialog: 확인 대화상자
 * - Validator: 폼 유효성 검사
 * - FileUpload: 파일 업로드 위젯 초기화
 * - 공통 이벤트: 취소 버튼, 폼 매니저, 유효성 검사 자동 바인딩
 * 
 * 사용법:
 * - window.cmms.common.TableManager.init(tableElement, config)
 * - window.cmms.common.FormManager.init(formElement, config)  
 * - window.cmms.common.DataLoader.load(url, options)
 * - window.cmms.common.ConfirmDialog.show(message, callback)
 * - window.cmms.common.Validator.validate(form)
 * - window.cmms.common.initFileUploadWidgets(root)
 * 
 * SPA 속성 지원:
 * - data-form-manager: 폼 자동 초기화
 * - data-cancel-btn: 취소 버튼 공통 처리
 * - data-validate: 유효성 검사 자동 바인딩
 * - data-table-manager: 테이블 매니저 자동 초기화
 * 
 * 의존성: app.js가 먼저 로드되어야 함
 */

// =============================================================================
// 의존성 확인
// =============================================================================
(function checkDependencies() {
  if (!window.cmms) {
    console.error('common.js: app.js가 먼저 로드되어야 합니다.');
    return false;
  }
  
  const required = ['csrf', 'notification', 'navigation'];
  const missing = required.filter(module => !window.cmms[module]);
  
  if (missing.length > 0) {
    console.warn(`common.js: 필요한 모듈이 없습니다: ${missing.join(', ')}`);
  }
  
  return missing.length === 0;
})();

// =============================================================================
// 테이블 행 관리 유틸리티 (app.js 확장)
// =============================================================================
window.cmms.common = window.cmms.common || {};

window.cmms.common.TableManager = {
  /**
   * 동적 테이블 행 추가/삭제를 관리하는 클래스
   * @param {HTMLElement} table - 대상 테이블 요소
   * @param {Object} options - 옵션 설정
   * @param {string} options.rowSelector - 행 선택자 (예: '.item-row')
   * @param {string} options.numberField - 순번 필드 선택자 (예: '.line-no')
   * @param {string} options.addButton - 추가 버튼 선택자
   * @param {string} options.removeButton - 삭제 버튼 선택자 (data 속성)
   * @param {Function} options.rowTemplate - 새 행 생성 함수
   * @param {Function} options.onAdd - 행 추가 후 콜백
   * @param {Function} options.onRemove - 행 삭제 후 콜백
   */
  init: function(table, options = {}) {
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
      
      // 순번 재정렬
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
      
      // 행 추가
      addRow: function(data = {}) {
        const currentRows = tbody.querySelectorAll(config.rowSelector);
        const index = currentRows.length;
        
        let rowElement;
        
        if (typeof config.rowTemplate === 'function') {
          rowElement = config.rowTemplate(index, data);
        } else {
          // 기본 템플릿 (<template> 태그 전제로 고정)
          const template = table.querySelector('[data-template]');
          if (template && template.tagName === 'TEMPLATE') {
            rowElement = template.content.firstElementChild.cloneNode(true);
          } else {
            console.warn('TableManager: <template data-template> 요소를 찾을 수 없습니다. 새로운 구조로 전환하세요.');
            return null;
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
      
      // 행 삭제
      removeRow: async function(rowElement) {
        if (!rowElement) return false;
        
        const currentRows = tbody.querySelectorAll(config.rowSelector);
        if (currentRows.length <= config.minRows) {
          window.cmms.notification.warning(`최소 ${config.minRows}개의 항목은 유지되어야 합니다.`);
          return false;
        }
        
      // 삭제 확인 (기존 notification 활용)
      if (config.confirmRemove !== false) {
        if (!await window.cmms.common.ConfirmDialog.delete()) {
          return false;
        }
      }
        
        rowElement.remove();
        manager.renumber();
        
        if (typeof config.onRemove === 'function') {
          config.onRemove(rowElement);
        }
        
        return true;
      },
      
      // 행 이벤트 바인딩
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
      
      // 기존 행들 이벤트 바인딩 (템플릿 행 제외)
      initExistingRows: function() {
        // tbody 내의 실제 행들만 처리 (템플릿은 제외)
        tbody.querySelectorAll(config.rowSelector).forEach(row => {
          manager.attachRowHandlers(row);
        });
        manager.renumber();
      },
      
      // 서버 데이터로 행들 초기화
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
      
      // 폼 데이터 직렬화
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
};

// =============================================================================
// 폼 관리 유틸리티 (app.js의 SPA 폼 처리와 협력)
// =============================================================================
window.cmms.common.FormManager = {
  /**
   * 폼 제출을 표준화하여 처리하는 함수
   * app.js의 기존 SPA 폼 처리와 협력하여 동작합니다.
   * @param {HTMLElement} form - 대상 폼 요소
   * @param {Object} options - 옵션 설정
   */
  init: function(form, options = {}) {
    const config = Object.assign({
      method: 'POST',
      showLoader: true,
      showSuccessMessage: true,
      bypassSPA: false, // SPA 처리를 우회할지 여부
      redirect: null,
      onSuccess: null,
      onError: null
    }, options);
    
    if (!form || form.__cmmsFormBound || form.__cmmsHandled) return null;
    form.__cmmsFormBound = true;
    
    const manager = {
      form,
      config,
      
      // 표준화된 폼 제출
      submit: async function(formData = null) {
        const data = formData || new FormData(form);
        const action = form.getAttribute('action') || window.location.href;
        const method = (form.getAttribute('method') || config.method).toUpperCase();
        
        // 로더 표시
        if (config.showLoader) {
          this.showLoader();
        }
        
        try {
          const response = await fetch(action, {
            method,
            body: data,
            credentials: 'same-origin'
          });
          
          if (!response.ok) {
            if (response.status === 403) {
              throw window.cmms.csrf.toCsrfError(response);
            }
            throw new Error(`폼 제출 실패: ${response.status}`);
          }
          
          // 성공 처리
          if (config.showSuccessMessage) {
            const mode = form.querySelector('[data-mode]')?.textContent || '처리';
            window.cmms.notification.success(`${mode}이 완료되었습니다.`);
          }
          
          // 리다이렉트 처리
          const redirectTo = form.getAttribute('data-redirect') || config.redirect;
          if (redirectTo) {
            if (window.cmms?.navigation?.navigate) {
              window.cmms.navigation.navigate(redirectTo);
            } else {
              window.location.href = redirectTo;
            }
          }
          
          // 성공 콜백
          if (typeof config.onSuccess === 'function') {
            config.onSuccess(response);
          }
          
        } catch (error) {
          console.error('폼 제출 오류:', error);
          window.cmms.notification.error('요청 처리에 실패했습니다. 다시 시도해주세요.');
          
          if (typeof config.onError === 'function') {
            config.onError(error);
          }
        } finally {
          if (config.showLoader) {
            this.hideLoader();
          }
        }
      },
      
      showLoader: function() {
        const submitBtn = form.querySelector('[type="submit"]');
        if (submitBtn) {
          submitBtn.disabled = true;
          const originalText = submitBtn.textContent;
          submitBtn.textContent = '처리중...';
          submitBtn.dataset.originalText = originalText;
        }
      },
      
      hideLoader: function() {
        const submitBtn = form.querySelector('[type="submit"]');
        if (submitBtn) {
          submitBtn.disabled = false;
          const originalText = submitBtn.dataset.originalText;
          if (originalText) {
            submitBtn.textContent = originalText;
          }
        }
      }
    };
    
    // 폼 제출 이벤트 바인딩
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      manager.submit();
    });
    
    return manager;
  }
};

// =============================================================================
// 확인 다이얼로그 표준화
// =============================================================================
window.cmms.common.ConfirmDialog = {
  /**
   * 표준화된 확인 다이얼로그
   * @param {string} message - 확인 메시지
   * @param {Object} options - 옵션 설정
   * @returns {Promise<boolean>} 확인 결과
   */
  show: function(message, options = {}) {
    const config = Object.assign({
      title: '확인',
      confirmText: '확인',
      cancelText: '취소',
      type: 'warning' // success, warning, error
    }, options);
    
    return new Promise((resolve) => {
      if (config.useNative !== false && typeof confirm === 'function') {
        // 네이티브 확인창 사용
        const result = confirm(message);
        resolve(result);
      } else {
        // 커스텀 모달 구현 가능
        // 현재는 네이티브 사용
        const result = confirm(`${config.title}\n\n${message}`);
        resolve(result);
      }
    });
  },
  
  delete: function(callback) {
    return this.show('정말로 삭제하시겠습니까?', {
      type: 'error',
      confirmText: '삭제'
    });
  },
  
  save: function(callback) {
    return this.show('저장하시겠습니까?', {
      type: 'success',
      confirmText: '저장'
    });
  }
};

// =============================================================================
// 데이터 로더 유틸리티
// =============================================================================
window.cmms.common.DataLoader = {
  /**
   * 서버에서 데이터를 로드하는 표준화된 함수
   * @param {string} endpoint - API 엔드포인트
   * @param {Object} options - 옵션 설정
   * @returns {Promise<any>} 로드된 데이터
   */
  load: async function(endpoint, options = {}) {
    const config = Object.assign({
      method: 'GET',
      params: {},
      showLoading: false,
      timeout: 30000
    }, options);
    
    try {
      if (config.showLoading) {
        window.cmms.notification.show('데이터를 불러오는 중...', 'info', 500);
      }
      
      const url = new URL(endpoint, window.location.origin);
      Object.keys(config.params).forEach(key => {
        url.searchParams.set(key, config.params[key]);
      });
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), config.timeout);
      
      const response = await fetch(url.toString(), {
        method: config.method,
        credentials: 'same-origin',
        signal: controller.signal
      });
      
      clearTimeout(timeoutId);
      
      if (!response.ok) {
        throw new Error(`데이터 로드 실패: ${response.status}`);
      }
      
      const data = await response.json();
      
      if (config.showLoading) {
        window.cmms.notification.success('데이터를 성공적으로 불러왔습니다.');
      }
      
      return data;
      
    } catch (error) {
      console.error('데이터 로드 오류:', error);
      
      if (config.showLoading) {
        window.cmms.notification.error('데이터 로드에 실패했습니다.');
      }
      
      throw error;
    }
  }
};

// =============================================================================
// 유효성 검사 유틸리티
// =============================================================================
window.cmms.common.Validator = {
  /**
   * 폼 유효성 검사
   * @param {HTMLElement} form - 검사할 폼 요소
   * @returns {Object} 검사 결과
   */
  validate: function(form) {
    const errors = [];
    const requiredFields = form.querySelectorAll('[required]');
    
    requiredFields.forEach(field => {
      const value = field.value.trim();
      const label = form.querySelector(`label[for="${field.id}"]`)?.textContent || field.name;
      
      if (!value) {
        errors.push(`${label}은(는) 필수 입력 항목입니다.`);
        field.classList.add('error');
      } else {
        field.classList.remove('error');
      }
    });
    
    return {
      isValid: errors.length === 0,
      errors
    };
  },
  
  /**
   * 특정 필드 유효성 검사
   * @param {HTMLElement} field - 검사할 필드 요소
   * @returns {boolean} 유효성 검사 결과
   */
  validateField: function(field) {
    const validation = this.validate(field.closest('form'));
    const fieldError = validation.errors.find(error => 
      field.name && error.includes(field.name)
    );
    
    if (fieldError) {
      field.classList.add('error');
      return false;
    } else {
      field.classList.remove('error');
      return true;
    }
  }
};

// =============================================================================
// 공통 초기화 함수 (app.js 보완)
// app.js의 DOMContentLoaded 이후 실행되어 충돌 방지
// =============================================================================
(function() {
  // app.js 초기화 완료 후 실행하기 위한 약간의 지연
  const initCommon = function() {
    // 앱 모듈이 준비되었는지 확인
    if (!window.cmms || !window.cmms.common) return;
    
    // 모든 테이블 매니저 자동 초기화
    document.querySelectorAll('[data-table-manager]').forEach(table => {
      // 이미 초기화된 테이블은 건너뛰기
      if (table.__tableManagerInitialized) return;
      table.__tableManagerInitialized = true;
      
      const config = {};
      
      // data 속성에서 설정 읽기
      if (table.dataset.rowSelector) config.rowSelector = table.dataset.rowSelector;
      if (table.dataset.numberField) config.numberField = table.dataset.numberField;
      if (table.dataset.minRows) config.minRows = parseInt(table.dataset.minRows);
      
      // 테이블 매니저 초기화
      window.cmms.common.TableManager.init(table, config);
    });
    
    // 모든 폼 매니저 자동 초기화 (SPA 폼과 구분)
    document.querySelectorAll('form[data-form-manager]:not([data-redirect])').forEach(form => {
      // 이미 app.js에 의해 처리된 폼은 건너뛰기
      if (form.__cmmsHandled) return;
      
      const config = {};
      
      if (form.dataset.method) config.method = form.dataset.method;
      if (form.dataset.redirect) config.redirect = form.dataset.redirect;
      
      window.cmms.common.FormManager.init(form, config);
    });
    
    // 취소 버튼 공통 처리 (Domain 모듈용)
    document.querySelectorAll('[data-cancel-btn]').forEach(btn => {
      if (btn.__cancelBound) return;
      btn.addEventListener('click', () => {
        if (window.cmms?.navigation?.navigate) {
          // 현재 경로에서 list로 이동
          const currentPath = window.location.pathname;
          const listPath = currentPath.replace(/\/form$/, '/list').replace(/\/edit\/[^/]+$/, '/list');
          window.cmms.navigation.navigate(listPath);
        } else {
          window.history.back();
        }
      });
      btn.__cancelBound = true;
    });
    
    // 유효성 검사 바인딩 (app.js 폼 처리와 중복되지 않도록)
    document.querySelectorAll('form[data-validate]:not([data-redirect])').forEach(form => {
      const submitBtn = form.querySelector('[type="submit"]');
      if (submitBtn && !submitBtn.__validationBound) {
        submitBtn.__validationBound = true;
        submitBtn.addEventListener('click', (e) => {
          const validation = window.cmms.common.Validator.validate(form);
          if (!validation.isValid) {
            e.preventDefault();
            validation.errors.forEach(error => {
              window.cmms.notification.error(error);
            });
          }
        });
      }
    });
  };
  
  // 초기화 실행 (DOM 로드 후)
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      setTimeout(initCommon, 10); // app.js 초기화 후 실행
    });
  } else {
    setTimeout(initCommon, 10);
  }
})();

// =============================================================================
// 레거시 호환성 및 헬퍼 함수들
// =============================================================================

/**
 * 간편 테이블 초기화 함수 (레거시 호환성)
 * @param {string} selector - 테이블 선택자
 * @param {Object} options - 옵션
 */
window.cmms.initTable = function(selector, options = {}) {
  const table = document.querySelector(selector);
  return table ? window.cmms.common.TableManager.init(table, options) : null;
};

/**
 * 간편 폼 초기화 함수 (레거시 호환성)
 * @param {string} selector - 폼 선택자
 * @param {Object} options - 옵션
 */
window.cmms.initForm = function(selector, options = {}) {
  const form = document.querySelector(selector);
  return form ? window.cmms.common.FormManager.init(form || document.forms[0], options) : null;
};
