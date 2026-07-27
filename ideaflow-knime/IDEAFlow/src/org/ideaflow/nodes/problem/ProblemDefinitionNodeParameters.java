package org.ideaflow.nodes.problem;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.ideaflow.api.ReservedColumns;
import org.knime.core.node.InvalidSettingsException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/** Modern, structured problem builder. */
final class ProblemDefinitionNodeParameters implements NodeParameters {

    @Section(title = "Run settings",
        description = "Configure the single optimization run created by this node. Use KNIME loops for repeated runs.")
    interface RunSection { }

    @Section(title = "Variables", description = "Define the values explored by the optimization algorithm.")
    @After(RunSection.class)
    interface VariablesSection { }

    @Section(title = "Objectives", description = "Values produced by the evaluation workflow.")
    @After(VariablesSection.class)
    interface ObjectivesSection { }

    @Section(title = "Constraints",
        description = "Optional limits applied to numeric results produced by the evaluation workflow.")
    @After(ObjectivesSection.class)
    interface ConstraintsSection { }

    enum VariableType {
        @Label(value = "Float", description = "A number that may contain decimals.")
        REAL,
        @Label(value = "Integer", description = "A whole number without decimals.")
        INTEGER,
        @Label(value = "Binary", description = "A value that is either 0 or 1.")
        BINARY
    }

    enum Direction {
        @Label(value = "Minimize", description = "Smaller values are better.")
        MINIMIZE,
        @Label(value = "Maximize", description = "Larger values are better.")
        MAXIMIZE
    }

    enum ConstraintRelationChoice {
        @Label(value = "Less than or equal to (≤)", description = "The result must not exceed the threshold.")
        LE,
        @Label(value = "Greater than or equal to (≥)", description = "The result must reach the threshold.")
        GE,
        @Label(value = "Equal to (=)", description = "The result must be within the configured tolerance.")
        EQ
    }

    static String logicalName(final String prefix, final int count, final int offset) {
        return count == 1 ? prefix.trim() : prefix.trim() + offset;
    }

    static String geneName(final String logicalName, final int bit) {
        return org.ideaflow.core.BinaryEncoding.geneName(logicalName, bit);
    }

    @Widget(title = "Experiment name",
        description = "A human-readable label stored with this run and its exported results.")
    @TextInputWidget(placeholder = "onemax_test")
    @Layout(RunSection.class)
    String m_experimentId = "experiment";

    @Widget(title = "Seed",
        description = "The population and stochastic operators use this seed to make the run reproducible.")
    @Layout(RunSection.class)
    long m_seed = 42L;

    @Widget(title = "Maximum evaluations",
        description = "Exact-evaluation budget recorded for this run. Loop stopping nodes can use the same budget.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(RunSection.class)
    int m_maxEvaluations = 10_000;

    @Widget(title = "Direct variables and groups",
        description = "Use a count of one for a single variable, or a larger count to generate a numbered group.")
    @ArrayWidget(addButtonText = "Add variable or group", elementTitle = "Variable group", showSortButtons = true)
    @Layout(VariablesSection.class)
    @ValueReference(DirectVariablesRef.class)
    DirectVariableGroup[] m_directVariables = {new DirectVariableGroup()};

    interface DirectVariablesRef extends ParameterReference<DirectVariableGroup[]> { }

    @Widget(title = "Binary-encoded numerical groups",
        description = "Advanced: represent each logical number with several binary population columns.")
    @ArrayWidget(addButtonText = "Add binary-encoded group", elementTitle = "Encoded group", showSortButtons = true)
    @Layout(VariablesSection.class)
    @ValueReference(EncodedVariablesRef.class)
    EncodedVariableGroup[] m_encodedVariables = new EncodedVariableGroup[0];

    interface EncodedVariablesRef extends ParameterReference<EncodedVariableGroup[]> { }

    @TextMessage(PreviewProvider.class)
    @Layout(VariablesSection.class)
    Void m_generatedPreview;

    @Widget(title = "Objectives", description = "Add one card for every objective column created by the evaluator.")
    @ArrayWidget(addButtonText = "Add objective", elementTitle = "Objective", showSortButtons = true)
    @Layout(ObjectivesSection.class)
    Objective[] m_objectives = {new Objective()};

    @Widget(title = "Constraint violations",
        description = "Declare business limits such as temperature ≤ 80; IdeaFlow calculates violations automatically.")
    @ArrayWidget(addButtonText = "Add constraint", elementTitle = "Constraint", showSortButtons = true)
    @Layout(ConstraintsSection.class)
    Constraint[] m_constraints = new Constraint[0];

    static final class DirectVariableGroup implements NodeParameters {
        interface TypeRef extends ParameterReference<VariableType> { }

        static final class UsesBounds implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer initializer) {
                return initializer.getEnum(TypeRef.class).isOneOf(VariableType.REAL, VariableType.INTEGER);
            }
        }

