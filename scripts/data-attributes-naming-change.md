# data-* 속성 네이밍 일관성 개선

**변경일:** 2025-10-10  
**변경 내용:** 
- `data-nav-button` → `data-nav-btn`
- `data-add-button` → `data-add-btn`
- `data-remove-button` → `data-remove-btn`

---

## 📌 변경 이유

### 프로젝트 네이밍 표준 통일
프로젝트 전체에서 버튼 관련 data 속성은 `-btn` 패턴을 사용하고 있었지만, `data-nav-button`만 예외적으로 `-button` 패턴을 사용하고 있었습니다.

**변경 전 현황:**
- ✅ `data-cancel-btn` - 17회 사용
- ✅ `data-print-btn` - 14회 사용
- ⚠️ `data-nav-button` - 26회 사용 (예외)
- ⚠️ `data-add-button` - 1회 사용 (예외)
- ⚠️ `data-remove-button` - 1회 사용 (예외)

**변경 후:**
- ✅ `data-cancel-btn` - 17회 사용
- ✅ `data-print-btn` - 14회 사용
- ✅ `data-nav-btn` - 26회 사용 (통일!)
- ✅ `data-add-btn` - 1회 사용 (통일!)
- ✅ `data-remove-btn` - 1회 사용 (통일!)

---

## 📝 변경된 파일 목록

### JavaScript 파일 (9개)

1. **`src/main/resources/static/assets/js/pages/workorder.js`**
   - `querySelectorAll('[data-nav-button]')` → `querySelectorAll('[data-nav-btn]')`
   - 주석 업데이트

2. **`src/main/resources/static/assets/js/pages/domain.js`**
   - `querySelectorAll('[data-nav-button]')` → `querySelectorAll('[data-nav-btn]')`

3. **`src/main/resources/static/assets/js/pages/inventory-tx.js`**
   - 주석 업데이트

4. **`src/main/resources/static/assets/js/pages/approval.js`**
   - 주석 업데이트

5. **`src/main/resources/static/assets/js/pages/workpermit.js`**
   - 주석 업데이트

6. **`src/main/resources/static/assets/js/pages/memo.js`**
   - 주석 업데이트

7. **`src/main/resources/static/assets/js/pages/inventory.js`**
   - 주석 업데이트

8. **`src/main/resources/static/assets/js/pages/plant.js`**
   - 주석 업데이트

9. **`src/main/resources/static/assets/js/pages/inspection.js`**
   - 주석 업데이트

### HTML 파일 (7개, 각 2개 버튼)

1. **`src/main/resources/templates/workorder/list.html`**
   - 이전/다음 버튼 (2개)

2. **`src/main/resources/templates/inventory/list.html`**
   - 이전/다음 버튼 (2개)

3. **`src/main/resources/templates/inspection/list.html`**
   - 이전/다음 버튼 (2개)

4. **`src/main/resources/templates/workpermit/list.html`**
   - 이전/다음 버튼 (2개)

5. **`src/main/resources/templates/memo/list.html`**
   - 이전/다음 버튼 (2개)

6. **`src/main/resources/templates/approval/list.html`**
   - 이전/다음 버튼 (2개)

7. **`src/main/resources/templates/plant/list.html`**
   - 이전/다음 버튼 (2개)

### JavaScript 파일 - table-manager.js (추가)

10. **`src/main/resources/static/assets/js/ui/table-manager.js`**
   - config 객체의 `addButton` → `addBtn` (4개 위치)
   - config 객체의 `removeButton` → `removeBtn` (4개 위치)

### HTML 파일 - workorder/form.html (추가)

8. **`src/main/resources/templates/workorder/form.html`**
   - `data-add-button` → `data-add-btn`
   - `data-remove-button` → `data-remove-btn`

---

## 🎯 변경 효과

### ✅ 개선 사항

1. **네이밍 일관성 향상**
   - 모든 버튼 관련 data 속성이 `-btn` 패턴으로 통일
   - 코드 가독성 향상

2. **유지보수성 개선**
   - 일관된 패턴으로 새로운 개발자가 쉽게 이해 가능
   - 검색 및 리팩토링 시 예측 가능한 패턴

3. **간결성**
   - `btn`이 `button`보다 짧고 일반적인 약어

---

## 📊 통계

- **변경된 파일:** 18개
  - JavaScript: 10개 (pages 9개 + ui/table-manager.js 1개)
  - HTML: 8개 (list 7개 + workorder/form.html 1개)
- **변경된 속성:**
  - `data-nav-button` → `data-nav-btn`: 24회
  - `data-add-button` → `data-add-btn`: 1회 (HTML) + 4회 (JS config)
  - `data-remove-button` → `data-remove-btn`: 1회 (HTML) + 4회 (JS config)
- **영향을 받는 페이지:** 
  - 7개 목록 페이지의 페이지네이션
  - 1개 폼 페이지의 테이블 관리 (작업지시 폼)

---

## ✅ 검증

모든 파일이 정상적으로 변경되었으며, linter 오류가 없습니다.

페이지네이션 기능은 다음과 같이 작동합니다:

```html
<!-- HTML -->
<button th:data-url="@{/workorder/list(page=1)}" 
        data-nav-btn>다음</button>
```

```javascript
// JavaScript
root.querySelectorAll('[data-nav-btn]').forEach(btn => {
  btn.addEventListener('click', () => {
    const url = btn.dataset.url;
    if (url) {
      window.cmms.navigation.navigate(url);
    }
  });
});
```

---

## 📚 참고

이 변경은 프로젝트의 data-* 속성 네이밍 컨벤션을 확립하는 첫 단계입니다.

**향후 가이드라인:**
- 버튼 관련: `data-*-btn` 패턴 사용
- 컨테이너 관련: `data-*-container` 패턴 사용
- 액션 관련: `data-*-url`, `data-*-action` 패턴 사용

