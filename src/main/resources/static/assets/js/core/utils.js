/**
 * 공통 유틸리티 모듈
 * 
 * - 파일 크기 포맷팅
 * - DOM 유틸리티 함수들
 * - 문자열 처리 함수들
 * - 날짜 처리 함수들
 * - 기타 공통 함수들
 * 
 * @functions
 * - findAll(root, selector) - 루트 요소에서 선택자 검색
 * - formatFileSize(bytes) - 바이트 크기를 읽기 쉬운 형식으로 변환
 * - createAttachmentListItem(file, doc) - 첨부 파일 목록 아이템 생성
 * - debounce(func, wait) - 디바운스 함수
 * - throttle(func, limit) - 쓰로틀 함수
 * - isValidEmail(email) - 이메일 유효성 검사
 * - sanitizeHtml(html) - HTML 태그 제거 및 안전화
 * - getCookie(name) - 쿠키 값 읽기
 * - setCookie(name, value, days) - 쿠키 설정
 * - removeCookie(name) - 쿠키 삭제
 * - copyToClipboard(text) - 클립보드에 텍스트 복사
 * - downloadFile(url, filename) - 파일 다운로드
 * - formatDate(date, format) - 날짜 포맷팅
 * - parseDate(dateString, format) - 날짜 문자열 파싱
 * - isDateValid(date) - 날짜 유효성 검사
 * - getRandomId(prefix) - 랜덤 ID 생성
 * - deepClone(obj) - 깊은 복사
 * - isEmpty(value) - 값이 비어있는지 확인
 * - generateUUID() - UUID 생성
 * - initUtils() - 유틸리티 모듈 초기화
 */

/**
 * 루트 요소에서 지정된 선택자에 해당하는 모든 요소를 찾는 함수
 * @param {Element} root - 검색할 루트 요소
 * @param {string} selector - CSS 선택자
 * @returns {Element[]} 찾은 요소의 배열
 */
export function findAll(root, selector) {
  if (!root) return [];
  const nodes = [];
  if (root instanceof Element && root.matches(selector)) {
    nodes.push(root);
  }
  const scoped = root.querySelectorAll ? root.querySelectorAll(selector) : [];
  return nodes.concat(Array.from(scoped));
}

/**
 * 바이트 크기를 사람이 읽기 쉬운 형식으로 변환하는 함수
 * @param {number} bytes - 바이트 크기
 * @returns {string} 변환된 파일 크기 문자열
 */
export function formatFileSize(bytes) {
  if (!bytes) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  const size = bytes / Math.pow(k, i);
  return `${parseFloat(size.toFixed(2))} ${sizes[i]}`;
}

/**
 * 첨부된 파일 목록을 생성하는 함수
 * @param {File} file - 첨부된 파일 객체
 * @param {Document} doc - 문서 객체 (기본 document)
 * @returns {HTMLLIElement} 생성된 리스트 아이템 요소
 */
export function createAttachmentListItem(file, doc) {
  const documentRef = doc || document;
  const li = documentRef.createElement('li');
  li.className = 'attachment-item';

  const fileName = documentRef.createElement('span');
  fileName.className = 'file-name';
  fileName.textContent = file.name;

  const fileSize = documentRef.createElement('span');
  fileSize.className = 'file-size';
  fileSize.textContent = formatFileSize(file.size);

  const removeButton = documentRef.createElement('button');
  removeButton.type = 'button';
  removeButton.className = 'btn-remove';
  removeButton.textContent = '삭제';
  removeButton.setAttribute('aria-label', `${file.name} 파일 삭제`);

  removeButton.addEventListener('click', () => {
    const list = li.parentElement;
    li.remove();
    if (!list) return;
    const remainingItems = list.querySelectorAll('.attachment-item');
    if (remainingItems.length === 1) {
      const emptyMessage = documentRef.createElement('li');
      emptyMessage.className = 'empty';
      emptyMessage.textContent = '첨부된 파일이 없습니다.';
      list.appendChild(emptyMessage);
    }
  });

  li.appendChild(fileName);
  li.appendChild(fileSize);
  li.appendChild(removeButton);

  return li;
}

/**
 * 디바운스 함수
 * @param {Function} func - 실행할 함수
 * @param {number} wait - 대기 시간 (밀리초)
 * @returns {Function} 디바운스된 함수
 */
export function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

/**
 * 쓰로틀 함수
 * @param {Function} func - 실행할 함수
 * @param {number} limit - 제한 시간 (밀리초)
 * @returns {Function} 쓰로틀된 함수
 */
