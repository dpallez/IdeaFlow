package org.ideaflow.nodes.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.OptimizationSummary;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Reduces accumulated per-checkpoint histories to plot-ready convergence, ECDF, and per-run tables.
 */
final class OptimizationRunAnalysisNodeModel extends NodeModel {
    static final String CFG_SERIES = "series_column";
    static final String CFG_RUN = "run_column";
    static final String CFG_NFE = "nfe_column";
    static final String CFG_VALUE = "performance_column";
    static final String CFG_DIRECTION = "performance_direction";
    static final String CFG_TARGET = "target";
    static final String CFG_CARRY_FORWARD = "carry_forward";

    private final SettingsModelString m_series =
        new SettingsModelString(CFG_SERIES, OptimizationSummary.SERIES);
    private final SettingsModelString m_run =
        new SettingsModelString(CFG_RUN, OptimizationSummary.RUN);
    private final SettingsModelString m_nfe =
        new SettingsModelString(CFG_NFE, PopulationState.NFE);
    private final SettingsModelString m_value =
        new SettingsModelString(CFG_VALUE, OptimizationSummary.BEST);
    private final SettingsModelString m_direction =
        new SettingsModelString(CFG_DIRECTION, "AUTO");
    private final SettingsModelDouble m_target = new SettingsModelDouble(CFG_TARGET, 0.1);
    private final SettingsModelBoolean m_carryForward =
        new SettingsModelBoolean(CFG_CARRY_FORWARD, true);

    /*
     * Seed is part of repeated-run identity. Called workflows can retain a stable run ID
     * while a loop overrides only the seed; omitting it merges independent trajectories.
     */
    private record Key(String series, String run, String population, long seed) { }

    private static final class Trajectory {
        private final Key key;
        private final long seed;
        private final String problem;
        private final TreeMap<Long, Double> values = new TreeMap<>();

        Trajectory(final Key key, final long seed, final String problem) {
            this.key = key;
            this.seed = seed;
            this.problem = problem;
        }
    }