        @Widget(title = "Name", description = "Column name for one variable, or prefix for a numbered group.")
        @TextInputWidget(placeholder = "x")
        String m_prefix = "x";

        @Widget(title = "Number of variables", description = "One creates the exact name above; larger values add indices.")
        @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = AtMost100000.class)
        int m_count = 2;

        @Widget(title = "Type", description = "How each variable is represented in the population table.")
        @ValueSwitchWidget
        @ValueReference(TypeRef.class)
        VariableType m_type = VariableType.REAL;

        @Widget(title = "Smallest value", description = "Inclusive lower bound for every variable in this group.")
        @Effect(predicate = UsesBounds.class, type = EffectType.SHOW)
        double m_lower = -5.0;

        @Widget(title = "Largest value", description = "Inclusive upper bound for every variable in this group.")
        @Effect(predicate = UsesBounds.class, type = EffectType.SHOW)
        double m_upper = 5.0;
    }

    static final class EncodedVariableGroup implements NodeParameters {
        @Widget(title = "Decoded name", description = "Name for one decoded value, or prefix for a numbered vector.")
        @TextInputWidget(placeholder = "x")
        String m_prefix = "x";

        @Widget(title = "Number of numerical values", description = "Dimension of the decoded numerical vector.")
        @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = AtMost100000.class)
        int m_count = 10;

        @Widget(title = "Smallest decoded value", description = "Lower bound after decoding the binary genes.")
        double m_lower = -5.0;

        @Widget(title = "Largest decoded value", description = "Upper bound after decoding the binary genes.")
        double m_upper = 5.0;

        @Widget(title = "Bits per value", description = "Number of binary genes used to represent each numerical value.")
        @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = AtMost52.class)
        int m_bits = 10;

    }

    static final class AtMost52 extends MaxValidation {
        @Override
        protected double getMax() {
            return 52;
        }
    }

    static final class AtMost100000 extends MaxValidation {
        @Override
        protected double getMax() {
            return 100_000;
        }
    }

    static final class Objective implements NodeParameters {
        interface ReferenceEnabledRef extends BooleanReference { }

        static final class HasReferenceValue implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer initializer) {
                return initializer.getBoolean(ReferenceEnabledRef.class).isTrue();
            }
        }

        @Widget(title = "Result column", description = "Numeric column created by the evaluation workflow.")
        @TextInputWidget(placeholder = "fitness")
        String m_name = "fitness";

        @Widget(title = "Optimization direction", description = "Choose whether smaller or larger values are better.")
        @ValueSwitchWidget
        Direction m_direction = Direction.MINIMIZE;

        @Widget(title = "Use a hypervolume reference value",
            description = "Enable for multi-objective hypervolume analysis.")
        @ValueReference(ReferenceEnabledRef.class)
        boolean m_hasReference;

        @Widget(title = "Reference value", description = "A value worse than the region of interest for this objective.")
        @Effect(predicate = HasReferenceValue.class, type = EffectType.SHOW)
        double m_reference;
    }

    static final class Constraint implements NodeParameters {
        @Widget(title = "Result column",
            description = "Numeric column produced by the evaluation workflow, for example temperature.")
        @TextInputWidget(placeholder = "temperature")
        String m_name = "constraint";

        interface RelationRef extends ParameterReference<ConstraintRelationChoice> { }

        static final class IsEquality implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer initializer) {
                return initializer.getEnum(RelationRef.class).isOneOf(ConstraintRelationChoice.EQ);
            }
        }

        @Widget(title = "Relation", description = "How the evaluated value is compared with the threshold.")
        @ValueSwitchWidget
        @ValueReference(RelationRef.class)
        ConstraintRelationChoice m_relation = ConstraintRelationChoice.LE;

        @Widget(title = "Threshold", description = "Numeric limit used by the relation, for example 80.")
        double m_threshold;

        @Widget(title = "Tolerance",
            description = "Allowed absolute difference around the threshold for an equality constraint.")
        @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
        @Effect(predicate = IsEquality.class, type = EffectType.SHOW)
        double m_tolerance;
    }

    static final class PreviewProvider implements StateProvider<Optional<Message>> {
        private Supplier<DirectVariableGroup[]> m_direct;
        private Supplier<EncodedVariableGroup[]> m_encoded;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_direct = initializer.computeFromValueSupplier(DirectVariablesRef.class);
            m_encoded = initializer.computeFromValueSupplier(EncodedVariablesRef.class);
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput context) {
            long logical = 0;
            long populationColumns = 0;
            final StringBuilder examples = new StringBuilder();
            final DirectVariableGroup[] direct = m_direct.get();
            if (direct != null) {
                for (DirectVariableGroup group : direct) {
                    if (group != null && group.m_count > 0) {
                        logical += group.m_count;
                        populationColumns += group.m_count;
                        appendExample(examples, group.m_prefix, group.m_count, "direct");
                    }
                }
            }
            final EncodedVariableGroup[] encoded = m_encoded.get();
            if (encoded != null) {
                for (EncodedVariableGroup group : encoded) {
                    if (group != null && group.m_count > 0 && group.m_bits > 0) {
                        logical += group.m_count;
                        populationColumns += (long)group.m_count * group.m_bits;
                        final String first = logicalName(group.m_prefix, group.m_count, 0);
                        final String last = logicalName(group.m_prefix, group.m_count, group.m_count - 1);
                        final double levels = Math.scalb(1.0, group.m_bits) - 1.0;
                        final double resolution = (group.m_upper - group.m_lower) / levels;
                        if (!examples.isEmpty()) examples.append("  ");
                        examples.append(first);
                        if (!first.equals(last)) examples.append(" … ").append(last);
                        examples.append(" → ").append(group.m_count * (long)group.m_bits).append(" genes (")
                            .append(String.format(java.util.Locale.ROOT, "%.6g", resolution))
                            .append(" approximate resolution)");
                    }
                }
            }
            String description = logical + " logical variable" + (logical == 1 ? "" : "s") + " will create "
                + populationColumns + " population column" + (populationColumns == 1 ? "." : "s.");
            if (!examples.isEmpty()) description += "  " + examples;
            final MessageType type = populationColumns > 10_000 ? MessageType.WARNING : MessageType.INFO;
            return Optional.of(new Message("Generated population structure", description, type));
        }

        private static void appendExample(final StringBuilder output, final String prefix, final int count,
                final String suffix) {
            if (prefix == null || prefix.isBlank()) return;
            final String first = logicalName(prefix, count, 0);
            final String last = logicalName(prefix, count, count - 1);
            if (!output.isEmpty()) output.append("  ");
            output.append(first);
            if (!first.equals(last)) output.append(" … ").append(last);
            output.append(" (").append(suffix).append(')');
        }
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_experimentId == null || m_experimentId.isBlank()) {
            throw new InvalidSettingsException("Experiment name is required.");
        }
        if (m_maxEvaluations < 1) {
            throw new InvalidSettingsException("Maximum evaluations must be positive.");
        }
        final Set<String> names = new HashSet<>();
        int variableCount = 0;
        if (m_directVariables != null) {
            for (DirectVariableGroup group : m_directVariables) {
                requirePrefix(group == null ? null : group.m_prefix);
                if (group.m_count < 1) {
                    throw new InvalidSettingsException("Variable counts must be positive.");
                }
                if (group.m_type == null) throw new InvalidSettingsException("Select a kind of value for every group.");
                if (group.m_type != VariableType.BINARY) validateBounds(group.m_prefix, group.m_lower, group.m_upper);
                if (group.m_type == VariableType.INTEGER
                        && (group.m_lower != Math.rint(group.m_lower) || group.m_upper != Math.rint(group.m_upper))) {
                    throw new InvalidSettingsException("Whole-number bounds must be integers for " + group.m_prefix + '.');
                }
                for (int i = 0; i < group.m_count; i++) {
                    requireUnique(names, logicalName(group.m_prefix, group.m_count, i));
                }
                variableCount += group.m_count;
            }
        }
        if (m_encodedVariables != null) {
            for (EncodedVariableGroup group : m_encodedVariables) {
                requirePrefix(group == null ? null : group.m_prefix);
                if (group.m_count < 1 || group.m_bits < 1 || group.m_bits > 52) {
                    throw new InvalidSettingsException("Encoded counts and bits must be positive; bits may not exceed 52.");
                }
                validateBounds(group.m_prefix, group.m_lower, group.m_upper);
                for (int i = 0; i < group.m_count; i++) {
                    final String logical = logicalName(group.m_prefix, group.m_count, i);
                    requireUnique(names, logical);
                    for (int bit = 0; bit < group.m_bits; bit++) requireUnique(names, geneName(logical, bit));
                }
                variableCount += group.m_count;
            }
        }
        if (variableCount == 0) throw new InvalidSettingsException("Add at least one variable or encoded group.");
        if (m_objectives == null || m_objectives.length == 0) {
            throw new InvalidSettingsException("Add at least one objective.");
        }
        int references = 0;
        for (Objective objective : m_objectives) {
            if (objective == null || objective.m_name == null || objective.m_name.isBlank()) {
                throw new InvalidSettingsException("Every objective needs a result column.");
            }
            if (objective.m_direction == null) throw new InvalidSettingsException("Every objective needs a direction.");
            requireUnique(names, objective.m_name.trim());
            if (objective.m_hasReference) {
                if (!Double.isFinite(objective.m_reference)) {
                    throw new InvalidSettingsException("Hypervolume reference values must be finite.");
                }
                references++;
            }
        }
        if (references != 0 && references != m_objectives.length) {
            throw new InvalidSettingsException("Enable a reference value for every objective, or for none of them.");
        }
        if (m_constraints != null) {
            for (Constraint constraint : m_constraints) {
                if (constraint == null || constraint.m_name == null || constraint.m_name.isBlank()) {
                    throw new InvalidSettingsException("Every constraint needs a result column.");
                }
                if (constraint.m_relation == null) {
                    throw new InvalidSettingsException("Every constraint needs a relation.");
                }
                if (!Double.isFinite(constraint.m_threshold)) {
                    throw new InvalidSettingsException("Every constraint threshold must be finite.");
                }
                if (!Double.isFinite(constraint.m_tolerance) || constraint.m_tolerance < 0.0) {
                    throw new InvalidSettingsException("Equality tolerances must be finite and non-negative.");
                }
                requireUnique(names, constraint.m_name.trim());
            }
        }
    }

    private static void validateBounds(final String name, final double lower, final double upper)
            throws InvalidSettingsException {
        if (!Double.isFinite(lower) || !Double.isFinite(upper) || lower >= upper) {
            throw new InvalidSettingsException("The smallest value must be below the largest value for " + name + '.');
        }
    }

    private static void requirePrefix(final String prefix) throws InvalidSettingsException {
        if (prefix == null || prefix.isBlank()) throw new InvalidSettingsException("Every variable group needs a name.");
    }

    private static void requireUnique(final Set<String> names, final String rawName) throws InvalidSettingsException {
        final String name = rawName.trim();
        if (name.isEmpty()) throw new InvalidSettingsException("Generated column names may not be empty.");
        if (ReservedColumns.isReserved(name)) {
            throw new InvalidSettingsException("This column name is reserved by IdeaFlow: " + name);
        }
        if (!names.add(name)) throw new InvalidSettingsException("Duplicate generated column: " + name);
    }
}
