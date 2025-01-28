package uk.gov.di.ipv.cri.kbv.api.service;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import java.io.IOException;

public class OtelHttpConduitFactory implements HTTPConduitFactory {
    private final OtelHttpClientWrapper otelHttpClientWrapper;

    public OtelHttpConduitFactory() {
        this.otelHttpClientWrapper = new OtelHttpClientWrapper();
    }

    @Override
    public HTTPConduit createConduit(
            HTTPTransportFactory factory,
            Bus bus,
            EndpointInfo endpointInfo,
            EndpointReferenceType target)
            throws IOException {
        return new OtelHttpConduit(bus, endpointInfo, target, otelHttpClientWrapper);
    }
}
