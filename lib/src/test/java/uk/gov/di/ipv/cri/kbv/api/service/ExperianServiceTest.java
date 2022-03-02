package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.Control;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperianServiceTest {

    private ExperianService experianService;

    @BeforeEach
    void setUp() {
        experianService = new ExperianService();
    }

    @AfterEach
    void tearDown() {}

    @Test
    void shouldConvertQuestionStateToQuestionAnswerRequest() {
        Control controlMock = mock(Control.class);
        when(controlMock.getAuthRefNo()).thenReturn("some-auth-ref");
        when(controlMock.getURN()).thenReturn("some-urn");

        QuestionState questionStateMock = mock(QuestionState.class);
        when(questionStateMock.getControl()).thenReturn(controlMock);
        when(questionStateMock.getQaPairs())
                .thenReturn(
                        List.of(
                                new QuestionAnswerPair(new Question()),
                                new QuestionAnswerPair(new Question())));

        QuestionAnswerRequest questionAnswerRequestMock =
                experianService.prepareToSubmitAnswers(questionStateMock);
        assertEquals("some-urn", questionAnswerRequestMock.getUrn());
        assertEquals("some-auth-ref", questionAnswerRequestMock.getAuthRefNo());
        assertEquals(2, questionAnswerRequestMock.getQuestionAnswers().size());
    }

    @Test
    void shouldCreateExperianUri() {
        String url = "EXPERIAN_API_WRAPPER_RTQ_RESOURCE";
        URI uri = experianService.createExperianUri(url);
        System.out.println(uri.getPath());
        assertNotNull(uri);
    }
}
