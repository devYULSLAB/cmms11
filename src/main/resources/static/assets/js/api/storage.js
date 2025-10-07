/**
 * 로컬 저장소 관리 모듈
 * 
 * 로컬 스토리지, 세션 스토리지, 캐시 관리를 담당하는 모듈입니다.
 * - 로컬 스토리지 관리
 * - 세션 스토리지 관리
 * - 캐시 관리
 * - 데이터 직렬화/역직렬화
 */

/**
 * 저장소 모듈 초기화 함수
 */
export function initStorage() {
  window.cmms = window.cmms || {};
  window.cmms.storage = {
    
    /**
     * 로컬 스토리지에 데이터 저장
     * @param {string} key - 저장할 키
     * @param {any} value - 저장할 값
     * @param {number} ttl - 만료 시간 (밀리초, 선택사항)
     */
    setLocal: function(key, value, ttl = null) {
      try {
        const data = {
          value: value,
          timestamp: Date.now(),
          ttl: ttl
        };
        localStorage.setItem(key, JSON.stringify(data));
        return true;
      } catch (error) {
        console.error('Local storage set error:', error);
        return false;
      }
    },
    
    /**
     * 로컬 스토리지에서 데이터 조회
     * @param {string} key - 조회할 키
     * @param {any} defaultValue - 기본값
     * @returns {any} 저장된 값 또는 기본값
     */
    getLocal: function(key, defaultValue = null) {
      try {
        const item = localStorage.getItem(key);
        if (!item) return defaultValue;
        
        const data = JSON.parse(item);
        
        // TTL 확인
        if (data.ttl && Date.now() - data.timestamp > data.ttl) {
          localStorage.removeItem(key);
          return defaultValue;
        }
        
        return data.value;
      } catch (error) {
        console.error('Local storage get error:', error);
        return defaultValue;
      }
    },
    
    /**
     * 로컬 스토리지에서 데이터 삭제
     * @param {string} key - 삭제할 키
     */
    removeLocal: function(key) {
      try {
        localStorage.removeItem(key);
        return true;
      } catch (error) {
        console.error('Local storage remove error:', error);
        return false;
      }
    },
    
    /**
     * 로컬 스토리지 전체 삭제
     */
    clearLocal: function() {
      try {
        localStorage.clear();
        return true;
      } catch (error) {
        console.error('Local storage clear error:', error);
        return false;
      }
    },
    
    /**
     * 세션 스토리지에 데이터 저장
     * @param {string} key - 저장할 키
     * @param {any} value - 저장할 값
     */
    setSession: function(key, value) {
      try {
        const data = {
          value: value,
          timestamp: Date.now()
        };
        sessionStorage.setItem(key, JSON.stringify(data));
        return true;
      } catch (error) {
        console.error('Session storage set error:', error);
        return false;
      }
    },
    
    /**
     * 세션 스토리지에서 데이터 조회
     * @param {string} key - 조회할 키
     * @param {any} defaultValue - 기본값
     * @returns {any} 저장된 값 또는 기본값
     */
    getSession: function(key, defaultValue = null) {
      try {
        const item = sessionStorage.getItem(key);
        if (!item) return defaultValue;
        
        const data = JSON.parse(item);
        return data.value;
      } catch (error) {
        console.error('Session storage get error:', error);
        return defaultValue;
      }
    },
    
    /**
     * 세션 스토리지에서 데이터 삭제
     * @param {string} key - 삭제할 키
     */
    removeSession: function(key) {
      try {
        sessionStorage.removeItem(key);
        return true;
      } catch (error) {
        console.error('Session storage remove error:', error);
        return false;
      }
    },
    
    /**
     * 세션 스토리지 전체 삭제
     */
    clearSession: function() {
      try {
        sessionStorage.clear();
        return true;
      } catch (error) {
        console.error('Session storage clear error:', error);
        return false;
      }
    },
    
    /**
     * 캐시 관리 (메모리 기반)
     */
    cache: {
      _data: new Map(),
      
      /**
       * 캐시에 데이터 저장
       * @param {string} key - 캐시 키
       * @param {any} value - 캐시 값
       * @param {number} ttl - 만료 시간 (밀리초)
       */
      set: function(key, value, ttl = 300000) { // 기본 5분
        const expiry = Date.now() + ttl;
        this._data.set(key, { value, expiry });
      },
      
      /**
       * 캐시에서 데이터 조회
       * @param {string} key - 캐시 키
       * @returns {any} 캐시된 값 또는 undefined
       */
      get: function(key) {
        const item = this._data.get(key);
        if (!item) return undefined;
        
        if (Date.now() > item.expiry) {
          this._data.delete(key);
          return undefined;
        }
        
        return item.value;
      },
      
      /**
       * 캐시에서 데이터 삭제
       * @param {string} key - 캐시 키
       */
      delete: function(key) {
        this._data.delete(key);
      },
      
      /**
       * 캐시 전체 삭제
       */
      clear: function() {
        this._data.clear();
      },
      
      /**
       * 만료된 캐시 정리
       */
      cleanup: function() {
        const now = Date.now();
        for (const [key, item] of this._data.entries()) {
          if (now > item.expiry) {
            this._data.delete(key);
          }
        }
      }
    },
    
    /**
     * 쿠키 관리
     */
    cookie: {
      /**
       * 쿠키 설정
       * @param {string} name - 쿠키 이름
       * @param {string} value - 쿠키 값
       * @param {number} days - 유효 기간 (일)
       * @param {string} path - 경로
       * @param {string} domain - 도메인
       * @param {boolean} secure - 보안 쿠키 여부
       */
      set: function(name, value, days = 30, path = '/', domain = '', secure = false) {
        const expires = new Date();
        expires.setTime(expires.getTime() + (days * 24 * 60 * 60 * 1000));
        
        let cookieString = `${name}=${encodeURIComponent(value)};expires=${expires.toUTCString()};path=${path}`;
        
        if (domain) cookieString += `;domain=${domain}`;
        if (secure) cookieString += ';secure';
        
        document.cookie = cookieString;
      },
      
      /**
       * 쿠키 조회
       * @param {string} name - 쿠키 이름
       * @returns {string|null} 쿠키 값 또는 null
       */
      get: function(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) {
          return decodeURIComponent(parts.pop().split(';').shift());
        }
        return null;
      },
      
      /**
       * 쿠키 삭제
       * @param {string} name - 쿠키 이름
       * @param {string} path - 경로
       * @param {string} domain - 도메인
       */
      remove: function(name, path = '/', domain = '') {
        let cookieString = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=${path}`;
        if (domain) cookieString += `;domain=${domain}`;
        document.cookie = cookieString;
      }
    },
    
    /**
     * 저장소 사용량 확인 (로컬 스토리지)
     * @returns {number} 사용량 (바이트)
     */
    getLocalStorageSize: function() {
      let total = 0;
      for (const key in localStorage) {
        if (localStorage.hasOwnProperty(key)) {
          total += localStorage[key].length + key.length;
        }
      }
      return total;
    },
    
    /**
     * 저장소 사용량 확인 (세션 스토리지)
     * @returns {number} 사용량 (바이트)
     */
    getSessionStorageSize: function() {
      let total = 0;
      for (const key in sessionStorage) {
        if (sessionStorage.hasOwnProperty(key)) {
          total += sessionStorage[key].length + key.length;
        }
      }
      return total;
    }
  };
}
