package uk.gov.di.ipv.cri.kbv.api.service.fixtures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionOptions;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public interface TestFixtures {
    String EC_PRIVATE_KEY_1 =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";
    String EC_PUBLIC_KEY_1 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEE9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIMqVMxm2EdlSRjPkCV5NDyN9/RMmJLerY4H0vkXDjEDTg==";
    String EC_PUBLIC_JWK_1 =
            "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}";

    String KBV_QUESTION_QUALITY_MAPPING =
            "{\"First\":0,\"Second\": 0, \"Third\": 0, \"Fourth\": 0}";

    Map<String, Integer> KBV_QUESTION_QUALITY_MAPPING_SERIALIZED =
            Map.of(
                    "First", 0,
                    "Second", 0,
                    "Third", 0,
                    "Fourth", 0);

    default ECPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return (ECPrivateKey)
                KeyFactory.getInstance("EC")
                        .generatePrivate(
                                new PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(EC_PRIVATE_KEY_1)));
    }

    default KbvQuestionAnswerSummary getKbvQuestionAnswerSummary(
            int asked, int answeredCorrect, int answeredIncorrect) {
        KbvQuestionAnswerSummary kbvQuestionAnswerSummary = new KbvQuestionAnswerSummary();
        kbvQuestionAnswerSummary.setAnsweredCorrect(answeredCorrect);
        kbvQuestionAnswerSummary.setAnsweredIncorrect(answeredIncorrect);
        kbvQuestionAnswerSummary.setQuestionsAsked(asked);
        return kbvQuestionAnswerSummary;
    }

    default void setKbvItemQuestionState(KBVItem kbvItem) throws JsonProcessingException {
        setKbvItemQuestionState(kbvItem, "First");
    }

    default void setKbvItemQuestionState(KBVItem kbvItem, String... questions)
            throws JsonProcessingException {
        List<KbvQuestion> kbvQuestions = getKbvQuestions(questions);
        List<QuestionAnswer> questionAnswers = getQuestionAnswers(questions);

        QuestionState questionStateWithAnswer =
                getQuestionStateWithAnswer(kbvQuestions, questionAnswers);

        kbvItem.setQuestionState(new ObjectMapper().writeValueAsString(questionStateWithAnswer));
    }

    default List<QuestionAnswer> getQuestionAnswers(String... questions) {
        var questionAnswers =
                Arrays.stream(questions)
                        .map(
                                questionId -> {
                                    QuestionAnswer questionAnswer = new QuestionAnswer();
                                    questionAnswer.setQuestionId(questionId);
                                    return questionAnswer;
                                })
                        .collect(Collectors.toList());
        return questionAnswers;
    }

    default List<KbvQuestion> getKbvQuestions(String... questions) {
        var kbvQuestions =
                Arrays.stream(questions).map(this::getQuestion).collect(Collectors.toList());
        return kbvQuestions;
    }

    default QuestionState getQuestionStateWithAnswer(
            List<KbvQuestion> kbvQuestions, List<QuestionAnswer> questionAnswers) {
        return getQuestionStateWithAnswer(
                kbvQuestions.toArray(KbvQuestion[]::new),
                questionAnswers.toArray(QuestionAnswer[]::new));
    }

    default QuestionState getQuestionStateWithAnswer(
            KbvQuestion[] kbvQuestions, QuestionAnswer[] questionAnswers) {
        QuestionState questionState = createKbvQuestionStateWithQAPairs(kbvQuestions);

        return getQuestionStateWithAnswer(questionState, kbvQuestions, questionAnswers);
    }

    default QuestionState loadKbvQuestionStateWithAnswers(
            QuestionState questionState,
            List<KbvQuestion> kbvQuestions,
            List<QuestionAnswer> questionAnswers) {
        return getQuestionStateWithAnswer(
                questionState,
                kbvQuestions.toArray(KbvQuestion[]::new),
                questionAnswers.toArray(QuestionAnswer[]::new));
    }

    default QuestionState getQuestionStateWithAnswer(
            QuestionState questionState,
            KbvQuestion[] kbvQuestions,
            QuestionAnswer[] questionAnswers) {

        for (KbvQuestion question : kbvQuestions) {
            Optional<QuestionAnswer> questionAnswer =
                    Arrays.stream(questionAnswers)
                            .filter(qa -> qa.getQuestionId().equals(question.getQuestionId()))
                            .findFirst();
            questionAnswer.ifPresent(
                    ans -> {
                        ans.setQuestionId(question.getQuestionId());
                        ans.setAnswer(String.format("%s Answer", question.getQuestionId()));
                        questionState.setAnswer(ans);
                    });
        }
        return questionState;
    }

    default QuestionState createKbvQuestionStateWithQAPairs(List<KbvQuestion> kbvQuestions) {
        return createKbvQuestionStateWithQAPairs(kbvQuestions.toArray(KbvQuestion[]::new));
    }

    default QuestionState createKbvQuestionStateWithQAPairs(
            QuestionState questionState, KbvQuestion[] kbvQuestions) {
        questionState.setQAPairs(kbvQuestions);
        return questionState;
    }

    default QuestionState createKbvQuestionStateWithQAPairs(KbvQuestion[] kbvQuestions) {
        QuestionState questionState = new QuestionState();
        questionState.setQAPairs(kbvQuestions);
        return questionState;
    }

    default KbvQuestion getQuestion(String questionId) {
        KbvQuestionOptions questionOptions = new KbvQuestionOptions();
        questionOptions.setIdentifier(questionId);
        questionOptions.setFieldType("G");

        KbvQuestion question = new KbvQuestion();
        question.setQuestionId(questionId);
        question.setQuestionOptions(questionOptions);

        return question;
    }

    default KBVItem getKbvItem() {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        return kbvItem;
    }

    default KbvResult getKbvResult(String transactionValue) {
        KbvResult kbvResult = new KbvResult();
        kbvResult.setNextTransId(new String[] {transactionValue});
        return kbvResult;
    }
}
