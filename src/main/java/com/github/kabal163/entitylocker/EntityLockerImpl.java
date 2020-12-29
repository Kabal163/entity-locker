package com.github.kabal163.entitylocker;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Supports reentrant locks.
 *
 * @param <T> type of the key which is used to acquire a lock
 */
@Immutable
@ThreadSafe
public class EntityLockerImpl<T> implements EntityLocker<T> {

    private final Map<T, LockOperation> locks = new ConcurrentHashMap<>();
    private final LockProperties<T> properties;

    public EntityLockerImpl(LockProperties<T> properties) {
        Objects.requireNonNull(properties, "Properties must not be null!");
        this.properties = properties;
    }

    @Override
    public void lock(final T key) {
        Objects.requireNonNull(key, "Error while locking! Parameter 'key' must not be null!");
        doLock(key, Timer.NO_TIMEOUT);
    }

    @Override
    public void lock(final Collection<T> keys) {
        Objects.requireNonNull(keys, "Error while locking! Parameter 'keys' must not be null!");
        doLock(keys, Timer.NO_TIMEOUT);
    }

    @Override
    public boolean tryLock(final T key) {
        Objects.requireNonNull(key, "Error while locking! Parameter 'key' must not be null!");
        return doLock(key, properties.getTimeoutMillis());
    }

    @Override
    public boolean tryLock(final Collection<T> keys) {
        Objects.requireNonNull(keys, "Error while locking! Parameter 'keys' must not be null!");
        return doLock(keys, properties.getTimeoutMillis());
    }

    @Override
    public boolean tryLock(final T key, final long timeoutMillis) {
        Objects.requireNonNull(key, "Error while locking! Parameter 'key' must not be null!");
        checkTimeoutIsPositive(timeoutMillis);
        return doLock(key, timeoutMillis);
    }

    @Override
    public boolean tryLock(final Collection<T> keys, final long timeoutMillis) {
        Objects.requireNonNull(keys, "Error while locking! Parameter 'keys' must not be null!");
        checkTimeoutIsPositive(timeoutMillis);
        return doLock(keys, timeoutMillis);
    }

    @Override
    public void unlock(final T key) {
        Objects.requireNonNull(key, "Parameter 'key' must not be null!");
        doUnlock(key);
    }

    @Override
    public void unlock(final Collection<T> keys) {
        Objects.requireNonNull(keys, "Parameter 'keys' must not be null!");
        filterAndSort(keys).forEach(this::doUnlock);
    }

    private boolean doLock(final Collection<T> keys, final long timeout) {
        final Set<T> sortedKeys = filterAndSort(keys);
        final LinkedList<T> lockedKeys = new LinkedList<>();

        Exception exception = null;
        for (final T key : sortedKeys) {
            boolean locked;
            try {
                locked = doLock(key, timeout);
            } catch (Exception ex) {
                locked = false;
                exception = ex;
            }
            if (locked) {
                lockedKeys.addFirst(key);
            } else {
                rollback(lockedKeys);
                if (exception != null) {
                    throw new LockAcquiringException("Error while acquiring collection of locks!", exception);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Gets lock operation until it will have the same threadId as the current thread has
     * or fail by timeout or an exception. It means one of the four:
     * <ul>
     * <li>{@link #locks} doesn't have a lock associated with the {@code key}
     * <li>The current thread gets its owning lock operation which means reentrancy
     * <li>Specified timeout is over
     * <li>{@link InterruptedException} is thrown
     * </ul>
     */
    private boolean doLock(final T key, final long timeout) {
        final long threadId = Thread.currentThread().getId();

        synchronized (key) {
            final Timer timer = new Timer(timeout);
            LockOperation lockOperation;
            do {
                lockOperation = locks.get(key);
                if (lockOperation == null) {
                    lockOperation = new LockOperation(threadId);
                }
                if (threadId == lockOperation.getThreadId()) {
                    lockOperation.increment();
                    locks.put(key, lockOperation);
                } else {
                    if (timer.isOver()) {
                        return false;
                    } else {
                        waitForSignal(key, timer);
                    }
                }
            } while (threadId != lockOperation.getThreadId());
        }
        return true;
    }

    private void waitForSignal(T key, Timer timer) {
        try {
            if (timer.hasTimeout()) {
                key.wait(timer.timeLeft());
            } else {
                key.wait();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LockAcquiringException("Waiting has been interrupted! Error while lock acquiring! Key: " + key, ex);
        }
    }

    private void doUnlock(final T key) {
        final long threadId = Thread.currentThread().getId();
        final LockOperation lockOperation = locks.get(key);

        if (lockOperation == null) {
            return;
        }
        if (threadId != lockOperation.getThreadId()) {
            throw new ThreadIsNotOwnerException("The current thread is not the owner of the lock! Current threadId: "
                    + threadId + "; Owner threadId: " + lockOperation.getThreadId());
        } else {
            lockOperation.decrement();
        }

        if (lockOperation.getReentrancyCount() == 0) {
            synchronized (key) {
                locks.remove(key);
                key.notifyAll();
            }
        }
    }

    private Set<T> filterAndSort(Collection<T> keys) {
        return keys.stream()
                .filter(Objects::nonNull)
                .sorted(properties.getComparator())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void rollback(List<T> keys) {
        keys.forEach(this::doUnlock);
    }

    private void checkTimeoutIsPositive(long timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive, otherwise use methods without explicit timeout.");
        }
    }

    /**
     * Contains information about a lock.
     */
    @NotThreadSafe
    private static final class LockOperation {
        /**
         * Unique thread identifier which own the lock
         */
        private final long threadId;

        /**
         * Counts the times of the thread acquiring the same lock
         */
        private int reentrancyCount = 0;

        public LockOperation(long threadId) {
            this.threadId = threadId;
        }

        public long getThreadId() {
            return threadId;
        }

        public void increment() {
            reentrancyCount++;
        }

        public void decrement() {
            reentrancyCount--;
        }

        public int getReentrancyCount() {
            return reentrancyCount;
        }
    }

    @ThreadSafe
    private static final class Timer {
        private final long timeout;
        private final long deadline;

        public static final long NO_TIMEOUT = -1L;

        public Timer(long timeout) {
            this.timeout = timeout;
            if (hasTimeout()) {
                this.deadline = System.currentTimeMillis() + timeout;
            } else {
                this.deadline = Long.MAX_VALUE;
            }
        }

        public long timeLeft() {
            if (hasTimeout()) {
                return deadline - System.currentTimeMillis();
            }
            return timeout;
        }

        public boolean hasTimeout() {
            return timeout != NO_TIMEOUT;
        }

        public boolean isOver() {
            return deadline <= System.currentTimeMillis();
        }
    }
}
