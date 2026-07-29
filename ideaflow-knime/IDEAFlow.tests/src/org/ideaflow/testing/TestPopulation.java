package org.ideaflow.testing;

import java.util.List;
import org.ideaflow.api.ConstraintRelation;
import org.ideaflow.api.IdeaFlowState;
import org.ideaflow.api.IdeaFlowStateCell;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.knime.KnimeTableSupport;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.knime.PopulationState;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;

/** Small, deterministic population tables shared by node-model tests. */
public final class TestPopulation {
  private TestPopulation() {}

  public static ProblemMetadata.Schema problem() {
    return new ProblemMetadata.Schema(
        "two-objective-test",
        100,
        List.of(new ProblemMetadata.Variable("x", "REAL", -5, 5, "DIRECT", "", 1)),
        List.of(
            new ProblemMetadata.Objective("f1", OptimizationDirection.MINIMIZE, 5.0),
            new ProblemMetadata.Objective("f2", OptimizationDirection.MINIMIZE, 5.0)),
        List.of(
            new ProblemMetadata.Constraint(
                PopulationState.CONSTRAINT_VIOLATION, ConstraintRelation.LE, 0.0, 0.0)));
  }

  public static DataTableSpec spec() {
    final DataTableSpec raw =
        new DataTableSpec(
            KnimeTableSupport.doubleColumn("x"),
            KnimeTableSupport.doubleColumn("f1"),
            KnimeTableSupport.doubleColumn("f2"),
            KnimeTableSupport.longColumn(PopulationState.NFE),
            KnimeTableSupport.doubleColumn(PopulationState.CONSTRAINT_VIOLATION),
            new DataColumnSpecCreator(PopulationState.FEASIBLE, BooleanCell.TYPE).createSpec(),
            PopulationState.column());
    return ProblemMetadata.attach(raw, "x", problem());
  }

  public static DataCell[] row(
      final String individual,
      final double variable,
      final double firstObjective,
      final double secondObjective,
      final long nfe,
      final double violation) {
    final IdeaFlowState state =
        IdeaFlowState.empty()
            .with(IdeaFlowState.RUN, "run-1")
            .with(IdeaFlowState.POPULATION, "population-0")
            .with(IdeaFlowState.INDIVIDUAL, individual)
            .with(IdeaFlowState.SEED, 42L)
            .with(IdeaFlowState.GENERATION, 1L)
            .with(IdeaFlowState.EVALUATED, true);
    return new DataCell[] {
      new DoubleCell(variable),
      new DoubleCell(firstObjective),
      new DoubleCell(secondObjective),
      new LongCell(nfe),
      new DoubleCell(violation),
      violation == 0.0 ? BooleanCell.TRUE : BooleanCell.FALSE,
      new IdeaFlowStateCell(state)
    };
  }
}
