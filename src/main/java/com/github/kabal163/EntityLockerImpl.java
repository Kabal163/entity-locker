package com.github.kabal163;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class EntityLockerImpl<T extends Comparable<T>> implements EntityLocker<T> {

    private static final int NO_TIMEOUT = 0;

    private final Map<T, LockOperation> locks = new ConcurrentHashMap<>();
    private final LockProperties properties;

    public EntityLockerImpl(LockProperties properties) {
        this.properties = properties;
    }

    @Override
    public void lock(T key) {
        Objects.requireNonNull(key, "Error while locking! Argument 'key' must not be null!");
        doLock(key, NO_TIMEOUT);
    }

    @Override
    public void lock(Collection<T> keys) {
        Objects.requireNonNull(keys, "Error while locking! Argument 'keys' must not be null!");
        doLock(keys, NO_TIMEOUT);
    }

    @Override
    public boolean tryLock(T key) {
        Objects.requireNonNull(key, "Error while locking! Argument 'key' must not be null!");
        return doLock(key, properties.getTimeoutMillis());
    }

    @Override
    public boolean tryLock(final Collection<T> keys) {
        Objects.requireNonNull(keys, "Error while locking! Argument 'keys' must not be null!");
        return doLock(keys, properties.getTimeoutMillis());
    }

    @Override
    public boolean tryLock(T key, long timeoutMillis) {
        Objects.requireNonNull(key, "Error while locking! Argument 'key' must not be null!");
        checkTimeoutIsPositive(timeoutMillis);
        return doLock(key, timeoutMillis);
    }

    @Override
    public boolean tryLock(Collection<T> keys, long timeoutMillis) {
        Objects.requireNonNull(keys, "Error while locking! Argument 'keys' must not be null!");
        checkTimeoutIsPositive(timeoutMillis);
        return doLock(keys, timeoutMillis);
    }

    @Override
    public void unlock(T key) {
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
        for (T key : sortedKeys) {
            if (doLock(key, timeout)) {
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
        LockOperation lockOperation = getOrCreateLockOperation(key, threadId);
        lockOperation.lock();

        long currentTime = System.currentTimeMillis();
        long startTime = currentTime;
        try {
            while (threadId != lockOperation.getThreadId()) {
                long actualTimeout = timeout - (currentTime - startTime);
                if (actualTimeout >= 0 && lockOperation.await(actualTimeout)) {
                    lockOperation = getOrCreateLockOperation(key, threadId);
                    currentTime = System.currentTimeMillis();
                } else {
                    return false;
                }
            }
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LockAcquiringException("Waiting was interrupted! Error while lock acquiring! Key: " + key, ex);
        } finally {
            lockOperation.unlock();
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
        }

        lockOperation.lock();
        try {
            lockOperation.signalAll();
        } finally {
            lockOperation.unlock();
        }
    }

    private LockOperation getOrCreateLockOperation(final T key, final long threadId) {
        return locks.computeIfAbsent(key, k -> new LockOperation(threadId));
    }

    private Set<T> filterAndSort(Collection<T> keys) {
        return keys.stream()
                .filter(Objects::isNull)
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
        private final ReentrantLock lock;
        private final Condition condition;

        public LockOperation(long threadId) {
            this.threadId = threadId;
            this.lock = new ReentrantLock();
            this.condition = lock.newCondition();
        }

        public long getThreadId() {
            return threadId;
        }

        public void lock() {
            lock.lock();
        }

        public boolean await(long timeoutMillis) throws InterruptedException {
            if (timeoutMillis == NO_TIMEOUT) {
                condition.await();
            } else {
                return condition.await(timeoutMillis, MILLISECONDS);
            }
            return true;
        }

        public void signalAll() {
            condition.signalAll();
        }

        public void unlock() {
            lock.unlock();
        }
    }
}
