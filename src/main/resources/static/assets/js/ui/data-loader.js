/**
 * 데이터 로더 모듈
 * 
 * common.js에서 데이터 로딩 기능을 추출한 모듈입니다.
 * - API 호출 및 데이터 로딩
 * - 로딩 상태 관리
 * - 에러 처리
 * - API 캐싱 (storage.js 활용)
 */

/**
 * 간단한 문자열 해시 함수
 * @param {string} str - 해시할 문자열
 * @returns {string} 해시 값
 */
function simpleHash(str) {
  if (!str) return 'default';
  
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // 32bit 정수로 변환
  }
  return Math.abs(hash).toString(36).substring(0, 8);
}

/**
 * 표준화된 캐시 키 생성
 * @param {string} endpoint - API 엔드포인트
 * @param {Object} params - 쿼리 파라미터
 * @returns {string} 캐시 키
 */
function generateCacheKey(endpoint, params = {}) {
  // 1. URL 정규화
  const url = new URL(endpoint, window.location.origin);
  const path = url.pathname.replace(/^\/api\//, ''); // /api/ 제거
  
  // 2. 파라미터 정렬 (순서 일관성 보장)
  const sortedParams = Object.keys(params)
    .sort()
    .reduce((acc, key) => {
      acc[key] = params[key];
      return acc;
    }, {});
  
  // 3. 파라미터 해시 생성
  const paramString = JSON.stringify(sortedParams);
  const hash = simpleHash(paramString);
  
  // 4. 표준 키 형식: api:{path}:{hash}
  return `api:${path.replace(/\//g, ':')}:${hash}`;
}

/**
 * 서버에서 데이터를 로드하는 표준화된 함수
 * @param {string} endpoint - API 엔드포인트
 * @param {Object} options - 옵션 설정
 * @returns {Promise<any>} 로드된 데이터
 */
export async function loadData(endpoint, options = {}) {
  const config = Object.assign({
    method: 'GET',
    params: {},
    showLoading: false,
    timeout: 30000,
    headers: {},
    useCache: true,        // GET 요청 시 캐싱 사용
    cacheTTL: 300000,      // 5분 (기본값)
    allowStaleCache: true  // 네트워크 오류 시 만료된 캐시도 사용
  }, options);
  
  // Step 1: 캐시 확인 (GET 요청만)
  let cacheKey = null;
  if (config.method === 'GET' && config.useCache) {
    cacheKey = generateCacheKey(endpoint, config.params);
    
    const cached = window.cmms?.storage?.cache?.get(cacheKey);
    if (cached) {
      console.log('📦 캐시 사용:', cacheKey);
      return cached;
    }
  }
  
  try {
    // Step 2: API 호출
    if (config.showLoading && window.cmms?.notification) {
      window.cmms.notification.info('데이터를 불러오는 중...', 500);
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
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...config.headers
      },
      body: config.body || (config.method !== 'GET' ? JSON.stringify(config.data) : undefined)
    });
    
    clearTimeout(timeoutId);
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    const contentType = response.headers.get('content-type');
    let data;
    
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      data = await response.text();
    }
    
    // Step 3: 성공 시 캐시 저장
    if (config.method === 'GET' && config.useCache && data && cacheKey) {
      window.cmms?.storage?.cache?.set(cacheKey, data, config.cacheTTL);
      console.log('💾 캐시 저장:', cacheKey);
    }
    
    if (config.showLoading && window.cmms?.notification) {
      window.cmms.notification.success('데이터 로드 완료');
    }
    
    return data;
    
  } catch (error) {
    console.error('데이터 로드 오류:', error);
    
    // Step 4: 에러 타입별 메시지
    let errorMessage = '데이터 로드에 실패했습니다.';
    let shouldUseCacheAsFallback = false;
    
    if (error.name === 'AbortError') {
      errorMessage = '요청 시간이 초과되었습니다. 네트워크 연결을 확인해주세요.';
      shouldUseCacheAsFallback = true;
    } else if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
      errorMessage = '네트워크 연결이 불안정합니다. 인터넷 연결을 확인해주세요.';
      shouldUseCacheAsFallback = true;
    } else if (error.message.includes('403')) {
      errorMessage = '세션이 만료되었습니다. 페이지를 새로고침합니다.';
      setTimeout(() => window.location.reload(), 2000);
    } else if (error.message.includes('404')) {
      errorMessage = '요청한 데이터를 찾을 수 없습니다.';
    } else if (error.message.includes('500')) {
      errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
      shouldUseCacheAsFallback = true;
    }
    
    // Step 5: 네트워크 오류 시 캐시 폴백
    if (shouldUseCacheAsFallback && config.allowStaleCache && cacheKey) {
      const staleCache = window.cmms?.storage?.cache?._data?.get(cacheKey);
      if (staleCache) {
        console.warn('⚠️ 네트워크 오류, 캐시된 데이터 사용:', cacheKey);
        if (window.cmms?.notification) {
          window.cmms.notification.warning('네트워크 오류로 인해 이전 데이터를 표시합니다.');
        }
        return staleCache.value;
      }
    }
    
    if (config.showLoading && window.cmms?.notification) {
      window.cmms.notification.error(errorMessage);
    }
    
    throw error;
  }
}

