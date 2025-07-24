package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import com.experian.uk.wasp.TokenServiceSoap;
import io.opentelemetry.api.trace.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;
import uk.gov.di.ipv.cri.kbv.api.util.OpenTelemetryUtil;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import java.util.Objects;

public class SoapToken {
    private static final Logger LOGGER = LogManager.getLogger();
    private final TokenService tokenService;
    private final String application;
    private final boolean checkIp;
    private final ConfigurationService configurationService;
    private final MetricsService metricsService;

    public SoapToken(
            String application,
            boolean checkIp,
            TokenService tokenService,
            ConfigurationService configurationService,
            MetricsService metricsService) {
        this.application = application;
        this.checkIp = checkIp;
        this.tokenService = tokenService;
        this.configurationService = Objects.requireNonNull(configurationService);
        this.metricsService = metricsService;
    }

    public String getToken(String clientId) {
        Span span =
                OpenTelemetryUtil.createSpan(
                        this.getClass(),
                        "getToken",
                        "LoginWithCertificate",
                        "http://www.uk.experian.com/WASP/LoginWithCertificate");

        long startTime = System.nanoTime();
        try {
            TokenServiceSoap tokenServiceSoap = tokenService.getTokenServiceSoap();

            LOGGER.info("Fetching SSM parameter experian/iiq-wasp-service/{}", clientId);

            String value =
                    configurationService.getParameterValue(
                            "experian/iiq-wasp-service/%s".formatted(clientId));

            LOGGER.info("Fetched value from SSM: {}", value);

            BindingProvider bindingProvider = (BindingProvider) tokenServiceSoap;
            bindingProvider
                    .getRequestContext()
                    .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, value);

            String token = tokenServiceSoap.loginWithCertificate(application, checkIp);

            long totalTimeInMs = (System.nanoTime() - startTime) / 1000000;

            OpenTelemetryUtil.endSpan(span);

            LOGGER.info("SoapToken#getToken latency is {}ms", totalTimeInMs);
            metricsService.sendDurationMetric("get_soap_token_duration", totalTimeInMs);

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
