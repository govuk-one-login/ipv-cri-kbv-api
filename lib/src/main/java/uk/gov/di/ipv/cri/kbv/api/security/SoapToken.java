package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import com.experian.uk.wasp.TokenServiceSoap;
import io.opentelemetry.api.trace.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;
import uk.gov.di.ipv.cri.kbv.api.util.OpenTelemetryUtil;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

public class SoapToken {
    private static final Logger LOGGER = LogManager.getLogger();
    private final TokenService tokenService;
    private final String application;
    private final boolean checkIp;
    private final ConfigurationService configurationService;
    private final MetricsService metricsService = new MetricsService(new EventProbe());

    public SoapToken(
            String application,
            boolean checkIp,
            TokenService tokenService,
            ConfigurationService configurationService) {
        this.application = application;
        this.checkIp = checkIp;
        this.tokenService = tokenService;
        this.configurationService = configurationService;
    }

    public String getToken() {
        long startTime = System.nanoTime();

        Span span =
                OpenTelemetryUtil.createSpan(
                        this.getClass(),
                        "getToken",
                        "Experian getToken",
                        "_UNKNOWN",
                        "http://www.uk.experian.com/WASP/LoginWithCertificate");

        try {
            TokenServiceSoap tokenServiceSoap = tokenService.getTokenServiceSoap();

            BindingProvider bindingProvider = (BindingProvider) tokenServiceSoap;
            bindingProvider
                    .getRequestContext()
                    .put(
                            BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                            configurationService.getSecretValue("experian/iiq-wasp-service"));

            String token = tokenServiceSoap.loginWithCertificate(application, checkIp);

            OpenTelemetryUtil.endSpan(span);

            long totalTimeInMs = (System.nanoTime() - startTime) / 1000000;

            LOGGER.info("SoapToken#getToken latency is {}ms", totalTimeInMs);

            metricsService
                    .getEventProbe()
                    .counterMetric("get_soap_token_duration", totalTimeInMs, Unit.MILLISECONDS);

            return token;
        } catch (SOAPFaultException e) {
            OpenTelemetryUtil.endSpanWithError(span);
            throw new InvalidSoapTokenException("SOAP Fault occurred: " + e.getMessage());
        } catch (WebServiceException e) {
            OpenTelemetryUtil.endSpanWithError(span);
            throw new InvalidSoapTokenException("Web Service error occurred: " + e.getMessage());
        } catch (Exception e) {
            OpenTelemetryUtil.endSpanWithError(span);
            throw new InvalidSoapTokenException("Unexpected error occurred: " + e.getMessage());
        }
    }
}
