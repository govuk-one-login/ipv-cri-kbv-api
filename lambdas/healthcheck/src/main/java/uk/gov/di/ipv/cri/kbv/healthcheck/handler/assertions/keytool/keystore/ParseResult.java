package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.keytool.keystore;

import java.util.Map;

class ParseResult {
    Map<String, Object> certificate;
    int nextIndex;

    ParseResult(Map<String, Object> certificate, int nextIndex) {
        this.certificate = certificate;
        this.nextIndex = nextIndex;
    }
}
