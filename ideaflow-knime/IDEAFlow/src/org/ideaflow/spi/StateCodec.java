package org.ideaflow.spi;

/** Serializes algorithm state without coupling the public state model to one encoding. */
public interface StateCodec<T> extends Strategy {
  Class<T> stateType();

  byte[] encode(T state);

  T decode(byte[] bytes);
}
