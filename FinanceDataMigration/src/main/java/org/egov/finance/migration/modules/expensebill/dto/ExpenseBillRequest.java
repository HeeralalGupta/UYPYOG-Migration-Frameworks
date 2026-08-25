package org.egov.finance.migration.modules.expensebill.dto;

public class ExpenseBillRequest {

    private String workFlowAction;
    private Long approvalPosition;
    private String approvalComment;
    private String approvalDesignation;
    private EgBillregister egBillregister;

    public String getWorkFlowAction() {
        return workFlowAction;
    }

    public void setWorkFlowAction(String workFlowAction) {
        this.workFlowAction = workFlowAction;
    }

    public Long getApprovalPosition() {
        return approvalPosition;
    }

    public void setApprovalPosition(Long approvalPosition) {
        this.approvalPosition = approvalPosition;
    }

    public String getApprovalComment() {
        return approvalComment;
    }

    public void setApprovalComment(String approvalComment) {
        this.approvalComment = approvalComment;
    }

    public String getApprovalDesignation() {
        return approvalDesignation;
    }

    public void setApprovalDesignation(String approvalDesignation) {
        this.approvalDesignation = approvalDesignation;
    }

    public EgBillregister getEgBillregister() {
        return egBillregister;
    }

    public void setEgBillregister(EgBillregister egBillregister) {
        this.egBillregister = egBillregister;
    }
}