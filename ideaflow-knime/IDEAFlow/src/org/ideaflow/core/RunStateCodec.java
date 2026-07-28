package org.ideaflow.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.ideaflow.api.RunState;
import org.ideaflow.spi.CapabilityDescriptor;
import org.ideaflow.spi.StateCodec;

/** Binary codec for the logical run state stored in IdeaFlow tables. */
public final class RunStateCodec implements StateCodec<RunState> {
  private static final int VERSION = 2;

  @Override
  public String id() {
    return "ideaflow.run-state.v2";
  }

  @Override
  public String displayName() {
    return "IdeaFlow Run State v2";
  }

  @Override
  public CapabilityDescriptor capabilities() {
    return CapabilityDescriptor.general();
  }

  @Override
  public Class<RunState> stateType() {
    return RunState.class;
  }

  @Override
  public byte[] encode(final RunState state) {
    try {
      final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(bytes)) {
        out.writeInt(VERSION);
        out.writeUTF(state.runId());
        out.writeUTF(state.populationId());
        out.writeLong(state.nfe());
        out.writeLong(state.maxEvaluations());
        out.writeLong(state.startedAtMillis());
        out.writeBoolean(state.stopped());
        out.writeUTF(state.stopReason());
        out.writeInt(state.algorithmState().size());
        for (Map.Entry<String, String> entry : state.algorithmState().entrySet()) {
          out.writeUTF(entry.getKey());
          out.writeUTF(entry.getValue());
        }
      }
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  @Override
  public RunState decode(final byte[] bytes) {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      final int version = in.readInt();
      if (version != VERSION)
        throw new IllegalArgumentException("Unsupported run-state version: " + version);
      final String run = in.readUTF(), population = in.readUTF();
      final long nfe = in.readLong(), maximum = in.readLong(), started = in.readLong();
      final boolean stopped = in.readBoolean();
      final String reason = in.readUTF();
      final int count = in.readInt();
      final Map<String, String> algorithmState = new LinkedHashMap<>();
      for (int i = 0; i < count; i++) algorithmState.put(in.readUTF(), in.readUTF());
      return new RunState(run, population, nfe, maximum, started, stopped, reason, algorithmState);
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid run-state payload.", e);
    }
  }
}
