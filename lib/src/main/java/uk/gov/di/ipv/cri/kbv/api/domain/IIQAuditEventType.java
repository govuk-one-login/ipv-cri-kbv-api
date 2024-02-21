package uk.gov.di.ipv.cri.kbv.api.domain;

public enum IIQAuditEventType {
    EXPERIAN_IIQ_STARTED, // Generated once after the very first successful request i.e
    // (IPV_KBV_CRI_REQUEST_SENT) to Experian for KBV (Knowledge Based
    // Verification) CRI. Signals an event that can be used for billing
    THIN_FILE_ENCOUNTERED
}
