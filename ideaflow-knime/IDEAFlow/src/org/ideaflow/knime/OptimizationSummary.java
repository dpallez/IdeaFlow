package org.ideaflow.knime;

import java.util.List;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;

/** Shared fixed-width schema and compact record for per-checkpoint optimization summaries. */
public final class OptimizationSummary {
  public static final String RUN = "Run";
  public static final String POPULATION = "Population";
  public static final String POPULATION_SIZE = "Population size";
  public static final String FEASIBLE_SIZE = "Feasible size";
  public static final String BEST = "Best";
  public static final String MEAN = "Mean";
  public static final String WORST = "Worst";
  public static final String STANDARD_DEVIATION = "Standard deviation";
  public static final String NONDOMINATED_SIZE = "Nondominated size";
  public static final String HYPERVOLUME = "Hypervolume";
  public static final String SERIES = "Series";
  public static final String SEED = "Seed";
  public static final String PROBLEM = "Problem";

  public record Entry(
      String run,
      String population,
      long nfe,
      int populationSize,
      int feasibleSize,
      double best,
      double mean,
      double worst,
      double standardDeviation,
      int nondominatedSize,
      double hypervolume,
      String series,
      long seed,
      String problem) {
    public DataCell[] cells() {
      return new DataCell[] {
        new StringCell(run),
        new StringCell(population),
        new LongCell(nfe),
        new IntCell(populationSize),
        new IntCell(feasibleSize),
        finite(best),
        finite(mean),
        finite(worst),
        finite(standardDeviation),
        new IntCell(nondominatedSize),
        finite(hypervolume),
        new StringCell(series),
        new LongCell(seed),
        new StringCell(problem)
      };
    }
  }

  private OptimizationSummary() {}

  public static DataTableSpec spec(final ProblemMetadata.Schema problem) {
    return ProblemMetadata.attach(rawSpec(), RUN, problem);
  }

  // Read by column name so summaries remain stable when KNIME adds unrelated table metadata.
  public static Entry read(final DataRow row, final DataTableSpec spec)
      throws InvalidSettingsException {
    validate(spec);
    return new Entry(
        text(row, spec, RUN),
        text(row, spec, POPULATION),
        ((LongValue) row.getCell(spec.findColumnIndex(PopulationState.NFE))).getLongValue(),
        ((IntValue) row.getCell(spec.findColumnIndex(POPULATION_SIZE))).getIntValue(),
        ((IntValue) row.getCell(spec.findColumnIndex(FEASIBLE_SIZE))).getIntValue(),
        number(row, spec, BEST),
        number(row, spec, MEAN),
        number(row, spec, WORST),
        number(row, spec, STANDARD_DEVIATION),
        ((IntValue) row.getCell(spec.findColumnIndex(NONDOMINATED_SIZE))).getIntValue(),
        number(row, spec, HYPERVOLUME),
        text(row, spec, SERIES),
        ((LongValue) row.getCell(spec.findColumnIndex(SEED))).getLongValue(),
        text(row, spec, PROBLEM));
  }

  public static void validate(final DataTableSpec spec) throws InvalidSettingsException {
    ProblemMetadata.require(spec);
    for (String name : List.of(RUN, POPULATION, SERIES, PROBLEM)) {
      final int index = spec.findColumnIndex(name);
      if (index < 0
          || !org.knime.core.data.StringValue.class.isAssignableFrom(
              spec.getColumnSpec(index).getType().getPreferredValueClass())) {
        throw new InvalidSettingsException("Progress summary is missing text column: " + name);
      }
    }
    KnimeTableSupport.requireNumericColumns(
        spec,
        List.of(
            PopulationState.NFE,
            POPULATION_SIZE,
            FEASIBLE_SIZE,
            BEST,
            MEAN,
            WORST,
            STANDARD_DEVIATION,
            NONDOMINATED_SIZE,
            HYPERVOLUME,
            SEED));
  }

  private static DataTableSpec rawSpec() {
    return new DataTableSpec(
        KnimeTableSupport.stringColumn(RUN),
        KnimeTableSupport.stringColumn(POPULATION),
        KnimeTableSupport.longColumn(PopulationState.NFE),
        KnimeTableSupport.intColumn(POPULATION_SIZE),
        KnimeTableSupport.intColumn(FEASIBLE_SIZE),
        KnimeTableSupport.doubleColumn(BEST),
        KnimeTableSupport.doubleColumn(MEAN),
        KnimeTableSupport.doubleColumn(WORST),
        KnimeTableSupport.doubleColumn(STANDARD_DEVIATION),
        KnimeTableSupport.intColumn(NONDOMINATED_SIZE),
        KnimeTableSupport.doubleColumn(HYPERVOLUME),
        KnimeTableSupport.stringColumn(SERIES),
        KnimeTableSupport.longColumn(SEED),
        KnimeTableSupport.stringColumn(PROBLEM));
  }

  private static String text(final DataRow row, final DataTableSpec spec, final String name)
      throws InvalidSettingsException {
    final DataCell cell = row.getCell(spec.findColumnIndex(name));
    if (cell.isMissing())
      throw new InvalidSettingsException("Progress summary value is missing: " + name);
    return cell.toString();
  }

  private static double number(final DataRow row, final DataTableSpec spec, final String name) {
    final DataCell cell = row.getCell(spec.findColumnIndex(name));
    return cell.isMissing() ? Double.NaN : ((DoubleValue) cell).getDoubleValue();
  }

  private static DataCell finite(final double value) {
    return Double.isFinite(value)
        ? new DoubleCell(value)
        : org.knime.core.data.DataType.getMissingCell();
  }
}
