package org.egov.finance.migration.modules.journalvoucher.response;

import lombok.Data;

@Data
public class ResponseInfo {

    private String apiId;
    private String ver;
    private String ts;
    private String resMsgId;
    private String msgId;
    private String status;
}
