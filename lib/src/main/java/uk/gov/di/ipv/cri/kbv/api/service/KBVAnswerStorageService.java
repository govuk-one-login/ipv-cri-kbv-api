package uk.gov.di.ipv.cri.kbv.api.service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import uk.gov.di.ipv.cri.common.library.persistence.DynamoDbEnhancedClientFactory;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVAnswerItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.util.KBVAnswerStorageMapper;

import java.util.List;
import java.util.stream.Collectors;

public class KBVAnswerStorageService {
    private final DynamoDbEnhancedClient enhancedClient;
    private final KBVAnswerStorageMapper kbvAnswerStorageMapper;
    private final String tableName;

    public KBVAnswerStorageService(
            DynamoDbEnhancedClient enhancedClient,
            KBVAnswerStorageMapper kbvAnswerStorageMapper,
            String tableName) {
        this.enhancedClient = enhancedClient;
        this.kbvAnswerStorageMapper = kbvAnswerStorageMapper;
        this.tableName = tableName;
    }

    public KBVAnswerStorageService(ConfigurationService configurationService) {
        this(
                new DynamoDbEnhancedClientFactory().getClient(),
                new KBVAnswerStorageMapper(),
                configurationService.getParameterValue("KBVAnswerTableName"));
    }

    public void save(QuestionsResponse questionsResponse) {
        DynamoDbTable<KBVAnswerItem> kbvAnswerTable =
                enhancedClient.table(tableName, TableSchema.fromBean(KBVAnswerItem.class));

        List<WriteBatch> writeBatchList =
                kbvAnswerStorageMapper.mapToKBVAnswerItems(questionsResponse).stream()
                        .map(
                                kbvAnswerItem ->
                                        WriteBatch.builder(KBVAnswerItem.class)
                                                .mappedTableResource(kbvAnswerTable)
                                                .addPutItem(r -> r.item(kbvAnswerItem))
                                                .build())
                        .collect(Collectors.toList());

        BatchWriteItemEnhancedRequest batchWriteItemEnhancedRequest =
                BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatchList).build();

        enhancedClient.batchWriteItem(batchWriteItemEnhancedRequest);
    }
}
