package com.cmms11.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.inspection.InspectionService;
import com.cmms11.workorder.WorkOrderService;
import com.cmms11.workpermit.WorkPermitService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private ApprovalStepRepository approvalStepRepository;

    @Mock
    private ApprovalInboxRepository approvalInboxRepository;

    @Mock
    private AutoNumberService autoNumberService;

    @Mock
    private InspectionService inspectionService;

    @Mock
    private WorkOrderService workOrderService;

    @Mock
    private WorkPermitService workPermitService;

    @InjectMocks
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(approvalService, "inspectionService", inspectionService);
        ReflectionTestUtils.setField(approvalService, "workOrderService", workOrderService);
        ReflectionTestUtils.setField(approvalService, "workPermitService", workPermitService);
        when(approvalRepository.findByIdCompanyId(anyString(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveShouldRejectWhenPreviousStepNotCompleted() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user2", "pwd"));

        Approval approval = buildApproval("APR001", "SUBMT");
        ApprovalStep firstStep = buildStep("APR001", 1, "user1", "APPRL", null, null, null);
        ApprovalStep secondStep = buildStep("APR001", 2, "user2", "AGREE", null, null, null);

        when(approvalRepository.findByIdCompanyIdAndIdApprovalId("CHROK", "APR001"))
            .thenReturn(Optional.of(approval));
        when(approvalStepRepository.findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo("CHROK", "APR001"))
            .thenReturn(List.of(firstStep, secondStep));

        assertThatThrownBy(() -> approvalService.approve("APR001", "ok"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이전 결재자(1번: user1)가 먼저 결재해야 합니다");
    }

    @Test
    void approveShouldRejectWhenPreviousStepRejected() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user2", "pwd"));

        Approval approval = buildApproval("APR002", "PROC");
        ApprovalStep firstStep = buildStep("APR002", 1, "user1", "APPRL", "REJECT", LocalDateTime.now(), null);
        ApprovalStep secondStep = buildStep("APR002", 2, "user2", "AGREE", null, null, null);

        when(approvalRepository.findByIdCompanyIdAndIdApprovalId("CHROK", "APR002"))
            .thenReturn(Optional.of(approval));
        when(approvalStepRepository.findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo("CHROK", "APR002"))
            .thenReturn(List.of(firstStep, secondStep));

        assertThatThrownBy(() -> approvalService.approve("APR002", "ok"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("1번 결재자가 반려하여 결재가 종료되었습니다");
    }

    @Test
    void approveShouldAllowInfoStepWithoutOrderCheck() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user2", "pwd"));

        Approval approval = buildApproval("APR003", "SUBMT");
        ApprovalStep firstStep = buildStep("APR003", 1, "user1", "APPRL", null, null, null);
        ApprovalStep infoStep = buildStep("APR003", 2, "user2", "INFO", null, null, null);

        when(approvalRepository.findByIdCompanyIdAndIdApprovalId("CHROK", "APR003"))
            .thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo("CHROK", "APR003"))
            .thenReturn(List.of(firstStep, infoStep));
        when(approvalStepRepository.save(any(ApprovalStep.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalInboxRepository.findByIdCompanyIdAndApprovalIdAndMemberId("CHROK", "APR003", "user2"))
            .thenReturn(Optional.of(new ApprovalInbox()));
        when(approvalInboxRepository.save(any(ApprovalInbox.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalResponse response = approvalService.approve("APR003", "확인");

        assertThat(response.status()).isEqualTo("PROC");
    }

    @Test
    void approveShouldCompleteWhenLastApproverFinishes() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user1", "pwd"));

        Approval approval = buildApproval("APR004", "SUBMT");
        approval.setRefEntity(null);
        ApprovalStep onlyStep = buildStep("APR004", 1, "user1", "APPRL", null, null, null);

        when(approvalRepository.findByIdCompanyIdAndIdApprovalId("CHROK", "APR004"))
            .thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo("CHROK", "APR004"))
            .thenReturn(List.of(onlyStep));
        when(approvalStepRepository.save(any(ApprovalStep.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalInboxRepository.findByIdCompanyIdAndApprovalIdAndMemberId("CHROK", "APR004", "user1"))
            .thenReturn(Optional.of(new ApprovalInbox()));
        when(approvalInboxRepository.save(any(ApprovalInbox.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalResponse response = approvalService.approve("APR004", "승인");

        assertThat(response.status()).isEqualTo("APPRV");
    }

    private Approval buildApproval(String approvalId, String status) {
        Approval approval = new Approval();
        approval.setId(new ApprovalId("CHROK", approvalId));
        approval.setStatus(status);
        approval.setTitle("테스트 결재");
        approval.setCreatedBy("user1");
        approval.setUpdatedBy("user1");
        return approval;
    }

    private ApprovalStep buildStep(
        String approvalId,
        int stepNo,
        String memberId,
        String decision,
        String result,
        LocalDateTime decidedAt,
        String comment
    ) {
        ApprovalStep step = new ApprovalStep();
        step.setId(new ApprovalStepId("CHROK", approvalId, stepNo));
        step.setMemberId(memberId);
        step.setDecision(decision);
        step.setResult(result);
        step.setDecidedAt(decidedAt);
        step.setComment(comment);
        return step;
    }
}
