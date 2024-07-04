package uk.gov.di.ipv.cri.kbv.api.builder.steps;

import java.util.List;

public interface UnSkippedQuestionIdsInAllBatches {
    CreateCheckDetailsWithKbvQuality createCheckDetailsWithKbvQuality();

    List<String> buildToList();
}
