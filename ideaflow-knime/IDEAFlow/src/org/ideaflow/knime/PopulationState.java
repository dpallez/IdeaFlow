package org.ideaflow.knime;

import java.util.List;

import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.IdeaFlowStateCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;

/** Access to the compact public population-table contract. */
public final class PopulationState {
    public static final String COLUMN = "IdeaFlow state";
    public static final String NFE = "NFE";
    public static final String CONSTRAINT_VIOLATION = "Constraint violation";
    public static final String FEASIBLE = "Feasible";

    private PopulationState() { }

    public static DataColumnSpec column() {
        return new DataColumnSpecCreator(COLUMN, IdeaFlowStateCell.TYPE).createSpec();
    }

    public static DataTableSpec append(final DataTableSpec input) {
        return KnimeTableSupport.appendOrReplace(input, column());
    }

    public static IdeaFlowState get(final DataRow row, final DataTableSpec spec)
            throws InvalidSettingsException {
        final int index = requireIndex(spec);
        if (row.getCell(index) instanceof IdeaFlowStateCell cell) return cell.state();
        throw new InvalidSettingsException("Invalid or missing IdeaFlow state at " + row.getKey() + ".");
    }

    public static void set(final DataCell[] cells, final DataTableSpec spec, final IdeaFlowState state)
            throws InvalidSettingsException {
        cells[requireIndex(spec)] = new IdeaFlowStateCell(state);
    }

    public static int requireIndex(final DataTableSpec spec) throws InvalidSettingsException {
        final int index = spec.findColumnIndex(COLUMN);
        if (index < 0 || !IdeaFlowStateCell.TYPE.equals(spec.getColumnSpec(index).getType())) {
            throw new InvalidSettingsException(
                "Input must be an IdeaFlow population containing the '" + COLUMN + "' column.");
        }
        return index;
    }

    public static void require(final DataTableSpec spec) throws InvalidSettingsException {
        requireIndex(spec);
    }

    public static String run(final DataRow row, final DataTableSpec spec) throws InvalidSettingsException {
        return get(row, spec).text(IdeaFlowState.RUN, "run");
    }

    public static String population(final DataRow row, final DataTableSpec spec)
            throws InvalidSettingsException {
        return get(row, spec).text(IdeaFlowState.POPULATION, "population-0");
    }

    public static String individual(final DataRow row, final DataTableSpec spec)
            throws InvalidSettingsException {
        return get(row, spec).text(IdeaFlowState.INDIVIDUAL, row.getKey().getString());
    }

    public static long seed(final DataRow row, final DataTableSpec spec) throws InvalidSettingsException {
        return get(row, spec).longValue(IdeaFlowState.SEED, 0L);
    }

    public static long nfe(final DataRow row, final DataTableSpec spec) {
        final int index = spec.findColumnIndex(NFE);
        if (index >= 0 && !row.getCell(index).isMissing() && row.getCell(index) instanceof LongValue value) {
            return value.getLongValue();
        }
        return 0L;
    }

    public static void setNfe(final DataCell[] cells, final DataTableSpec spec, final long nfe) {
        final int index = spec.findColumnIndex(NFE);
        if (index >= 0) cells[index] = new LongCell(nfe);
    }

    public static String groupKey(final DataRow row, final DataTableSpec spec)
            throws InvalidSettingsException {
        return run(row, spec) + "\u0000" + population(row, spec);
    }

    public static void requireVisibleColumns(final DataTableSpec spec) throws InvalidSettingsException {
        require(spec);
        if (spec.findColumnIndex(NFE) < 0) {
            throw new InvalidSettingsException("IdeaFlow population is missing the NFE column.");
        }
    }

    public static List<String> technicalVisibleColumns() {
        return List.of(COLUMN, NFE, CONSTRAINT_VIOLATION, FEASIBLE);
    }
}
