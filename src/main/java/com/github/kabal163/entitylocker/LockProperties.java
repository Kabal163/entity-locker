package com.github.kabal163.entitylocker;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Comparator;

@Immutable
@ThreadSafe
public class LockProperties<T> {

    private final long timeoutMillis;
    private final Comparator<T> comparator;

    private LockProperties(long timeoutMillis,
                           Comparator<T> comparator) {
        this.timeoutMillis = timeoutMillis;
        this.comparator = comparator;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public Comparator<T> getComparator() {
        return comparator;
    }

    public static final class Builder<T> {
        private long timeoutMillis;
        private Comparator<T> comparator;

        public Builder<T> timeout(long timeout) {
            this.timeoutMillis = timeout;
            return this;
        }

        public Builder<T> comparator(Comparator<T> comparator) {
            this.comparator = comparator;
            return this;
        }

        public LockProperties<T> build() {
            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("Timeout must be positive!");
            }
            if (comparator == null) {
                throw new IllegalStateException("Comparator must not be null!");
            }
            return new LockProperties<>(timeoutMillis, comparator);
        }
    }
}
