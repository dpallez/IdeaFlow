package org.ideaflow.nodes.variation.mutation;

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
import org.ideaflow.core.DeterministicRandom;
import org.ideaflow.core.ShadeMemory;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.ideaflow.nodes.variation.recombination.CrossoverNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/** Applies ordinary or Differential Evolution mutation to each candidate. */
final class MutationNodeModel extends NodeModel {
  static final String CFG_STRATEGY = "strategy";
  static final String CFG_AUTO_RATE = "auto_rate";
  static final String CFG_RATE = "mutation_rate";
  static final String CFG_SCALE = "gaussian_scale";
  static final String CFG_ETA = "distribution_index";
  static final String CFG_F = "differential_weight";
  static final String CFG_ADAPTATION = "adaptation_mode";
  static final String CFG_INITIAL_CR = "initial_cr";
  static final String CFG_TAU_F = "tau_f";
  static final String CFG_TAU_CR = "tau_cr";
  static final String CFG_MEMORY_SIZE = "memory_size";
  static final String CFG_REPAIR = "bounds_repair";

  private static final List<String> DE_STRATEGIES =
      List.of("DE_RAND_1", "DE_BEST_1", "DE_CURRENT_TO_BEST_1", "DE_CURRENT_TO_PBEST_1");

  private final SettingsModelString m_strategy =
      new SettingsModelString(CFG_STRATEGY, "POLYNOMIAL");
  private final SettingsModelBoolean m_autoRate = new SettingsModelBoolean(CFG_AUTO_RATE, true);
  private final SettingsModelDoubleBounded m_rate =
      new SettingsModelDoubleBounded(CFG_RATE, 0.1, 0, 1);
  private final SettingsModelDoubleBounded m_scale =
      new SettingsModelDoubleBounded(CFG_SCALE, 0.1, 0, Double.MAX_VALUE);
  private final SettingsModelDoubleBounded m_eta =
      new SettingsModelDoubleBounded(CFG_ETA, 20, 0.01, 10000);
  private final SettingsModelDoubleBounded m_f =
      new SettingsModelDoubleBounded(CFG_F, 0.5, Double.MIN_NORMAL, 2.0);
  private final SettingsModelString m_adaptation = new SettingsModelString(CFG_ADAPTATION, "FIXED");
  private final SettingsModelDoubleBounded m_initialCr =
      new SettingsModelDoubleBounded(CFG_INITIAL_CR, 0.9, 0, 1);
  private final SettingsModelDoubleBounded m_tauF =
      new SettingsModelDoubleBounded(CFG_TAU_F, 0.1, 0, 1);
  private final SettingsModelDoubleBounded m_tauCr =
      new SettingsModelDoubleBounded(CFG_TAU_CR, 0.1, 0, 1);
  private final SettingsModelIntegerBounded m_memorySize =
      new SettingsModelIntegerBounded(CFG_MEMORY_SIZE, 6, 1, 1000);
  private final SettingsModelString m_repair = new SettingsModelString(CFG_REPAIR, "REFLECT");

