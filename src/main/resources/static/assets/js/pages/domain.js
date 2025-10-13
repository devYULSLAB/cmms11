/**
 * Domain 모듈 JavaScript
 * 
 * 기본정보 관리 모듈(company, site, member, role, dept, func, storage)의 페이지별 초기화를 담당합니다.
 * 공통 유틸(DataLoader, notification)만 사용하며, 복잡한 로직은 최소화합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  if (!window.cmms) window.cmms = {};
  if (!window.cmms.domain) window.cmms.domain = {};

  // 기존 객체를 보존하면서 메서드만 추가
  Object.assign(window.cmms.domain, {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('Domain list page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Domain list already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      this.initPagination(root);
      this.initSearch(root);
      // this.initResetForm(root); // 주석처리: 초기화 버튼 제거됨
      this.initDeleteButtons(root);
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Domain form page initialized', root);
      
      // 중복 초기화 방지 (DOM 기반)
      if (root.dataset.initialized === 'true') {
        console.log('Domain form already initialized, skipping');
        return;
      }
      root.dataset.initialized = 'true';
      
      // this.initFormManager(root); // 주석처리: 이미 app.js SPA 폼 처리로 충분
      // this.initCancelButton(root); // 주석처리: 취소 버튼 제거됨
    },

    // 페이지네이션 초기화 (공통 유틸 활용)
    initPagination: function(root) {
      console.log('Initializing domain pagination');
      
      // 공통 pagination 유틸이 있다면 사용, 없다면 기본 처리
      if (window.cmms?.common?.initPagination) {
        window.cmms.common.initPagination(root);
      } else {
        // 기본 pagination 처리
        const paginationBtns = root.querySelectorAll('[data-nav-btn]');
        paginationBtns.forEach(btn => {
          btn.addEventListener('click', (e) => {
            e.preventDefault();
            const url = btn.dataset.url;
            if (url && window.cmms?.navigation?.navigate) {
              window.cmms.navigation.navigate(url);
            }
          });
        });
      }
    },

    // 검색 기능 초기화 (공통 유틸 활용)
    initSearch: function(root) {
      console.log('Initializing domain search');
      
      // 공통 search 유틸이 있다면 사용, 없다면 기본 처리
      if (window.cmms?.common?.initSearch) {
        window.cmms.common.initSearch(root);
      } else {
        // 기본 검색 처리
        const searchForms = root.querySelectorAll('form[method="get"]');
        searchForms.forEach(form => {
          form.addEventListener('submit', (e) => {
            e.preventDefault();
            const formData = new FormData(form);
            const params = new URLSearchParams(formData);
            const url = `${form.action}?${params.toString()}`;
            if (window.cmms?.navigation?.navigate) {
              window.cmms.navigation.navigate(url);
            }
          });
        });
      }
    },

    // 폼 초기화 버튼 (공통 유틸 활용) - 주석처리: 초기화 버튼 제거됨
    // initResetForm: function(root) {
    //   console.log('Initializing domain reset form');
    //   
    //   // 공통 reset form 유틸이 있다면 사용, 없다면 기본 처리
    //   if (window.cmms?.common?.initResetForm) {
    //     window.cmms.common.initResetForm(root);
    //   } else {
    //     // 기본 폼 초기화 처리
    //     const resetBtns = root.querySelectorAll('button[type="button"]');
    //     resetBtns.forEach(btn => {
    //       if (btn.textContent.includes('초기화')) {
    //         btn.addEventListener('click', () => {
    //           const form = root.querySelector('form');
    //           if (form) {
    //             form.reset();
    //             if (window.cmms?.notification) {
    //               window.cmms.notification.success('검색 조건이 초기화되었습니다.');
    //             }
    //           }
    //         });
    //       }
    //     });
    //   }
    // },

    // 삭제 버튼 초기화 (공통 유틸 활용)
    initDeleteButtons: function(root) {
      console.log('Initializing domain delete buttons');
      
      // 공통 delete buttons 유틸이 있다면 사용, 없다면 기본 처리
      if (window.cmms?.common?.initDeleteButtons) {
        window.cmms.common.initDeleteButtons(root);
      } else {
        // 기본 삭제 버튼 처리
        const deleteBtns = root.querySelectorAll('[data-delete-url]');
        deleteBtns.forEach(btn => {
          btn.addEventListener('click', async (e) => {
            e.preventDefault();
            
            const confirmMsg = btn.dataset.confirm || '정말 삭제하시겠습니까?';
            if (!confirm(confirmMsg)) return;
            
            const deleteUrl = btn.dataset.deleteUrl;
            const redirectUrl = btn.dataset.redirect;
            
            try {
              const response = await fetch(deleteUrl, {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json',
                  'X-CSRF-Token': document.querySelector('[data-csrf-field]')?.value || ''
                }
              });
              
              if (response.ok) {
                if (window.cmms?.notification) {
                  window.cmms.notification.success('삭제되었습니다.');
                }
                
                if (redirectUrl && window.cmms?.navigation?.navigate) {
                  window.cmms.navigation.navigate(redirectUrl);
                }
              } else {
                throw new Error('삭제 실패');
              }
            } catch (error) {
              console.error('Delete error:', error);
              if (window.cmms?.notification) {
                window.cmms.notification.error('삭제 중 오류가 발생했습니다.');
              }
            }
          });
        });
      }
    },

    // 폼 매니저 초기화 (공통 FormManager 활용)
    // initFormManager: function(root) {
    //   console.log('Initializing domain form manager');
    //   
    //   const forms = root.querySelectorAll('[data-form-manager]');
    //   forms.forEach(form => {
    //     if (window.cmms?.formManager?.init) {
    //       window.cmms.formManager.init(form, {
    //         onSuccess: (result) => {
    //           if (window.cmms?.notification) {
    //             window.cmms.notification.success('저장되었습니다.');
    //           }
    //           
    //           const redirectUrl = form.dataset.redirect;
    //           if (redirectUrl && window.cmms?.navigation?.navigate) {
    //             setTimeout(() => {
    //               window.cmms.navigation.navigate(redirectUrl);
    //             }, 1000);
    //           }
    //         },
    //         onError: (error) => {
    //           console.error('Form submit error:', error);
    //           if (window.cmms?.notification) {
    //             window.cmms.notification.error('저장 중 오류가 발생했습니다.');
    //           }
    //         }
    //       });
    //     }
    //   });
    // },

    // 취소 버튼 초기화 (공통 유틸 활용) - 주석처리: 취소 버튼 제거됨
    // initCancelButton: function(root) {
    //   console.log('Initializing domain cancel button');
    //   
    //   // 공통 cancel button 유틸이 있다면 사용, 없다면 기본 처리
    //   if (window.cmms?.common?.initCancelButton) {
    //     window.cmms.common.initCancelButton(root);
    //   } else {
    //     // 기본 취소 버튼 처리
    //     const cancelBtns = root.querySelectorAll('[data-cancel-btn]');
    //     cancelBtns.forEach(btn => {
    //       btn.addEventListener('click', () => {
    //         if (window.cmms?.navigation?.navigate) {
    //           // 현재 경로에서 list로 이동
    //           const currentPath = window.location.pathname;
    //           const listPath = currentPath.replace(/\/form$/, '/list').replace(/\/edit\/[^/]+$/, '/list');
    //           window.cmms.navigation.navigate(listPath);
    //         } else {
    //           window.history.back();
    //         }
    //       });
    //     });
    //   }
    // }
  });

  // 페이지별 초기화 등록 (root 기반 구조)
  // Domain 모듈의 모든 페이지를 등록
  const domainPages = [
    'domain-company-list', 'domain-company-form',
    'domain-site-list', 'domain-site-form',
    'domain-dept-list', 'domain-dept-form',
    'domain-member-list', 'domain-member-form',
    'domain-role-list', 'domain-role-form',
    'domain-func-list', 'domain-func-form',
    'domain-storage-list', 'domain-storage-form'
  ];

  domainPages.forEach(pageId => {
    window.cmms.pages.register(pageId, function(root) {
      if (pageId.includes('-list')) {
        window.cmms.domain.initList(root);
      } else if (pageId.includes('-form')) {
        window.cmms.domain.initForm(root);
      }
    });
  });

})();
