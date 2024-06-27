package uk.gov.di.ipv.cri.kbv.api.strategy;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class KbvStrategyParserTest {
    @ParameterizedTest
    @CsvSource({
        "3 out of 4 Prioritised, 3,4",
        "3 out of 4, 3,4",
        "2 out of 3 Prioritised, 2,3",
        "2 out of 3, 2,3",
        "3 of 4, 3,4",
        "2 of 3, 2,3",
        "3 4, 3,4",
        "2 3, 2,3"
    })
    void shouldReturnMinAndMaxNumberOfQuestionsNeededToPassStrategy(
            String strategy, int minimum, int maximum) {
        KbvStrategyParser strategyParser = new KbvStrategyParser(strategy);

        Strategy result = strategyParser.parse();
        assertAll(
                () -> assertEquals(minimum, result.min()),
                () -> assertEquals(maximum, result.max()));
    }

    @ParameterizedTest
    @CsvSource({
        "4 out of 3 Prioritised,3,4",
        "3 out of 2 Prioritised,2,3",
    })
    void throwsAnErrorWhenStrategyInputHasFirstNumberGreaterThanSecond(
            String strategy, int minimum, int maximum) {
        KbvStrategyParser strategyParser = new KbvStrategyParser(strategy);
        String errorMessage = "First number %d must be less than second number %d: %s";

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> strategyParser.parse(),
                        String.format(errorMessage, maximum, minimum, strategy));

        assertEquals(
                String.format(errorMessage, maximum, minimum, strategy), exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "out of 3 Prioritised",
        "out of 2 Prioritised",
        "Prioritised",
    })
    void throwsAnErrorWhenStrategyInputIsInValid(String strategy) {
        KbvStrategyParser strategyParser = new KbvStrategyParser(strategy);
        String errorMessage = "Invalid input string: %s";

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> strategyParser.parse(),
                        String.format(errorMessage, strategy));

        assertEquals(String.format(errorMessage, strategy), exception.getMessage());
    }
}
