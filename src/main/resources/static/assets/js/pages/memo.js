/**
 * Memo 모듈 JavaScript
 * 
 * 게시/메모 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 공통 유틸(TableManager, DataLoader, fileUpload, notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  window.cmms = window.cmms || {};
  window.cmms.memo = window.cmms.memo || {};

  // 초기화 상태 추적을 위한 플래그
  window.cmms.memo.initialized = {
    list: false,
    detail: false,
    form: false
  };

  Object.assign(window.cmms.memo, {
    
    // 목록 페이지 초기화 (root 기반)
    initList: function(root) {
      console.log('Memo list page initialized', root);
      
      // 중복 초기화 방지
      if (window.cmms.memo.initialized.list) {
        console.log('Memo list already initialized, skipping');
        return;
      }
      window.cmms.memo.initialized.list = true;
      
      this.initPagination(root);
      this.initSearch(root);
      this.initResetForm(root);
    },
    
    // 상세 페이지 초기화 (root 기반)
    initDetail: function(root) {
      console.log('Memo detail page initialized', root);
      
      // 중복 초기화 방지
      if (window.cmms.memo.initialized.detail) {
        console.log('Memo detail already initialized, skipping');
        return;
      }
      window.cmms.memo.initialized.detail = true;
      this.initPrintButtons(root);
    },
    
    // 폼 페이지 초기화 (root 기반)
    initForm: function(root) {
      console.log('Memo form page initialized', root);
      
      // 중복 초기화 방지
      if (window.cmms.memo.initialized.form) {
        console.log('Memo form already initialized, skipping');
        return;
      }
      window.cmms.memo.initialized.form = true;
      this.initCancelButton(root);
      this.initEditor(root);
      // this.initFormSubmit(root);  // Form Manager 제거로 인한 주석 처리
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('Memo pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-button을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
    },
    
    // 검색 초기화 (root 기반)
    initSearch: function(root) {
      console.log('Memo search initialized - 기본 폼 제출로 처리됨');
      // 기본 폼 제출로 처리되므로 별도 초기화 불필요
    },
    
    // 폼 초기화 버튼 (root 기반)
    initResetForm: function(root) {
      const resetBtn = root.querySelector('[data-reset-form]');
      if (resetBtn) {
        resetBtn.addEventListener('click', () => {
          const form = resetBtn.closest('form');
          if (form) {
            form.reset();
            if (window.cmms?.notification) {
              window.cmms.notification.success('검색 조건이 초기화되었습니다.');
            }
          }
        });
      }
    },
    
    // 인쇄 버튼 초기화 (통합 모듈 사용)
    initPrintButtons: function(root) {
      window.cmms.printUtils.initPrintButton(root);
    },
    
    
    // 취소 버튼 초기화 (root 기반)
    initCancelButton: function(root) {
      const cancelBtn = root.querySelector('[data-cancel-btn]');
      if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
          window.cmms.navigation.navigate('/memo/list');
        });
      }
    },
    
    // 에디터 초기화 (root 기반)
    initEditor: function(root) {
      const editor = root.querySelector('.editor');
      if (!editor) return;
      
      // 중복 초기화 방지
      if (editor.dataset.initialized === 'true') {
        console.log('Editor already initialized');
        return;
      }
      
      // 에디터 툴바 버튼들
      const boldBtn = root.querySelector('[data-format="bold"]');
      const italicBtn = root.querySelector('[data-format="italic"]');
      const underlineBtn = root.querySelector('[data-format="underline"]');
      
      if (boldBtn) {
        boldBtn.addEventListener('click', () => {
          document.execCommand('bold');
          editor.focus();
        });
      }
      
      if (italicBtn) {
        italicBtn.addEventListener('click', () => {
          document.execCommand('italic');
          editor.focus();
        });
      }
      
      if (underlineBtn) {
        underlineBtn.addEventListener('click', () => {
          document.execCommand('underline');
          editor.focus();
        });
      }
      
      // 에디터 포커스 시 툴바 활성화
      editor.addEventListener('focus', () => {
        const toolbar = root.querySelector('.editor-toolbar');
        if (toolbar) {
          toolbar.classList.add('active');
        }
      });
      
      editor.dataset.initialized = 'true';
    },
    
    // 댓글 초기화 (root 기반)
    initComments: function(root) {
      const memoId = root.querySelector('[data-memo-id]')?.dataset.memoId;
      if (!memoId) return;
      
      // 중복 초기화 방지
      const commentsContainer = root.querySelector('#comments-container');
      if (commentsContainer?.dataset.initialized === 'true') {
        console.log('Comments already initialized');
        return;
      }
      
      this.loadComments(memoId, root);
      this.initCommentForm(memoId, root);
      
      if (commentsContainer) {
        commentsContainer.dataset.initialized = 'true';
      }
    },
    
    // 댓글 로드 (root 기반)
    loadComments: async function(memoId, root) {
      try {
        const result = await window.cmms.common.DataLoader.load('/api/memo/comments', {
          params: { memoId: memoId }
        });
        
        const container = root.querySelector('#comments-container');
        if (!container) return;
        
        if (result.items.length === 0) {
          container.innerHTML = '<div class="notice">댓글이 없습니다.</div>';
          return;
        }
        
        const list = document.createElement('ul');
        list.className = 'comments-list';
        
        result.items.forEach(comment => {
          const li = document.createElement('li');
          li.className = 'comment-item';
          li.innerHTML = `
            <div class="comment-header">
              <span class="comment-author">${comment.createdBy}</span>
              <span class="comment-date">${comment.createdAt}</span>
            </div>
            <div class="comment-content">${comment.content}</div>
          `;
          list.appendChild(li);
        });
        
        container.innerHTML = '';
        container.appendChild(list);
        
      } catch (error) {
        console.error('Load comments error:', error);
        const container = root.querySelector('#comments-container');
        if (container) {
          container.innerHTML = '<div class="notice danger">댓글을 불러올 수 없습니다.</div>';
        }
      }
    },
    
    // 댓글 폼 초기화 (root 기반)
    initCommentForm: function(memoId, root) {
      const form = root.querySelector('#comment-form');
      if (!form) return;
      
      // 중복 초기화 방지
      if (form.dataset.initialized === 'true') {
        console.log('Comment form already initialized');
        return;
      }
      
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        
        const content = form.querySelector('[name="content"]').value.trim();
        if (!content) {
          if (window.cmms?.notification) {
            window.cmms.notification.warning('댓글 내용을 입력해주세요.');
          } else {
            alert('댓글 내용을 입력해주세요.');
          }
          return;
        }
        
        try {
          const response = await fetch('/api/memo/comments', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              memoId: memoId,
              content: content
            })
          });
          
          if (!response.ok) {
            throw new Error('댓글 작성 실패');
          }
          
          if (window.cmms?.notification) {
            window.cmms.notification.success('댓글이 작성되었습니다.');
          } else {
            alert('댓글이 작성되었습니다.');
          }
          
          // 댓글 목록 새로고침
          this.loadComments(memoId, root);
          
          // 폼 초기화
          form.reset();
          
        } catch (error) {
          console.error('Comment submit error:', error);
          if (window.cmms?.notification) {
            window.cmms.notification.error('댓글 작성 중 오류가 발생했습니다.');
          } else {
            alert('댓글 작성 중 오류가 발생했습니다.');
          }
        }
      });
      
      form.dataset.initialized = 'true';
    },
    
    // 폼 제출 초기화 (공통 FormManager 활용, root 기반)
    // initFormSubmit: function(root) {
    //   const form = root.querySelector('[data-form-manager]');
    //   if (!form) return;
    //   
    //   // 중복 초기화 방지
    //   if (form.dataset.initialized === 'true') {
    //     console.log('Form submit already initialized');
    //     return;
    //   }
    //   
    //   // 공통 FormManager를 활용한 폼 제출 처리
    //   if (window.cmms?.formManager) {
    //     window.cmms.formManager.init(form, {
    //       onSuccess: (result) => {
    //         this.handleFormSuccess(result, root);
    //       },
    //       onError: (error) => {
    //         this.handleFormError(error, root);
    //       }
    //     });
    //   } else {
    //     // FormManager가 없는 경우 직접 처리
    //     form.addEventListener('submit', async (event) => {
    //       event.preventDefault();
    //       await this.handleDirectForm(form, root);
    //     });
    //   }
    //   
    //   form.dataset.initialized = 'true';
    // },
    
    // 폼 성공 처리 (root 기반)
    // handleFormSuccess: function(result, root) {
    //   if (window.cmms?.notification) {
    //     window.cmms.notification.success('메모가 성공적으로 저장되었습니다.');
    //   } else {
    //     alert('메모가 성공적으로 저장되었습니다.');
    //   }
    //   
    //   // SPA 네비게이션으로 레이아웃 유지
    //   setTimeout(() => {
    //     window.cmms.navigation.navigate('/memo/list');
    //   }, 1000);
    // },
    
    // 폼 에러 처리 (root 기반)
    // handleFormError: function(error, root) {
    //   console.error('Form submit error:', error);
    //   if (window.cmms?.notification) {
    //     window.cmms.notification.error('저장 중 오류가 발생했습니다.');
    //   } else {
    //     alert('저장 중 오류가 발생했습니다.');
    //   }
    // },
    
    // 직접 폼 처리 (FormManager 없을 때)
    // handleDirectForm: async function(form, root) {
    //   try {
    //     const formData = new FormData(form);
    //     
    //     const response = await fetch(form.action, {
    //       method: 'POST',
    //       body: formData,
    //     });
    //     
    //     if (!response.ok) {
    //       throw new Error('저장 실패');
    //     }
    //     
    //     const result = await response.json();
    //     this.handleFormSuccess(result, root);
    //     
    //   } catch (error) {
    //     this.handleFormError(error, root);
    //   }
    // },
    
    // 초기화 상태 리셋 (페이지 전환 시 호출)
    resetInitialization: function(pageType) {
      if (pageType) {
        window.cmms.memo.initialized[pageType] = false;
      } else {
        // 모든 초기화 상태 리셋
        Object.keys(window.cmms.memo.initialized).forEach(key => {
          window.cmms.memo.initialized[key] = false;
        });
      }
      console.log('Memo initialization state reset:', pageType || 'all');
    }
  });
  
  // 페이지별 초기화 등록 (root 기반 구조)
  window.cmms.pages.register('memo-list', function(root) {
    window.cmms.memo.initList(root);
  });
  
  window.cmms.pages.register('memo-detail', function(root) {
    window.cmms.memo.initDetail(root);
  });
  
  window.cmms.pages.register('memo-form', function(root) {
    window.cmms.memo.initForm(root);
  });
  
})();