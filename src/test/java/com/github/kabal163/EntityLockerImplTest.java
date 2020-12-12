package com.github.kabal163;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class EntityLockerImplTest {

    @Mock
    LockProperties properties;

    EntityLockerImpl<String> locker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(properties.getTimeoutMillis()).thenReturn(100L);

        locker = new EntityLockerImpl<>(properties);
    }

    @Test
    @DisplayName("Given null key " +
            "When call lock(String) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallLock_thenThrowsNullPointerException_1() {
        final String nullable = null;
        assertThatThrownBy(() -> locker.lock(nullable))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Given null key " +
            "When call lock(Collection<String>) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallLock_thenThrowsNullPointerException_2() {
        final Collection<String> nullable = null;
        assertThatThrownBy(() -> locker.lock(nullable))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }
}