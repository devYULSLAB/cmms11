# 코드 품질 검증 검토안

**작성일**: 2025-10-07  
**대상**: JavaScript 모듈 (특히 navigation.js)  
**검토 범위**: 미사용 코드 제거 및 에러 처리 표준화

---

## 📌 요약

### 검토 대상 문제
1. **문제 3**: 미사용 코드 3건 (navigation.js)
2. **문제 4**: 에러 처리 불일치 (프로젝트 전체)

### 권장 조치 우선순위
| 우선순위 | 문제 | 심각도 | 예상 소요 시간 | 비고 |
|---------|------|--------|---------------|------|
| 🔴 높음 | executePageScripts() 보안 개선 | 높음 | 2-3시간 | 보안 위험 |
| 🟡 중간 | 에러 처리 전략 통일 | 중간 | 4-6시간 | 유지보수성 |
| 🟢 낮음 | 미사용 코드 제거 | 낮음 | 30분 | 코드 정리 |

---

## 🔍 문제 3: 미사용 코드 상세 분석

### 3-1. `executePageScripts()` (line 335-357)

**현황**
```javascript
executePageScripts: function(doc) {
  const scripts = doc.querySelectorAll('script');
  scripts.forEach(script => {
    try {
      if (script.textContent.trim()) {
        const scriptContent = script.textContent;
        const modifiedScript = scriptContent.replace(
          /document\.addEventListener\('DOMContentLoaded',\s*\(\)\s*=>\s*\{/g,
          '(function() {'
        );
        
        // new Function() 사용 (eval()보다는 안전하지만 여전히 위험)
        const func = new Function(modifiedScript);
        func();
      }
    } catch (error) {
      console.warn('⚠️ 페이지 스크립트 실행 실패:', error);
    }
  });
}
```

**실제 사용 여부**
- ✅ **사용 중**: `loadContent()` 함수에서 line 166에서 호출됨
- 용도: SPA 콘텐츠 로드 시 HTML 내 인라인 스크립트 실행

**보안 위험도: 🔴 높음**
- `new Function()`은 eval()보다 안전하지만 여전히 임의 코드 실행 가능
- XSS 공격 가능성 존재 (악의적인 HTML 콘텐츠 주입 시)
- 현재는 내부 템플릿만 로딩하므로 위험도는 낮지만, 아키텍처상 위험

**권장 조치**

#### 옵션 A: 화이트리스트 기반 실행 (권장) ⭐
```javascript
executePageScripts: function(doc) {
  const ALLOWED_PATTERNS = [
    'window.cmms.pages',
    'document.getElementById',
    'document.querySelector'
  ];
  
  const scripts = doc.querySelectorAll('script[data-safe]'); // 명시적 마킹
  scripts.forEach(script => {
    try {
      const scriptContent = script.textContent.trim();
      
      // 안전성 검증
      const isAllowed = ALLOWED_PATTERNS.some(pattern => 
        scriptContent.includes(pattern)
      );
      
      if (!isAllowed) {
        console.warn('❌ 허용되지 않은 스크립트 패턴:', scriptContent.substring(0, 50));
        return;
      }
      
      // 위험 패턴 차단
      const BLOCKED_PATTERNS = ['eval(', 'Function(', 'setTimeout(', 'setInterval('];
      const hasBlockedPattern = BLOCKED_PATTERNS.some(pattern => 
        scriptContent.includes(pattern)
      );
      
      if (hasBlockedPattern) {
        console.warn('❌ 차단된 스크립트 패턴 감지');
        return;
      }
      
      const func = new Function(scriptContent);
      func();
    } catch (error) {
      console.warn('⚠️ 페이지 스크립트 실행 실패:', error);
    }
  });
}
```

**장점**: 보안 강화, 점진적 적용 가능  
**단점**: 템플릿 수정 필요 (script 태그에 data-safe 속성 추가)  
**예상 소요 시간**: 2-3시간

#### 옵션 B: Content Security Policy (CSP) 적용
```html
<!-- application.yml 또는 Spring Security 설정 -->
Content-Security-Policy: script-src 'self' 'nonce-{random}'
```

**장점**: 브라우저 레벨 보안, 표준 방식  
**단점**: 기존 인라인 스크립트 모두 수정 필요, 대규모 작업  
**예상 소요 시간**: 8-16시간

#### 옵션 C: 모듈화로 대체 (장기 전략)
- 인라인 스크립트를 모두 모듈로 분리
- `window.cmms.pages.run()` 패턴으로 통일 (이미 line 168-170에 존재)

**장점**: 근본적 해결, 유지보수성 향상  
**단점**: 전체 리팩토링 필요  
**예상 소요 시간**: 20-40시간

