package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidStrategyScoreException;

import java.util.Map;

import static java.util.Optional.ofNullable;

public class KbvConfigService {
    private static final Logger LOGGER = LogManager.getLogger(KbvConfigService.class);
    public static final String IIQ_STRATEGY_PARAM_NAME = "IIQStrategy";
    private final ConfigurationService configuration;
    private final ObjectMapper objectMapper;

    public KbvConfigService(ObjectMapper objectMapper, ConfigurationService configurationService) {
        this.configuration = configurationService;
        this.objectMapper = objectMapper;
    }

    public ConfigurationService configService() {
        return configuration;
    }

    public String getKbvQuestionStrategy(int verificationScore) throws JsonProcessingException {
        var strategyParam = this.configuration.getParameterValue(IIQ_STRATEGY_PARAM_NAME);

        Map<String, String> strategyMap =
                objectMapper.readValue(strategyParam, new TypeReference<>() {});
        String strategy =
                ofNullable(strategyMap.get(String.valueOf(verificationScore)))
                        .orElseThrow(InvalidStrategyScoreException::new);
        LOGGER.info("Using IIQStrategy: {}", strategy);
        return strategy;
    }
}
