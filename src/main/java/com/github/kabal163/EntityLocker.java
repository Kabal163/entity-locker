package com.github.kabal163;

import java.util.Collection;

/**
 * Provides ability to acquire locks via keys.
 * Only one thread at a time can hold the locks with specified keys.
 * If a thread try to acquire a lock which is already acquired
 * by another thread then it will be locked. See the methods for more details.
 * Client's code is responsible for locks releasing which has been
 * acquired by a thread.
 * In order to locks work properly client must use the same key object
 * in all threads which try to acquire a lock. Keys are treated the same
 * only if they have reference to the same object. Otherwise they are
 * treated as different keys and client will get different locks for them.
 *
 * @param <T> type of the key which is used to acquire a lock
 */
public interface EntityLocker<T> {

    /**
     * Associates specified key with a new lock. Only one thread at a time can own the
     * associated lock. Method doesn't use a timeout thus it may wait forever until lock
     * is released. Client's code is responsible for the lock releasing.
     *
     * @param key a key which must be associated with a lock
     * @throws NullPointerException   if {@code key} argument is {@code null}
     * @throws LockAcquiringException if a thread was interrupted while waiting a lock release.
     * @see EntityLocker#unlock(Object) ) to release a lock
     */
    void lock(T key);

    /**
     * Creates a collection of locks and associates them with the specified keys.
     * Only one thread at a time can own the associated locks.
     * There is no a phenomenon such as deadlock. It's achieved via sorting the keys
     * and getting locks with the same order in any thread.
     * In order to specify a comparator use {@link LockProperties properties}.
     * Method doesn't use a timeout thus it may wait forever until lock is released.
     * Client's code is responsible for the lock releasing.
     *
     * @param keys a collection of keys which must be associated with a locks
     * @throws NullPointerException   if {@code keys} argument is {@code null}
     * @throws LockAcquiringException if a thread was interrupted while waiting a lock release.
     * @see LockProperties#getComparator()
     */
    void lock(Collection<T> keys);

    /**
     * @param key
     * @throws NullPointerException   if {@code key} argument is {@code null}
     * @throws LockAcquiringException
     */
    boolean tryLock(T key);

    /**
     * @param keys
     * @throws NullPointerException   if {@code keys} argument is {@code null}
     * @throws LockAcquiringException
     */
    boolean tryLock(Collection<T> keys);

    /**
     * @param key
     * @throws NullPointerException     if {@code key} argument is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     * @throws LockAcquiringException
     */
    boolean tryLock(T key, long timeoutMillis);

    /**
     * @param keys
     * @throws NullPointerException     if {@code keys} argument is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     * @throws LockAcquiringException
     */
    boolean tryLock(Collection<T> keys, long timeoutMillis);

    /**
     * @param key
     * @throws NullPointerException   if {@code key} argument is {@code null}
     * @throws LockAcquiringException
     */
    void unlock(T key);

    /**
     * @param keys
     * @throws NullPointerException   if {@code keys} argument is {@code null}
     * @throws LockAcquiringException
     */
    void unlock(Collection<T> keys);
}
