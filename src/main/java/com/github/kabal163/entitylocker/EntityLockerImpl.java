package com.github.kabal163.entitylocker;

import javax.annotation.concurrent.Immutable;
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

    private boolean doLock(final T key, final long timeout) {
        final long threadId = Thread.currentThread().getId();

        synchronized (key) {
            final Timer timer = new Timer(timeout);
            LockOperation lockOperation = getOrCreateLockOperation(key, threadId);
            while (threadId != lockOperation.getThreadId()) {
                try {
                    if (timer.isOver()) {
                        return false;
                    } else if (timer.hasTimeout()) {
                        key.wait(timer.timeLeft());
                    } else {
                        key.wait();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new LockAcquiringException("Waiting has been interrupted! Error while lock acquiring! Key: " + key, ex);
                }
                lockOperation = getOrCreateLockOperation(key, threadId);
            }
        }
        return true;
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
        }

        synchronized (key) {
            locks.remove(key);
            key.notifyAll();
        }
    }

    private LockOperation getOrCreateLockOperation(final T key, final long threadId) {
        return locks.computeIfAbsent(key, k -> new LockOperation(threadId));
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

    private static final class LockOperation {
        private final long threadId;

        public LockOperation(long threadId) {
            this.threadId = threadId;
        }

        public long getThreadId() {
            return threadId;
        }
    }

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