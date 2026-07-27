package org.ideaflow.nodes.problem;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ideaflow.api.ConstraintRelation;
import org.ideaflow.api.OptimizationDirection;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.ideaflow.nodes.problem.ProblemDefinitionNodeParameters.Constraint;
import org.ideaflow.nodes.problem.ProblemDefinitionNodeParameters.DirectVariableGroup;
import org.ideaflow.nodes.problem.ProblemDefinitionNodeParameters.EncodedVariableGroup;
import org.ideaflow.nodes.problem.ProblemDefinitionNodeParameters.Objective;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.NodeFactory;
import org.knime.node.DefaultModel.ConfigureInput;
import org.knime.node.DefaultModel.ConfigureOutput;
import org.knime.node.DefaultModel.ExecuteInput;
import org.knime.node.DefaultModel.ExecuteOutput;
import org.knime.node.DefaultNode;
import org.knime.node.DefaultNodeFactory;

/** Modern declarative source for one run and its optimization problem. */
public final class ProblemDefinitionNodeFactory extends DefaultNodeFactory {

    private static final DefaultNode NODE = DefaultNode.create()
        .name("Problem Setup")
        .icon("../../../default.png")
        .shortDescription("Defines one reproducible run and the optimization problem.")
        .fullDescription("Creates one combined setup table containing run settings and a language-neutral problem "
            + "definition. "
            + "Use standard KNIME loops or Components when several runs or seeds are required. Repeated variables "
            + "and binary-encoded numerical vectors are generated automatically, while objectives, directions, "
            + "constraints, and optional hypervolume references remain independent of the evaluator.")
        .sinceVersion(5, 10, 0)
        .ports(ports -> ports.addOutputTable("Problem setup",
            "Run identity, seed, evaluation budget, variables, objectives, constraints, and representations."))
        .model(model -> model
            .parametersClass(ProblemDefinitionNodeParameters.class)
            .configure(ProblemDefinitionNodeFactory::configure)
            .execute(ProblemDefinitionNodeFactory::execute))
        .nodeType(NodeFactory.NodeType.Source);

    public ProblemDefinitionNodeFactory() {
        super(NODE);
    }

    private static void configure(final ConfigureInput input, final ConfigureOutput output)
            throws InvalidSettingsException {
        final ProblemDefinitionNodeParameters parameters = input.getParameters();
        parameters.validate();
        output.setOutSpec(0, outputSpec(parameters));
    }

    private static void execute(final ExecuteInput input, final ExecuteOutput output)
            throws KNIMEException, CanceledExecutionException {
        final ProblemDefinitionNodeParameters parameters = input.getParameters();
        try {
            parameters.validate();
        } catch (InvalidSettingsException exception) {
            throw new KNIMEException(exception.getMessage(), exception);
        }
        final String runId = runId(parameters);
        final BufferedDataContainer setupContainer =
            input.getExecutionContext().createDataContainer(outputSpec(parameters));
        int row = 0;
        if (parameters.m_directVariables != null) {
            for (DirectVariableGroup group : parameters.m_directVariables) {
                for (int offset = 0; offset < group.m_count; offset++) {
                    final String name =
                        ProblemDefinitionNodeParameters.logicalName(group.m_prefix, group.m_count, offset);
                    final boolean binary = group.m_type == ProblemDefinitionNodeParameters.VariableType.BINARY;
                    add(setupContainer, "Definition" + row++, parameters, runId, "variable", name,
                        group.m_type.name(), binary ? 0.0 : group.m_lower, binary ? 1.0 : group.m_upper, "", null,
                        "DIRECT", "", binary ? 1 : null, group.m_prefix.trim(), offset,
                        "", null, null);
                }
            }
        }
        if (parameters.m_encodedVariables != null) {
            for (EncodedVariableGroup group : parameters.m_encodedVariables) {
                for (int offset = 0; offset < group.m_count; offset++) {
                    final String name =
                        ProblemDefinitionNodeParameters.logicalName(group.m_prefix, group.m_count, offset);
                    add(setupContainer, "Definition" + row++, parameters, runId, "variable", name, "REAL",
                        group.m_lower, group.m_upper, "", null, "BINARY_ENCODED", "NATURAL",
                        group.m_bits, group.m_prefix.trim(), offset, "", null, null);
                }
            }
        }
        for (Objective objective : parameters.m_objectives) {
            add(setupContainer, "Definition" + row++, parameters, runId, "objective", objective.m_name.trim(),
                "DOUBLE", null, null, objective.m_direction.name(),
                objective.m_hasReference ? objective.m_reference : null, "", "", null, "", null, "", null, null);
        }
        if (parameters.m_constraints != null) {
            for (Constraint constraint : parameters.m_constraints) {
                add(setupContainer, "Definition" + row++, parameters, runId, "constraint", constraint.m_name.trim(),
                    "RESULT_LIMIT", null, null, "", null, "", "", null, "", null,
                    constraint.m_relation.name(), constraint.m_threshold,
                    constraint.m_relation == ProblemDefinitionNodeParameters.ConstraintRelationChoice.EQ
                        ? constraint.m_tolerance : 0.0);
            }
        }
        setupContainer.close();
        output.setOutData(0, setupContainer.getTable());
    }

