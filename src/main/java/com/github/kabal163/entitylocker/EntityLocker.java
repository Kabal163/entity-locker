package com.github.kabal163.entitylocker;

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
     * @param key a key which must be associated with a lock.
     * @throws NullPointerException   if {@code key} argument is {@code null}.
     * @throws LockAcquiringException if a thread was interrupted while waiting a lock release.
     */
    void lock(T key);

    /**
     * Creates a collection of locks and associates them with the specified keys.
     * Only one thread at a time can own the associated locks.
     * There is no a phenomenon such as deadlock. It's achieved via sorting the keys
     * and getting locks with the same order in any thread. If an exception occurs during
     * locks acquiring then all locks which has been acquired will be released and
     * {@link LockAcquiringException} will be thrown. It should contain a cause.
     * Collection may contains {@code null} elements, they will not be taken into account.
     * In order to specify a comparator use {@link LockProperties properties} and specify
     * the comparator there.
     * Method doesn't use a timeout thus it may wait forever until lock is released.
     * Client's code is responsible for the lock releasing.
     *
     * @param keys a collection of keys which must be associated with a locks.
     * @throws NullPointerException   if {@code keys} argument is {@code null}.
     * @throws LockAcquiringException if a thread was interrupted while waiting a lock release.
     * @see LockProperties#getComparator()
     */
    void lock(Collection<T> keys);

    /**
     * Associates specified key with a new lock. Only one thread at a time can own the
     * associated lock. Method uses timeout which is specified in the {@link LockProperties#getTimeoutMillis() properties}.
     * If you want to override this timeout or specify it explicitly use {@link #tryLock(Object, long)}.
     * If the specified key is already locked then a thread which is trying to acquire the lock will wait until
     * one of three things happen:
     * <ul>
     * <li>The owner thread releases the lock; or
     * <li>The timeout is over; or
     * <li>Thread is interrupted while waiting a lock release;
     * </ul>
     * If the lock is successfully acquired then returns {@code true}.
     * If timeout is over then returns {@code false}.
     * if thread is interrupter then set interrupted flag on the thread and throws {@link LockAcquiringException}.
     * Client's code is responsible for the lock releasing.
     *
     * @param key a key which must be associated with a lock.
     * @return {@code true} if lock has been successfully acquired; otherwise {@code false}
     * @throws NullPointerException   if {@code key} argument is {@code null}.
     * @throws LockAcquiringException if a thread was interrupted while waiting a lock release.
     * @see LockProperties#getTimeoutMillis()
     */
    boolean tryLock(T key);

    /**
     * Creates a collection of locks and associates them with the specified keys.
     * Only one thread at a time can own the associated locks.
     * There is no a phenomenon such as deadlock. It's achieved via sorting the keys
     * and getting locks with the same order in any thread. If an exception occurs during
     * locks acquiring then all locks which has been acquired will be released. Depending
     * on the reason of fail locks will be rolled back and:
     * <ul>
     * <li>Throw {@link LockAcquiringException} if there was an exception while a lock acquiring; or
     * <li>Returns {@code false} if key is locked by another thread and timeout is over;
     * </ul>
     * Collection may contains {@code null} elements, they will not be taken into account.
     * In order to specify a comparator use {@link LockProperties properties}.
     * Method uses timeout which is specified in the {@link LockProperties#getTimeoutMillis() properties}.
     * If you want to override this timeout or specify it explicitly use {@link #tryLock(Collection, long)}.
     * If the specified keys are already locked then a thread which is trying to acquire
     * the lock will wait until one of three things happen:
     * <ul>
     * <li>The owner thread releases the lock; or
     * <li>The timeout is over; or
     * <li>Thread is interrupted while waiting a lock release;
     * </ul>
     * if thread is interrupter then set interrupted flag on the thread and throws {@link LockAcquiringException}.
     * Client's code is responsible for the lock releasing.
     *
     * @param keys
     * @return {@code true} if lock has been successfully acquired; otherwise {@code false}
     * @throws NullPointerException   if {@code keys} argument is {@code null}.
     * @throws LockAcquiringException if a thread was interrupted while waiting a lock release.
     * @see LockProperties#getTimeoutMillis()
     * @see LockProperties#getComparator()
     */
    boolean tryLock(Collection<T> keys);

    /**
     * Associates specified key with a new lock. Only one thread at a time can own the
     * associated lock. Method uses timeout which is specified in the argument {@code timeoutMillis}.
     * It overrides the default timeout {@link LockProperties#getTimeoutMillis()}.
     * If the specified key is already locked then a thread which is trying to acquire
     * the lock will wait until one of three things happen:
     * <ul>
     * <li>The owner thread releases the lock; or
     * <li>The timeout is over; or
     * <li>Thread is interrupted while waiting a lock release;
     * </ul>
     * If the lock is successfully acquired then returns {@code true}.
     * If timeout is over then returns {@code false}.
     * if thread is interrupter then set interrupted flag on the thread and throws {@link LockAcquiringException}.
     * Client's code is responsible for the lock releasing.
     *
     * @param key a key which must be associated with a lock.
     * @return {@code true} if lock has been successfully acquired; otherwise {@code false}
     * @throws NullPointerException     if {@code key} argument is {@code null}.
     * @throws IllegalArgumentException if {@code timeout} is zero or negative.
     * @throws LockAcquiringException   if a thread was interrupted while waiting a lock release.
     */
    boolean tryLock(T key, long timeoutMillis);

    /**
     * Creates a collection of locks and associates them with the specified keys.
     * Only one thread at a time can own the associated locks.
     * There is no a phenomenon such as deadlock. It's achieved via sorting the keys
     * and getting locks with the same order in any thread. If an exception occurs during
     * locks acquiring then all locks which has been acquired will be released. Depending
     * on the reason of fail locks will be rolled back and:
     * <ul>
     * <li>Throw {@link LockAcquiringException} if there was an exception while a lock acquiring; or
     * <li>Returns {@code false} if key is locked by another thread and timeout is over;
     * </ul>
     * Collection may contains {@code null} elements, they will not be taken into account.
     * In order to specify a comparator use {@link LockProperties properties} and specify
     * the comparator there.
     * Method uses timeout which is specified in the argument {@code timeoutMillis}.
     * It overrides the default timeout {@link LockProperties#getTimeoutMillis()}.
     * If the specified keys are already locked then a thread which is trying to acquire
     * the lock will wait until one of three things happen:
     * <ul>
     * <li>The owner thread releases the lock; or
     * <li>The timeout is over; or
     * <li>Thread is interrupted while waiting a lock release;
     * </ul>
     * If the lock is successfully acquired then returns {@code true}.
     * If timeout is over then returns {@code false}.
     * if thread is interrupter then set interrupted flag on the thread and throws {@link LockAcquiringException}.
     * Client's code is responsible for the lock releasing.
     *
     * @param keys a key which must be associated with a lock.
     * @return {@code true} if lock has been successfully acquired; otherwise {@code false}
     * @throws NullPointerException     if {@code keys} argument is {@code null}.
     * @throws IllegalArgumentException if {@code timeout} is zero or negative.
     * @throws LockAcquiringException   if a thread was interrupted while waiting a lock release.
     * @see LockProperties#getTimeoutMillis()
     */
    boolean tryLock(Collection<T> keys, long timeoutMillis);

    /**
     * Releases a lock which is associated with the specified key. If there
     * is no lock associated then nothing happens.
     *
     * @param key a key which is associated which a lock which must be released
     * @throws NullPointerException      if {@code key} argument is {@code null}
     * @throws ThreadIsNotOwnerException if the current thread is not the owner of the lock
     */
    void unlock(T key);

    /**
     * Releases locks which are associated with the specified keys. If there
     * are no locks associated then nothing happens.
     *
     * @param keys a key which is associated which a lock which must be released
     * @throws NullPointerException      if {@code keys} argument is {@code null}
     * @throws ThreadIsNotOwnerException if the current thead is not the owned of at least one lock
     */
    void unlock(Collection<T> keys);
}
