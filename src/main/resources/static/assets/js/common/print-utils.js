/**
 * 통합 인쇄 유틸리티 모듈
 * 
 * 모든 모듈에서 공통으로 사용하는 인쇄 기능을 통합 관리합니다.
 * - 사용자 정보 조회
 * - 날짜 포맷팅
 * - 인쇄 정보 업데이트
 * - 인쇄 버튼 초기화
 */

(function() {
  'use strict';
  
  window.cmms = window.cmms || {};
  window.cmms.printUtils = {
    
    /**
     * 현재 사용자 정보 가져오기
     * @returns {Promise<Object|null>} 사용자 정보 객체 또는 null
     */
    async getCurrentUser() {
      try {
        const response = await fetch('/api/auth/me', {
          credentials: 'same-origin'
        });
        
        if (response.ok) {
          return await response.json();
        } else {
          console.warn('사용자 정보 조회 실패:', response.status);
          return null;
        }
      } catch (error) {
        console.warn('사용자 정보를 가져올 수 없습니다:', error);
        return null;
      }
    },
    
    /**
     * 날짜를 YYYY-MM-DD HH:mm 형식으로 포맷팅
     * @param {Date} date - 포맷팅할 날짜
     * @returns {string} 포맷팅된 날짜 문자열
     */
    formatDate(date) {
      const z = (n) => n < 10 ? '0' + n : n;
      return date.getFullYear() + '-' + z(date.getMonth() + 1) + '-' + z(date.getDate()) + ' ' + z(date.getHours()) + ':' + z(date.getMinutes());
    },
    
    /**
     * 인쇄 정보 업데이트 (날짜 + 사용자)
     * @param {Element} root - 루트 요소
     */
    async updatePrintInfo(root) {
      // 현재 날짜 업데이트
      const printDateEl = root.querySelector('#print-date');
      if (printDateEl) {
        const now = new Date();
        printDateEl.textContent = this.formatDate(now);
      }
      
      // 인쇄자 정보 업데이트
      const printUserEl = root.querySelector('#print-user');
      if (printUserEl) {
        const currentUser = await this.getCurrentUser();
        if (currentUser) {
          // 이름이 있으면 이름, 없으면 ID 사용
          printUserEl.textContent = currentUser.name || currentUser.memberId;
        } else {
          printUserEl.textContent = '알 수 없음';
        }
      }
    },
    
    /**
     * 인쇄 버튼 초기화
     * @param {Element} root - 루트 요소
     */
    initPrintButton(root) {
      const printBtn = root.querySelector('[data-print-btn]');
      if (printBtn) {
        printBtn.addEventListener('click', async (e) => {
          e.preventDefault();
          
          // 인쇄 정보 업데이트
          await this.updatePrintInfo(root);
          
          // 인쇄 실행
          window.print();
        });
      }
    }
  };
  
})();
