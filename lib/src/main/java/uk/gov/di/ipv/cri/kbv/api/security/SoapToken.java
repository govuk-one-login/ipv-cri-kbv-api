package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import com.experian.uk.wasp.TokenServiceSoap;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

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

            LOGGER.info("SoapToken#getToken latency is {}ms", totalTimeInMs);
            metricsService.sendDurationMetric("get_soap_token_duration", totalTimeInMs);

            return token;
        } catch (SOAPFaultException e) {
            throw new InvalidSoapTokenException("SOAP Fault occurred: " + e.getMessage());
        } catch (WebServiceException e) {
            throw new InvalidSoapTokenException("Web Service error occurred: " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidSoapTokenException("Unexpected error occurred: " + e.getMessage());
        }
    }
}
