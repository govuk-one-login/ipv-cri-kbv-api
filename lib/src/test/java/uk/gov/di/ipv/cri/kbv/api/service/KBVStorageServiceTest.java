package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KBVStorageServiceTest {
    private static final UUID SESSION_ID = UUID.randomUUID();
    @Mock private DataStore<KBVItem> dataStore;

    private KBVStorageService kbvStorageService;

    @BeforeEach
    void setUp() {
        kbvStorageService = new KBVStorageService(dataStore);
    }

    @Test
    void getSessionIdReturnsKBVItemWhenItExists() {
        KBVItem kbvItem = new KBVItem();
        when(dataStore.getItem(SESSION_ID.toString())).thenReturn(kbvItem);

        Optional<KBVItem> result = kbvStorageService.getSessionId(SESSION_ID.toString());

        assertTrue(result.isPresent());
        assertEquals(kbvItem, result.get());
        verify(dataStore, times(1)).getItem(SESSION_ID.toString());
    }

    @Test
    void getSessionIdReturnsEmptyOptionalKbvItemWhenNotExists() {
        when(dataStore.getItem(SESSION_ID.toString())).thenReturn(null);

        Optional<KBVItem> result = kbvStorageService.getSessionId(SESSION_ID.toString());

        assertFalse(result.isPresent());
        verify(dataStore, times(1)).getItem(SESSION_ID.toString());
    }

    @Test
    void getKBVItemReturnsKBVItem() {
        UUID sessionId = UUID.randomUUID();
        KBVItem kbvItem = new KBVItem();

        when(dataStore.getItem(sessionId.toString())).thenReturn(kbvItem);

        KBVItem result = kbvStorageService.getKBVItem(sessionId);

        assertEquals(kbvItem, result);
        verify(dataStore, times(1)).getItem(sessionId.toString());
    }

    @Test
    void updateCallsDataStoreUpdate() {
        KBVItem kbvItem = new KBVItem();
        kbvStorageService.update(kbvItem);

        verify(dataStore, times(1)).update(kbvItem);
    }

    @Test
    void saveCallsDataStoreCreate() {
        KBVItem kbvItem = new KBVItem();

        kbvStorageService.save(kbvItem);

        verify(dataStore, times(1)).create(kbvItem);
    }
}