**최종 권장**: **옵션 A (단기)** + **옵션 C (장기 로드맵)**

---

### 3-2. `loadUserInfo()` (line 433-437)

**현황**
```javascript
/**
 * @deprecated Thymeleaf 템플릿에서 사용자 정보를 직접 주입하는 경우 사용
 */
loadUserInfo: function loadUserInfo() {
  // Thymeleaf 템플릿에서 사용자 정보를 직접 주입하는 경우
  // JavaScript에서 별도로 로드하는 경우 사용
  console.log('사용자 정보 로드 함수 (Thymeleaf 템플릿에서 직접 주입된 경우)');
}
```

**실제 사용 여부**
- ❌ **미사용**: line 598에서 주석 처리됨 (`// this.loadUserInfo();`)
- 과거 유물로 판단됨

**권장 조치: 제거**
```javascript
// 함수 전체 삭제 (line 430-438)
```

**영향도**: 없음  
**예상 소요 시간**: 5분

---

### 3-3. `preloadRelatedPages()` (line 513-517)

**현황**
```javascript
/**
 * preloadRelatedPages 더미 함수 (안전한 구현)
 */
preloadRelatedPages: function(moduleName) {
  console.log(`preloadRelatedPages 호출됨: ${moduleName} (더미 구현)`);
  // 실제 구현이 필요한 경우 여기에 로직 추가
}
```

**실제 사용 여부**
- 🔍 **사용 여부 불명**: 프로젝트 전체에서 호출 확인 필요
- 더미 구현으로 실제 기능 없음

**권장 조치: 두 가지 옵션**

#### 옵션 A: 제거
```javascript
// 함수 전체 삭제 (호출하는 곳이 없다면)
```

#### 옵션 B: 실제 구현 (성능 최적화가 목표라면)
```javascript
preloadRelatedPages: function(moduleName) {
  // 관련 페이지 맵핑
  const relatedPages = {
    'plant': ['/plant/detail.html', '/inspection/form.html'],
    'workorder': ['/workorder/detail.html', '/plant/detail.html'],
    // ...
  };
  
  const pages = relatedPages[moduleName] || [];
  pages.forEach(page => {
    const link = document.createElement('link');
    link.rel = 'prefetch';
    link.href = page;
    document.head.appendChild(link);
  });
}
```

**최종 권장**: 호출하는 곳이 없으면 **옵션 A (제거)**  
**예상 소요 시간**: 15분 (검색) + 10분 (제거)

---

## 🔍 문제 4: 에러 처리 불일치 상세 분석

### 현황 파악

**에러 처리 패턴 3가지 발견**

| 패턴 | 사용 위치 | 발견 건수 | 특징 |
|------|----------|----------|------|
| `console.error` 만 | 전체 모듈 | 86건 | 에러 삼킴, 디버깅용 |
| `console.error` + `throw` | core, ui 모듈 | 42건 | 에러 전파 |
| `window.cmms.notification` | pages, ui 모듈 | 110건 | 사용자 알림 |

### 문제점

#### 1. 같은 파일 내 불일치
**예시: navigation.js**
```javascript
// 패턴 1: 에러 삼킴
catch (err) {
  console.error(err);  // line 212, 219, 269
}

// 패턴 2: 사용자 알림
catch (err) {
  console.error(err);
  window.cmms.notification.error('요청이 실패했습니다...');  // line 462, 491
}

// 패턴 3: 에러 표시 (DOM)
catch (err) {
  console.error('Content load error:', err);
  this.slot.innerHTML = `<div class="notice danger">...</div>`;  // line 282-314
}
```

#### 2. 모듈 간 불일치
- **data-loader.js**: `console.error` + `throw` + `notification` (3가지 혼용)
- **module-loader.js**: `console.error`만 사용 (에러 삼킴)
- **notification.js**: 에러 처리 없음 (fallback만)

#### 3. 에러 정보 손실
```javascript
// 나쁜 예: 에러 객체 정보 손실
catch (error) {
  console.error('Upload error:', error);  // 스택 트레이스는 있음
  window.cmms.notification.error('파일 업로드에 실패했습니다.');  // 에러 원인 없음
}
```

### 권장 조치: 통일된 에러 처리 전략

#### 1단계: 에러 타입 분류

