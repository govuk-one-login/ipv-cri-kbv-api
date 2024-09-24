package uk.gov.di.ipv.cri.kbv.api.security;

import uk.gov.di.ipv.cri.kbv.api.util.SOAPMessageWriterHandler;

import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;

import java.util.List;

public class HeaderHandlerResolver implements HandlerResolver {
    private final HeaderHandler headerHandler;
    private SOAPMessageWriterHandler soapMessageWriterHandler;

    public HeaderHandlerResolver(
            HeaderHandler headerHandler, SOAPMessageWriterHandler soapMessageWriterHandler) {
        this.headerHandler = headerHandler;
        this.soapMessageWriterHandler = soapMessageWriterHandler;
    }

    public List<Handler> getHandlerChain(PortInfo portInfo) {
        return List.of(headerHandler, soapMessageWriterHandler);
    }
}
