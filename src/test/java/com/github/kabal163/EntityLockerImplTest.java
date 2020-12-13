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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Some tests may fail due to timeouts.
 * It depends on the machine and it's current load where tests are running.
 * If the machine is slow be sure you have selected appropriate timeout.
 */
class EntityLockerImplTest {

    static final int NUMBER_OF_THREADS = 20;
    static final int NUMBER_OF_INCREMENTS = 5000;
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
    @DisplayName("Given 'n' threads which increment a not thread safe counter 'x' times each " +
            "When they increment the counter under the lock " +
            "Then result counter equals to 'n * x'")
    void givenNThreads_andXIncrements_whenIncrementUnderLock_thenResultCounterIsNX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
        Counter counter = new Counter(TEST_KEY);

        runInParallel(
                singletonList(counter),
                latch,
                keys -> {locker.lock(counter.getKey()); return true;});
        latch.await();

        assertThat(counter.getValue()).isEqualTo(NUMBER_OF_THREADS * NUMBER_OF_INCREMENTS);
    }

    @Test
    @DisplayName("Given 'n' threads which increment a not thread safe counter 'x' times each " +
            "When they increment the counter under the tryLock(T) " +
            "Then result counter equals to 'n * x'")
    void givenNThreads_andXIncrements_whenIncrementUnderTryLock_thenResultCounterIsNX_1() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
        Counter counter = new Counter(TEST_KEY);

        runInParallel(
                singletonList(counter),
                latch,
                keys -> locker.tryLock(counter.getKey(), TEST_TIMEOUT_MILLIS));
        latch.await();

        assertThat(counter.getValue()).isEqualTo(NUMBER_OF_THREADS * NUMBER_OF_INCREMENTS);
    }

    @Test
    @DisplayName("Given 'n' threads which increment a not thread safe counter 'x' times each " +
            "When they increment the counter under the tryLock(T, long) " +
            "Then result counter equals to 'n * x'")
    void givenNThreads_andXIncrements_whenIncrementUnderTryLock_thenResultCounterIsNX_2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
        Counter counter = new Counter(TEST_KEY);

        runInParallel(
                singletonList(counter),
                latch,
                keys -> locker.tryLock(counter.getKey()));
        latch.await();

        assertThat(counter.getValue()).isEqualTo(NUMBER_OF_THREADS * NUMBER_OF_INCREMENTS);
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
    @DisplayName("Given 'n' thread increment 'm' counters 'x' times each " +
            "When increment them under lock(Collection<T>) " +
            "Then result value of each counter will be 'n * m' and total result is 'm * n * x' ")
    void givenNCounters_andMThreads_andXIncrements_whenIncrementUnderLock_thenResultOfEachCounterIsNM() throws InterruptedException {
        List<Counter> counters = List.of(
                new Counter("key1"),
                new Counter("key2"),
                new Counter("key3"),
                new Counter("key4"),
                new Counter("key5"));
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);

        runInParallel(
                counters,
                latch,
                keys -> {locker.lock(keys); return true;});
        latch.await();

        counters.forEach(c ->
                assertThat(c.getValue())
                        .isEqualTo(NUMBER_OF_THREADS * NUMBER_OF_INCREMENTS));
        final int totalValue = counters.stream()
                .map(Counter::getValue)
                .reduce(0, Integer::sum);
        assertThat(totalValue).isEqualTo(NUMBER_OF_THREADS * NUMBER_OF_INCREMENTS * counters.size());
    }

    private void runInParallel(final List<Counter> counters,
                               final CountDownLatch latch,
                               final Function<List<String>, Boolean> lockFunction) {
        final ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        final CyclicBarrier barrier = new CyclicBarrier(NUMBER_OF_THREADS);
        final List<String> keys = counters.stream()
                .map(Counter::getKey)
                .collect(toUnmodifiableList());

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            executorService.execute(() -> {
                try {
                    barrier.await();
                    if (lockFunction.apply(keys)) {
                        for (int j = 0; j < NUMBER_OF_INCREMENTS; j++) {
                            counters.forEach(Counter::increment);
                        }
                    }
                    locker.unlock(keys);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Error while waiting a test suite is started!");
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException("Barrier is broken! Error while waiting a test suite is started!");
                } finally {
                    latch.countDown();
                }
            });
        }
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