```javascript
/**
 * 에러 타입 정의
 */
export class CmmsError extends Error {
  constructor(message, code, detail = {}) {
    super(message);
    this.name = 'CmmsError';
    this.code = code;
    this.detail = detail;
    this.timestamp = new Date().toISOString();
  }
}

export class NetworkError extends CmmsError {
  constructor(message, detail) {
    super(message, 'NETWORK_ERROR', detail);
    this.name = 'NetworkError';
  }
}

export class ValidationError extends CmmsError {
  constructor(message, detail) {
    super(message, 'VALIDATION_ERROR', detail);
    this.name = 'ValidationError';
  }
}

export class AuthError extends CmmsError {
  constructor(message, detail) {
    super(message, 'AUTH_ERROR', detail);
    this.name = 'AuthError';
  }
}
```

#### 2단계: 중앙 집중식 에러 핸들러

```javascript
/**
 * 중앙 집중식 에러 핸들러
 * @param {Error} error - 발생한 에러
 * @param {Object} context - 에러 컨텍스트 (함수명, 파일명 등)
 * @param {Object} options - 처리 옵션
 */
export function handleError(error, context = {}, options = {}) {
  const config = Object.assign({
    showUser: true,        // 사용자에게 알림 표시 여부
    logConsole: true,      // 콘솔 로그 여부
    rethrow: false,        // 에러 재전파 여부
    userMessage: null      // 사용자 메시지 커스터마이징
  }, options);
  
  // 1. 콘솔 로깅 (개발자용)
  if (config.logConsole) {
    console.error('❌ [CMMS Error]', {
      message: error.message,
      type: error.name,
      code: error.code,
      context: context,
      stack: error.stack,
      timestamp: new Date().toISOString()
    });
  }
  
  // 2. 사용자 알림
  if (config.showUser && window.cmms?.notification) {
    const userMessage = config.userMessage || getUserFriendlyMessage(error);
    
    if (error instanceof ValidationError) {
      window.cmms.notification.warning(userMessage);
    } else if (error instanceof AuthError) {
      window.cmms.notification.error(userMessage);
      // 인증 에러는 자동으로 로그인 페이지로
      setTimeout(() => {
        window.location.href = '/auth/login';
      }, 2000);
    } else if (error instanceof NetworkError) {
      window.cmms.notification.warning(userMessage);
    } else {
      window.cmms.notification.error(userMessage);
    }
  }
  
  // 3. 서버 로깅 (선택적 - 추후 구현)
  // sendErrorToServer(error, context);
  
  // 4. 에러 재전파
  if (config.rethrow) {
    throw error;
  }
}

/**
 * 사용자 친화적 에러 메시지 생성
 */
function getUserFriendlyMessage(error) {
  const messages = {
    'NetworkError': '네트워크 연결을 확인해주세요.',
    'ValidationError': '입력 정보를 확인해주세요.',
    'AuthError': '세션이 만료되었습니다. 다시 로그인해주세요.',
    'TimeoutError': '요청 시간이 초과되었습니다.',
    'NotFoundError': '요청한 데이터를 찾을 수 없습니다.',
    'ServerError': '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
  };
  
  return messages[error.name] || error.message || '알 수 없는 오류가 발생했습니다.';
}
```

#### 3단계: 적용 예시

**Before: navigation.js (불일치)**
```javascript
// 패턴 1
catch (err) {
  console.error(err);
}

// 패턴 2
catch (err) {
  console.error(err);
  window.cmms.notification.error('요청이 실패했습니다. 잠시 후 다시 시도하세요');
}

// 패턴 3
catch (err) {
  console.error('Content load error:', err);
  this.slot.innerHTML = `<div class="notice danger">...</div>`;
}
```

**After: navigation.js (통일)**
```javascript
// 패턴 통일
catch (error) {
  handleError(error, {
    module: 'navigation',
    function: 'loadContent',
    url: contentUrl
  }, {
    showUser: true,
    userMessage: '페이지를 불러오는 데 실패했습니다.',
    rethrow: false
  });
  
  // UI 폴백 처리
  this.slot.innerHTML = `
    <div class="notice danger">
      <h3>페이지 로드 실패</h3>
      <p>페이지를 불러오는 데 실패했습니다.</p>
      <button class="btn primary" onclick="location.reload()">새로고침</button>
    </div>
  `;
}
```

**data-loader.js 적용**
```javascript
catch (error) {
  // 에러 타입 변환
  const cmmsError = error.message.includes('Failed to fetch')
    ? new NetworkError('네트워크 연결 실패', { endpoint, originalError: error })
    : new CmmsError(error.message, 'FETCH_ERROR', { endpoint });
  
  handleError(cmmsError, {
    module: 'data-loader',
    function: 'loadData',
    endpoint: endpoint
  }, {
    showUser: config.showLoading,
    rethrow: true  // 호출자가 처리할 수 있도록 재전파
  });
}
```