  MutationNodeModel() {
    super(1, 1);
  }

  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
    validate(input[0]);
    return new DataTableSpec[] {input[0]};
  }

  @Override
  protected BufferedDataTable[] execute(
      final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input[0].getDataTableSpec();
    validate(spec);
    return new BufferedDataTable[] {
      isDifferential()
          ? differentialMutation(input[0], execution)
          : ordinaryMutation(input[0], execution)
    };
  }

  // Ordinary mutation changes the row values directly; DE mutation instead consumes donor vectors
  // from state.
  private BufferedDataTable ordinaryMutation(
      final BufferedDataTable input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input.getDataTableSpec();
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final VariableMetadata metadata = variables(problem, false);
    final List<String> names = metadata.names();
    final List<String> results = evaluationResults(problem);
    final int[] variableIndices = KnimeTableSupport.requireNumericColumns(spec, names);
    final BufferedDataContainer output = execution.createDataContainer(spec);
    final double mutationRate =
        m_autoRate.getBooleanValue() ? 1.0 / names.size() : m_rate.getDoubleValue();
    int rowNumber = 0;
    for (List<DataRow> group : groups(input).values()) {
      final RandomGenerator random = random(group.get(0), spec, "mutation");
      for (DataRow row : group) {
        final DataCell[] cells = KnimeTableSupport.copyToSpec(row, spec, spec);
        boolean changed = false;
        for (int variable = 0; variable < variableIndices.length; variable++) {
          if (random.nextDouble() < mutationRate) {
            final double current =
                KnimeTableSupport.number(
                    row.getCell(variableIndices[variable]), row, names.get(variable));
            final double mutated =
                mutate(current, metadata.lows()[variable], metadata.highs()[variable], random);
            cells[variableIndices[variable]] =
                CrossoverNodeModel.numericCell(
                    spec.getColumnSpec(variableIndices[variable]).getType(), mutated);
            changed = true;
          }
        }
        if (changed) CrossoverNodeModel.invalidate(cells, spec, results);
        output.addRowToTable(new DefaultRow("Mutant" + rowNumber++, cells));
        execution.checkCanceled();
      }
    }
    output.close();
    return output.getTable();
  }

  // Keep each DE target with its own adaptive parameters and donor vectors.
  private BufferedDataTable differentialMutation(
      final BufferedDataTable input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input.getDataTableSpec();
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final VariableMetadata metadata = variables(problem, true);
    final int[] variableIndices = KnimeTableSupport.requireNumericColumns(spec, metadata.names());
    final BufferedDataContainer output = execution.createDataContainer(spec);
    int rowNumber = 0;
    for (List<DataRow> group : groups(input).values()) {
      final RandomGenerator random = random(group.get(0), spec, "de-mutation");
      final ShadeMemory.State memory = updateMemory(group, spec);
      for (DataRow row : group) {
        IdeaFlowState state = adapt(PopulationState.get(row, spec), row, spec, memory);
        final double[] target = requiredVector(state, IdeaFlowState.DE_TARGET_VECTOR);
        final double[] difference1 = requiredVector(state, IdeaFlowState.DE_DIFFERENCE_1_VECTOR);
        final double[] difference2 =
            "DE_CURRENT_TO_PBEST_1".equals(m_strategy.getStringValue())
                ? requiredVector(state, IdeaFlowState.DE_PBEST_DIFFERENCE_2_VECTOR)
                : requiredVector(state, IdeaFlowState.DE_DIFFERENCE_2_VECTOR);
        final double[] anchor =
            switch (m_strategy.getStringValue()) {
              case "DE_RAND_1" -> requiredVector(state, IdeaFlowState.DE_RANDOM_BASE_VECTOR);
              case "DE_BEST_1", "DE_CURRENT_TO_BEST_1" ->
                  requiredVector(state, IdeaFlowState.DE_BEST_VECTOR);
              case "DE_CURRENT_TO_PBEST_1" -> requiredVector(state, IdeaFlowState.DE_PBEST_VECTOR);
              default ->
                  throw new IllegalStateException("Unsupported differential mutation strategy.");
            };
        final double weight = state.doubleValue(IdeaFlowState.DE_F, m_f.getDoubleValue());
        final double[] mutant = new double[target.length];
        for (int dimension = 0; dimension < mutant.length; dimension++) {
          mutant[dimension] =
              switch (m_strategy.getStringValue()) {
                case "DE_RAND_1", "DE_BEST_1" ->
                    anchor[dimension] + weight * (difference1[dimension] - difference2[dimension]);
                case "DE_CURRENT_TO_BEST_1", "DE_CURRENT_TO_PBEST_1" ->
                    target[dimension]
                        + weight * (anchor[dimension] - target[dimension])
                        + weight * (difference1[dimension] - difference2[dimension]);
                default ->
                    throw new IllegalStateException("Unsupported differential mutation strategy.");
              };
          mutant[dimension] =
              repair(
                  mutant[dimension],
                  metadata.lows()[dimension],
                  metadata.highs()[dimension],
                  random);
        }
        final DataCell[] cells = KnimeTableSupport.copyToSpec(row, spec, spec);
        for (int dimension = 0; dimension < mutant.length; dimension++) {
          cells[variableIndices[dimension]] =
              CrossoverNodeModel.numericCell(
                  spec.getColumnSpec(variableIndices[dimension]).getType(), mutant[dimension]);
        }
        CrossoverNodeModel.invalidate(cells, spec, evaluationResults(problem));
        PopulationState.set(
            cells,
            spec,
            state.with(IdeaFlowState.EVALUATED, false).without(IdeaFlowState.EVALUATION));
        output.addRowToTable(new DefaultRow("DEMutant" + rowNumber++, cells));
        execution.checkCanceled();
      }
    }
    output.close();
    return output.getTable();
  }

  // SHADE learns only from successful trials recorded by the previous Elitism pass.
  private ShadeMemory.State updateMemory(final List<DataRow> rows, final DataTableSpec spec)
      throws InvalidSettingsException {
    final IdeaFlowState first = PopulationState.get(rows.get(0), spec);
    final double[] memoryF =
        parseMemory(first.text(IdeaFlowState.SHADE_MEMORY_F, ""), m_f.getDoubleValue());
    final double[] memoryCr =
        parseMemory(first.text(IdeaFlowState.SHADE_MEMORY_CR, ""), m_initialCr.getDoubleValue());
    final ShadeMemory.State current =
        new ShadeMemory.State(
            memoryF, memoryCr, first.intValue(IdeaFlowState.SHADE_MEMORY_INDEX, 0));
    if (!"SHADE".equals(m_adaptation.getStringValue())) return current;

    final List<ShadeMemory.Success> successes = new ArrayList<>();
    for (DataRow row : rows) {
      final IdeaFlowState state = PopulationState.get(row, spec);
      final double improvement = state.doubleValue(IdeaFlowState.DE_IMPROVEMENT, 0.0);
      if (state.booleanValue(IdeaFlowState.DE_SUCCESS, false) && improvement > 0.0) {
        successes.add(
            new ShadeMemory.Success(
                state.doubleValue(IdeaFlowState.DE_F, m_f.getDoubleValue()),
                state.doubleValue(IdeaFlowState.DE_CR, m_initialCr.getDoubleValue()),
                improvement));
      }
    }
    return ShadeMemory.update(current, successes);
  }

  // Parameter adaptation updates state before the mutant vector is calculated.
  private IdeaFlowState adapt(
      final IdeaFlowState original,
      final DataRow row,
      final DataTableSpec spec,
      final ShadeMemory.State memory)
      throws InvalidSettingsException {
    final RandomGenerator random =
        DeterministicRandom.forScope(
            PopulationState.seed(row, spec), PopulationState.run(row, spec),
            PopulationState.population(row, spec), PopulationState.nfe(row, spec),
            PopulationState.individual(row, spec), "adaptive-parameters");
    double f = original.doubleValue(IdeaFlowState.DE_F, m_f.getDoubleValue());
    double cr = original.doubleValue(IdeaFlowState.DE_CR, m_initialCr.getDoubleValue());
    switch (m_adaptation.getStringValue()) {
      case "FIXED" -> {
        f = m_f.getDoubleValue();
        cr = m_initialCr.getDoubleValue();
      }
      case "JDE" -> {
        if (random.nextDouble() < m_tauF.getDoubleValue()) {
          f = 0.1 + 0.9 * random.nextDouble();
        }
        if (random.nextDouble() < m_tauCr.getDoubleValue()) {
          cr = random.nextDouble();
        }
      }
      case "SHADE" -> {
        final int slot = random.nextInt(memory.f().length);
        do {
          f = memory.f()[slot] + 0.1 * Math.tan(Math.PI * (random.nextDouble() - 0.5));
        } while (f <= 0);
        f = Math.min(1, f);
        cr = Math.max(0, Math.min(1, memory.cr()[slot] + 0.1 * random.nextGaussian()));
      }
      default -> throw new IllegalStateException("Unsupported DE parameter control.");
    }
    return original
        .with(IdeaFlowState.DE_F, f)
        .with(IdeaFlowState.DE_CR, cr)
        .with(IdeaFlowState.DE_SUCCESS, false)
        .without(IdeaFlowState.DE_IMPROVEMENT)
        .with(IdeaFlowState.SHADE_MEMORY_F, encode(memory.f()))
        .with(IdeaFlowState.SHADE_MEMORY_CR, encode(memory.cr()))
        .with(IdeaFlowState.SHADE_MEMORY_INDEX, memory.index());
  }

  private double[] parseMemory(final String encoded, final double fallback) {
    if (encoded.isBlank()) return initialMemory(fallback);
    final String[] parts = encoded.split(",");
    if (parts.length != m_memorySize.getIntValue()) return initialMemory(fallback);
    final double[] result = new double[parts.length];
    try {
      for (int index = 0; index < parts.length; index++) {
        result[index] = Double.parseDouble(parts[index]);
      }
      return result;
    } catch (NumberFormatException exception) {
      return initialMemory(fallback);
    }
  }

  private double[] initialMemory(final double value) {
    final double[] result = new double[m_memorySize.getIntValue()];
    Arrays.fill(result, value);
    return result;
  }

  private static String encode(final double[] values) {
    final StringJoiner joiner = new StringJoiner(",");
    for (double value : values) joiner.add(Double.toString(value));
    return joiner.toString();
  }

  private double mutate(
      final double current, final double low, final double high, final RandomGenerator random) {
    return switch (m_strategy.getStringValue()) {
      case "BIT_FLIP" -> current == 0 ? 1 : 0;
      case "RANDOM_RESET" -> low + random.nextDouble() * (high - low);
      case "GAUSSIAN" ->
          reflect(
              current + random.nextGaussian() * m_scale.getDoubleValue() * (high - low), low, high);
      case "POLYNOMIAL" -> polynomial(current, low, high, random);
      default -> throw new IllegalStateException("Unsupported mutation strategy");
    };
  }

  private double polynomial(
      final double current, final double low, final double high, final RandomGenerator random) {
    if (high <= low) return current;
    final double value = Math.max(low, Math.min(high, current));
    final double delta1 = (value - low) / (high - low);
    final double delta2 = (high - value) / (high - low);
    final double sample = random.nextDouble();
    final double power = 1.0 / (m_eta.getDoubleValue() + 1);
    final double delta;
    if (sample <= 0.5) {
      final double distance = 1 - delta1;
      final double term =
          2 * sample + (1 - 2 * sample) * Math.pow(distance, m_eta.getDoubleValue() + 1);
      delta = Math.pow(term, power) - 1;
    } else {
      final double distance = 1 - delta2;
      final double term =
          2 * (1 - sample) + 2 * (sample - 0.5) * Math.pow(distance, m_eta.getDoubleValue() + 1);
      delta = 1 - Math.pow(term, power);
    }
    return Math.max(low, Math.min(high, value + delta * (high - low)));
  }

  // Bounds repair is deliberately applied after mutation so every emitted candidate is valid.
  private double repair(
      final double value, final double low, final double high, final RandomGenerator random) {
    if (value >= low && value <= high) return value;
    return switch (m_repair.getStringValue()) {
      case "CLAMP" -> Math.max(low, Math.min(high, value));
      case "RANDOM" -> random.nextDouble(low, Math.nextUp(high));
      case "REFLECT" -> reflect(value, low, high);
      default -> throw new IllegalStateException("Unsupported bounds repair strategy.");
    };
  }

  private static double reflect(double value, final double low, final double high) {
    if (high == low) return low;
    final double width = high - low;
    double offset = (value - low) % (2.0 * width);
    if (offset < 0) offset += 2.0 * width;
    return offset <= width ? low + offset : high - (offset - width);
  }

  private static List<String> evaluationResults(final ProblemMetadata.Schema problem) {
    final List<String> results = new ArrayList<>(problem.objectiveNames());
    problem.constraints().forEach(constraint -> results.add(constraint.column()));
    return results;
  }

  private record VariableMetadata(List<String> names, double[] lows, double[] highs) {}

  private static VariableMetadata variables(
      final ProblemMetadata.Schema problem, final boolean directFloatOnly)
      throws InvalidSettingsException {
    final List<String> names = new ArrayList<>();
    final List<Double> lows = new ArrayList<>();
    final List<Double> highs = new ArrayList<>();
    for (ProblemMetadata.Variable variable : problem.variables()) {
      if (directFloatOnly && (variable.encoded() || !"REAL".equalsIgnoreCase(variable.type())))
        continue;
      for (String column : variable.populationColumns()) {
        names.add(column);
        lows.add(variable.encoded() ? 0.0 : variable.lower());
        highs.add(variable.encoded() ? 1.0 : variable.upper());
      }
    }
    if (names.isEmpty()) {
      throw new InvalidSettingsException(
          "Differential mutation requires direct Float variables in Problem Setup.");
    }
    final double[] lower = new double[lows.size()];
    final double[] upper = new double[highs.size()];
    for (int index = 0; index < lower.length; index++) {
      lower[index] = lows.get(index);
      upper[index] = highs.get(index);
    }
    return new VariableMetadata(List.copyOf(names), lower, upper);
  }

  private static Map<String, List<DataRow>> groups(final BufferedDataTable table)
      throws InvalidSettingsException {
    final DataTableSpec spec = table.getDataTableSpec();
    final Map<String, List<DataRow>> groups = new LinkedHashMap<>();
    for (DataRow row : table) {
      groups
          .computeIfAbsent(PopulationState.groupKey(row, spec), ignored -> new ArrayList<>())
          .add(row);
    }
    return groups;
  }

  private static RandomGenerator random(
      final DataRow row, final DataTableSpec spec, final String scope)
      throws InvalidSettingsException {
    return DeterministicRandom.forScope(
        PopulationState.seed(row, spec),
        PopulationState.run(row, spec),
        PopulationState.population(row, spec),
        PopulationState.nfe(row, spec),
        scope);
  }

  private static double[] requiredVector(final IdeaFlowState state, final String key)
      throws InvalidSettingsException {
    final double[] vector = state.vector(key);
    if (vector.length == 0) {
      throw new InvalidSettingsException(
          "Differential mutation requires Selection in Differential Evolution donor mode.");
    }
    return vector;
  }

  private boolean isDifferential() {
    return DE_STRATEGIES.contains(m_strategy.getStringValue());
  }

  private void validate(final DataTableSpec spec) throws InvalidSettingsException {
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final VariableMetadata metadata = variables(problem, isDifferential());
    KnimeTableSupport.requireNumericColumns(spec, metadata.names());
    for (String objective : problem.objectiveNames()) {
      if (spec.findColumnIndex(objective) < 0) {
        throw new InvalidSettingsException("Missing objective column: " + objective);
      }
    }
    if (!List.of(
            "POLYNOMIAL",
            "GAUSSIAN",
            "BIT_FLIP",
            "RANDOM_RESET",
            "DE_RAND_1",
            "DE_BEST_1",
            "DE_CURRENT_TO_BEST_1",
            "DE_CURRENT_TO_PBEST_1")
        .contains(m_strategy.getStringValue())) {
      throw new InvalidSettingsException("Unsupported mutation strategy.");
    }
    if (isDifferential()) {
      if (!List.of("REFLECT", "CLAMP", "RANDOM").contains(m_repair.getStringValue())) {
        throw new InvalidSettingsException("Unsupported bounds repair strategy.");
      }
      if (!List.of("FIXED", "JDE", "SHADE").contains(m_adaptation.getStringValue())) {
        throw new InvalidSettingsException("Unsupported DE parameter control.");
      }
    }
    for (int index = 0; index < metadata.names().size(); index++) {
      final String name = metadata.names().get(index);
      if (metadata.lows()[index] >= metadata.highs()[index]) {
        throw new InvalidSettingsException("Invalid bounds for " + name);
      }
      if ("BIT_FLIP".equals(m_strategy.getStringValue())
          && !spec.getColumnSpec(name).getType().isCompatible(IntValue.class)) {
        throw new InvalidSettingsException("Bit flip requires Binary variables.");
      }
      if (("POLYNOMIAL".equals(m_strategy.getStringValue())
              || "GAUSSIAN".equals(m_strategy.getStringValue())
              || isDifferential())
          && !spec.getColumnSpec(name).getType().isCompatible(DoubleValue.class)) {
        throw new InvalidSettingsException(
            m_strategy.getStringValue() + " requires Float variables.");
      }
    }
    PopulationState.requireVisibleColumns(spec);
  }

  private SettingsModel[] models() {
    return new SettingsModel[] {
      m_strategy,
      m_autoRate,
      m_rate,
      m_scale,
      m_eta,
      m_f,
      m_adaptation,
      m_initialCr,
      m_tauF,
      m_tauCr,
      m_memorySize,
      m_repair
    };
  }

  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    for (SettingsModel model : models()) model.saveSettingsTo(settings);
  }

  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    for (SettingsModel model : models()) model.validateSettings(settings);
  }

  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    for (SettingsModel model : models()) model.loadSettingsFrom(settings);
  }

  @Override
  protected void loadInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}

  @Override
  protected void saveInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}

  @Override
  protected void reset() {}
}
