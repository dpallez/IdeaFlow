package org.ideaflow.nodes.selection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.api.ReservedColumns;
import org.ideaflow.core.CrowdingDistance;
import org.ideaflow.core.DeterministicRandom;
import org.ideaflow.core.FastNonDominatedSort;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
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
import org.knime.core.node.port.PortType;

/** Selects mating parents or builds the donor state required by DE mutation. */
final class SelectionNodeModel extends NodeModel {
  static final String CFG_MODE = "selection_strategy";
  static final String CFG_TOURNAMENT_SIZE = "tournament_size";
  static final String CFG_PARENT_COUNT = "parent_count";
  static final String CFG_WITH_REPLACEMENT = "with_replacement";
  static final String CFG_PBEST = "pbest_rate";

  private final SettingsModelString m_mode = new SettingsModelString(CFG_MODE, "TOURNAMENT");
  private final SettingsModelIntegerBounded m_tournamentSize =
      new SettingsModelIntegerBounded(CFG_TOURNAMENT_SIZE, 2, 1, Integer.MAX_VALUE);
  private final SettingsModelIntegerBounded m_parentCount =
      new SettingsModelIntegerBounded(CFG_PARENT_COUNT, 100, 1, Integer.MAX_VALUE);
  private final SettingsModelBoolean m_withReplacement =
      new SettingsModelBoolean(CFG_WITH_REPLACEMENT, true);
  private final SettingsModelDoubleBounded m_pbest =
      new SettingsModelDoubleBounded(CFG_PBEST, 0.2, Double.MIN_NORMAL, 1.0);

