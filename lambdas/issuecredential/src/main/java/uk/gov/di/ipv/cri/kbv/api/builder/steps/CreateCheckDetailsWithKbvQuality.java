package uk.gov.di.ipv.cri.kbv.api.builder.steps;

import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;

public interface CreateCheckDetailsWithKbvQuality extends BuildTo {
    SortByKbvQuality sortByKbvQualityFromLowestToHighest();

    BuildTo filterByNumberOfCorrectQuestions();

    CheckDetail[] buildToArray();
}
