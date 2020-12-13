package com.github.kabal163;

import java.util.Collection;

public interface EntityLocker<T extends Comparable<T>> {

    /**
     *
     * @param key
     * @throws NullPointerException if {@code key} argument is {@code null}
     */
    void lock(T key);

    /**
     *
     * @param keys
     * @throws NullPointerException if {@code keys} argument is {@code null}
     */
    void lock(Collection<T> keys);

    /**
     *
     * @param key
     * @throws NullPointerException if {@code key} argument is {@code null}
     */
    boolean tryLock(T key);

    /**
     *
     * @param keys
     * @throws NullPointerException if {@code keys} argument is {@code null}
     */
    boolean tryLock(Collection<T> keys);

    /**
     *
     * @param key
     * @throws NullPointerException if {@code key} argument is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     */
    boolean tryLock(T key, long timeoutMillis);

    /**
     *
     * @param keys
     * @throws NullPointerException if {@code keys} argument is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     */
    boolean tryLock(Collection<T> keys, long timeoutMillis);

    /**
     *
     * @param key
     * @throws NullPointerException if {@code key} argument is {@code null}
     */
    void unlock(T key);

    /**
     *
     * @param keys
     * @throws NullPointerException if {@code keys} argument is {@code null}
     */
    void unlock(Collection<T> keys);
}
