package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidStrategyScoreException;
import uk.gov.di.ipv.cri.kbv.api.util.EvidenceUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.service.KbvConfigService.IIQ_STRATEGY_PARAM_NAME;

@ExtendWith(MockitoExtension.class)
class KbvConfigServiceTest {
    private static final String MOCK_IIQ_STRATEGY_PARAM_VALUE =
            "{\"2\": \"3 out of 4 prioritised\"}";
    private static final Map<String, String> MOCK_IIQ_STRATEGY_MAPPED_VALUE =
            Map.of("2", "3 out of 4 prioritised");
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private ObjectMapper mockObjectMapper;
    @InjectMocks KbvConfigService kbvConfigService;

    @Test
    void shouldReturnUnderlyingCommonConfiguration() {
        assertNotNull(kbvConfigService.configService());
    }

    @Test
    void shouldReturnConfiguredKbvStrategyWhenVerificationScoreIsNotSetOnSessionItem()
            throws JsonProcessingException {
        SessionItem sessionItem = new SessionItem();

        when(mockConfigurationService.getParameterValue(IIQ_STRATEGY_PARAM_NAME))
                .thenReturn(MOCK_IIQ_STRATEGY_PARAM_VALUE);
        when(mockObjectMapper.readValue(
                        anyString(), Mockito.<TypeReference<Map<String, String>>>any()))
                .thenReturn(MOCK_IIQ_STRATEGY_MAPPED_VALUE);

        var strategy =
                kbvConfigService.getKbvQuestionStrategy(
                        EvidenceUtils.getVerificationScoreForPass(
                                sessionItem.getEvidenceRequest()));

        assertEquals("3 out of 4 prioritised", strategy);
    }

    @Test
    void shouldReturnConfiguredKbvStrategyWhenVerificationScoreIsExplicitlySetOnSessionItem()
            throws JsonProcessingException {
        SessionItem sessionItem = new SessionItem();
        EvidenceRequest evidenceRequest = new EvidenceRequest();
        evidenceRequest.setVerificationScore(2);
        sessionItem.setEvidenceRequest(evidenceRequest);

        when(mockConfigurationService.getParameterValue(IIQ_STRATEGY_PARAM_NAME))
                .thenReturn(MOCK_IIQ_STRATEGY_PARAM_VALUE);
        when(mockObjectMapper.readValue(
                        anyString(), Mockito.<TypeReference<Map<String, String>>>any()))
                .thenReturn(MOCK_IIQ_STRATEGY_MAPPED_VALUE);

        var strategy =
                kbvConfigService.getKbvQuestionStrategy(
                        EvidenceUtils.getVerificationScoreForPass(
                                sessionItem.getEvidenceRequest()));

        assertEquals("3 out of 4 prioritised", strategy);
    }

    @Test
    void throwsErrorWhenKbvStrategyIsNotConfiguredForGivenVerificationScoreSetOnSessionItem()
            throws JsonProcessingException {
        SessionItem sessionItem = new SessionItem();
        EvidenceRequest evidenceRequest = new EvidenceRequest();
        evidenceRequest.setVerificationScore(1);
        sessionItem.setEvidenceRequest(evidenceRequest);

        when(mockConfigurationService.getParameterValue(IIQ_STRATEGY_PARAM_NAME))
                .thenReturn(MOCK_IIQ_STRATEGY_PARAM_VALUE);
        when(mockObjectMapper.readValue(
                        anyString(), Mockito.<TypeReference<Map<String, String>>>any()))
                .thenReturn(MOCK_IIQ_STRATEGY_MAPPED_VALUE);

        int verificationScore =
                EvidenceUtils.getVerificationScoreForPass(sessionItem.getEvidenceRequest());

        InvalidStrategyScoreException expectedException =
                assertThrows(
                        InvalidStrategyScoreException.class,
                        () -> kbvConfigService.getKbvQuestionStrategy(verificationScore),
                        "No question strategy found for score provided");
        assertEquals(
                "No question strategy found for score provided", expectedException.getMessage());
    }
}