/**
 * GET 요청으로 데이터 로드
 * @param {string} endpoint - API 엔드포인트
 * @param {Object} params - 쿼리 파라미터
 * @param {Object} options - 추가 옵션
 * @returns {Promise<any>} 로드된 데이터
 */
export function getData(endpoint, params = {}, options = {}) {
  return loadData(endpoint, {
    method: 'GET',
    params,
    ...options
  });
}

/**
 * POST 요청으로 데이터 전송
 * @param {string} endpoint - API 엔드포인트
 * @param {Object} data - 전송할 데이터
 * @param {Object} options - 추가 옵션
 * @returns {Promise<any>} 응답 데이터
 */
export function postData(endpoint, data = {}, options = {}) {
  return loadData(endpoint, {
    method: 'POST',
    data,
    ...options
  });
}

/**
 * PUT 요청으로 데이터 업데이트
 * @param {string} endpoint - API 엔드포인트
 * @param {Object} data - 업데이트할 데이터
 * @param {Object} options - 추가 옵션
 * @returns {Promise<any>} 응답 데이터
 */
export function putData(endpoint, data = {}, options = {}) {
  return loadData(endpoint, {
    method: 'PUT',
    data,
    ...options
  });
}

/**
 * DELETE 요청으로 데이터 삭제
 * @param {string} endpoint - API 엔드포인트
 * @param {Object} options - 추가 옵션
 * @returns {Promise<any>} 응답 데이터
 */
export function deleteData(endpoint, options = {}) {
  return loadData(endpoint, {
    method: 'DELETE',
    ...options
  });
}

/**
 * 폼 데이터 전송
 * @param {string} endpoint - API 엔드포인트
 * @param {FormData|HTMLFormElement} formData - 폼 데이터 또는 폼 요소
 * @param {Object} options - 추가 옵션
 * @returns {Promise<any>} 응답 데이터
 */
export function submitForm(endpoint, formData, options = {}) {
  let data;
  let headers = {};
  
  if (formData instanceof HTMLFormElement) {
    data = new FormData(formData);
  } else if (formData instanceof FormData) {
    data = formData;
  } else {
    data = new FormData();
    Object.keys(formData).forEach(key => {
      data.append(key, formData[key]);
    });
  }
  
  // FormData인 경우 Content-Type 헤더를 설정하지 않음 (브라우저가 자동 설정)
  if (data instanceof FormData) {
    headers = {};
  } else {
    headers['Content-Type'] = 'application/json';
  }
  
  return loadData(endpoint, {
    method: 'POST',
    body: data,
    headers,
    ...options
  });
}

/**
 * 파일 업로드
 * @param {string} endpoint - 업로드 엔드포인트
 * @param {File|FileList} files - 업로드할 파일
 * @param {Object} additionalData - 추가 데이터
 * @param {Object} options - 추가 옵션
 * @returns {Promise<any>} 응답 데이터
 */
export function uploadFiles(endpoint, files, additionalData = {}, options = {}) {
  const formData = new FormData();
  
  // 파일 추가
  if (files instanceof FileList) {
    Array.from(files).forEach(file => {
      formData.append('files', file);
    });
  } else if (files instanceof File) {
    formData.append('files', files);
  }
  
  // 추가 데이터 추가
  Object.keys(additionalData).forEach(key => {
    formData.append(key, additionalData[key]);
  });
  
  return submitForm(endpoint, formData, {
    showLoading: true,
    ...options
  });
}

/**
 * 특정 패턴의 캐시 무효화
 * @param {string} pattern - 삭제할 키 패턴 (예: 'memo', 'plant')
 */
export function clearCache(pattern = null) {
  if (!window.cmms?.storage?.cache) {
    console.warn('캐시 시스템을 사용할 수 없습니다.');
    return;
  }
  
  if (pattern) {
    const cache = window.cmms.storage.cache._data;
    let removed = 0;
    
    for (const [key] of cache.entries()) {
      if (key.includes(pattern)) {
        cache.delete(key);
        removed++;
      }
    }
    
    console.log(`🗑️ 캐시 무효화: ${removed}개 항목 제거 (패턴: ${pattern})`);
  } else {
    window.cmms.storage.cache.clear();
    console.log('🗑️ 전체 캐시 삭제');
  }
}

/**
 * 데이터 로더 모듈 초기화 함수
 */
export function initDataLoader() {
  // 기존 window.cmms.common.DataLoader 호환성 유지
  window.cmms = window.cmms || {};
  window.cmms.common = window.cmms.common || {};
  window.cmms.common.DataLoader = {
    load: loadData,
    get: getData,
    post: postData,
    put: putData,
    delete: deleteData,
    submitForm: submitForm,
    uploadFiles: uploadFiles,
    clearCache: clearCache
  };
}
