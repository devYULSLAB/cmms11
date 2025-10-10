/**
 * 유효성 검사 모듈
 * 
 * - 폼 유효성 검사
 * - 필드별 유효성 검사
 * - 에러 메시지 표시
 * 
 * @functions
 * - validateForm(form) - 폼 유효성 검사
 * - validateField(field) - 특정 필드 유효성 검사
 * - isValidEmail(email) - 이메일 유효성 검사
 * - isValidDate(dateString) - 날짜 유효성 검사
 * - isValidNumber(value, options) - 숫자 유효성 검사
 * - isValidLength(value, options) - 문자열 길이 검사
 * - isValidPattern(value, pattern) - 패턴 검사
 * - validatePasswordStrength(password) - 비밀번호 강도 검사
 * - showValidationErrors(form, errors) - 에러 메시지 표시
 * - setupRealTimeValidation(form) - 실시간 유효성 검사 설정
 * - initValidator() - 유효성 검사 모듈 초기화
 */

/**
 * 폼 유효성 검사
 * @param {HTMLElement} form - 검사할 폼 요소
 * @returns {Object} 검사 결과
 */
export function validateForm(form) {
  const errors = [];
  const requiredFields = form.querySelectorAll('[required]');
  
  requiredFields.forEach(field => {
    const value = field.value.trim();
    const label = form.querySelector(`label[for="${field.id}"]`)?.textContent || field.name;
    
    if (!value) {
      errors.push(`${label}은(는) 필수 입력 항목입니다.`);
      field.classList.add('error', 'is-invalid');
      field.classList.remove('is-valid');
    } else {
      field.classList.remove('error', 'is-invalid');
      field.classList.add('is-valid');
    }
  });
  
  // 이메일 필드 검사
  const emailFields = form.querySelectorAll('input[type="email"]');
  emailFields.forEach(field => {
    if (field.value && !isValidEmail(field.value)) {
      const label = form.querySelector(`label[for="${field.id}"]`)?.textContent || field.name;
      errors.push(`${label}의 형식이 올바르지 않습니다.`);
      field.classList.add('error', 'is-invalid');
      field.classList.remove('is-valid');
    }
  });
  
  // 숫자 필드 검사
  const numberFields = form.querySelectorAll('input[type="number"]');
  numberFields.forEach(field => {
    if (field.value && isNaN(Number(field.value))) {
      const label = form.querySelector(`label[for="${field.id}"]`)?.textContent || field.name;
      errors.push(`${label}은(는) 숫자여야 합니다.`);
      field.classList.add('error', 'is-invalid');
      field.classList.remove('is-valid');
    }
  });
  
  // 날짜 필드 검사
  const dateFields = form.querySelectorAll('input[type="date"]');
  dateFields.forEach(field => {
    if (field.value && !isValidDate(field.value)) {
      const label = form.querySelector(`label[for="${field.id}"]`)?.textContent || field.name;
      errors.push(`${label}의 형식이 올바르지 않습니다.`);
      field.classList.add('error', 'is-invalid');
      field.classList.remove('is-valid');
    }
  });
  
  return {
    isValid: errors.length === 0,
    errors
  };
}

/**
 * 특정 필드 유효성 검사
 * @param {HTMLElement} field - 검사할 필드 요소
 * @returns {boolean} 유효성 검사 결과
 */
export function validateField(field) {
  const form = field.closest('form');
  if (!form) return true;
  
  const validation = validateForm(form);
  const fieldError = validation.errors.find(error => 
    field.name && error.includes(field.name)
  );
  
  if (fieldError) {
    field.classList.add('error', 'is-invalid');
    field.classList.remove('is-valid');
    return false;
  } else {
    field.classList.remove('error', 'is-invalid');
    field.classList.add('is-valid');
    return true;
  }
}

/**
 * 이메일 유효성 검사
 * @param {string} email - 검사할 이메일 주소
 * @returns {boolean} 유효성 검사 결과
 */
export function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * 날짜 유효성 검사
 * @param {string} dateString - 검사할 날짜 문자열
 * @returns {boolean} 유효성 검사 결과
 */
export function isValidDate(dateString) {
  if (!dateString) return true;
  const date = new Date(dateString);
  return date instanceof Date && !isNaN(date.getTime());
}

/**
 * 숫자 유효성 검사
 * @param {string|number} value - 검사할 값
 * @param {Object} options - 검사 옵션
 * @returns {boolean} 유효성 검사 결과
 */
