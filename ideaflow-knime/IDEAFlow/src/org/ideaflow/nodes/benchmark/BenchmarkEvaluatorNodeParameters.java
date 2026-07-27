package org.ideaflow.nodes.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.ideaflow.core.FormulaExpression;
import org.ideaflow.knime.KnimeTableSupport.ProblemMetadata;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.text.TextInputWidget;

/** Modern dialog for the unified exact-evaluation boundary. */
final class BenchmarkEvaluatorNodeParameters implements NodeParameters {

    enum Method {
        @Label(value = "Built-in benchmark",
            description = "IdeaFlow calculates Ackley, OneMax, ZDT, or another included test problem.")
        BUILT_IN,
        @Label(value = "Write formulas here",
            description = "Create objectives and constraint results with simple mathematical formulas.")
        FORMULAS,
        @Label(value = "Use results from previous nodes",
            description = "Python, ML, a simulation, or other KNIME nodes already created the result columns.")
        EXISTING_RESULTS
    }

    interface MethodRef extends ParameterReference<Method> { }

    static final class MethodPersistor implements NodeParametersPersistor<Method> {
        @Override public Method load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String value = settings.getString(BenchmarkEvaluatorNodeModel.CFG_METHOD, "BUILT_IN");
            try {
                return Method.valueOf(value);
            } catch (IllegalArgumentException exception) {
                throw new InvalidSettingsException("Unsupported evaluation method: " + value, exception);
            }
        }
        @Override public void save(final Method value, final NodeSettingsWO settings) {
            settings.addString(BenchmarkEvaluatorNodeModel.CFG_METHOD,
                (value == null ? Method.BUILT_IN : value).name());
        }
        @Override public String[][] getConfigPaths() {
            return new String[][]{{BenchmarkEvaluatorNodeModel.CFG_METHOD}};
        }
    }

    static final class IsBuiltIn implements EffectPredicateProvider {
        @Override public EffectPredicate init(final PredicateInitializer initializer) {
            return initializer.getEnum(MethodRef.class).isOneOf(Method.BUILT_IN);
        }
    }

    static final class UsesFormulas implements EffectPredicateProvider {
        @Override public EffectPredicate init(final PredicateInitializer initializer) {
            return initializer.getEnum(MethodRef.class).isOneOf(Method.FORMULAS);
        }
    }

    static final class BenchmarkChoices implements StringChoicesProvider {
        @Override public List<StringChoice> computeState(final NodeParametersInput context) {
            return List.of(new StringChoice("ACKLEY", "Ackley"), new StringChoice("SPHERE", "Sphere"),
                new StringChoice("ROSENBROCK", "Rosenbrock"), new StringChoice("RASTRIGIN", "Rastrigin"),
                new StringChoice("GRIEWANK", "Griewank"), new StringChoice("ONEMAX", "OneMax"),
                new StringChoice("ZDT1", "ZDT1"), new StringChoice("ZDT2", "ZDT2"),
                new StringChoice("ZDT3", "ZDT3"), new StringChoice("DTLZ2", "DTLZ2"));
        }
    }

    static final class ResultChoices implements StringChoicesProvider {
        @Override public List<StringChoice> computeState(final NodeParametersInput context) {
            return context.getInTableSpec(1).map(spec -> {
                try {
                    final ProblemMetadata.Schema problem = ProblemMetadata.require(spec);
                    final List<StringChoice> choices = new ArrayList<>();
                    problem.objectives().forEach(objective -> choices.add(new StringChoice(objective.column(),
                        objective.column() + " (objective, " + objective.direction().name().toLowerCase() + ")")));
                    problem.constraints().forEach(constraint -> choices.add(new StringChoice(constraint.column(),
                        constraint.column() + " (constraint result)")));
                    return List.copyOf(choices);
                } catch (InvalidSettingsException exception) {
                    return List.<StringChoice>of();
                }
            }).orElse(List.of());
        }
    }

    static final class FormulaCard implements NodeParameters {
        @Widget(title = "Result to calculate",
            description = "Choose an objective or constraint-result column declared in Problem Setup.")
        @ChoicesProvider(ResultChoices.class)
        String m_result = "";

        @Widget(title = "Formula",
            description = "Use variable names directly, for example (x - 1.5)^2 + (y - 4)^2.")
        @TextInputWidget(placeholder = "(x - 1.5)^2 + (y - 4)^2")
        String m_expression = "";
    }

    static final class FormulaCardsPersistor implements NodeParametersPersistor<FormulaCard[]> {
        @Override public FormulaCard[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return BenchmarkEvaluatorNodeModel.decodeFormulaSettings(
                settings.getString(BenchmarkEvaluatorNodeModel.CFG_FORMULAS)).stream().map(definition -> {
                    final FormulaCard card = new FormulaCard();
                    card.m_result = definition.result();
                    card.m_expression = definition.expression();
                    return card;
                }).toArray(FormulaCard[]::new);
        }

        @Override public void save(final FormulaCard[] value, final NodeSettingsWO settings) {
            final List<BenchmarkEvaluatorNodeModel.FormulaDefinition> definitions = new ArrayList<>();
            if (value != null) {
                for (FormulaCard card : value) {
                    definitions.add(new BenchmarkEvaluatorNodeModel.FormulaDefinition(
                        card == null ? "" : card.m_result, card == null ? "" : card.m_expression));
                }
            }
            settings.addString(BenchmarkEvaluatorNodeModel.CFG_FORMULAS,
                BenchmarkEvaluatorNodeModel.encodeFormulaSettings(definitions));
        }

        @Override public String[][] getConfigPaths() {
            return new String[][]{{BenchmarkEvaluatorNodeModel.CFG_FORMULAS}};
        }
    }

    static final class FormulaHelp implements org.knime.node.parameters.updates.StateProvider<Optional<Message>> {
        @Override public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override public Optional<Message> computeState(final NodeParametersInput context) {
            final String variables = context.getInTableSpec(1).flatMap(spec -> {
                try {
                    return Optional.of(String.join(", ", ProblemMetadata.require(spec).evaluatorVariableNames()));
                } catch (InvalidSettingsException exception) {
                    return Optional.empty();
                }
            }).orElse("connect Problem Setup to see the available variables");
            return Optional.of(new Message("Formula guide",
                "Available variables: " + variables + ". Use +, -, *, /, ^ and parentheses. "
                + "Functions include abs, sqrt, exp, log, sin, cos, min, max and pow. "
                + "Names containing spaces can be written as [variable name]. Example: "
                + "(x - 1.5)^2 + (y - 4)^2 + 2*(1 - b).",
                MessageType.INFO));
        }
    }

    @Widget(title = "How should candidates be evaluated?",
        description = "Choose where the objective and constraint-result values come from.")
    @ValueSwitchWidget
    @ValueReference(MethodRef.class)
    @Persistor(MethodPersistor.class)
    Method m_method = Method.BUILT_IN;

    @Widget(title = "Benchmark", description = "Included reference problem to calculate.")
    @ChoicesProvider(BenchmarkChoices.class)
    @Effect(predicate = IsBuiltIn.class, type = EffectType.SHOW)
    @Persist(configKey = BenchmarkEvaluatorNodeModel.CFG_FUNCTION)
    String m_benchmark = "ACKLEY";

    @TextMessage(FormulaHelp.class)
    @Effect(predicate = UsesFormulas.class, type = EffectType.SHOW)
    Void m_formulaHelp;

    @Widget(title = "Results and formulas",
        description = "Add one card for every objective and constraint result declared in Problem Setup.")
    @ArrayWidget(addButtonText = "Add result formula", elementTitle = "Result formula", showSortButtons = true)
    @Effect(predicate = UsesFormulas.class, type = EffectType.SHOW)
    @Persistor(FormulaCardsPersistor.class)
    FormulaCard[] m_formulas = new FormulaCard[0];

    @Override public void validate() throws InvalidSettingsException {
        if (m_method == Method.FORMULAS) {
            if (m_formulas == null || m_formulas.length == 0) {
                throw new InvalidSettingsException("Add at least one result formula.");
            }
            final java.util.Set<String> results = new java.util.HashSet<>();
            for (FormulaCard formula : m_formulas) {
                if (formula == null || formula.m_result == null || formula.m_result.isBlank()) {
                    throw new InvalidSettingsException("Choose a result for every formula card.");
                }
                if (!results.add(formula.m_result)) {
                    throw new InvalidSettingsException("A result can only be calculated once: " + formula.m_result);
                }
                try {
                    FormulaExpression.compile(formula.m_expression);
                } catch (IllegalArgumentException exception) {
                    throw new InvalidSettingsException(
                        "Invalid formula for " + formula.m_result + ": " + exception.getMessage(), exception);
                }
            }
        }
    }
}
