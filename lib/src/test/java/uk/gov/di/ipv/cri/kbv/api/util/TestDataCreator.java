package uk.gov.di.ipv.cri.kbv.api.util;

import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.AddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvAlert;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionOptions;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

public class TestDataCreator {
    public static PersonIdentity createTestPersonIdentity(AddressType addressType) {
        PersonIdentity personIdentity = new PersonIdentity();
        personIdentity.setDateOfBirth(LocalDate.of(1976, 12, 26));
        Address address = new Address();
        address.setPostalCode("Postcode");
        address.setStreetName("Street name");
        address.setAddressLocality("PostTown");
        address.setValidFrom(LocalDate.now().minus(2, ChronoUnit.YEARS));
        personIdentity.setAddresses(List.of(address));
        return personIdentity;
    }

    public static PersonIdentity createTestPersonIdentityWithDuplicateAddresses(
            AddressType addressType) {
        PersonIdentity personIdentity = new PersonIdentity();
        personIdentity.setDateOfBirth(LocalDate.of(1976, 12, 26));

        Address address1 = new Address();
        address1.setPostalCode("Postcode 1");
        address1.setStreetName("Street name 1");
        address1.setAddressLocality("PostTown");
        address1.setValidFrom(LocalDate.now().minus(2, ChronoUnit.YEARS));

        Address address2 = new Address();
        address2.setPostalCode("Postcode 2");
        address2.setStreetName("Street name 2");
        address2.setAddressLocality("PostTown");
        address2.setValidFrom(LocalDate.now().minus(1, ChronoUnit.YEARS));

        Address address3 = new Address();
        address3.setPostalCode("Postcode 1");
        address3.setStreetName("Street name 1");
        address3.setAddressLocality("PostTown");

        Address address4 = new Address();
        address4.setPostalCode("Postcode 2");
        address4.setStreetName("Street name 2");
        address4.setAddressLocality("PostTown");

        personIdentity.setAddresses(List.of(address1, address2, address3, address4));
        return personIdentity;
    }

    public static PersonIdentity createTestPersonIdentity() {
        return createTestPersonIdentity(AddressType.CURRENT);
    }

    public static QuestionAnswerRequest createTestQuestionAnswerRequest() {
        QuestionAnswerRequest answerRequest = new QuestionAnswerRequest();
        answerRequest.setUrn("urn");
        answerRequest.setAuthRefNo("auth-ref-no");
        answerRequest.setQuestionAnswers(List.of(new QuestionAnswer(), new QuestionAnswer()));
        return answerRequest;
    }

    public static QuestionRequest createTestQuestionAnswerRequest(AddressType addressType) {
        QuestionRequest questionRequest = new QuestionRequest();
        questionRequest.setUrn("urn");
        questionRequest.setStrategy("1 out of 2");
        questionRequest.setPersonIdentity(createTestPersonIdentity(addressType));
        return questionRequest;
    }

    public static QuestionRequest createTestQuestionAnswerRequestWithDuplicateAddresses(
            AddressType addressType) {
        QuestionRequest questionRequest = new QuestionRequest();
        questionRequest.setUrn("urn");
        questionRequest.setStrategy("1 out of 2");
        questionRequest.setPersonIdentity(
                createTestPersonIdentityWithDuplicateAddresses(addressType));
        return questionRequest;
    }

    public static QuestionsResponse getExperianQuestionResponse(KbvQuestion[] kbvQuestions) {
        QuestionsResponse questionsResponse = new QuestionsResponse();
        questionsResponse.setAuthReference("authrefno");
        questionsResponse.setUniqueReference("urn");
        questionsResponse.setQuestions(kbvQuestions);

        return questionsResponse;
    }

