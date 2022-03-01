package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {
    @Mock private DataStore<KBVSessionItem> mockDataStore;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(mockDataStore);
    }

    @Test
    void shouldCreateKBVSessionItem() {
        ArgumentCaptor<KBVSessionItem> kbvSessionItemArgumentCaptor =
                ArgumentCaptor.forClass(KBVSessionItem.class);

        storageService.save("uuid4-identifier", "person-identity", "question-state");

        verify(mockDataStore).create(kbvSessionItemArgumentCaptor.capture());
        assertNotNull(kbvSessionItemArgumentCaptor.getValue().getQuestionState());
        assertNotNull(kbvSessionItemArgumentCaptor.getValue().getSessionId());
    }

    @Test
    void shouldUpdateKBVSessionItem() {
        ArgumentCaptor<KBVSessionItem> kbvSessionItemArgumentCaptor =
                ArgumentCaptor.forClass(KBVSessionItem.class);
        KBVSessionItem item = mock(KBVSessionItem.class);
        storageService.update(item);
        verify(mockDataStore).update(kbvSessionItemArgumentCaptor.capture());
    }
}
