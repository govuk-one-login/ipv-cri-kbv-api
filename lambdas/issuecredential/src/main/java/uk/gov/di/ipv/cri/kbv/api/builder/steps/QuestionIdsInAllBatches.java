package uk.gov.di.ipv.cri.kbv.api.builder.steps;

import java.util.List;

public interface QuestionIdsInAllBatches {
    CreateCheckDetailsWithKbvQuality createCheckDetailsWithKbvQuality();

    List<String> buildToList();
}
