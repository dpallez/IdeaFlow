package org.ideaflow.nodes.adaptation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.random.RandomGenerator;

import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.IdeaFlowStateCell;
import org.ideaflow.core.DeterministicRandom;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Updates DE control parameters inside the compact IdeaFlow state cell. */
final class AdaptiveParameterControllerNodeModel extends NodeModel {
    static final String CFG_MODE = "adaptation_mode";
    static final String CFG_F = "initial_f";
    static final String CFG_CR = "initial_cr";
    static final String CFG_TAU_F = "tau_f";
    static final String CFG_TAU_CR = "tau_cr";
    static final String CFG_MEMORY_SIZE = "memory_size";

    private final SettingsModelString mode = new SettingsModelString(CFG_MODE, "JDE");
    private final SettingsModelDoubleBounded initialF =
        new SettingsModelDoubleBounded(CFG_F, 0.5, 0.000001, 2);
    private final SettingsModelDoubleBounded initialCr =
        new SettingsModelDoubleBounded(CFG_CR, 0.9, 0, 1);
    private final SettingsModelDoubleBounded tauF =
        new SettingsModelDoubleBounded(CFG_TAU_F, 0.1, 0, 1);
    private final SettingsModelDoubleBounded tauCr =
        new SettingsModelDoubleBounded(CFG_TAU_CR, 0.1, 0, 1);
    private final SettingsModelIntegerBounded memorySize =
        new SettingsModelIntegerBounded(CFG_MEMORY_SIZE, 6, 1, 1000);

