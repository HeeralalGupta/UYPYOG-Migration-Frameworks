package org.egov.finance.migration.common.enums;

public enum MigrationStatus {

    READY,
    VALIDATING,
    READING_EXCEL,
    CHECKING_DUPLICATES,
    PREPARING_REQUEST,
    CALLING_API,
    COMPLETED
}
