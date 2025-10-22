/**
 * 워크플로우 액션 모듈
 * 
 * inspection, workorder, workpermit의 공통 워크플로우 함수 제공
 * - 결재 상신
 * - 담당자 확정
 * - 실적 준비
 * 
 * (approval.js는 독립적인 패턴을 사용하므로 제외)
 */

/**
 * 워크플로우 액션 초기화
 */
export function initWorkflowActions() {
  if (!window.cmms) window.cmms = {};
  if (!window.cmms.workflow) window.cmms.workflow = {};

  /**
   * 결재 상신
   * 
   * @param {string} id - 문서 ID
   * @param {string} stage - 단계 (PLN/ACT)
   * @param {string} module - 모듈명 (inspections/workorders/workpermits)
   * @param {string} detailPath - 상세 페이지 경로 (inspection/workorder/workpermit)
   * 
   * @example
   * submitApproval('I123', 'PLN', 'inspections', 'inspection')
   * submitApproval('W456', 'ACT', 'workorders', 'workorder')
   */
  async function submitApproval(id, stage, module = 'inspections', detailPath = 'inspection') {
    const modalEnabledModules = ['inspections', 'workorders', 'workpermits'];
    if (modalEnabledModules.includes(module) && window.cmms?.approvalLineModal) {
      const contextSelector = `[data-module="${detailPath}-detail"]`;
      const contextElement = document.querySelector(contextSelector);
      const deptId = contextElement?.dataset?.deptId || null;
      window.cmms.approvalLineModal.open({
        entityId: id,
        stage,
        module,
        detailPath,
        deptId
      });
      return;
    }

    try {
      if (!confirm('결재를 상신하시겠습니까?')) {
        return;
      }
      
      const apiUrl = stage === 'PLN' 
        ? `/api/${module}/${id}/submit-plan-approval`
        : `/api/${module}/${id}/submit-actual-approval`;
      
      const csrfToken = window.cmms?.csrf?.readToken() || '';
      
      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': csrfToken
        },
        credentials: 'same-origin'
      });
      
      if (!response.ok) {
        throw new Error(`결재 상신 실패: ${response.status}`);
      }
      
      if (window.cmms?.notification) {
        window.cmms.notification.success('결재가 상신되었습니다.');
      }
      
      setTimeout(() => {
        window.cmms.navigation.navigate(`/${detailPath}/detail/${id}`);
      }, 1000);
      
    } catch (error) {
      console.error('Approval submission error:', error);
      if (window.cmms?.notification) {
        window.cmms.notification.error('결재 상신 중 오류가 발생했습니다.');
      }
    }
  }

  /**
   * 담당자 확정 (결재 없이 즉시 완료)
   * 
   * @param {string} id - 문서 ID
   * @param {string} module - 모듈명 (inspections/workorders/workpermits)
   * @param {string} detailPath - 상세 페이지 경로 (inspection/workorder/workpermit)
   * 
   * @example
   * confirmComplete('I123', 'inspections', 'inspection')
   */
  async function confirmComplete(id, module = 'inspections', detailPath = 'inspection') {
    try {
      if (!confirm('담당자 확정하시겠습니까?\n(결재 없이 즉시 완료 처리됩니다)')) {
        return;
      }
      
      // ApiController와 일치하는 경로 사용
      const apiUrl = `/api/${module}/${id}/confirm`;
      
      const csrfToken = window.cmms?.csrf?.readToken() || '';
      
      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': csrfToken
        },
        credentials: 'same-origin'
      });
      
      if (!response.ok) {
        throw new Error(`담당자 확정 실패: ${response.status}`);
      }
      
      if (window.cmms?.notification) {
        window.cmms.notification.success('담당자 확정이 완료되었습니다.');
      }
      
      // 상세 페이지 새로고침
      setTimeout(() => {
        window.cmms.navigation.navigate(`/${detailPath}/detail/${id}`);
      }, 1000);
      
    } catch (error) {
      console.error('Confirm complete error:', error);
      if (window.cmms?.notification) {
        window.cmms.notification.error('담당자 확정 중 오류가 발생했습니다.');
      }
    }
  }

  /**
   * 실적 준비 (계획 데이터 복사)
   * 
   * @param {string} id - 문서 ID
   * @param {string} module - 모듈명 (inspections/workorders)
   * @param {string} detailPath - 상세 페이지 경로 (inspection/workorder)
   * 
   * @example
   * prepareActual('I123', 'inspections', 'inspection')
   * 
   * @note workpermit은 실적이 없으므로 이 함수를 사용하지 않음
   */
  async function prepareActual(id, module = 'inspections', detailPath = 'inspection') {
    try {
      if (!confirm('실적 입력을 시작하시겠습니까?\n(계획 데이터가 복사됩니다)')) {
        return;
      }
      
      const apiUrl = `/api/${module}/${id}/prepare-actual`;
      
      const csrfToken = window.cmms?.csrf?.readToken() || '';
      
      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': csrfToken
        },
        credentials: 'same-origin'
      });
      
      if (!response.ok) {
        throw new Error(`실적 준비 실패: ${response.status}`);
      }
      
      if (window.cmms?.notification) {
        window.cmms.notification.success('실적 입력이 준비되었습니다.');
      }
      
      // 상세 페이지로 이동
      setTimeout(() => {
        window.cmms.navigation.navigate(`/${detailPath}/detail/${id}`);
      }, 1000);
      
    } catch (error) {
      console.error('Prepare actual error:', error);
      if (window.cmms?.notification) {
        window.cmms.notification.error('실적 준비 중 오류가 발생했습니다.');
      }
    }
  }

  // 네임스페이스에 등록
  window.cmms.workflow = {
    submitApproval,
    confirmComplete,
    prepareActual
  };

  // ⭐ HTML onclick에서 사용하기 위한 전역 함수 등록
  window.submitApproval = submitApproval;
  window.confirmComplete = confirmComplete;
  window.prepareActual = prepareActual;
  
  console.log('✅ Workflow actions initialized');
}