**module-loader.js 적용**
```javascript
catch (error) {
  handleError(error, {
    module: 'module-loader',
    function: 'injectScript',
    script: src
  }, {
    showUser: false,  // 모듈 로딩 실패는 사용자 알림 불필요
    logConsole: true,
    rethrow: false    // 페이지 동작 계속
  });
}
```

#### 4단계: 적용 가이드라인

**언제 어떤 옵션을 사용할까?**

| 상황 | showUser | logConsole | rethrow | 예시 |
|------|----------|-----------|---------|------|
| 사용자 액션 실패 (폼 제출, 삭제) | ✅ true | ✅ true | ❌ false | 파일 업로드 실패 |
| 백그라운드 작업 실패 | ❌ false | ✅ true | ❌ false | 모듈 로딩 실패 |
| 인증/세션 에러 | ✅ true | ✅ true | ✅ true | CSRF 토큰 만료 |
| 데이터 로드 실패 (캐시 사용 가능) | ⚠️ warning | ✅ true | ❌ false | API 호출 실패 |
| Validation 에러 | ⚠️ warning | ✅ true | ❌ false | 필수 입력값 누락 |

### 마이그레이션 계획

#### Phase 1: 인프라 구축 (1-2시간)
1. `core/error-handler.js` 생성
2. 에러 클래스 정의
3. `handleError` 함수 구현
4. `main.js`에서 초기화

#### Phase 2: 핵심 모듈 적용 (2-3시간)
1. **우선순위 1**: `core/navigation.js` (7개 catch 블록)
2. **우선순위 2**: `ui/data-loader.js` (1개 catch 블록)
3. **우선순위 3**: `core/module-loader.js` (1개 catch 블록)

#### Phase 3: 페이지 모듈 적용 (2-3시간)
1. `pages/plant.js`
2. `pages/inventory-tx.js`
3. `pages/inspection.js`
4. 나머지 페이지 모듈

#### Phase 4: 레거시 모듈 정리 (1시간)
1. `app.js` (legacy)
2. `common.js` (legacy)
3. 중복 코드 제거

**총 예상 소요 시간**: 6-9시간

---

## 📊 우선순위 결정 매트릭스

| 항목 | 심각도 | 긴급도 | 영향 범위 | 작업 시간 | 최종 우선순위 |
|------|--------|--------|----------|----------|-------------|
| executePageScripts 보안 | 높음 | 중간 | 전체 SPA | 2-3h | 🔴 1순위 |
| 에러 처리 통일 | 중간 | 높음 | 전체 프로젝트 | 6-9h | 🟡 2순위 |
| loadUserInfo 제거 | 낮음 | 낮음 | navigation.js | 5m | 🟢 3순위 |
| preloadRelatedPages 제거 | 낮음 | 낮음 | navigation.js | 25m | 🟢 3순위 |

---

## 🎯 최종 권장 사항

### 즉시 조치 (이번 스프린트)
1. ✅ **executePageScripts 보안 강화** (옵션 A: 화이트리스트)
   - 예상 시간: 2-3시간
   - 담당: 프론트엔드 리드
   - 완료 기준: 안전성 검증 로직 추가, 템플릿에 data-safe 속성 추가

2. ✅ **미사용 코드 제거**
   - `loadUserInfo()` 삭제
   - `preloadRelatedPages()` 사용 여부 확인 후 삭제 또는 구현
   - 예상 시간: 30분
   - 담당: 주니어 개발자 가능

### 단기 조치 (다음 스프린트)
3. ✅ **에러 처리 통일 (Phase 1-2)**
   - 에러 핸들러 인프라 구축
   - 핵심 모듈 적용 (navigation, data-loader, module-loader)
   - 예상 시간: 4-5시간
   - 담당: 프론트엔드 리드 + 시니어 개발자

### 중기 조치 (1-2개월 내)
4. ✅ **에러 처리 통일 (Phase 3-4)**
   - 페이지 모듈 적용
   - 레거시 코드 정리
   - 예상 시간: 3-4시간
   - 담당: 전체 프론트엔드 팀

5. ✅ **executePageScripts 모듈화 마이그레이션**
   - 인라인 스크립트를 모듈로 분리
   - `window.cmms.pages.run()` 패턴으로 통일
   - 예상 시간: 20-40시간 (점진적 진행)
   - 담당: 전체 프론트엔드 팀 (로드맵에 포함)

---

## 📝 체크리스트

### executePageScripts 보안 강화
- [ ] 화이트리스트 패턴 정의
- [ ] 차단 패턴 정의
- [ ] `executePageScripts()` 함수 수정
- [ ] 템플릿에 `data-safe` 속성 추가 (전체 template 디렉토리)
- [ ] 보안 테스트 (악의적 스크립트 주입 시나리오)
- [ ] 문서화 (개발자 가이드 업데이트)

