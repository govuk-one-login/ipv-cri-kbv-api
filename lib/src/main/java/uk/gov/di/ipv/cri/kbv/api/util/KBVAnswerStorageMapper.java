package uk.gov.di.ipv.cri.kbv.api.util;

import uk.gov.di.ipv.cri.kbv.api.domain.KBVAnswerItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class KBVAnswerStorageMapper {
    public List<KBVAnswerItem> mapToKBVAnswerItems(QuestionsResponse questionsResponse) {
        List<KBVAnswerItem> kbvAnswerItems = new ArrayList<>();
        for (KbvQuestion kbvQuestion : questionsResponse.getQuestions()) {
            kbvAnswerItems.addAll(getAnswersForQuestion(kbvQuestion));
        }
        return kbvAnswerItems;
    }

    private List<KBVAnswerItem> getAnswersForQuestion(KbvQuestion kbvQuestion) {
        if (Objects.nonNull(kbvQuestion.getQuestionOptions())
                && Objects.nonNull(kbvQuestion.getQuestionOptions().getOptions())) {
            return kbvQuestion.getQuestionOptions().getOptions().stream()
                    .map(
                            answer -> {
                                KBVAnswerItem kbvAnswerItem = new KBVAnswerItem();
                                kbvAnswerItem.setQuestionId(kbvQuestion.getQuestionId());
                                kbvAnswerItem.setAnswer(answer);
                                return kbvAnswerItem;
                            })
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