export function isValidNumber(value, options = {}) {
  const { min, max, integer = false } = options;
  const num = Number(value);
  
  if (isNaN(num)) return false;
  if (integer && !Number.isInteger(num)) return false;
  if (min !== undefined && num < min) return false;
  if (max !== undefined && num > max) return false;
  
  return true;
}

/**
 * 문자열 길이 검사
 * @param {string} value - 검사할 문자열
 * @param {Object} options - 검사 옵션
 * @returns {boolean} 유효성 검사 결과
 */
export function isValidLength(value, options = {}) {
  const { min, max } = options;
  const length = value.length;
  
  if (min !== undefined && length < min) return false;
  if (max !== undefined && length > max) return false;
  
  return true;
}

/**
 * 패턴 검사
 * @param {string} value - 검사할 값
 * @param {RegExp} pattern - 검사할 정규식 패턴
 * @returns {boolean} 유효성 검사 결과
 */
export function isValidPattern(value, pattern) {
  return pattern.test(value);
}

/**
 * 비밀번호 강도 검사
 * @param {string} password - 검사할 비밀번호
 * @returns {Object} 비밀번호 강도 정보
 */
export function validatePasswordStrength(password) {
  const result = {
    score: 0,
    feedback: []
  };
  
  if (password.length < 8) {
    result.feedback.push('비밀번호는 최소 8자 이상이어야 합니다.');
  } else {
    result.score++;
  }
  
  if (!/[a-z]/.test(password)) {
    result.feedback.push('소문자를 포함해야 합니다.');
  } else {
    result.score++;
  }
  
  if (!/[A-Z]/.test(password)) {
    result.feedback.push('대문자를 포함해야 합니다.');
  } else {
    result.score++;
  }
  
  if (!/[0-9]/.test(password)) {
    result.feedback.push('숫자를 포함해야 합니다.');
  } else {
    result.score++;
  }
  
  if (!/[^a-zA-Z0-9]/.test(password)) {
    result.feedback.push('특수문자를 포함해야 합니다.');
  } else {
    result.score++;
  }
  
  return result;
}

/**
 * 에러 메시지 표시
 * @param {HTMLElement} form - 폼 요소
 * @param {Array} errors - 에러 메시지 배열
 */
export function showValidationErrors(form, errors) {
  // 기존 에러 메시지 제거
  form.querySelectorAll('.validation-error').forEach(el => el.remove());
  
  if (errors.length === 0) return;
  
  // 에러 메시지 컨테이너 생성
  const errorContainer = document.createElement('div');
  errorContainer.className = 'alert alert-danger validation-error';
  errorContainer.innerHTML = `
    <h6>다음 항목을 확인해주세요:</h6>
    <ul class="mb-0">
      ${errors.map(error => `<li>${error}</li>`).join('')}
    </ul>
  `;
  
  // 폼 상단에 에러 메시지 삽입
  form.insertBefore(errorContainer, form.firstChild);
}

/**
 * 실시간 유효성 검사 설정
 * @param {HTMLElement} form - 폼 요소
 */
export function setupRealTimeValidation(form) {
  const fields = form.querySelectorAll('input, select, textarea');
  
  fields.forEach(field => {
    // 실시간 검사 이벤트 추가
    field.addEventListener('blur', () => {
      validateField(field);
    });
    
    field.addEventListener('input', () => {
      // 입력 중에는 에러 클래스만 제거
      if (field.classList.contains('is-invalid')) {
        field.classList.remove('is-invalid');
      }
    });
  });
  
  // 폼 제출 시 전체 검사
  form.addEventListener('submit', (e) => {
    const validation = validateForm(form);
    
    if (!validation.isValid) {
      e.preventDefault();
      showValidationErrors(form, validation.errors);
      
      // 첫 번째 에러 필드로 포커스
      const firstErrorField = form.querySelector('.is-invalid');
      if (firstErrorField) {
        firstErrorField.focus();
      }
    }
  });
}

/**
 * 유효성 검사 모듈 초기화 함수
 */
export function initValidator() {
  // 기존 window.cmms.common.Validator 호환성 유지
  window.cmms = window.cmms || {};
  window.cmms.common = window.cmms.common || {};
  window.cmms.common.Validator = {
    validate: validateForm,
    validateField: validateField,
    isValidEmail: isValidEmail,
    isValidDate: isValidDate,
    isValidNumber: isValidNumber,
    isValidLength: isValidLength,
    isValidPattern: isValidPattern,
    validatePasswordStrength: validatePasswordStrength,
    showErrors: showValidationErrors,
    setupRealTime: setupRealTimeValidation
  };
}
