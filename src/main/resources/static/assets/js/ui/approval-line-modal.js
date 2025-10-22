const DECISIONS = [
  { value: 'APPRL', label: '결재' },
  { value: 'AGREE', label: '합의' },
  { value: 'INFO', label: '참조' }
];

const AUTOCOMPLETE_DEBOUNCE = 200;
const AUTOCOMPLETE_LIMIT = 7;
const AUTOCOMPLETE_CACHE_TTL = 60_000;

const suggestionCache = new Map();

function decisionOptionsHtml(selectedValue) {
  return DECISIONS.map(({ value, label }) => {
    const selected = value === selectedValue ? 'selected' : '';
    return `<option value="${value}" ${selected}>${label}</option>`;
  }).join('');
}

function formatDisplayLabel(member) {
  if (!member) {
    return '';
  }
  const { name, memberId, deptName } = member;
  if (name && memberId) {
    return deptName ? `${name} (${memberId}) · ${deptName}` : `${name} (${memberId})`;
  }
  return memberId || name || '';
}

function createRowTemplate(index, step = {}) {
  const stepNo = step.stepNo ?? index + 1;
  const memberId = step.memberId ?? '';
  const decision = step.decision ?? 'APPRL';
  const memberName = step.memberName ?? step.name ?? null;
  const deptName = step.deptName ?? null;
  const displayLabel = memberId ? formatDisplayLabel({ name: memberName, memberId, deptName }) : '';

  return `
    <tr data-approval-line-row>
      <td><input type="number" min="1" value="${stepNo}" data-approval-step-no /></td>
      <td>
        <div class="approval-member-input" data-approval-member-wrapper>
          <input
            type="text"
            value="${displayLabel}"
            placeholder="이름 또는 사번"
            autocomplete="off"
            data-approval-member-display
          />
          <input type="hidden" value="${memberId}" data-approval-member-id />
          <div class="approval-member-suggestions" data-approval-member-suggestions hidden></div>
        </div>
      </td>
      <td>
        <select data-approval-decision>
          ${decisionOptionsHtml(decision)}
        </select>
      </td>
      <td>
        <button type="button" class="approval-modal__row-remove" title="삭제" data-approval-line-remove>&times;</button>
      </td>
    </tr>
  `;
}

