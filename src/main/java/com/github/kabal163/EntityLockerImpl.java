package com.github.kabal163;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntityLockerImpl<T extends Comparable<T>> implements EntityLocker<T> {

    private final Map<T, LockOperation> locks = new ConcurrentHashMap<>();
    private final LockProperties properties;

    protected EntityLockerImpl(LockProperties properties) {
        this.properties = properties;
    }

    @Override
    public void lock(final T key) {
        Objects.requireNonNull(key, "Error while locking! Argument 'key' must not be null!");
        doLock(key, Timer.NO_TIMEOUT);
    }

    @Override
    public void lock(final Collection<T> keys) {
        Objects.requireNonNull(keys, "Error while locking! Argument 'keys' must not be null!");
        doLock(keys, Timer.NO_TIMEOUT);
    }

    @Override
    public boolean tryLock(final T key) {
        Objects.requireNonNull(key, "Error while locking! Argument 'key' must not be null!");
        return doLock(key, properties.getTimeoutMillis());
    }

    @Override
    public boolean tryLock(final Collection<T> keys) {
        Objects.requireNonNull(keys, "Error while locking! Argument 'keys' must not be null!");
        return doLock(keys, properties.getTimeoutMillis());
    }

    @Override
    public boolean tryLock(final T key, final long timeoutMillis) {
        Objects.requireNonNull(key, "Error while locking! Argument 'key' must not be null!");
        checkTimeoutIsPositive(timeoutMillis);
        return doLock(key, timeoutMillis);
    }

    @Override
    public boolean tryLock(final Collection<T> keys, final long timeoutMillis) {
        Objects.requireNonNull(keys, "Error while locking! Argument 'keys' must not be null!");
        checkTimeoutIsPositive(timeoutMillis);
        return doLock(keys, timeoutMillis);
    }

    @Override
    public void unlock(final T key) {
        Objects.requireNonNull(key, "Argument 'key' must not be null!");
        doUnlock(key);
    }

    @Override
    public void unlock(final Collection<T> keys) {
        Objects.requireNonNull(keys, "Argument 'keys' must not be null!");
        filterAndSort(keys).forEach(this::doUnlock);
    }

    private boolean doLock(final Collection<T> keys, final long timeout) {
        final Set<T> sortedKeys = filterAndSort(keys);
        final LinkedList<T> lockedKeys = new LinkedList<>();

        for (final T key : sortedKeys) {
            if (doLock(key, timeout)) { // todo handle exceptions
                lockedKeys.addFirst(key);
            } else {
                rollback(lockedKeys);
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
                System.out.println(Thread.currentThread().getName() + ", " + threadId + " Lock has already been acquired be another thread. Waiting... Owner threadId: " + lockOperation.getThreadId());
                try {
                    if (timer.isOver()) {
                        System.out.println(Thread.currentThread().getName() + ", " + threadId + " Timeout is over");
                        return false;
                    } else if (timer.hasTimeout()) {
                        key.wait(timer.timeLeft());
                    } else {
                        key.wait();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new LockAcquiringException("Waiting was interrupted! Error while lock acquiring! Key: " + key, ex);
                }
                lockOperation = getOrCreateLockOperation(key, threadId);
            }
        }
        System.out.println(Thread.currentThread().getName() + ", " + threadId + " Lock successfully acquired.");
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
            System.out.println(Thread.currentThread().getName() + ", " + threadId + " Removing lock...");
            locks.remove(key);
            key.notifyAll();
            System.out.println(Thread.currentThread().getName() + ", " + threadId + " Removed");
        }
    }

    private LockOperation getOrCreateLockOperation(final T key, final long threadId) {
        return locks.computeIfAbsent(key, k -> new LockOperation(threadId));
    }

    private Set<T> filterAndSort(Collection<T> keys) {
        return keys.stream()
                .filter(Objects::nonNull)
                .sorted()
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
            if (NO_TIMEOUT != timeout) {
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
