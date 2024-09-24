package uk.gov.di.ipv.cri.kbv.api.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import static java.lang.System.out;

@ExcludeFromGeneratedCoverageReport
public class SOAPMessageWriterHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Logger LOGGER = LogManager.getLogger();

    public boolean handleMessage(SOAPMessageContext smc) {
        try {
            return logToSystemOut(smc);
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Set getHeaders() {
        return null;
    }

    public boolean handleFault(SOAPMessageContext smc) {
        try {
            return logToSystemOut(smc);
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close(MessageContext context) {}

    /*
     * Check the MESSAGE_OUTBOUND_PROPERTY in the context
     * to see if this is an outgoing or incoming message.
     * Write a brief message to the print stream and
     * output the message. The writeTo() method can throw
     * SOAPException or IOException
     */
    private Boolean logToSystemOut(SOAPMessageContext smc) throws SOAPException, IOException {
        Boolean outboundProperty = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        SOAPMessage message = smc.getMessage();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            message.writeTo(outputStream);
            String messageContent = outputStream.toString("UTF-8");

            // Check if it's an outbound message and log it
            if (outboundProperty.booleanValue() && LOGGER.isDebugEnabled()) {
                LOGGER.info("Outbound message:\n{}", messageContent);
                // You can write the message to the 'out' stream without logging to console
                message.writeTo(out);
            }
            // If it's an inbound message, log at the DEBUG level
            else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Inbound message:\n{}", messageContent);
                // Write the inbound message to 'out' if needed
                message.writeTo(out);
            }

            // Write the message content to the output stream (for final output processing)
            message.writeTo(out);

        } catch (Exception e) {
            // Use the Logger to log the exception, no out.println required
            LOGGER.error("Exception in message handling", e);
        }
        return outboundProperty;
    }
}
