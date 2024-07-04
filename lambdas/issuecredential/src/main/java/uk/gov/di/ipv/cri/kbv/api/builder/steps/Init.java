package uk.gov.di.ipv.cri.kbv.api.builder.steps;

public interface Init {
    Skip1stQuestionIdIn2ndBatch skip1stQuestionIdIn2ndBatch();

    QuestionIdsInAllBatches getQuestionIdsInAllBatches();
}