export function initApprovalLineModal() {
  const modal = document.getElementById('approval-line-modal');
  if (!modal) {
    return;
  }

  const rowsContainer = modal.querySelector('#approval-line-rows');
  const stageBadge = modal.querySelector('#approval-modal-stage');
  const targetSpan = modal.querySelector('#approval-modal-target');
  const addButton = modal.querySelector('[data-approval-line-add]');
  const submitButton = modal.querySelector('[data-approval-line-submit]');
  const dismissElements = modal.querySelectorAll('[data-approval-modal-dismiss]');

  const state = {
    entityId: null,
    stage: 'PLN',
    module: 'inspections',
    detailPath: 'inspection',
    deptId: null
  };

  function getSelectedMemberIds(excludeRow) {
    return new Set(
      Array.from(rowsContainer.querySelectorAll('[data-approval-member-id]'))
        .map((input) => {
          const row = input.closest('tr');
          if (excludeRow && row === excludeRow) {
            return null;
          }
          return input.value.trim();
        })
        .filter(Boolean)
    );
  }

  function extractMemberId(text) {
    if (!text) return null;
    const trimmed = text.trim();
    if (!trimmed) return null;
    const parenMatch = trimmed.match(/\(([A-Za-z0-9_\-]+)\)\s*$/);
    if (parenMatch) {
      return parenMatch[1];
    }
    if (/^[A-Za-z0-9_\-]{3,}$/.test(trimmed)) {
      return trimmed;
    }
    return null;
  }

  function ensureHiddenMemberId(row) {
    const hidden = row.querySelector('[data-approval-member-id]');
    if (hidden?.value?.trim()) {
      return true;
    }
    const input = row.querySelector('[data-approval-member-display]');
    if (!input) {
      return false;
    }
    const id = extractMemberId(input.value);
    if (id) {
      hidden.value = id;
      return true;
    }
    return false;
  }

  function renderSuggestions(row, items, keyword) {
    const suggestionsEl = row.querySelector('[data-approval-member-suggestions]');
    const hidden = row.querySelector('[data-approval-member-id]');
    if (!suggestionsEl) return;

    const selectedIds = getSelectedMemberIds(row);
    const normalizedKeyword = keyword?.trim()?.toLowerCase() ?? '';
    let html = '';
    const usableItems = [];

    items.forEach((item) => {
      if (!item?.memberId) {
        return;
      }
      const isSelectedElsewhere = selectedIds.has(item.memberId);
      const highlightClass = '';
      const disabledAttr = isSelectedElsewhere ? 'aria-disabled="true"' : '';
      usableItems.push({ ...item, disabled: isSelectedElsewhere });
      html += `
        <button type="button" class="approval-member-suggestion ${highlightClass}" data-approval-suggestion ${disabledAttr}
          data-member-id="${item.memberId}"
          data-member-name="${item.name ?? ''}"
          data-dept-name="${item.deptName ?? ''}"
        >
          <div>
            <strong>${item.name ?? item.memberId}</strong>
            <small>${item.memberId}</small>
          </div>
          <div>
            ${item.deptName ? `<small>${item.deptName}</small>` : ''}
            ${item.position ? `<small>${item.position}</small>` : ''}
          </div>
        </button>
      `;
    });

    if (!usableItems.length) {
      html = `<div class="approval-member-empty">
        ${normalizedKeyword ? `'${keyword}'에 대한 검색 결과가 없습니다.` : '검색어를 입력해주세요.'}
      </div>`;
    }

    suggestionsEl.innerHTML = html;
    suggestionsEl.hidden = false;
    suggestionsEl.dataset.items = JSON.stringify(usableItems);
    hidden.dataset.lastKeyword = keyword ?? '';
    suggestionsEl.dataset.activeIndex = '-1';
  }

  function hideSuggestions(row) {
    const suggestionsEl = row.querySelector('[data-approval-member-suggestions]');
    if (!suggestionsEl) return;
    suggestionsEl.hidden = true;
    suggestionsEl.innerHTML = '';
    delete suggestionsEl.dataset.items;
    delete suggestionsEl.dataset.activeIndex;
  }

  function selectCandidate(row, candidate) {
    if (!candidate) return;
    const hidden = row.querySelector('[data-approval-member-id]');
    const input = row.querySelector('[data-approval-member-display]');
    hidden.value = candidate.memberId;
    const label = formatDisplayLabel(candidate);
    input.value = label || candidate.memberId;
    hideSuggestions(row);
  }

  function getCachedSuggestions(keyword) {
    const cacheKey = keyword.toLowerCase();
    const cached = suggestionCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < AUTOCOMPLETE_CACHE_TTL) {
      return cached.data;
    }
    return null;
  }

  async function fetchSuggestions(keyword, options = {}) {
    const trimmed = keyword.trim();
    if (!trimmed) {
      return [];
    }
    const cached = getCachedSuggestions(trimmed);
    if (cached) {
      return cached;
    }
    const params = new URLSearchParams({
      q: trimmed,
      size: String(AUTOCOMPLETE_LIMIT)
    });
    if (state.deptId) {
      params.set('deptId', state.deptId);
    }
    const response = await fetch(`/api/members/approval-candidates?${params.toString()}`, {
      credentials: 'same-origin',
      signal: options.signal
    });
    if (!response.ok) {
      throw new Error(`Search failed: ${response.status}`);
    }
    const json = await response.json();
    const list = Array.isArray(json?.content) ? json.content : [];
    suggestionCache.set(trimmed.toLowerCase(), { data: list, timestamp: Date.now() });
    return list;
  }

  function activateNextSuggestion(row, direction) {
    const suggestionsEl = row.querySelector('[data-approval-member-suggestions]');
    if (!suggestionsEl || suggestionsEl.hidden) return;
    const items = JSON.parse(suggestionsEl.dataset.items ?? '[]');
    if (!items.length) return;

    let activeIndex = Number.parseInt(suggestionsEl.dataset.activeIndex ?? '-1', 10);
    activeIndex += direction;
    if (activeIndex < 0) {
      activeIndex = items.length - 1;
    } else if (activeIndex >= items.length) {
      activeIndex = 0;
    }
    suggestionsEl.dataset.activeIndex = String(activeIndex);
    const buttons = suggestionsEl.querySelectorAll('[data-approval-suggestion]');
    buttons.forEach((btn, index) => {
      if (index === activeIndex) {
        btn.classList.add('is-active');
        btn.focus();
      } else {
        btn.classList.remove('is-active');
      }
    });
  }

  function attachAutocomplete(row) {
    const input = row.querySelector('[data-approval-member-display]');
    const hidden = row.querySelector('[data-approval-member-id]');
    const suggestionsEl = row.querySelector('[data-approval-member-suggestions]');
    if (!input || !hidden || !suggestionsEl) {
      return;
    }

    let debounceTimer = null;
    let activeFetch = null;

    function scheduleSearch(value) {
      if (debounceTimer) {
        window.clearTimeout(debounceTimer);
      }
      if (!value.trim()) {
        hidden.value = '';
        hideSuggestions(row);
        return;
      }

      debounceTimer = window.setTimeout(async () => {
        if (activeFetch) {
          activeFetch.abort();
        }
        const controller = new AbortController();
        activeFetch = controller;
        try {
          const suggestions = await fetchSuggestions(value, { signal: controller.signal });
          renderSuggestions(row, suggestions, value);
        } catch (error) {
          if (error.name !== 'AbortError') {
            console.error('Member search error', error);
            hideSuggestions(row);
          }
        } finally {
          if (activeFetch === controller) {
            activeFetch = null;
          }
        }
      }, AUTOCOMPLETE_DEBOUNCE);
    }

    input.addEventListener('input', (event) => {
      hidden.value = '';
      scheduleSearch(event.target.value);
    });

    input.addEventListener('focus', (event) => {
      const value = event.target.value.trim();
      if (value) {
        scheduleSearch(value);
      }
    });

    input.addEventListener('blur', () => {
      window.setTimeout(() => hideSuggestions(row), 150);
    });

    input.addEventListener('keydown', (event) => {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        activateNextSuggestion(row, 1);
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        activateNextSuggestion(row, -1);
      } else if (event.key === 'Enter') {
        const suggestions = JSON.parse(suggestionsEl.dataset.items ?? '[]');
        const activeIndex = Number.parseInt(suggestionsEl.dataset.activeIndex ?? '-1', 10);
        if (!suggestionsEl.hidden && suggestions.length && activeIndex >= 0) {
          event.preventDefault();
          const candidate = suggestions[activeIndex];
          if (!candidate?.disabled) {
            selectCandidate(row, candidate);
          }
        } else {
          ensureHiddenMemberId(row);
        }
      } else if (event.key === 'Escape') {
        hideSuggestions(row);
      }
    });

    suggestionsEl.addEventListener('mousedown', (event) => {
      const button = event.target.closest('[data-approval-suggestion]');
      if (!button) return;
      const disabled = button.getAttribute('aria-disabled') === 'true';
      if (disabled) return;
      const candidate = {
        memberId: button.dataset.memberId,
        name: button.dataset.memberName,
        deptName: button.dataset.deptName
      };
      selectCandidate(row, candidate);
    });
  }

  function open(config) {
    state.entityId = config.entityId;
    state.stage = config.stage || 'PLN';
    state.module = config.module || 'inspections';
    state.detailPath = config.detailPath || 'inspection';
    state.deptId = config.deptId || null;

    stageBadge.textContent = state.stage === 'PLN' ? '계획 결재' : '실적 결재';
    targetSpan.textContent = `문서번호: ${state.entityId}`;

    rowsContainer.innerHTML = createRowTemplate(0);
    modal.hidden = false;
    attachAutocomplete(rowsContainer.querySelector('tr'));
  }

  function close() {
    modal.hidden = true;
    rowsContainer.innerHTML = '';
  }

  function addRow() {
    const index = rowsContainer.querySelectorAll('tr').length;
    rowsContainer.insertAdjacentHTML('beforeend', createRowTemplate(index));
    focusLastRow();
    attachAutocomplete(rowsContainer.querySelector('tr:last-child'));
  }

  function focusLastRow() {
    const lastRow = rowsContainer.querySelector('tr:last-child');
    lastRow?.querySelector('[data-approval-member-display]')?.focus();
  }

  function collectPayload() {
    const rows = Array.from(rowsContainer.querySelectorAll('tr'));
    if (rows.length === 0) {
      throw new Error('결재선을 한 명 이상 등록하세요.');
    }

    const memberIds = new Set();

    const steps = rows.map((row) => {
      const stepNo = parseInt(row.querySelector('[data-approval-step-no]').value, 10);
      const hiddenMember = row.querySelector('[data-approval-member-id]');
      const decision = row.querySelector('[data-approval-decision]').value;

      if (!ensureHiddenMemberId(row)) {
        throw new Error('결재자를 선택하거나 사번을 정확히 입력하세요.');
      }

      const memberId = hiddenMember.value.trim();
      if (memberIds.has(memberId)) {
        throw new Error('동일한 결재자를 중복으로 선택할 수 없습니다.');
      }
      memberIds.add(memberId);

      return {
        stepNo: Number.isNaN(stepNo) ? null : stepNo,
        memberId,
        decision
      };
    });

    return {
      stage: state.stage,
      steps
    };
  }

  async function submit() {
    try {
      const payload = collectPayload();

      const csrfToken = window.cmms?.csrf?.readToken?.() || '';
      const response = await fetch(`/api/${state.module}/${state.entityId}/approvals`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': csrfToken
        },
        credentials: 'same-origin',
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        throw new Error(`상신 실패: ${response.status}`);
      }

      if (window.cmms?.notification) {
        window.cmms.notification.success('결재가 상신되었습니다.');
      }

      close();

      setTimeout(() => {
        window.cmms?.navigation?.navigate?.(`/${state.detailPath}/detail/${state.entityId}`) || window.location.reload();
      }, 800);
    } catch (error) {
      console.error('Approval modal submit error', error);
      if (window.cmms?.notification) {
        window.cmms.notification.error(error.message || '결재 상신 중 오류가 발생했습니다.');
      }
    }
  }

  addButton?.addEventListener('click', addRow);

  submitButton?.addEventListener('click', submit);

  rowsContainer.addEventListener('click', (event) => {
    if (event.target.matches('[data-approval-line-remove]')) {
      const row = event.target.closest('tr');
      row?.remove();
    }
  });

  dismissElements.forEach((el) => el.addEventListener('click', close));

  window.cmms = window.cmms || {};
  window.cmms.approvalLineModal = { open, close };
}
