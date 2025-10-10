/**
 * 작업허가 체크리스트 JavaScript
 * 체크리스트 동적 표시/숨김 및 데이터 처리
 */

document.addEventListener('DOMContentLoaded', function() {
    // 보충작업 선택에 따른 체크리스트 그룹 표시/숨김
    document.querySelectorAll('input[name="workTypes"]').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const group = document.querySelector(`[data-work-type="${this.value}"]`);
            if (group) {
                group.style.display = this.checked ? 'block' : 'none';
            }
        });
    });
    
    // 확인/해당없음 체크박스 상호 배타적 처리
    document.querySelectorAll('.check-confirm').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            if (this.checked) {
                const notApplicableCheckbox = this.parentElement.querySelector('.check-not-applicable');
                notApplicableCheckbox.checked = false;
            }
        });
    });
    
    document.querySelectorAll('.check-not-applicable').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            if (this.checked) {
                const confirmCheckbox = this.parentElement.querySelector('.check-confirm');
                confirmCheckbox.checked = false;
            }
        });
    });
    
    // 폼 제출 시 체크리스트 데이터를 JSON으로 변환
    const form = document.querySelector('form[data-validate]');
    if (form) {
        form.addEventListener('submit', function() {
            const checklistData = collectChecklistData();
            document.getElementById('checksheet-json').value = JSON.stringify(checklistData);
        });
    }
    
    // 기존 데이터 로드 시 체크리스트 상태 복원
    const existingData = document.getElementById('checksheet-json').value;
    if (existingData) {
        try {
            const data = JSON.parse(existingData);
            loadChecklistData(data);
        } catch (e) {
            console.warn('체크리스트 데이터 파싱 실패:', e);
        }
    }
});

/**
 * 체크리스트 데이터 수집
 */
function collectChecklistData() {
    const workTypes = Array.from(document.querySelectorAll('input[name="workTypes"]:checked'))
        .map(cb => cb.value);
    
    const items = [];
    
    // 일반작업 항목들
    document.querySelectorAll('[name^="common_checked_"]').forEach(checkbox => {
        const index = checkbox.name.match(/\d+/)[0];
        const checked = checkbox.checked;
        const notApplicable = document.querySelector(`[name="common_not_applicable_${index}"]`).checked;
        const itemName = document.querySelector(`[name="common_item_name_${index}"]`).value;
        
        if (checked || notApplicable) {
            items.push({
                workType: 'COMMON',
                itemName: itemName,
                checked: checked,
                notApplicable: notApplicable
            });
        }
    });
    
    // 화기작업 항목들
    document.querySelectorAll('[name^="fire_checked_"]').forEach(checkbox => {
        const index = checkbox.name.match(/\d+/)[0];
        const checked = checkbox.checked;
        const notApplicable = document.querySelector(`[name="fire_not_applicable_${index}"]`).checked;
        const itemName = document.querySelector(`[name="fire_item_name_${index}"]`).value;
        
        if (checked || notApplicable) {
            items.push({
                workType: 'FIRE',
                itemName: itemName,
                checked: checked,
                notApplicable: notApplicable
            });
        }
    });
    
    // 밀폐공간작업 항목들
    document.querySelectorAll('[name^="confined_checked_"]').forEach(checkbox => {
        const index = checkbox.name.match(/\d+/)[0];
        const checked = checkbox.checked;
        const notApplicable = document.querySelector(`[name="confined_not_applicable_${index}"]`).checked;
        const itemName = document.querySelector(`[name="confined_item_name_${index}"]`).value;
        
        if (checked || notApplicable) {
            items.push({
                workType: 'CONFINED',
                itemName: itemName,
                checked: checked,
                notApplicable: notApplicable
            });
        }
    });
    
    // 전기작업 항목들
    document.querySelectorAll('[name^="electric_checked_"]').forEach(checkbox => {
        const index = checkbox.name.match(/\d+/)[0];
        const checked = checkbox.checked;
        const notApplicable = document.querySelector(`[name="electric_not_applicable_${index}"]`).checked;
        const itemName = document.querySelector(`[name="electric_item_name_${index}"]`).value;
        
        if (checked || notApplicable) {
            items.push({
                workType: 'ELECTRIC',
                itemName: itemName,
                checked: checked,
                notApplicable: notApplicable
            });
        }
    });
    
    // 고소작업 항목들
    document.querySelectorAll('[name^="high_checked_"]').forEach(checkbox => {
        const index = checkbox.name.match(/\d+/)[0];
        const checked = checkbox.checked;
        const notApplicable = document.querySelector(`[name="high_not_applicable_${index}"]`).checked;
        const itemName = document.querySelector(`[name="high_item_name_${index}"]`).value;
        
        if (checked || notApplicable) {
            items.push({
                workType: 'HIGH',
                itemName: itemName,
                checked: checked,
                notApplicable: notApplicable
            });
        }
    });
    
    // 굴착작업 항목들
    document.querySelectorAll('[name^="excavation_checked_"]').forEach(checkbox => {
        const index = checkbox.name.match(/\d+/)[0];
        const checked = checkbox.checked;
        const notApplicable = document.querySelector(`[name="excavation_not_applicable_${index}"]`).checked;
        const itemName = document.querySelector(`[name="excavation_item_name_${index}"]`).value;
        
        if (checked || notApplicable) {
            items.push({
                workType: 'EXCAVATION',
                itemName: itemName,
                checked: checked,
                notApplicable: notApplicable
            });
        }
    });
    
    return {
        workTypes: workTypes,
        items: items
    };
}

/**
 * 기존 체크리스트 데이터 로드
 */
function loadChecklistData(jsonData) {
    if (!jsonData || !jsonData.items) return;
    
    // 작업 유형 선택 복원
    if (jsonData.workTypes) {
        jsonData.workTypes.forEach(workType => {
            const checkbox = document.querySelector(`input[name="workTypes"][value="${workType}"]`);
            if (checkbox) {
                checkbox.checked = true;
                checkbox.dispatchEvent(new Event('change'));
            }
        });
    }
    
    // 체크리스트 항목 상태 복원
    jsonData.items.forEach(item => {
        const workType = item.workType.toLowerCase();
        const itemName = item.itemName;
        
        // 해당 항목의 체크박스 찾기
        const itemElements = document.querySelectorAll(`[name^="${workType}_item_name_"]`);
        itemElements.forEach(element => {
            if (element.value === itemName) {
                const index = element.name.match(/\d+/)[0];
                const checkedCheckbox = document.querySelector(`[name="${workType}_checked_${index}"]`);
                const notApplicableCheckbox = document.querySelector(`[name="${workType}_not_applicable_${index}"]`);
                
                if (item.checked) {
                    checkedCheckbox.checked = true;
                } else if (item.notApplicable) {
                    notApplicableCheckbox.checked = true;
                }
            }
        });
    });
}
