package org.ideaflow.knime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.ideaflow.api.ConstraintRelation;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;

final class ProblemMetadataTest {
  @Test
  void metadataRoundTripsThroughAColumnSpecification() throws Exception {
    final ProblemMetadata.Schema expected = schema();
    final DataTableSpec original =
        new DataTableSpec(new DataColumnSpecCreator("marker", StringCell.TYPE).createSpec());

    final DataTableSpec attached = ProblemMetadata.attach(original, "marker", expected);

    assertEquals(expected, ProblemMetadata.require(attached));
    assertFalse(ProblemMetadata.read(original).isPresent());
  }

  @Test
  void encodedVariablesExposeTheirPhysicalGeneColumns() {
    final ProblemMetadata.Variable encoded = schema().variables().get(1);

    assertEquals(List.of("encoded_bit0", "encoded_bit1", "encoded_bit2"), encoded.populationColumns());
    assertEquals(List.of("x"), schema().variables().get(0).populationColumns());
  }

  @Test
  void requiresAMetadataMarkerAndValidSchema() {
    final DataTableSpec original =
        new DataTableSpec(new DataColumnSpecCreator("marker", StringCell.TYPE).createSpec());

    assertThrows(
        IllegalArgumentException.class,
        () -> ProblemMetadata.attach(original, "missing", schema()));
    assertThrows(InvalidSettingsException.class, () -> ProblemMetadata.require(original));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProblemMetadata.Schema("problem", 0, schema().variables(), schema().objectives(), List.of()));
  }

  private static ProblemMetadata.Schema schema() {
    return new ProblemMetadata.Schema(
        "problem",
        100,
        List.of(
            new ProblemMetadata.Variable("x", "REAL", -5, 5, "DIRECT", "", 1),
            new ProblemMetadata.Variable("encoded", "REAL", 0, 7, "BINARY_ENCODED", "NATURAL_BINARY", 3)),
        List.of(new ProblemMetadata.Objective("fitness", OptimizationDirection.MINIMIZE, 10.0)),
        List.of(new ProblemMetadata.Constraint("constraint", ConstraintRelation.LE, 0.0, 0.0)));
  }
}
