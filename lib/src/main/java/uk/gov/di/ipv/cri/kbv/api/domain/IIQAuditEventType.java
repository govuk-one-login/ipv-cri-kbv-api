package uk.gov.di.ipv.cri.kbv.api.domain;

import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

@ExcludeFromGeneratedCoverageReport
public enum IIQAuditEventType {
    EXPERIAN_IIQ_STARTED, // Generated once after the very first successful request i.e
    // (IPV_KBV_CRI_REQUEST_SENT) to Experian for KBV (Knowledge Based
    // Verification) CRI. Signals an event that can be used for billing
    ABANDONED, // An event for when a user explicit abandons an Experian KBV journey.
    THIN_FILE_ENCOUNTERED // A Thin File event (Insufficient questions); occurs when a user has 2 or
    // less
    // s questions
}
