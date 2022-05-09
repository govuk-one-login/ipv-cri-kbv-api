package uk.gov.di.ipv.cri.experian.kbv.api.security;

import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;

import java.util.Collections;
import java.util.List;

public class HeaderHandlerResolver implements HandlerResolver {

    private final HeaderHandler headerHandler;

    public HeaderHandlerResolver(HeaderHandler headerHandler) {
        this.headerHandler = headerHandler;
    }

    public List<Handler> getHandlerChain(PortInfo portInfo) {
        return Collections.singletonList(headerHandler);
    }
}
