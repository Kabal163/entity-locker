package com.github.kabal163;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Some tests may fail due to timeouts.
 * It depends on the machine and it's current load where tests are running.
 * If the machine is slow be sure you have selected appropriate timeout.
 */
class EntityLockerImplTest {

    static final long TEST_TIMEOUT_MILLIS = 500L;
    static final String TEST_KEY = "testKey";

    @Mock
    LockProperties properties;

    EntityLockerImpl<String> locker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(properties.getTimeoutMillis()).thenReturn(TEST_TIMEOUT_MILLIS);

        locker = new EntityLockerImpl<>(properties);
    }

    @Test
    @DisplayName("Given null key " +
            "When call lock(T) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallLock_thenThrowsNullPointerException_1() {
        final String nullable = null;
        assertThatThrownBy(() -> locker.lock(nullable))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Given null key " +
            "When call lock(Collection<T>) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallLock_thenThrowsNullPointerException_2() {
        final Collection<String> nullable = null;
        assertThatThrownBy(() -> locker.lock(nullable))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Given null key " +
            "When call tryLock(T) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallTryLock_thenThrowsNullPointerException_1() {
        final String nullable = null;
        assertThatThrownBy(() -> locker.tryLock(nullable))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Given null key " +
            "When call tryLock(Collection<T>) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallTryLock_thenThrowsNullPointerException_2() {
        final Collection<String> nullable = null;
        assertThatThrownBy(() -> locker.tryLock(nullable))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Given null key " +
            "When call tryLock(T, long) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallTryLock_thenThrowsNullPointerException_3() {
        final String nullable = null;
        assertThatThrownBy(() -> locker.tryLock(nullable, TEST_TIMEOUT_MILLIS))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Given null key " +
            "When call tryLock(Collection<T>, long) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallTryLock_thenThrowsNullPointerException_4() {
        final Collection<String> nullable = null;
        assertThatThrownBy(() -> locker.tryLock(nullable, TEST_TIMEOUT_MILLIS))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @ParameterizedTest
    @MethodSource("getInvalidTimeouts")
    @DisplayName("Given invalid timeout " +
            "When call tryLock(T, long) method " +
            "Then throws IllegalArgumentException")
    void givenInvalidTimeout_whenCallTryLock_thenThrowsIllegalArgumentException_1(long timeout) {
        assertThatThrownBy(() -> locker.tryLock("any", timeout))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }

    @ParameterizedTest
    @MethodSource("getInvalidTimeouts")
    @DisplayName("Given invalid timeout " +
            "When call tryLock(Collection<T>, long) method " +
            "Then throws IllegalArgumentException")
    void givenInvalidTimeout_whenCallTryLock_thenThrowsIllegalArgumentException_2(long timeout) {
        List<String> emptyList = emptyList();
        assertThatThrownBy(() -> locker.tryLock(emptyList, timeout))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    @DisplayName("Given null key " +
            "When call unlock(T) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallUnlock_thenThrowsNullPointerException_3() {
        final String nullable = null;
        assertThatThrownBy(() -> locker.unlock(nullable))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Given null key " +
            "When call unlock(Collection<T>) method " +
            "Then throws NullPointerException")
    void givenNullKey_whenCallUnlock_thenThrowsNullPointerException_4() {
        final Collection<String> nullable = null;
        assertThatThrownBy(() -> locker.unlock(nullable))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Given 20 threads which increment a not thread safe counter 5000 times each " +
            "When they increment the counter under the lock " +
            "Then result counter equals to 100000")
    void given20Threads_and5000increments_whenIncrementUnderLock_thenResultCounterIs100000() throws InterruptedException {
        final int numberOfThreads = 20;
        final int numberOfIncrements = 5000;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);

        final Counter counter = new Counter(TEST_KEY);
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    barrier.await();
                    locker.lock(counter.getKey());
                    for (int j = 0; j < numberOfIncrements; j++) {
                        counter.increment();
                    }
                    locker.unlock(counter.getKey());
                } catch (InterruptedException e) {
                    throw new RuntimeException("Error while waiting a test suite is started!");
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException("Barrier is broken! Error while waiting a test suite is started!");
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        assertThat(counter.getValue()).isEqualTo(numberOfIncrements * numberOfThreads);
    }

    @Test
    @DisplayName("Given 20 threads which increment a not thread safe counter 5000 times each " +
            "When they increment the counter under the tryLock(T) " +
            "Then result counter equals to 100000")
    void given20Threads_and5000increments_whenIncrementUnderTryLock_thenResultCounterIs100000_1() throws InterruptedException {
        final int numberOfThreads = 20;
        final int numberOfIncrements = 5000;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);

        final Counter counter = new Counter(TEST_KEY);
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    barrier.await();
                    if (locker.tryLock(counter.getKey())) {
                        for (int j = 0; j < numberOfIncrements; j++) {
                            counter.increment();
                        }
                    }
                    locker.unlock(counter.getKey());
                } catch (InterruptedException e) {
                    throw new RuntimeException("Error while waiting a test suite is started!");
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException("Barrier is broken! Error while waiting a test suite is started!");
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        assertThat(counter.getValue()).isEqualTo(numberOfIncrements * numberOfThreads);
    }

    @Test
    @DisplayName("Given 20 threads which increment a not thread safe counter 5000 times each " +
            "When they increment the counter under the tryLock(T, long) " +
            "Then result counter equals to 100000")
    void given20Threads_and5000increments_whenIncrementUnderTryLock_thenResultCounterIs100000_2() throws InterruptedException {
        final int numberOfThreads = 20;
        final int numberOfIncrements = 5000;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);

        final Counter counter = new Counter(TEST_KEY);
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    barrier.await();
                    if (locker.tryLock(counter.getKey(), TEST_TIMEOUT_MILLIS)) {
                        for (int j = 0; j < numberOfIncrements; j++) {
                            counter.increment();
                        }
                    }
                    locker.unlock(counter.getKey());
                } catch (InterruptedException e) {
                    throw new RuntimeException("Error while waiting a test suite is started!");
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException("Barrier is broken! Error while waiting a test suite is started!");
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        assertThat(counter.getValue()).isEqualTo(numberOfIncrements * numberOfThreads);
    }

    @Test
    @DisplayName("Given default timeout and entity is lock for long time " +
            "When call tryLock(T) " +
            "Then returns false due to timeout")
    void givenDefaultTimeout_andEntityIsLocked_whenCallTryLock_thenReturnsFalse() throws InterruptedException {
        final int numberOfThreads = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch assertLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(numberOfThreads);

        executorService.execute(() -> {
            locker.lock(TEST_KEY);
            lockAcquiredLatch.countDown();
            try {
                assertLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error while waiting a test suite is completed!");
            } finally {
                locker.unlock(TEST_KEY);
            }
        });

        lockAcquiredLatch.await();
        boolean actual = locker.tryLock(TEST_KEY);
        assertLatch.countDown();

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("Given explicit timeout and entity is lock for long time " +
            "When call tryLock(T) " +
            "Then returns false due to timeout")
    void givenExplicitTimeout_andEntityIsLocked_whenCallTryLock_thenReturnsFalse() throws InterruptedException {
        final int numberOfThreads = 1;
        final long timeoutMillis = 20L;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch assertLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(numberOfThreads);

        executorService.execute(() -> {
            locker.lock(TEST_KEY);
            lockAcquiredLatch.countDown();
            try {
                assertLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error while waiting a test suite is completed!");
            } finally {
                locker.unlock(TEST_KEY);
            }
        });

        lockAcquiredLatch.await();
        boolean actual = locker.tryLock(TEST_KEY, timeoutMillis);
        assertLatch.countDown();

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("Given 20 thread increment 5 counters 5000 times each " +
            "When increment them under lock(Collection<T>) " +
            "Then result value of each counter will be 100000 ")
    void given5Counters_and20Threads_and5000increments_whenIncrementUnderLock_thenResultOfEachCounterIs100000() throws InterruptedException {
        List<Counter> counters = List.of(
                new Counter("key1"),
                new Counter("key2"),
                new Counter("key3"),
                new Counter("key4"),
                new Counter("key5"));
        Set<String> keys = counters.stream()
                .map(Counter::getKey)
                .collect(toUnmodifiableSet());

        final int numberOfThreads = 20;
        final int numberOfIncrements = 5000;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    barrier.await();
                    locker.lock(keys);
                    for (int j = 0; j < numberOfIncrements; j++) {
                        counters.forEach(Counter::increment);
                    }
                    locker.unlock(keys);
                    latch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Error while waiting a test suite is started!");
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException("Barrier is broken! Error while waiting a test suite is started!");
                }
            });
        }
        latch.await();
        final int totalValue = counters.stream()
                .map(Counter::getValue)
                .reduce(0, Integer::sum);
        assertThat(totalValue).isEqualTo(numberOfIncrements * numberOfThreads * counters.size());
        counters.forEach(c ->
                assertThat(c.getValue())
                        .isEqualTo(numberOfIncrements * numberOfThreads));
    }

    static Stream<Arguments> getInvalidTimeouts() {
        return Stream.of(
                Arguments.of(-10L),
                Arguments.of(0L)
        );
    }

    private static final class Counter {
        private final String key;
        private int value = 0;

        public Counter(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void increment() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }
}