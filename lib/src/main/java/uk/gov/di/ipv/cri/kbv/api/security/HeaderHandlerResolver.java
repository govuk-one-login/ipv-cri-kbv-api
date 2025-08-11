package uk.gov.di.ipv.cri.kbv.api.security;

import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.HandlerResolver;
import jakarta.xml.ws.handler.PortInfo;

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
