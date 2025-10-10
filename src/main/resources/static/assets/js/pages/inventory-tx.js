/**
 * InventoryTx 모듈 JavaScript
 * 
 * 재고거래 관리 모듈의 페이지별 초기화 및 기능을 담당합니다.
 * 공통 유틸(TableManager, DataLoader, fileUpload, notification)을 우선 사용합니다.
 * root 기반 DOM 접근으로 중복 바인딩 방지 및 SPA 최적화를 구현합니다.
 */

(function() {
  'use strict';
  
  window.cmms = window.cmms || {};
  window.cmms.inventoryTx = window.cmms.inventoryTx || {};

  Object.assign(window.cmms.inventoryTx, {
    
    // 거래 페이지 초기화 (root 기반)
    initTransaction: function(root) {
      console.log('InventoryTx transaction page initialized', root);
      console.log('Root element:', root.tagName, root.className);
      this.initTransactionForm(root);
      this.initInventorySearch(root);
    },
    
    // 원장 페이지 초기화 (root 기반)
    initLedger: function(root) {
      console.log('InventoryTx ledger page initialized', root);
      this.initPagination(root);
      this.initSearch(root);
    },
    
    // 마감 페이지 초기화 (root 기반)
    initClosing: function(root) {
      console.log('InventoryTx closing page initialized', root);
      this.initClosingForm(root);
    },
    
    // 거래 폼 초기화 (root 기반)
    initTransactionForm: function(root) {
      console.log('InventoryTx transaction form initialized');
      
      const tabs = root.querySelectorAll('.tab-btn');
      const panels = root.querySelectorAll('.tx-panel');
      const txTypeInput = root.querySelector('#txType');
      const form = root.querySelector('#txForm');
      
      console.log('Found elements:', {
        tabs: tabs.length,
        panels: panels.length,
        txTypeInput: !!txTypeInput,
        form: !!form
      });
      
      if (!tabs.length || !form) {
        console.warn('Required elements not found for tab initialization');
        return;
      }
      
      // 탭 전환 처리
      tabs.forEach(btn => {
        btn.addEventListener('click', () => {
          console.log('Tab clicked:', btn.getAttribute('data-tab'));
          
          tabs.forEach(b => b.classList.remove('active'));
          panels.forEach(p => p.classList.remove('active'));
          btn.classList.add('active');
          
          const tabType = btn.getAttribute('data-tab');
          
          const activePanel = root.querySelector(`.tx-panel[data-panel="${tabType}"]`);
          console.log('Active panel found:', !!activePanel);
          if (activePanel) activePanel.classList.add('active');
          
          // 폼 초기화 및 오늘 날짜 설정
          form.reset();
          
          // 폼 리셋 후 txType 다시 설정 (리셋하면 초기값 'IN'으로 되돌아가므로)
          if (txTypeInput) txTypeInput.value = tabType;
          
          // 활성화된 패널 내의 날짜 입력 필드 찾기
          if (activePanel) {
            const txDateInput = activePanel.querySelector('input[name="txDate"]');
            if (txDateInput) {
              txDateInput.value = new Date().toISOString().split('T')[0];
            }
          }
        });
      });
      
      // 초기 날짜 설정 (모든 탭의 날짜 필드)
      const today = new Date().toISOString().split('T')[0];
      const allDateInputs = root.querySelectorAll('input[name="txDate"]');
      allDateInputs.forEach(input => {
        input.value = today;
      });
      
      // 초기 탭 상태 설정 (첫 번째 탭 활성화)
      const firstTab = root.querySelector('.tab-btn[data-tab="IN"]');
      const firstPanel = root.querySelector('.tx-panel[data-panel="IN"]');
      console.log('Initial tab setup:', {
        firstTab: !!firstTab,
        firstPanel: !!firstPanel
      });
      if (firstTab && firstPanel) {
        firstTab.classList.add('active');
        firstPanel.classList.add('active');
        console.log('Initial tab activated: IN');
      }
      
      // 자재 선택 시 현재 재고 표시 (모든 탭의 inventoryId 필드에 바인딩)
      const inventoryInputs = root.querySelectorAll('input[name="inventoryId"]');
      inventoryInputs.forEach(input => {
        input.addEventListener('change', () => this.loadCurrentStock(root));
        input.addEventListener('blur', () => this.loadCurrentStock(root));
      });
      
      // 수량 변경 시 재고 반영
      const quantityInput = root.querySelector('#quantity');
      if (quantityInput) {
        quantityInput.addEventListener('input', () => this.updateStockPreview(root));
      }
      
      // 거래 내역 로드
      this.loadTransactionHistory(root);
    },
    
    // 현재 재고 로드
    loadCurrentStock: async function(root) {
      const inventorySelect = root.querySelector('#inventoryId');
      const currentStockSpan = root.querySelector('#currentStock');
      
      if (!inventorySelect || !currentStockSpan) return;
      
      const inventoryId = inventorySelect.value;
      if (!inventoryId) {
        currentStockSpan.textContent = '0';
        this.updateStockPreview(root);
        return;
      }
      
      try {
        const response = await fetch(`/api/inventories/${inventoryId}/stock`);
        if (!response.ok) throw new Error('Failed to load stock info');
        
        const data = await response.json();
        currentStockSpan.textContent = data.currentStock || 0;
        this.updateStockPreview(root);
      } catch (error) {
        console.error('Stock load error:', error);
        currentStockSpan.textContent = '0';
        this.updateStockPreview(root);
      }
    },
    
    // 재고 미리보기 업데이트
    updateStockPreview: function(root) {
      const txTypeInput = root.querySelector('#txType');
      const quantityInput = root.querySelector('#quantity');
      const currentStockSpan = root.querySelector('#currentStock');
      const newStockSpan = root.querySelector('#newStock');
      
      if (!txTypeInput || !quantityInput || !currentStockSpan || !newStockSpan) return;
      
      const txType = txTypeInput.value;
      const quantity = parseInt(quantityInput.value) || 0;
      const currentStock = parseInt(currentStockSpan.textContent) || 0;
      
      let newStock = currentStock;
      if (txType === 'IN') {
        newStock = currentStock + quantity;
      } else if (txType === 'OUT') {
        newStock = currentStock - quantity;
      }
      
      newStockSpan.textContent = newStock;
    },
    
    // 거래 내역 로드
    loadTransactionHistory: async function(root) {
      const tbody = root.querySelector('#txHistoryBody');
      if (!tbody) return;
      
      try {
        const response = await fetch('/api/inventory-tx?size=5&sort=transactionDate,desc');
        if (!response.ok) throw new Error('Failed to load transaction history');
        
        const data = await response.json();
        
        if (!data || !data.content || data.content.length === 0) {
          tbody.innerHTML = '<tr><td colspan="9" class="cell-center">거래 내역이 없습니다.</td></tr>';
          return;
        }
        
        tbody.innerHTML = data.content.map(tx => `
          <tr>
            <td>${tx.transactionDate || '-'}</td>
            <td class="cell-center">${tx.txType || '-'}</td>
            <td>${tx.inventoryId || '-'}</td>
            <td class="cell-center">${tx.storageId || '-'}</td>
            <td class="cell-right">${tx.inQty ? Number(tx.inQty).toFixed(3) : '0.000'}</td>
            <td class="cell-right">${tx.outQty ? Number(tx.outQty).toFixed(3) : '0.000'}</td>
            <td class="cell-right">${tx.unitCost ? Number(tx.unitCost).toLocaleString() : '-'}</td>
            <td class="cell-right">${tx.amount ? Number(tx.amount).toLocaleString() : '-'}</td>
            <td>${tx.note || '-'}</td>
          </tr>
        `).join('');
      } catch (error) {
        console.error('History load error:', error);
        tbody.innerHTML = '<tr><td colspan="9" class="cell-center error">거래 내역을 불러올 수 없습니다.</td></tr>';
      }
    },
    
    // 재고 검색 초기화 (root 기반)
    initInventorySearch: function(root) {
      const searchBtn = root.querySelector('[data-inventory-search]');
      if (searchBtn) {
        searchBtn.addEventListener('click', () => {
          this.searchInventory(root);
        });
      }
    },
    
    // 재고 검색 (공통 DataLoader 사용, root 기반)
    searchInventory: async function(root) {
      const searchInput = root.querySelector('#inventory-search');
      if (!searchInput) return;
      
      const searchValue = searchInput.value.trim();
      if (!searchValue) {
        if (window.cmms?.notification) {
          window.cmms.notification.warning('검색할 재고번호나 재고명을 입력해주세요.');
        } else {
          alert('검색할 재고번호나 재고명을 입력해주세요.');
        }
        return;
      }

      try {
        // 공통 DataLoader 사용
        const data = await window.cmms.common.DataLoader.load('/api/inventories', {
          params: { search: searchValue, size: 10 }
        });
        
        const resultsSection = root.querySelector('#inventory-search-results');
        const resultsBody = root.querySelector('#inventory-results-body');
        
        if (data.content && data.content.length > 0) {
          resultsBody.innerHTML = '';
          data.content.forEach(inventory => {
            const row = document.createElement('tr');
            row.innerHTML = `
              <td>${inventory.inventoryId}</td>
              <td>${inventory.name || '-'}</td>
              <td>${inventory.deptId || '-'}</td>
              <td>${inventory.model || '-'}</td>
              <td class="actions">
                <button class="btn btn-sm" onclick="window.cmms.inventoryTx.selectInventory('${inventory.inventoryId}', '${inventory.name}', arguments[0].closest('[data-slot-root]'))">선택</button>
              </td>
            `;
            resultsBody.appendChild(row);
          });
          resultsSection.classList.remove('hidden');
        } else {
          resultsSection.classList.add('hidden');
          if (window.cmms?.notification) {
            window.cmms.notification.warning('해당 재고를 찾을 수 없습니다.');
          } else {
            alert('해당 재고를 찾을 수 없습니다.');
          }
        }
      } catch (error) {
        console.error('재고 검색 오류:', error);
        if (window.cmms?.notification) {
          window.cmms.notification.error('재고 검색 중 오류가 발생했습니다.');
        } else {
          alert('재고 검색 중 오류가 발생했습니다.');
        }
      }
    },
    
    // 재고 선택 (root 기반)
    selectInventory: function(inventoryId, inventoryName, root) {
      // 폼 필드에 선택된 재고 정보 입력
      const inventoryIdField = root.querySelector('#inventory-id');
      const inventoryNameField = root.querySelector('#inventory-name');
      
      if (inventoryIdField) inventoryIdField.value = inventoryId;
      if (inventoryNameField) inventoryNameField.value = inventoryName;
      
      // 검색 결과 숨기기
      const resultsSection = root.querySelector('#inventory-search-results');
      if (resultsSection) resultsSection.classList.add('hidden');
      
      // 검색 입력 필드 초기화
      const searchField = root.querySelector('#inventory-search');
      if (searchField) searchField.value = '';
      
      if (window.cmms?.notification) {
        window.cmms.notification.success(`${inventoryName} 재고가 선택되었습니다.`);
      }
    },
    
    // 페이지네이션 초기화 (공통 유틸 사용, root 기반)
    initPagination: function(root) {
      console.log('InventoryTx pagination initialized - app.js 공통 로직에서 처리됨');
      // app.js의 공통 페이지네이션 로직이 data-nav-btn을 자동으로 처리
      // root 범위 내에서만 처리되므로 중복 바인딩 방지됨
    },
    
    // 검색 초기화 (root 기반)
    initSearch: function(root) {
      console.log('InventoryTx search initialized');
      
      // 폼 검증 처리 (ledger.html용)
      const form = root.querySelector('form');
      if (form) {
        form.addEventListener('submit', (e) => {
          if (!this.validateLedgerForm(root)) {
            e.preventDefault();
          }
        });
      }
    },
    
    // 원장 폼 검증
    validateLedgerForm: function(root) {
      const fromYm = root.querySelector('#fromYm');
      const toYm = root.querySelector('#toYm');
      
      if (!fromYm || !toYm) return true;
      
      if (!fromYm.value || !toYm.value) {
        window.cmms.notification.error('기간을 선택하세요');
        return false;
      }
      
      if (fromYm.value > toYm.value) {
        window.cmms.notification.error('시작월은 종료월보다 클 수 없습니다');
        return false;
      }
      
      return true;
    },
    
    // 마감 폼 초기화 (root 기반)
    initClosingForm: function(root) {
      console.log('InventoryTx closing form initialized');
      
      // 마감월 최대값 설정 (이전월까지만 가능)
      const yyyymmInput = root.querySelector('#yyyymm');
      if (yyyymmInput) {
        const d = new Date();
        const year = d.getMonth() === 0 ? d.getFullYear() - 1 : d.getFullYear();
        const month = d.getMonth() === 0 ? 12 : d.getMonth();
        const mm = String(month).padStart(2, '0');
        yyyymmInput.setAttribute('max', `${year}-${mm}`);
      }
      
      // this.initClosingSubmit(root); // FormManager 제거됨
    },
    
    // 마감 제출 초기화 (FormManager 제거됨 - app.js SPA 폼 처리 활용)
    // initClosingSubmit: function(root) {
    //   const form = root.querySelector('#closingForm');
    //   if (!form) return;
    //   
    //   // 공통 FormManager를 활용한 폼 제출 처리
    //   if (window.cmms?.formManager) {
    //     // FormManager가 처리하도록 위임
    //     window.cmms.formManager.init(form, {
    //       onSuccess: (result) => {
    //         this.handleClosingSuccess(result, root);
    //       },
    //       onError: (error) => {
    //         this.handleClosingError(error, root);
    //       }
    //     });
    //   } else {
    //     // FormManager가 없는 경우 직접 처리
    //     form.addEventListener('submit', async (event) => {
    //       event.preventDefault();
    //       await this.handleDirectClosing(form, root);
    //     });
    //   }
    // },
    
    // 마감 성공 처리 (root 기반)
    handleClosingSuccess: function(result, root) {
      const monthYear = root.querySelector('#month-year')?.value;
      
      if (window.cmms?.notification) {
        window.cmms.notification.success(`${monthYear} 월 마감이 완료되었습니다.`);
      } else {
        alert(`${monthYear} 월 마감이 완료되었습니다.`);
      }
      
      // SPA 네비게이션으로 레이아웃 유지
      setTimeout(() => {
        window.cmms.navigation.navigate('/inventory-tx/closing');
      }, 1000);
    },
    
    // 마감 에러 처리 (root 기반)
    handleClosingError: function(error, root) {
      console.error('마감 처리 오류:', error);
      if (window.cmms?.notification) {
        window.cmms.notification.error('마감 처리 중 오류가 발생했습니다.');
      } else {
        alert('마감 처리 중 오류가 발생했습니다.');
      }
    },
    
    // 직접 마감 처리 (FormManager 없을 때)
    handleDirectClosing: async function(form, root) {
      const monthYear = root.querySelector('#month-year')?.value;
      
      if (!monthYear) {
        if (window.cmms?.notification) {
          window.cmms.notification.warning('마감할 년월을 선택해주세요.');
        } else {
          alert('마감할 년월을 선택해주세요.');
        }
        return;
      }
      
      try {
        // 공통 DataLoader 사용
        const result = await window.cmms.common.DataLoader.load(form.action, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ monthYear: monthYear })
        });
        
        this.handleClosingSuccess(result, root);
        
      } catch (error) {
        this.handleClosingError(error, root);
      }
    }
  });
  
  // 페이지별 초기화 등록 (root 기반 구조)
  window.cmms.pages.register('inventory-tx-transaction', function(root) {
    window.cmms.inventoryTx.initTransaction(root);
  });
  
  window.cmms.pages.register('inventory-tx-ledger', function(root) {
    window.cmms.inventoryTx.initLedger(root);
  });
  
  window.cmms.pages.register('inventory-tx-closing', function(root) {
    window.cmms.inventoryTx.initClosing(root);
  });
  
})();