### 미사용 코드 제거
- [ ] `preloadRelatedPages()` 호출 여부 검색 (전체 프로젝트)
- [ ] `preloadRelatedPages()` 함수 삭제
- [ ] `loadUserInfo()` 함수 삭제
- [ ] 빌드 테스트 (에러 없는지 확인)
- [ ] 주요 페이지 수동 테스트

### 에러 처리 통일 - Phase 1
- [ ] `core/error-handler.js` 파일 생성
- [ ] 에러 클래스 정의 (CmmsError, NetworkError, ValidationError, AuthError)
- [ ] `handleError()` 함수 구현
- [ ] `getUserFriendlyMessage()` 함수 구현
- [ ] `main.js`에서 초기화
- [ ] 유닛 테스트 작성

### 에러 처리 통일 - Phase 2
- [ ] `core/navigation.js` 적용 (7개 catch 블록)
- [ ] `ui/data-loader.js` 적용
- [ ] `core/module-loader.js` 적용
- [ ] 통합 테스트 (주요 사용자 시나리오)

### 에러 처리 통일 - Phase 3
- [ ] `pages/plant.js` 적용
- [ ] `pages/inventory-tx.js` 적용
- [ ] `pages/inspection.js` 적용
- [ ] 나머지 페이지 모듈 적용
- [ ] 회귀 테스트

### 에러 처리 통일 - Phase 4
- [ ] `app.js` 중복 코드 제거
- [ ] `common.js` 중복 코드 제거
- [ ] 전체 코드 리뷰
- [ ] 문서화 (에러 처리 가이드 작성)

---

## 📚 참고 자료

### 보안 관련
- [OWASP - Code Injection Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Injection_Prevention_Cheat_Sheet.html)
- [MDN - Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [Function() vs eval()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval#never_use_eval!)

### 에러 처리 모범 사례
- [JavaScript Error Handling Best Practices](https://www.valentinog.com/blog/error/)
- [Error Handling in Node.js](https://nodejs.dev/en/learn/error-handling-in-nodejs/)
- [Custom Error Classes in JavaScript](https://javascript.info/custom-errors)

### 코드 품질
- [Clean Code JavaScript](https://github.com/ryanmcdermott/clean-code-javascript)
- [Google JavaScript Style Guide](https://google.github.io/styleguide/jsguide.html)

---

## 💬 Q&A

### Q1: executePageScripts를 완전히 제거하면 안 되나요?
**A**: 현재 SPA 아키텍처에서 HTML 템플릿 내 인라인 스크립트를 실행하기 위해 필요합니다. 제거하려면 모든 인라인 스크립트를 모듈로 마이그레이션해야 하며, 이는 중기 과제로 권장합니다.

### Q2: 에러 처리 통일이 꼭 필요한가요? 지금도 잘 작동하는데요.
**A**: 현재는 작동하지만 다음 문제가 있습니다:
- 개발자마다 다른 패턴 사용 → 유지보수 어려움
- 에러 추적 어려움 → 버그 발견/수정 시간 증가
- 사용자 경험 불일치 → 어떤 에러는 알림, 어떤 에러는 침묵
- 서버 로깅 불가능 → 프로덕션 이슈 디버깅 어려움

### Q3: 모든 모듈을 한 번에 마이그레이션해야 하나요?
**A**: 아니요. 점진적 마이그레이션 권장합니다. 새로운 에러 핸들러와 기존 방식이 공존할 수 있도록 설계했습니다.

### Q4: 작업 시간이 너무 길지 않나요?
**A**: Phase별로 분리하여 우선순위에 따라 진행하면 됩니다:
- Phase 1-2 (핵심): 4-5시간 → 즉시 효과
- Phase 3-4 (확장): 3-4시간 → 천천히 진행
- 전체: 6-9시간 → 2-3주에 걸쳐 분산 가능

### Q5: CSP를 적용하지 않는 이유는?
**A**: CSP는 가장 강력한 보안 방법이지만, 현재 프로젝트의 모든 인라인 스크립트와 스타일을 수정해야 하므로 작업량이 너무 큽니다. 화이트리스트 방식으로 당장의 위험을 완화하고, 장기적으로 모듈화를 진행한 후 CSP를 적용하는 것이 현실적입니다.

---

**검토자**: AI Code Reviewer  
**승인 필요**: 프론트엔드 리드, CTO  
**다음 단계**: 팀 리뷰 미팅 → 우선순위 확정 → 작업 티켓 생성