    OptimizationRunAnalysisNodeModel() {
        super(1, 3);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validate(input[0]);
        return new DataTableSpec[]{convergenceSpec(), ecdfSpec(), runSpec()};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] input, final ExecutionContext execution)
            throws Exception {
        final DataTableSpec spec = input[0].getDataTableSpec();
        validate(spec);
        final Map<Key, Trajectory> trajectories = read(input[0], spec, execution);
        if (trajectories.isEmpty()) {
            throw new InvalidSettingsException("The optimization history contains no trajectories.");
        }
        final OptimizationDirection direction = direction(spec);
        final BufferedDataContainer convergence = execution.createDataContainer(convergenceSpec());
        final BufferedDataContainer ecdf = execution.createDataContainer(ecdfSpec());
        final BufferedDataContainer runs = execution.createDataContainer(runSpec());
        writeResults(trajectories, direction, convergence, ecdf, runs, execution);
        convergence.close();
        ecdf.close();
        runs.close();
        return new BufferedDataTable[]{convergence.getTable(), ecdf.getTable(), runs.getTable()};
    }

    private Map<Key, Trajectory> read(final BufferedDataTable input, final DataTableSpec spec,
            final ExecutionContext execution) throws Exception {
        final Map<Key, Trajectory> result = new TreeMap<>((left, right) -> {
            int compared = left.series.compareTo(right.series);
            if (compared == 0) compared = left.run.compareTo(right.run);
            if (compared == 0) compared = left.population.compareTo(right.population);
            if (compared == 0) compared = Long.compare(left.seed, right.seed);
            return compared;
        });
        final int seriesIndex = seriesIndex(spec);
        final int runIndex = spec.findColumnIndex(m_run.getStringValue());
        final int nfeIndex = spec.findColumnIndex(m_nfe.getStringValue());
        final int valueIndex = spec.findColumnIndex(m_value.getStringValue());
        final int populationIndex = spec.findColumnIndex(OptimizationSummary.POPULATION);
        final int seedIndex = spec.findColumnIndex(OptimizationSummary.SEED);
        final int problemIndex = spec.findColumnIndex(OptimizationSummary.PROBLEM);
        final String metadataProblem = metadataProblem(spec);
        for (DataRow row : input) {
            final String population = optionalText(row, populationIndex, "population-0");
            final long seed = optionalLong(row, seedIndex, 0L);
            final Key key = new Key(text(row, seriesIndex), text(row, runIndex), population, seed);
            final String problem = optionalText(row, problemIndex, metadataProblem);
            final Trajectory trajectory =
                result.computeIfAbsent(key, ignored -> new Trajectory(key, seed, problem));
            final long nfe = longNumber(row.getCell(nfeIndex), row, m_nfe.getStringValue());
            final DataCell value = row.getCell(valueIndex);
            if (!value.isMissing()) {
                trajectory.values.put(nfe,
                    KnimeTableSupport.number(value, row, m_value.getStringValue()));
            }
            execution.checkCanceled();
        }
        return result;
    }

    private void writeResults(final Map<Key, Trajectory> all, final OptimizationDirection direction,
            final BufferedDataContainer convergence, final BufferedDataContainer ecdf,
            final BufferedDataContainer runs, final ExecutionContext execution) throws Exception {
        final Map<String, List<Trajectory>> bySeries = new TreeMap<>();
        for (Trajectory trajectory : all.values()) {
            bySeries.computeIfAbsent(trajectory.key.series, ignored -> new ArrayList<>()).add(trajectory);
        }
        int convergenceRow = 0;
        int ecdfRow = 0;
        int runRow = 0;
        for (Map.Entry<String, List<Trajectory>> seriesEntry : bySeries.entrySet()) {
            final String series = seriesEntry.getKey();
            final List<Trajectory> trajectories = seriesEntry.getValue();
            final TreeSet<Long> checkpoints = new TreeSet<>();
            for (Trajectory trajectory : trajectories) {
                checkpoints.addAll(trajectory.values.keySet());
            }
            for (long checkpoint : checkpoints) {
                final double[] values = valuesAt(trajectories, checkpoint);
                if (values.length == 0) continue;
                Arrays.sort(values);
                final double mean = Arrays.stream(values).average().orElse(Double.NaN);
                double squared = 0.0;
                for (double value : values) squared += (value - mean) * (value - mean);
                final double standardDeviation =
                    values.length < 2 ? 0.0 : Math.sqrt(squared / (values.length - 1));
                convergence.addRowToTable(new DefaultRow("Convergence-" + convergenceRow++,
                    new DataCell[]{new StringCell(series), new LongCell(checkpoint),
                        new IntCell(values.length), new DoubleCell(quantile(values, 0.5)),
                        new DoubleCell(quantile(values, 0.25)), new DoubleCell(quantile(values, 0.75)),
                        new DoubleCell(mean), new DoubleCell(standardDeviation)}));
                execution.checkCanceled();
            }

            final TreeMap<Double, Integer> finalFitnessCounts = new TreeMap<>();
            for (Trajectory trajectory : trajectories) {
                final Long firstHit = firstHit(trajectory, direction);
                final Map.Entry<Long, Double> last = trajectory.values.lastEntry();
                if (last != null && Double.isFinite(last.getValue())) {
                    finalFitnessCounts.merge(last.getValue(), 1, Integer::sum);
                }
                runs.addRowToTable(new DefaultRow("Run-" + runRow++, new DataCell[]{
                    new StringCell(series), new StringCell(trajectory.problem),
                    new StringCell(trajectory.key.run), new StringCell(trajectory.key.population),
                    new LongCell(trajectory.seed),
                    last == null ? missing() : new LongCell(last.getKey()),
                    last == null ? missing() : new DoubleCell(last.getValue()),
                    BooleanCell.get(firstHit != null),
                    firstHit == null ? missing() : new LongCell(firstHit)}));
            }
            int atOrBelow = 0;
            final int validRuns = finalFitnessCounts.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<Double, Integer> fitness : finalFitnessCounts.entrySet()) {
                atOrBelow += fitness.getValue();
                ecdfRow = addEcdf(ecdf, ecdfRow, series, fitness.getKey(), atOrBelow, validRuns);
            }
        }
    }

    private double[] valuesAt(final List<Trajectory> trajectories, final long checkpoint) {
        final double[] buffer = new double[trajectories.size()];
        int count = 0;
        for (Trajectory trajectory : trajectories) {
            final Map.Entry<Long, Double> entry = m_carryForward.getBooleanValue()
                ? trajectory.values.floorEntry(checkpoint) : trajectory.values.ceilingEntry(checkpoint);
            if (entry != null && (m_carryForward.getBooleanValue() || entry.getKey() == checkpoint)) {
                buffer[count++] = entry.getValue();
            }
        }
        return Arrays.copyOf(buffer, count);
    }

    private Long firstHit(final Trajectory trajectory, final OptimizationDirection direction) {
        for (Map.Entry<Long, Double> entry : trajectory.values.entrySet()) {
            final boolean reached = direction == OptimizationDirection.MINIMIZE
                ? entry.getValue() <= m_target.getDoubleValue()
                : entry.getValue() >= m_target.getDoubleValue();
            if (reached) return entry.getKey();
        }
        return null;
    }

    private static int addEcdf(final BufferedDataContainer output, final int row, final String series,
            final double fitness, final int atOrBelow, final int total) {
        output.addRowToTable(new DefaultRow("ECDF-" + row, new DataCell[]{
            new StringCell(series), new DoubleCell(fitness), new IntCell(atOrBelow), new IntCell(total),
            new DoubleCell(total == 0 ? 0.0 : (double)atOrBelow / total)}));
        return row + 1;
    }

    private OptimizationDirection direction(final DataTableSpec spec) {
        if ("MINIMIZE".equals(m_direction.getStringValue())) return OptimizationDirection.MINIMIZE;
        if ("MAXIMIZE".equals(m_direction.getStringValue())) return OptimizationDirection.MAXIMIZE;
        if (OptimizationSummary.HYPERVOLUME.equals(m_value.getStringValue())) {
            return OptimizationDirection.MAXIMIZE;
        }
        try {
            final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
            return problem.objectives().get(0).direction();
        } catch (InvalidSettingsException | IndexOutOfBoundsException exception) {
            return OptimizationDirection.MINIMIZE;
        }
    }

    private void validate(final DataTableSpec spec) throws InvalidSettingsException {
        seriesIndex(spec);
        requireText(spec, m_run.getStringValue());
        KnimeTableSupport.requireNumericColumns(spec,
            List.of(m_nfe.getStringValue(), m_value.getStringValue()));
        final String direction = m_direction.getStringValue();
        if (!List.of("AUTO", "MINIMIZE", "MAXIMIZE").contains(direction)) {
            throw new InvalidSettingsException("Direction must be AUTO, MINIMIZE, or MAXIMIZE.");
        }
        if (!Double.isFinite(m_target.getDoubleValue())) {
            throw new InvalidSettingsException("Target must be finite.");
        }
    }

    private int seriesIndex(final DataTableSpec spec) throws InvalidSettingsException {
        int index = spec.findColumnIndex(m_series.getStringValue());
        if (index < 0 && OptimizationSummary.SERIES.equals(m_series.getStringValue())) {
            index = spec.findColumnIndex(OptimizationSummary.POPULATION);
        }
        if (index < 0) {
            throw new InvalidSettingsException("Missing series column: " + m_series.getStringValue());
        }
        return index;
    }

    private static void requireText(final DataTableSpec spec, final String name)
            throws InvalidSettingsException {
        final int index = spec.findColumnIndex(name);
        if (index < 0 || !StringValue.class.isAssignableFrom(
                spec.getColumnSpec(index).getType().getPreferredValueClass())) {
            throw new InvalidSettingsException("Missing or non-text column: " + name);
        }
    }

    private static String text(final DataRow row, final int index) throws InvalidSettingsException {
        if (row.getCell(index).isMissing()) {
            throw new InvalidSettingsException("A required analysis grouping value is missing at " + row.getKey());
        }
        return row.getCell(index).toString();
    }

    private static String optionalText(final DataRow row, final int index, final String fallback) {
        return index < 0 || row.getCell(index).isMissing() ? fallback : row.getCell(index).toString();
    }

    private static long optionalLong(final DataRow row, final int index, final long fallback)
            throws InvalidSettingsException {
        return index < 0 || row.getCell(index).isMissing()
            ? fallback : longNumber(row.getCell(index), row, OptimizationSummary.SEED);
    }

    private static long longNumber(final DataCell cell, final DataRow row, final String column)
            throws InvalidSettingsException {
        if (cell instanceof LongValue value) return value.getLongValue();
        if (cell instanceof IntValue value) return value.getIntValue();
        if (cell instanceof DoubleValue value) return Math.round(value.getDoubleValue());
        throw new InvalidSettingsException("Missing or non-numeric value in '" + column + "' at " + row.getKey());
    }

    private static String metadataProblem(final DataTableSpec spec) {
        try {
            return ProblemMetadata.require(spec).problemId();
        } catch (InvalidSettingsException exception) {
            return "problem";
        }
    }

    private static double quantile(final double[] sorted, final double probability) {
        if (sorted.length == 1) return sorted[0];
        final double position = probability * (sorted.length - 1);
        final int lower = (int)Math.floor(position);
        final int upper = (int)Math.ceil(position);
        final double fraction = position - lower;
        return sorted[lower] * (1.0 - fraction) + sorted[upper] * fraction;
    }

    private static DataCell missing() {
        return org.knime.core.data.DataType.getMissingCell();
    }

    static DataTableSpec convergenceSpec() {
        return new DataTableSpec(KnimeTableSupport.stringColumn(OptimizationSummary.SERIES),
            KnimeTableSupport.longColumn(PopulationState.NFE), KnimeTableSupport.intColumn("Runs"),
            KnimeTableSupport.doubleColumn("Median"), KnimeTableSupport.doubleColumn("Q1"),
            KnimeTableSupport.doubleColumn("Q3"), KnimeTableSupport.doubleColumn("Mean"),
            KnimeTableSupport.doubleColumn("Standard deviation"));
    }

    static DataTableSpec ecdfSpec() {
        return new DataTableSpec(KnimeTableSupport.stringColumn(OptimizationSummary.SERIES),
            KnimeTableSupport.doubleColumn("Fitness"), KnimeTableSupport.intColumn("Runs at or below"),
            KnimeTableSupport.intColumn("Total runs"), KnimeTableSupport.doubleColumn("ECDF"));
    }

    static DataTableSpec runSpec() {
        return new DataTableSpec(KnimeTableSupport.stringColumn(OptimizationSummary.SERIES),
            KnimeTableSupport.stringColumn(OptimizationSummary.PROBLEM),
            KnimeTableSupport.stringColumn(OptimizationSummary.RUN),
            KnimeTableSupport.stringColumn(OptimizationSummary.POPULATION),
            KnimeTableSupport.longColumn(OptimizationSummary.SEED),
            KnimeTableSupport.longColumn("Final NFE"), KnimeTableSupport.doubleColumn("Final value"),
            new DataColumnSpecCreator("Success", BooleanCell.TYPE).createSpec(),
            KnimeTableSupport.longColumn("First hit NFE"));
    }

    private org.knime.core.node.defaultnodesettings.SettingsModel[] models() {
        return new org.knime.core.node.defaultnodesettings.SettingsModel[]{
            m_series, m_run, m_nfe, m_value, m_direction, m_target, m_carryForward};
    }

    @Override protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (var model : models()) model.saveSettingsTo(settings);
    }

    @Override protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        for (var model : models()) model.validateSettings(settings);
    }

    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        for (var model : models()) model.loadSettingsFrom(settings);
    }

    @Override protected void reset() { }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
        throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
        throws IOException, CanceledExecutionException { }
}