    AdaptiveParameterControllerNodeModel() { super(1, 1); }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
        validate(input[0]);
        return new DataTableSpec[]{input[0]};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] input,
            final ExecutionContext execution) throws Exception {
        final DataTableSpec spec = input[0].getDataTableSpec();
        validate(spec);
        final Map<String, List<DataRow>> groups = new LinkedHashMap<>();
        for (DataRow row : input[0]) {
            groups.computeIfAbsent(PopulationState.groupKey(row, spec), ignored -> new ArrayList<>()).add(row);
        }
        final BufferedDataContainer output = execution.createDataContainer(spec);
        int number = 0;
        for (List<DataRow> rows : groups.values()) {
            IdeaFlowState firstState = PopulationState.get(rows.get(0), spec);
            double[] memoryF = parseMemory(firstState.text(IdeaFlowState.SHADE_MEMORY_F, ""),
                initialF.getDoubleValue());
            double[] memoryCr = parseMemory(firstState.text(IdeaFlowState.SHADE_MEMORY_CR, ""),
                initialCr.getDoubleValue());
            int memoryIndex = Math.floorMod(
                firstState.intValue(IdeaFlowState.SHADE_MEMORY_INDEX, 0), memoryF.length);

            if ("SHADE".equals(mode.getStringValue())) {
                double numerator = 0;
                double denominator = 0;
                double crSum = 0;
                int successes = 0;
                for (DataRow row : rows) {
                    IdeaFlowState state = PopulationState.get(row, spec);
                    if (state.booleanValue(IdeaFlowState.DE_SUCCESS, false)) {
                        double successfulF = state.doubleValue(IdeaFlowState.DE_F, initialF.getDoubleValue());
                        double successfulCr = state.doubleValue(IdeaFlowState.DE_CR, initialCr.getDoubleValue());
                        numerator += successfulF * successfulF;
                        denominator += successfulF;
                        crSum += successfulCr;
                        successes++;
                    }
                }
                if (successes > 0 && denominator > 0) {
                    memoryF[memoryIndex] = numerator / denominator;
                    memoryCr[memoryIndex] = crSum / successes;
                    memoryIndex = (memoryIndex + 1) % memoryF.length;
                }
            }

            final String encodedF = encode(memoryF);
            final String encodedCr = encode(memoryCr);
            for (DataRow row : rows) {
                IdeaFlowState state = PopulationState.get(row, spec);
                final RandomGenerator random = DeterministicRandom.forScope(
                    PopulationState.seed(row, spec), PopulationState.run(row, spec),
                    PopulationState.population(row, spec), PopulationState.nfe(row, spec),
                    PopulationState.individual(row, spec), "adaptive-parameters");
                double f = state.doubleValue(IdeaFlowState.DE_F, initialF.getDoubleValue());
                double cr = state.doubleValue(IdeaFlowState.DE_CR, initialCr.getDoubleValue());
                switch (mode.getStringValue()) {
                    case "FIXED" -> {
                        f = initialF.getDoubleValue();
                        cr = initialCr.getDoubleValue();
                    }
                    case "JDE" -> {
                        if (random.nextDouble() < tauF.getDoubleValue()) f = 0.1 + 0.9 * random.nextDouble();
                        if (random.nextDouble() < tauCr.getDoubleValue()) cr = random.nextDouble();
                    }
                    case "SHADE" -> {
                        final int slot = random.nextInt(memoryF.length);
                        do {
                            f = memoryF[slot] + 0.1 * Math.tan(Math.PI * (random.nextDouble() - 0.5));
                        } while (f <= 0);
                        f = Math.min(1, f);
                        cr = Math.max(0, Math.min(1, memoryCr[slot] + 0.1 * random.nextGaussian()));
                    }
                    default -> throw new IllegalStateException("Unsupported adaptation mode");
                }
                state = state.with(IdeaFlowState.DE_F, f)
                    .with(IdeaFlowState.DE_CR, cr)
                    .with(IdeaFlowState.DE_SUCCESS, false)
                    .with(IdeaFlowState.SHADE_MEMORY_F, encodedF)
                    .with(IdeaFlowState.SHADE_MEMORY_CR, encodedCr)
                    .with(IdeaFlowState.SHADE_MEMORY_INDEX, memoryIndex);
                final DataCell[] cells = KnimeTableSupport.copyToSpec(row, spec, spec);
                cells[spec.findColumnIndex(PopulationState.COLUMN)] = new IdeaFlowStateCell(state);
                output.addRowToTable(new DefaultRow("Adaptive" + number++, cells));
            }
        }
        output.close();
        return new BufferedDataTable[]{output.getTable()};
    }

    private double[] initialMemory(final double value) {
        final double[] result = new double[memorySize.getIntValue()];
        Arrays.fill(result, value);
        return result;
    }

    private double[] parseMemory(final String encoded, final double fallback) {
        if (encoded.isBlank()) return initialMemory(fallback);
        final String[] parts = encoded.split(",");
        if (parts.length != memorySize.getIntValue()) return initialMemory(fallback);
        final double[] result = new double[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) result[i] = Double.parseDouble(parts[i]);
            return result;
        } catch (NumberFormatException exception) {
            return initialMemory(fallback);
        }
    }

    private static String encode(final double[] values) {
        final StringJoiner joiner = new StringJoiner(",");
        for (double value : values) joiner.add(Double.toString(value));
        return joiner.toString();
    }

    private void validate(final DataTableSpec spec) throws InvalidSettingsException {
        if (!List.of("FIXED", "JDE", "SHADE").contains(mode.getStringValue())) {
            throw new InvalidSettingsException("Unsupported adaptation mode.");
        }
        PopulationState.requireVisibleColumns(spec);
    }

    private SettingsModel[] models() {
        return new SettingsModel[]{mode, initialF, initialCr, tauF, tauCr, memorySize};
    }
    @Override protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (SettingsModel model : models()) model.saveSettingsTo(settings);
    }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (SettingsModel model : models()) model.validateSettings(settings);
    }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        for (SettingsModel model : models()) model.loadSettingsFrom(settings);
    }
    @Override protected void loadInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void saveInternals(final File directory, final ExecutionMonitor monitor)
            throws IOException, CanceledExecutionException { }
    @Override protected void reset() { }
}
