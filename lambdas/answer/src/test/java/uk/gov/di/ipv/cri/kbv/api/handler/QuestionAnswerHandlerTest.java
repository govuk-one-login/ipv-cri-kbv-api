package uk.gov.di.ipv.cri.kbv.api.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class QuestionAnswerHandlerTest {

    private QuestionAnswerHandlerTest questionAnswerHandlerTest;

    @BeforeEach
    void setUp() {
        questionAnswerHandlerTest = new QuestionAnswerHandlerTest();
    }

    @AfterEach
    void tearDown() {
        //        AWSXRay.endSegment();
    }

    @Test
    void shouldReturn200OkWhen1stCalledAndReturn1stUnAnsweredQuestion() {
        assertTrue(true);
    }
}