    static DataTableSpec outputSpec() {
        return baseOutputSpec();
    }

    private static String runId(final ProblemDefinitionNodeParameters parameters) {
        final String source = parameters.m_experimentId.trim() + ':' + parameters.m_seed;
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static DataTableSpec outputSpec(final ProblemDefinitionNodeParameters parameters) {
        return ProblemMetadata.attach(baseOutputSpec(), "problem_id", metadata(parameters));
    }

    private static DataTableSpec baseOutputSpec() {
        return new DataTableSpec(column("experiment_id", StringCell.TYPE), column("run_id", StringCell.TYPE),
            column("seed", LongCell.TYPE), column("max_evaluations", IntCell.TYPE),
            column("problem_id", StringCell.TYPE), column("kind", StringCell.TYPE),
            column("name", StringCell.TYPE), column("type", StringCell.TYPE),
            column("lower_bound", DoubleCell.TYPE), column("upper_bound", DoubleCell.TYPE),
            column("direction", StringCell.TYPE), column("reference_point", DoubleCell.TYPE),
            column("representation", StringCell.TYPE), column("encoding", StringCell.TYPE),
            column("bits", IntCell.TYPE), column("group", StringCell.TYPE), column("group_index", IntCell.TYPE),
            column("relation", StringCell.TYPE), column("threshold", DoubleCell.TYPE),
            column("tolerance", DoubleCell.TYPE));
    }

    private static ProblemMetadata.Schema metadata(final ProblemDefinitionNodeParameters parameters) {
        final List<ProblemMetadata.Variable> variables = new ArrayList<>();
        if (parameters.m_directVariables != null) {
            for (DirectVariableGroup group : parameters.m_directVariables) {
                for (int offset = 0; offset < group.m_count; offset++) {
                    final String name =
                        ProblemDefinitionNodeParameters.logicalName(group.m_prefix, group.m_count, offset);
                    final boolean binary = group.m_type == ProblemDefinitionNodeParameters.VariableType.BINARY;
                    variables.add(new ProblemMetadata.Variable(name, group.m_type.name(),
                        binary ? 0.0 : group.m_lower, binary ? 1.0 : group.m_upper, "DIRECT", "", 1));
                }
            }
        }
        if (parameters.m_encodedVariables != null) {
            for (EncodedVariableGroup group : parameters.m_encodedVariables) {
                for (int offset = 0; offset < group.m_count; offset++) {
                    final String name =
                        ProblemDefinitionNodeParameters.logicalName(group.m_prefix, group.m_count, offset);
                    variables.add(new ProblemMetadata.Variable(name, "REAL", group.m_lower, group.m_upper,
                        "BINARY_ENCODED", "NATURAL", group.m_bits));
                }
            }
        }
        final List<ProblemMetadata.Objective> objectives = new ArrayList<>();
        for (Objective objective : parameters.m_objectives) {
            objectives.add(new ProblemMetadata.Objective(objective.m_name.trim(),
                OptimizationDirection.parse(objective.m_direction.name()),
                objective.m_hasReference ? objective.m_reference : null));
        }
        final List<ProblemMetadata.Constraint> constraints = new ArrayList<>();
        if (parameters.m_constraints != null) {
            for (Constraint constraint : parameters.m_constraints) {
                constraints.add(new ProblemMetadata.Constraint(constraint.m_name.trim(),
                    ConstraintRelation.valueOf(constraint.m_relation.name()), constraint.m_threshold,
                    constraint.m_relation == ProblemDefinitionNodeParameters.ConstraintRelationChoice.EQ
                        ? constraint.m_tolerance : 0.0));
            }
        }
        return new ProblemMetadata.Schema(parameters.m_experimentId, parameters.m_maxEvaluations,
            variables, objectives, constraints);
    }

    private static DataColumnSpec column(final String name, final DataType type) {
        return new DataColumnSpecCreator(name, type).createSpec();
    }

    private static void add(final BufferedDataContainer output, final String key,
            final ProblemDefinitionNodeParameters parameters, final String runId, final String kind,
            final String name, final String type, final Double lower, final Double upper,
            final String direction, final Double reference, final String representation, final String encoding,
            final Integer bits, final String group, final Integer groupIndex, final String relation,
            final Double threshold, final Double tolerance) {
        final DataCell missing = DataType.getMissingCell();
        output.addRowToTable(new DefaultRow(key, new DataCell[]{
            new StringCell(parameters.m_experimentId.trim()), new StringCell(runId), new LongCell(parameters.m_seed),
            new IntCell(parameters.m_maxEvaluations), new StringCell(parameters.m_experimentId.trim()),
            new StringCell(kind), new StringCell(name), new StringCell(type),
            lower == null ? missing : new DoubleCell(lower),
            upper == null ? missing : new DoubleCell(upper), new StringCell(direction),
            reference == null ? missing : new DoubleCell(reference), new StringCell(representation),
            new StringCell(encoding), bits == null ? missing : new IntCell(bits), new StringCell(group),
            groupIndex == null ? missing : new IntCell(groupIndex), new StringCell(relation),
            threshold == null ? missing : new DoubleCell(threshold),
            tolerance == null ? missing : new DoubleCell(tolerance)}));
    }
}
