package gov.uk.kbv.api.util;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Map;

public class DynamoDbHelper {

    private static final DynamoDbClient DYNAMO_DB_CLIENT = DynamoDbClient.builder().build();

    public static boolean sessionExists(String tableName, String sessionId) {
        QueryRequest request =
                QueryRequest.builder()
                        .tableName(tableName)
                        .keyConditionExpression("sessionId = :sessionId")
                        .expressionAttributeValues(
                                Map.of(":sessionId", AttributeValue.builder().s(sessionId).build()))
                        .limit(1)
                        .build();

        QueryResponse response = DYNAMO_DB_CLIENT.query(request);

        return response.count() > 0;
    }
}
