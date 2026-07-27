package org.ideaflow.spi;

public interface StateCodec<T> extends Strategy {
    Class<T> stateType();
    byte[] encode(T state);
    T decode(byte[] bytes);
}
