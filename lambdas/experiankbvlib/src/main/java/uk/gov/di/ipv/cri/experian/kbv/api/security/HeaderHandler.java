package uk.gov.di.ipv.cri.experian.kbv.api.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HeaderHandler implements SOAPHandler<SOAPMessageContext> {
    private final LoadingCache<String, Base64TokenEncoder> cache;

    public HeaderHandler(Base64TokenCacheLoader tokenEncoder) {
        this.cache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.HOURS).build(tokenEncoder);
    }

    public boolean handleMessage(SOAPMessageContext smc) {

        Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outbound) {

            SOAPMessage soapMessage = smc.getMessage();

            try {

                SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();

                SOAPHeader currentHeader = envelope.getHeader();
                if (currentHeader != null) {
                    currentHeader.detachNode();
                }
                SOAPHeader header = envelope.addHeader();

                Name securityName =
                        envelope.createName(
                                "Security",
                                "wsse",
                                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
                SOAPHeaderElement security = header.addHeaderElement(securityName);

                SOAPElement binarySecurityToken =
                        security.addChildElement("BinarySecurityToken", "wsse");
                binarySecurityToken.addAttribute(
                        new QName("xmlns:wsu"),
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
                binarySecurityToken.addAttribute(new QName("EncodingType"), "wsse:Base64Binary");
                binarySecurityToken.addAttribute(new QName("ValueType"), "ExperianWASP");
                Base64TokenEncoder tokenEncoder = cache.get("experian-soap-token");
                binarySecurityToken.setValue(tokenEncoder.getToken());
                soapMessage.saveChanges();

            } catch (SOAPException e) {
                throw new RuntimeException("Error in HeaderHandler", e);
            }
        }

        return true;
    }

    public Set getHeaders() {
        return Collections.emptySet();
    }

    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    public void close(MessageContext context) {}
}
