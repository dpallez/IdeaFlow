package org.ideaflow.nodes.metrics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;
import org.ideaflow.api.ReservedColumns;
import org.ideaflow.core.FastNonDominatedSort;
import org.ideaflow.core.QualityIndicators;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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

/** Reference-front indicators driven entirely by Problem Setup metadata. */
final class CompareParetoFrontsNodeModel extends NodeModel {
  CompareParetoFrontsNodeModel() {
    super(2, 1);
  }

  private static DataTableSpec outputSpec() {
    return new DataTableSpec(
        KnimeTableSupport.stringColumn("Run"),
        KnimeTableSupport.stringColumn("Population"),
        KnimeTableSupport.longColumn(PopulationState.NFE),
        KnimeTableSupport.doubleColumn("GD"),
        KnimeTableSupport.doubleColumn("IGD"),
        KnimeTableSupport.doubleColumn("IGD+"),
        KnimeTableSupport.doubleColumn("Additive epsilon"),
        KnimeTableSupport.doubleColumn("Spacing"));
  }

  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] input) throws InvalidSettingsException {
    validate(input);
    return new DataTableSpec[] {attachedOutputSpec(input[0])};
  }

  @Override
  protected BufferedDataTable[] execute(
      final BufferedDataTable[] input, final ExecutionContext execution) throws Exception {
    final DataTableSpec spec = input[0].getDataTableSpec();
    final DataTableSpec referenceSpec = input[1].getDataTableSpec();
    validate(new DataTableSpec[] {spec, referenceSpec});
    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
    final List<String> names = problem.objectiveNames();
    final int[] indices = KnimeTableSupport.requireNumericColumns(spec, names);
    final int[] referenceIndices = KnimeTableSupport.requireNumericColumns(referenceSpec, names);
    final List<ObjectiveDefinition> definitions =
        KnimeTableSupport.objectives(
            names,
            problem.objectives().stream().map(ProblemMetadata.Objective::direction).toList(),
            List.of());
    final int violationIndex = spec.findColumnIndex(ReservedColumns.CONSTRAINT_VIOLATION);
    final List<Candidate> reference = new ArrayList<>();
    for (DataRow row : input[1]) {
      reference.add(
          KnimeTableSupport.candidate(row, new int[0], referenceIndices, -1, List.of(), names));
    }
    if (reference.isEmpty())
      throw new InvalidSettingsException("Reference front must not be empty.");
    final Map<String, List<DataRow>> groups = new LinkedHashMap<>();
    for (DataRow row : input[0]) {
      groups
          .computeIfAbsent(PopulationState.groupKey(row, spec), ignored -> new ArrayList<>())
          .add(row);
    }
    final BufferedDataContainer output = execution.createDataContainer(attachedOutputSpec(spec));
    int number = 0;
    for (List<DataRow> rows : groups.values()) {
      final List<Candidate> approximation = new ArrayList<>();
      for (DataRow row : rows) {
        final Candidate candidate =
            KnimeTableSupport.candidate(row, new int[0], indices, violationIndex, List.of(), names);
        if (candidate.constraintViolation() == 0.0) approximation.add(candidate);
      }
      if (approximation.isEmpty()) continue;
      final List<List<Integer>> fronts = FastNonDominatedSort.sort(approximation, definitions);
      final List<Candidate> nondominated = fronts.get(0).stream().map(approximation::get).toList();
      final DataRow first = rows.get(0);
      long nfe = 0;
      for (DataRow row : rows) nfe = Math.max(nfe, PopulationState.nfe(row, spec));
      output.addRowToTable(
          new DefaultRow(
              "ReferenceIndicators" + number++,
              new DataCell[] {
                new StringCell(PopulationState.run(first, spec)),
                new StringCell(PopulationState.population(first, spec)),
                new LongCell(nfe),
                new DoubleCell(
                    QualityIndicators.generationalDistance(nondominated, reference, definitions)),
                new DoubleCell(
                    QualityIndicators.invertedGenerationalDistance(
                        nondominated, reference, definitions)),
                new DoubleCell(
                    QualityIndicators.invertedGenerationalDistancePlus(
                        nondominated, reference, definitions)),
                new DoubleCell(
                    QualityIndicators.additiveEpsilon(nondominated, reference, definitions)),
                new DoubleCell(QualityIndicators.spacing(nondominated, definitions))
              }));
    }
    output.close();
    return new BufferedDataTable[] {output.getTable()};
  }

  private static DataTableSpec attachedOutputSpec(final DataTableSpec input)
      throws InvalidSettingsException {
    return ProblemMetadata.attach(outputSpec(), "Run", ProblemMetadata.require(input));
  }

  private static void validate(final DataTableSpec[] specs) throws InvalidSettingsException {
    final ProblemMetadata.Schema problem = ProblemMetadata.require(specs[0]);
    KnimeTableSupport.requireNumericColumns(specs[0], problem.objectiveNames());
    KnimeTableSupport.requireNumericColumns(specs[1], problem.objectiveNames());
    PopulationState.requireVisibleColumns(specs[0]);
  }

  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {}

  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {}

  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {}

  @Override
  protected void loadInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}

  @Override
  protected void saveInternals(final File directory, final ExecutionMonitor monitor)
      throws IOException, CanceledExecutionException {}

  @Override
  protected void reset() {}
}
