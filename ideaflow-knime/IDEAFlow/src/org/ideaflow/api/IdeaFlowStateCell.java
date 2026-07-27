package org.ideaflow.api;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;

/** A compact KNIME cell whose table rendering intentionally hides technical state details. */
public final class IdeaFlowStateCell extends DataCell {
    private static final long serialVersionUID = 1L;
    public static final DataType TYPE = DataType.getType(IdeaFlowStateCell.class);

    private final IdeaFlowState m_state;

    public IdeaFlowStateCell(final IdeaFlowState state) {
        m_state = state == null ? IdeaFlowState.empty() : state;
    }

    public IdeaFlowState state() {
        return m_state;
    }

    @Override public String toString() {
        return "IdeaFlow state";
    }

    @Override protected boolean equalsDataCell(final DataCell other) {
        return other instanceof IdeaFlowStateCell cell && m_state.equals(cell.m_state);
    }

    @Override public int hashCode() {
        return m_state.hashCode();
    }

    public static DataCellSerializer<IdeaFlowStateCell> getCellSerializer() {
        return new Serializer();
    }

    public static final class Serializer implements DataCellSerializer<IdeaFlowStateCell> {
        @Override
        public void serialize(final IdeaFlowStateCell cell, final DataCellDataOutput output) throws IOException {
            output.writeInt(IdeaFlowState.VERSION);
            output.writeInt(cell.m_state.values().size());
            for (Map.Entry<String, String> entry : cell.m_state.values().entrySet()) {
                output.writeUTF(entry.getKey());
                output.writeUTF(entry.getValue());
            }
        }

        @Override
        public IdeaFlowStateCell deserialize(final DataCellDataInput input) throws IOException {
            final int version = input.readInt();
            if (version != IdeaFlowState.VERSION) {
                throw new IOException("Unsupported IdeaFlow state version: " + version);
            }
            final int count = input.readInt();
            if (count < 0 || count > 100_000) throw new IOException("Invalid IdeaFlow state entry count.");
            final Map<String, String> values = new LinkedHashMap<>();
            for (int index = 0; index < count; index++) values.put(input.readUTF(), input.readUTF());
            return new IdeaFlowStateCell(new IdeaFlowState(values));
        }
    }
}