    public static QuestionsResponse getExperianQuestionResponseWithQuestions() {
        KbvQuestion[] questions = new KbvQuestion[] {getQuestionOne(), getQuestionTwo()};
        QuestionsResponse questionsResponse = getExperianQuestionResponse(questions);

        KbvResult kbvResult = new KbvResult();
        kbvResult.setOutcome("Authentication Questions returned");
        kbvResult.setNextTransId(new String[] {"RTQ"});
        questionsResponse.setResults(kbvResult);
        return questionsResponse;
    }

    public static QuestionsResponse getExperianQuestionResponseWithAlert() {
        QuestionsResponse questionsResponse = getExperianQuestionResponseWithQuestions();
        KbvResult kbvResult = questionsResponse.getResults();

        KbvAlert alert = new KbvAlert();
        alert.setCode("U501");
        alert.setText("Applicant has previously requested authentication");

        kbvResult.setAlerts(Collections.singletonList(alert));
        questionsResponse.setResults(kbvResult);

        return questionsResponse;
    }

    public static KbvQuestion getQuestionOne() {
        KbvQuestionOptions questionOptions = new KbvQuestionOptions();
        questionOptions.setIdentifier("A00004");
        questionOptions.setFieldType("G");
        questionOptions.setOptions(
                List.of(
                        "UP TO £10,000",
                        "OVER £35,000 UP TO £60,000",
                        "OVER £60,000 UP TO £85,000",
                        "NONE OF THE ABOVE / DOES NOT APPLY"));

        KbvQuestion question = new KbvQuestion();
        question.setQuestionId("Q00015");
        question.setText("What is the outstanding balance ");
        question.setTooltip("outstanding balance tooltip");
        question.setQuestionOptions(questionOptions);

        return question;
    }

    public static KbvQuestion getQuestionTwo() {
        KbvQuestionOptions questionOptions = new KbvQuestionOptions();
        questionOptions.setIdentifier("A00005");
        questionOptions.setFieldType("G");
        questionOptions.setOptions(
                List.of("Blue", "Red", "Green", "NONE OF THE ABOVE / DOES NOT APPLY"));

        KbvQuestion question = new KbvQuestion();
        question.setQuestionId("Q00040");
        question.setText("What your favorite color");
        question.setTooltip("favorite color tooltip");
        question.setQuestionOptions(questionOptions);

        return question;
    }

    public static KbvQuestion getQuestion(String questionId) {
        KbvQuestionOptions questionOptions = new KbvQuestionOptions();
        questionOptions.setIdentifier(questionId);
        questionOptions.setFieldType("G");

        KbvQuestion question = new KbvQuestion();
        question.setQuestionId(questionId);
        question.setQuestionOptions(questionOptions);

        return question;
    }

    public static QuestionState getQuestionState(KbvQuestion[] kbvQuestions) {
        QuestionState questionState = new QuestionState();
        questionState.setQAPairs(kbvQuestions);
        return questionState;
    }

    public static KbvQuestionAnswerSummary getKbvQuestionAnswerSummary(
            int answeredCorrect, int answeredIncorrect, int totalQuestionsAsked) {
        KbvQuestionAnswerSummary kbvQuestionAnswerSummary = new KbvQuestionAnswerSummary();
        kbvQuestionAnswerSummary.setAnsweredCorrect(answeredCorrect);
        kbvQuestionAnswerSummary.setAnsweredIncorrect(answeredIncorrect);
        kbvQuestionAnswerSummary.setQuestionsAsked(totalQuestionsAsked);
        return kbvQuestionAnswerSummary;
    }

    public static QuestionsResponse getQuestionResponseWithResults(
            String authenticationResult, KbvQuestionAnswerSummary kbvQuestionAnswerSummary) {
        QuestionsResponse questionsResponse = new QuestionsResponse();
        KbvResult kbvResult = new KbvResult();
        kbvResult.setNextTransId(new String[] {"END"});
        kbvResult.setAuthenticationResult(authenticationResult);
        kbvResult.setAnswerSummary(kbvQuestionAnswerSummary);
        questionsResponse.setResults(kbvResult);
        return questionsResponse;
    }
}
