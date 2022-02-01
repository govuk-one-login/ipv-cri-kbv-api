package uk.gov.di.ipv.cri.kbv.api.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataStoreTest {
    private static final String TEST_TABLE_NAME = "test-auth-code-table";

    @Mock private DynamoDbEnhancedClient mockDynamoDbEnhancedClient;
    @Mock private DynamoDbTable<KBVSessionItem> mockDynamoDbTable;
    @Mock private PageIterable<KBVSessionItem> mockPageIterable;

    private KBVSessionItem kbvSessionItem;
    private DataStore<KBVSessionItem> dataStore;

    @BeforeEach
    void setUp() {
        when(mockDynamoDbEnhancedClient.table(
                        anyString(), ArgumentMatchers.<TableSchema<KBVSessionItem>>any()))
                .thenReturn(mockDynamoDbTable);

        kbvSessionItem = new KBVSessionItem();
        kbvSessionItem.setAuthorizationCode("test-auth-code");
        kbvSessionItem.setSessionId("test-session-12345");

        dataStore =
                new DataStore<KBVSessionItem>(
                        TEST_TABLE_NAME, KBVSessionItem.class, mockDynamoDbEnhancedClient, false);
    }

    @Test
    void shouldPutItemIntoDynamoDbTable() {
        dataStore.create(kbvSessionItem);

        ArgumentCaptor<KBVSessionItem> KBVSessionItemArgumentCaptor =
                ArgumentCaptor.forClass(KBVSessionItem.class);

        verify(mockDynamoDbEnhancedClient)
                .table(eq(TEST_TABLE_NAME), ArgumentMatchers.<TableSchema<KBVSessionItem>>any());
        verify(mockDynamoDbTable).putItem(KBVSessionItemArgumentCaptor.capture());
        assertEquals(
                kbvSessionItem.getAuthorizationCode(),
                KBVSessionItemArgumentCaptor.getValue().getAuthorizationCode());
        assertEquals(
                kbvSessionItem.getSessionId(),
                KBVSessionItemArgumentCaptor.getValue().getSessionId());
    }

    @Test
    void shouldGetItemFromDynamoDbTableViaPartitionKeyAndSortKey() {
        dataStore.getItem("partition-key-12345", "sort-key-12345");

        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);

        verify(mockDynamoDbEnhancedClient)
                .table(eq(TEST_TABLE_NAME), ArgumentMatchers.<TableSchema<KBVSessionItem>>any());
        verify(mockDynamoDbTable).getItem(keyCaptor.capture());
        assertEquals("partition-key-12345", keyCaptor.getValue().partitionKeyValue().s());
        assertEquals("sort-key-12345", keyCaptor.getValue().sortKeyValue().get().s());
    }

    @Test
    void shouldGetItemFromDynamoDbTableViaPartitionKey() {
        dataStore.getItem("partition-key-12345");

        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);

        verify(mockDynamoDbEnhancedClient)
                .table(eq(TEST_TABLE_NAME), ArgumentMatchers.<TableSchema<KBVSessionItem>>any());
        verify(mockDynamoDbTable).getItem(keyCaptor.capture());
        assertEquals("partition-key-12345", keyCaptor.getValue().partitionKeyValue().s());
        assertTrue(keyCaptor.getValue().sortKeyValue().isEmpty());
    }

    @Test
    void shouldGetItemsFromDynamoDbTableViaPartitionKeyQueryRequest() {
        when(mockDynamoDbTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
        when(mockPageIterable.stream()).thenReturn(Stream.empty());

        dataStore.getItems("partition-key-12345");

        verify(mockDynamoDbEnhancedClient)
                .table(eq(TEST_TABLE_NAME), ArgumentMatchers.<TableSchema<KBVSessionItem>>any());
        verify(mockDynamoDbTable).query(any(QueryConditional.class));
    }

    @Test
    void shouldUpdateItemInDynamoDbTable() {
        dataStore.update(kbvSessionItem);

        ArgumentCaptor<KBVSessionItem> KBVSessionItemArgumentCaptor =
                ArgumentCaptor.forClass(KBVSessionItem.class);

        verify(mockDynamoDbEnhancedClient)
                .table(eq(TEST_TABLE_NAME), ArgumentMatchers.<TableSchema<KBVSessionItem>>any());
        verify(mockDynamoDbTable).updateItem(KBVSessionItemArgumentCaptor.capture());
        assertEquals(
                kbvSessionItem.getAuthorizationCode(),
                KBVSessionItemArgumentCaptor.getValue().getAuthorizationCode());
        assertEquals(
                kbvSessionItem.getSessionId(),
                KBVSessionItemArgumentCaptor.getValue().getSessionId());
    }

    @Test
    void shouldDeleteItemFromDynamoDbTableViaPartitionKeyAndSortKey() {
        dataStore.delete("partition-key-12345", "sort-key-12345");

        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);

        verify(mockDynamoDbEnhancedClient)
                .table(eq(TEST_TABLE_NAME), ArgumentMatchers.<TableSchema<KBVSessionItem>>any());
        verify(mockDynamoDbTable).deleteItem(keyCaptor.capture());
        assertEquals("partition-key-12345", keyCaptor.getValue().partitionKeyValue().s());
        assertEquals("sort-key-12345", keyCaptor.getValue().sortKeyValue().get().s());
    }

    @Test
    void shouldDeleteItemFromDynamoDbTableViaPartitionKey() {
        dataStore.delete("partition-key-12345");

        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);

        verify(mockDynamoDbEnhancedClient)
                .table(eq(TEST_TABLE_NAME), ArgumentMatchers.<TableSchema<KBVSessionItem>>any());
        verify(mockDynamoDbTable).deleteItem(keyCaptor.capture());
        assertEquals("partition-key-12345", keyCaptor.getValue().partitionKeyValue().s());
        assertTrue(keyCaptor.getValue().sortKeyValue().isEmpty());
    }
}
