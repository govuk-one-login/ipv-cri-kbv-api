package uk.gov.di.ipv.cri.kbv.api.util;

import uk.gov.di.ipv.cri.kbv.api.domain.KBVAnswerItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KBVAnswerStorageMapper {
    public List<KBVAnswerItem> mapToKBVAnswerItems(QuestionsResponse questionsResponse) {
        return Arrays.stream(questionsResponse.getQuestions())
                .filter(Objects::nonNull)
                .filter(this::containsAListOfAnswers)
                .flatMap(this::getKbvAnswerItemStream)
                .collect(Collectors.toList());
    }

    private boolean containsAListOfAnswers(KbvQuestion kbvQuestion) {
        return Objects.nonNull(kbvQuestion.getQuestionOptions())
                && Objects.nonNull(kbvQuestion.getQuestionOptions().getOptions())
                && !kbvQuestion.getQuestionOptions().getOptions().isEmpty();
    }

    private Stream<KBVAnswerItem> getKbvAnswerItemStream(KbvQuestion kbvQuestion) {
        return kbvQuestion.getQuestionOptions().getOptions().stream()
                .map(
                        answer -> {
                            KBVAnswerItem kbvAnswerItem = new KBVAnswerItem();
                            kbvAnswerItem.setQuestionId(kbvQuestion.getQuestionId());
                            kbvAnswerItem.setIdentifier(
                                    kbvQuestion.getQuestionOptions().getIdentifier());
                            kbvAnswerItem.setAnswer(answer);
                            return kbvAnswerItem;
                        });
    }
}
