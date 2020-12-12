package com.github.kabal163;

public class LockProperties {

    private final long timeoutMillis;

    private LockProperties(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public static final class Builder {
        private long timeoutMillis;

        public Builder timeout(long timeout) {
            this.timeoutMillis = timeout;
            return this;
        }

        public LockProperties build() {
            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("Timeout must be positive!");
            }
            return new LockProperties(timeoutMillis);
        }
    }
}
