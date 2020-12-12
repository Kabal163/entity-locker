package com.github.kabal163;

import java.util.Collection;

public interface EntityLocker<T extends Comparable<T>> {

    void lock(T id);

    void lock(Collection<T> ids);

    boolean tryLock(T id);

    boolean tryLock(Collection<T> ids);

    boolean tryLock(T id, long timeoutMillis);

    boolean tryLock(Collection<T> ids, long timeoutMillis);

    void unlock(T id);

    void unlock(Collection<T> ids);
}