  SelectionNodeModel() {
    super(
        new PortType[] {BufferedDataTable.TYPE, BufferedDataTable.TYPE_OPTIONAL},
        new PortType[] {BufferedDataTable.TYPE});
  }

  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
    validate(input[0]);
    if (input[1] != null && "DE_DONORS".equals(m_mode.getStringValue())) {
      validateArchive(input[1], input[0]);
    }
    return new DataTableSpec[] {input[0]};
  }

  @Override
  protected BufferedDataTable[] execute(
      final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input[0].getDataTableSpec();
    validate(spec);
    if ("DE_DONORS".equals(m_mode.getStringValue())) {
      return new BufferedDataTable[] {selectDifferentialDonors(input[0], input[1], execution)};
    }
    return new BufferedDataTable[] {selectMatingParents(input[0], execution)};
  }

  // GA-style selection emits parent rows; replacement controls whether the same row can be chosen
  // twice.
  private BufferedDataTable selectMatingParents(
      final BufferedDataTable input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input.getDataTableSpec();
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final int[] objectives =
        KnimeTableSupport.requireNumericColumns(spec, problem.objectiveNames());
    final List<OptimizationDirection> directions =
        problem.objectives().stream().map(ProblemMetadata.Objective::direction).toList();
    final BufferedDataContainer output = execution.createDataContainer(spec);
    int outputRow = 0;
    for (List<DataRow> group : groups(input).values()) {
      if (!m_withReplacement.getBooleanValue() && m_parentCount.getIntValue() > group.size()) {
        throw new InvalidSettingsException(
            "Parent count exceeds population size while duplicate selection is disabled.");
      }
      final RandomGenerator random = random(group.get(0), spec, "selection");
      final List<DataRow> available = new ArrayList<>(group);
      for (int selected = 0; selected < m_parentCount.getIntValue(); selected++) {
        final DataRow winner =
            "RANDOM".equals(m_mode.getStringValue())
                ? available.get(random.nextInt(available.size()))
                : tournament(available, random, spec, objectives, directions);
        output.addRowToTable(new DefaultRow("Parent" + outputRow++, winner));
        if (!m_withReplacement.getBooleanValue()) available.remove(winner);
      }
    }
    output.close();
    return output.getTable();
  }

  // DE selection leaves one row per target and stores all donor vectors in its state cell.
  private BufferedDataTable selectDifferentialDonors(
      final BufferedDataTable input,
      final BufferedDataTable archive,
      final ExecutionContext execution)
      throws Exception {
    final DataTableSpec spec = input.getDataTableSpec();
    final DataTableSpec outputSpec = spec;
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final List<String> variableNames = directFloatVariables(problem);
    final int[] variableIndices = KnimeTableSupport.requireNumericColumns(spec, variableNames);
    final Map<String, List<DataRow>> archiveGroups = archive == null ? Map.of() : groups(archive);
    final int[] archiveVariableIndices =
        archive == null
            ? new int[0]
            : KnimeTableSupport.requireNumericColumns(archive.getDataTableSpec(), variableNames);

    final BufferedDataContainer output = execution.createDataContainer(outputSpec);
    int outputRow = 0;
    for (Map.Entry<String, List<DataRow>> entry : groups(input).entrySet()) {
      final List<DataRow> population = entry.getValue();
      if (population.size() < 4) {
        throw new InvalidSettingsException(
            "Differential Evolution selection requires at least four individuals per population.");
      }
      final List<Integer> preference = preferenceOrder(population, spec, problem);
      final int pbestCount =
          Math.max(
              2,
              Math.min(
                  population.size(),
                  (int) Math.ceil(m_pbest.getDoubleValue() * population.size())));
      final RandomGenerator random = random(population.get(0), spec, "de-selection");
      for (int targetIndex = 0; targetIndex < population.size(); targetIndex++) {
        final DataRow target = population.get(targetIndex);
        final List<Integer> donors = distinctDonors(population.size(), targetIndex, 3, random);
        final DataRow randomBase = population.get(donors.get(0));
        final DataRow difference1 = population.get(donors.get(1));
        final DataRow difference2 = population.get(donors.get(2));
        final DataRow best = population.get(preference.get(0));
        final DataRow pbest = population.get(preference.get(random.nextInt(pbestCount)));
        final double[] pbestDifference2 =
            secondPbestDonor(
                population,
                spec,
                variableIndices,
                archiveGroups.getOrDefault(entry.getKey(), List.of()),
                archive == null ? null : archive.getDataTableSpec(),
                archiveVariableIndices,
                targetIndex,
                donors.get(1),
                random,
                variableNames);

        final DataCell[] cells = KnimeTableSupport.copyToSpec(target, spec, outputSpec);
        IdeaFlowState state =
            PopulationState.get(target, spec)
                .withVector(
                    IdeaFlowState.DE_TARGET_VECTOR, vector(target, variableIndices, variableNames))
                .withVector(
                    IdeaFlowState.DE_RANDOM_BASE_VECTOR,
                    vector(randomBase, variableIndices, variableNames))
                .withVector(
                    IdeaFlowState.DE_DIFFERENCE_1_VECTOR,
                    vector(difference1, variableIndices, variableNames))
                .withVector(
                    IdeaFlowState.DE_DIFFERENCE_2_VECTOR,
                    vector(difference2, variableIndices, variableNames))
                .withVector(
                    IdeaFlowState.DE_BEST_VECTOR, vector(best, variableIndices, variableNames))
                .withVector(
                    IdeaFlowState.DE_PBEST_VECTOR, vector(pbest, variableIndices, variableNames))
                .withVector(IdeaFlowState.DE_PBEST_DIFFERENCE_2_VECTOR, pbestDifference2);
        PopulationState.set(cells, outputSpec, state);
        output.addRowToTable(new DefaultRow("DETarget" + outputRow++, cells));
        execution.checkCanceled();
      }
    }
    output.close();
    return output.getTable();
  }

  // Best and p-best choices use feasibility first, then objective quality or Pareto rank.
  private List<Integer> preferenceOrder(
      final List<DataRow> rows, final DataTableSpec spec, final ProblemMetadata.Schema problem)
      throws InvalidSettingsException {
    final int[] objectiveIndices =
        KnimeTableSupport.requireNumericColumns(spec, problem.objectiveNames());
    final int violationIndex = spec.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION);
    final List<Integer> order = new ArrayList<>();
    for (int index = 0; index < rows.size(); index++) order.add(index);
    if (problem.objectives().size() == 1) {
      final List<OptimizationDirection> directions =
          problem.objectives().stream().map(ProblemMetadata.Objective::direction).toList();
      order.sort(
          (left, right) ->
              compare(rows.get(left), rows.get(right), spec, objectiveIndices, directions));
      return order;
    }
    final List<ObjectiveDefinition> definitions =
        KnimeTableSupport.objectives(
            problem.objectiveNames(),
            problem.objectives().stream().map(ProblemMetadata.Objective::direction).toList(),
            List.of());
    final List<Candidate> candidates = new ArrayList<>();
    for (DataRow row : rows) {
      candidates.add(
          KnimeTableSupport.candidate(
              row,
              new int[0],
              objectiveIndices,
              violationIndex,
              List.of(),
              problem.objectiveNames()));
    }
    final List<List<Integer>> fronts = FastNonDominatedSort.sort(candidates, definitions);
    final double[] crowding = CrowdingDistance.compute(candidates, fronts, definitions);
    final int[] ranks = new int[rows.size()];
    for (int rank = 0; rank < fronts.size(); rank++) {
      for (int index : fronts.get(rank)) ranks[index] = rank;
    }
    order.sort(
        Comparator.comparingInt((Integer index) -> ranks[index])
            .thenComparing(
                Comparator.comparingDouble((Integer index) -> crowding[index]).reversed())
            .thenComparing(index -> rows.get(index).getKey().getString()));
    return order;
  }

  private DataRow tournament(
      final List<DataRow> rows,
      final RandomGenerator random,
      final DataTableSpec spec,
      final int[] objectiveIndices,
      final List<OptimizationDirection> directions) {
    DataRow best = rows.get(random.nextInt(rows.size()));
    for (int index = 1; index < Math.min(m_tournamentSize.getIntValue(), rows.size()); index++) {
      final DataRow challenger = rows.get(random.nextInt(rows.size()));
      if (compare(challenger, best, spec, objectiveIndices, directions) < 0) best = challenger;
    }
    return best;
  }

  private static int compare(
      final DataRow left,
      final DataRow right,
      final DataTableSpec spec,
      final int[] objectiveIndices,
      final List<OptimizationDirection> directions) {
    final int violation = spec.findColumnIndex(PopulationState.CONSTRAINT_VIOLATION);
    if (violation >= 0) {
      final double leftViolation = number(left, violation);
      final double rightViolation = number(right, violation);
      if (leftViolation > 0 || rightViolation > 0) {
        final int result = Double.compare(leftViolation, rightViolation);
        if (result != 0) return result;
      }
    }
    final int rank = spec.findColumnIndex(ReservedColumns.PARETO_RANK);
    final int crowding = spec.findColumnIndex(ReservedColumns.CROWDING_DISTANCE);
    if (rank >= 0
        && crowding >= 0
        && !left.getCell(rank).isMissing()
        && !right.getCell(rank).isMissing()) {
      final int result =
          Integer.compare(
              ((IntValue) left.getCell(rank)).getIntValue(),
              ((IntValue) right.getCell(rank)).getIntValue());
      if (result != 0) return result;
      return -Double.compare(number(left, crowding), number(right, crowding));
    }
    for (int objective = 0; objective < objectiveIndices.length; objective++) {
      final int result =
          Double.compare(
              directions.get(objective).normalize(number(left, objectiveIndices[objective])),
              directions.get(objective).normalize(number(right, objectiveIndices[objective])));
      if (result != 0) return result;
    }
    return left.getKey().getString().compareTo(right.getKey().getString());
  }

  private static List<Integer> distinctDonors(
      final int size, final int target, final int count, final RandomGenerator random) {
    final List<Integer> choices = new ArrayList<>();
    for (int index = 0; index < size; index++) if (index != target) choices.add(index);
    Collections.shuffle(choices, new java.util.Random(random.nextLong()));
    return List.copyOf(choices.subList(0, count));
  }

  // Canonical SHADE samples the second difference donor from population union archive.
  private static double[] secondPbestDonor(
      final List<DataRow> population,
      final DataTableSpec populationSpec,
      final int[] populationVariables,
      final List<DataRow> archive,
      final DataTableSpec archiveSpec,
      final int[] archiveVariables,
      final int target,
      final int firstDifference,
      final RandomGenerator random,
      final List<String> variableNames)
      throws InvalidSettingsException {
    final List<Donor> choices = new ArrayList<>();
    for (int index = 0; index < population.size(); index++) {
      if (index != target && index != firstDifference) {
        choices.add(new Donor(population.get(index), populationSpec, populationVariables));
      }
    }
    if (archiveSpec != null) {
      for (DataRow row : archive) choices.add(new Donor(row, archiveSpec, archiveVariables));
    }
    final Donor selected = choices.get(random.nextInt(choices.size()));
    return vector(selected.row(), selected.spec(), selected.variableIndices(), variableNames);
  }

  private record Donor(DataRow row, DataTableSpec spec, int[] variableIndices) {}

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

  private static double[] vector(
      final DataRow row, final int[] variableIndices, final List<String> variableNames)
      throws InvalidSettingsException {
    return vector(row, null, variableIndices, variableNames);
  }

  private static double[] vector(
      final DataRow row,
      final DataTableSpec ignored,
      final int[] variableIndices,
      final List<String> variableNames)
      throws InvalidSettingsException {
    final double[] vector = new double[variableIndices.length];
    for (int dimension = 0; dimension < vector.length; dimension++) {
      vector[dimension] =
          KnimeTableSupport.number(
              row.getCell(variableIndices[dimension]), row, variableNames.get(dimension));
    }
    return vector;
  }

  private static double number(final DataRow row, final int index) {
    final var cell = row.getCell(index);
    if (cell instanceof DoubleValue value) return value.getDoubleValue();
    if (cell instanceof IntValue value) return value.getIntValue();
    if (cell instanceof LongValue value) return value.getLongValue();
    return Double.NaN;
  }

  private static List<String> directFloatVariables(final ProblemMetadata.Schema problem)
      throws InvalidSettingsException {
    final List<String> names =
        problem.variables().stream()
            .filter(variable -> !variable.encoded() && "REAL".equalsIgnoreCase(variable.type()))
            .map(ProblemMetadata.Variable::name)
            .toList();
    if (names.isEmpty()) {
      throw new InvalidSettingsException(
          "Differential Evolution requires direct Float variables in Problem Setup.");
    }
    return names;
  }

  private void validate(final DataTableSpec spec) throws InvalidSettingsException {
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    KnimeTableSupport.requireNumericColumns(spec, problem.objectiveNames());
    if (!List.of("TOURNAMENT", "RANDOM", "DE_DONORS").contains(m_mode.getStringValue())) {
      throw new InvalidSettingsException("Unsupported selection strategy.");
    }
    PopulationState.requireVisibleColumns(spec);
    if ("DE_DONORS".equals(m_mode.getStringValue())) {
      final List<String> variables = directFloatVariables(problem);
      KnimeTableSupport.requireNumericColumns(spec, variables);
      for (String variable : variables) {
        if (!spec.getColumnSpec(variable).getType().isCompatible(DoubleValue.class)) {
          throw new InvalidSettingsException(
              "Differential Evolution requires Float variables: " + variable);
        }
      }
    }
  }

  private static void validateArchive(final DataTableSpec archive, final DataTableSpec population)
      throws InvalidSettingsException {
    KnimeTableSupport.requireSameSchema(population, archive, "Selection archive");
    final List<String> variables = directFloatVariables(ProblemMetadata.require(population));
    KnimeTableSupport.requireNumericColumns(archive, variables);
    PopulationState.requireVisibleColumns(archive);
  }

  private SettingsModel[] models() {
    return new SettingsModel[] {
      m_mode, m_tournamentSize, m_parentCount, m_withReplacement, m_pbest
    };
  }

  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    for (SettingsModel model : models()) model.saveSettingsTo(settings);
  }

  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    m_mode.validateSettings(settings);
    m_tournamentSize.validateSettings(settings);
    m_parentCount.validateSettings(settings);
    m_withReplacement.validateSettings(settings);
    m_pbest.validateSettings(settings);
  }

  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    m_mode.loadSettingsFrom(settings);
    m_tournamentSize.loadSettingsFrom(settings);
    m_parentCount.loadSettingsFrom(settings);
    m_withReplacement.loadSettingsFrom(settings);
    m_pbest.loadSettingsFrom(settings);
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