export function throttle(func, limit) {
  let inThrottle;
  return function() {
    const args = arguments;
    const context = this;
    if (!inThrottle) {
      func.apply(context, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
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
 * HTML 태그 제거 및 안전화
 * @param {string} html - 안전화할 HTML 문자열
 * @returns {string} 안전화된 문자열
 */
export function sanitizeHtml(html) {
  const div = document.createElement('div');
  div.textContent = html;
  return div.innerHTML;
}

/**
 * 쿠키 값 읽기
 * @param {string} name - 쿠키 이름
 * @returns {string|null} 쿠키 값 또는 null
 */
export function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

/**
 * 쿠키 설정
 * @param {string} name - 쿠키 이름
 * @param {string} value - 쿠키 값
 * @param {number} days - 유효 기간 (일)
 */
export function setCookie(name, value, days) {
  const expires = new Date();
  expires.setTime(expires.getTime() + (days * 24 * 60 * 60 * 1000));
  document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/`;
}

/**
 * 쿠키 삭제
 * @param {string} name - 삭제할 쿠키 이름
 */
export function removeCookie(name) {
  document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;`;
}

/**
 * 클립보드에 텍스트 복사
 * @param {string} text - 복사할 텍스트
 * @returns {Promise} 복사 작업 Promise
 */
export function copyToClipboard(text) {
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(text);
  } else {
    // Fallback for older browsers
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
      document.execCommand('copy');
      return Promise.resolve();
    } catch (err) {
      return Promise.reject(err);
    } finally {
      document.body.removeChild(textArea);
    }
  }
}

/**
 * 파일 다운로드
 * @param {string} url - 다운로드할 파일 URL
 * @param {string} filename - 파일명
 */
export function downloadFile(url, filename) {
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

/**
 * 날짜 포맷팅
 * @param {Date|string} date - 포맷팅할 날짜
 * @param {string} format - 포맷 문자열 (기본: 'YYYY-MM-DD')
 * @returns {string} 포맷된 날짜 문자열
 */
export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return '';
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds);
}

/**
 * 날짜 문자열 파싱
 * @param {string} dateString - 파싱할 날짜 문자열
 * @param {string} format - 날짜 형식 (기본: 'YYYY-MM-DD')
 * @returns {Date|null} 파싱된 Date 객체 또는 null
 */
export function parseDate(dateString, format = 'YYYY-MM-DD') {
  if (!dateString) return null;
  
  const parts = dateString.split(/[-/]/);
  if (parts.length !== 3) return null;
  
  const year = parseInt(parts[0]);
  const month = parseInt(parts[1]) - 1;
  const day = parseInt(parts[2]);
  
  return new Date(year, month, day);
}

/**
 * 날짜 유효성 검사
 * @param {Date} date - 검사할 Date 객체
 * @returns {boolean} 유효성 검사 결과
 */
export function isDateValid(date) {
  return date instanceof Date && !isNaN(date.getTime());
}

/**
 * 랜덤 ID 생성
 * @param {string} prefix - ID 접두사
 * @returns {string} 생성된 랜덤 ID
 */
export function getRandomId(prefix = 'id') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * 깊은 복사
 * @param {any} obj - 복사할 객체
 * @returns {any} 복사된 객체
 */
export function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj;
  if (obj instanceof Date) return new Date(obj.getTime());
  if (obj instanceof Array) return obj.map(item => deepClone(item));
  if (typeof obj === 'object') {
    const clonedObj = {};
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        clonedObj[key] = deepClone(obj[key]);
      }
    }
    return clonedObj;
  }
}

/**
 * 값이 비어있는지 확인
 * @param {any} value - 확인할 값
 * @returns {boolean} 비어있음 여부
 */
export function isEmpty(value) {
  if (value === null || value === undefined) return true;
  if (typeof value === 'string') return value.trim().length === 0;
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === 'object') return Object.keys(value).length === 0;
  return false;
}

/**
 * UUID 생성
 * @returns {string} 생성된 UUID
 */
export function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

/**
 * 유틸리티 모듈 초기화 함수
 */
export function initUtils() {
  // 기존 window.cmms.utils 호환성 유지
  window.cmms = window.cmms || {};
  window.cmms.utils = {
    formatFileSize,
    findAll,
    createAttachmentListItem,
    debounce,
    throttle,
    isValidEmail,
    sanitizeHtml,
    getCookie,
    setCookie,
    removeCookie,
    copyToClipboard,
    downloadFile,
    formatDate,
    parseDate,
    isDateValid,
    getRandomId,
    deepClone,
    isEmpty,
    generateUUID
  };
}
