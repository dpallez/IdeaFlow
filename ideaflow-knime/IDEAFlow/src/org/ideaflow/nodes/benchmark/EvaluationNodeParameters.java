package org.ideaflow.nodes.benchmark;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
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
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
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
final class EvaluationNodeParameters implements NodeParameters {

  enum Method {
    @Label(
        value = "Built-in benchmark",
        description = "IdeaFlow calculates Ackley, OneMax, ZDT, or another included test problem.")
    BUILT_IN,
    @Label(
        value = "Write formulas here",
        description = "Calculate custom objectives and constraints with mathematical formulas.")
    FORMULAS,
    @Label(
        value = "Use results from previous nodes",
        description =
            "Python, ML, a simulation, or other KNIME nodes already created the result columns.")
    EXISTING_RESULTS
  }

  interface MethodRef extends ParameterReference<Method> {}

  interface ObjectiveFormulasRef extends ParameterReference<ObjectiveFormulaCard[]> {}

  interface ConstraintFormulasRef extends ParameterReference<ConstraintFormulaCard[]> {}

  static final class MethodPersistor implements NodeParametersPersistor<Method> {
    @Override
    public Method load(final NodeSettingsRO settings) throws InvalidSettingsException {
      final String value = settings.getString(EvaluationNodeModel.CFG_METHOD);
      try {
        return Method.valueOf(value);
      } catch (IllegalArgumentException exception) {
        throw new InvalidSettingsException("Unsupported evaluation method: " + value, exception);
      }
    }

    @Override
    public void save(final Method value, final NodeSettingsWO settings) {
      settings.addString(
          EvaluationNodeModel.CFG_METHOD, (value == null ? Method.BUILT_IN : value).name());
    }

    @Override
    public String[][] getConfigPaths() {
      return new String[][] {{EvaluationNodeModel.CFG_METHOD}};
    }
  }

  static final class IsBuiltIn implements EffectPredicateProvider {
    @Override
    public EffectPredicate init(final PredicateInitializer initializer) {
      return initializer.getEnum(MethodRef.class).isOneOf(Method.BUILT_IN);
    }
  }

  static final class UsesFormulas implements EffectPredicateProvider {
    @Override
    public EffectPredicate init(final PredicateInitializer initializer) {
      return initializer.getEnum(MethodRef.class).isOneOf(Method.FORMULAS);
    }
  }

  static final class SupportsFormulas implements EffectPredicateProvider {
    @Override
    public EffectPredicate init(final PredicateInitializer initializer) {
      return initializer.getEnum(MethodRef.class).isOneOf(Method.BUILT_IN, Method.FORMULAS);
    }
  }

  static final class BenchmarkChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return List.of(
          new StringChoice("ACKLEY", "Ackley"),
          new StringChoice("SPHERE", "Sphere"),
          new StringChoice("ROSENBROCK", "Rosenbrock"),
          new StringChoice("RASTRIGIN", "Rastrigin"),
          new StringChoice("GRIEWANK", "Griewank"),
          new StringChoice("ONEMAX", "OneMax"),
          new StringChoice("ZDT1", "ZDT1"),
          new StringChoice("ZDT2", "ZDT2"),
          new StringChoice("ZDT3", "ZDT3"),
          new StringChoice("DTLZ2", "DTLZ2"));
    }
  }

  static final class ObjectiveResultChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return problem(context)
          .map(
              metadata ->
                  metadata.objectives().stream()
                      .map(
                          objective ->
                              new StringChoice(
                                  objective.column(),
                                  objective.column()
                                      + " ("
                                      + objective.direction().name().toLowerCase()
                                      + " objective)"))
                      .toList())
          .orElse(List.of());
    }
  }

  static final class ConstraintResultChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return problem(context)
          .map(
              metadata ->
                  metadata.constraints().stream()
                      .map(
                          constraint ->
                              new StringChoice(
                                  constraint.column(), constraint.column() + " (constraint)"))
                      .toList())
          .orElse(List.of());
    }
  }

  static final class ObjectiveFormulaCard implements NodeParameters {
    @Widget(
        title = "Objective",
        description = "Objective result declared by the connected Problem Setup node.")
    @ChoicesProvider(ObjectiveResultChoices.class)
    String m_result = "";

    @Widget(
        title = "Formula",
        description = "Use variable names directly, for example (x - 1.5)^2 + (y - 4)^2.")
    @TextInputWidget(placeholder = "(x - 1.5)^2 + (y - 4)^2")
    String m_expression = "";
  }

  static final class ConstraintFormulaCard implements NodeParameters {
    @Widget(
        title = "Constraint",
        description = "Constraint result declared by the connected Problem Setup node.")
    @ChoicesProvider(ConstraintResultChoices.class)
    String m_result = "";

    @Widget(
        title = "Formula",
        description = "Enter the constraint's left-hand expression using the problem variables.")
    @TextInputWidget(placeholder = "x^2 + y^2")
    String m_expression = "";
  }

  static final class ObjectiveFormulaCardsPersistor
      implements NodeParametersPersistor<ObjectiveFormulaCard[]> {
    @Override
    public ObjectiveFormulaCard[] load(final NodeSettingsRO settings)
        throws InvalidSettingsException {
      return objectiveCards(
          EvaluationNodeModel.decodeFormulaSettings(
              settings.getString(EvaluationNodeModel.CFG_OBJECTIVE_FORMULAS)));
    }

    @Override
    public void save(final ObjectiveFormulaCard[] value, final NodeSettingsWO settings) {
      settings.addString(
          EvaluationNodeModel.CFG_OBJECTIVE_FORMULAS,
          EvaluationNodeModel.encodeFormulaSettings(objectiveDefinitions(value)));
    }

    @Override
    public String[][] getConfigPaths() {
      return new String[][] {{EvaluationNodeModel.CFG_OBJECTIVE_FORMULAS}};
    }
  }

  static final class ConstraintFormulaCardsPersistor
      implements NodeParametersPersistor<ConstraintFormulaCard[]> {
    @Override
    public ConstraintFormulaCard[] load(final NodeSettingsRO settings)
        throws InvalidSettingsException {
      return constraintCards(
          EvaluationNodeModel.decodeFormulaSettings(
              settings.getString(EvaluationNodeModel.CFG_CONSTRAINT_FORMULAS)));
    }

    @Override
    public void save(final ConstraintFormulaCard[] value, final NodeSettingsWO settings) {
      settings.addString(
          EvaluationNodeModel.CFG_CONSTRAINT_FORMULAS,
          EvaluationNodeModel.encodeFormulaSettings(constraintDefinitions(value)));
    }

    @Override
    public String[][] getConfigPaths() {
      return new String[][] {{EvaluationNodeModel.CFG_CONSTRAINT_FORMULAS}};
    }
  }

  static final class ObjectiveFormulaCardsProvider
      implements StateProvider<ObjectiveFormulaCard[]> {
    private Supplier<Method> m_method;
    private Supplier<ObjectiveFormulaCard[]> m_current;

    @Override
    public void init(final StateProviderInitializer initializer) {
      m_method = initializer.computeFromValueSupplier(MethodRef.class);
      m_current = initializer.getValueSupplier(ObjectiveFormulasRef.class);
      initializer.computeBeforeOpenDialog();
    }

    @Override
    public ObjectiveFormulaCard[] computeState(final NodeParametersInput context) {
      final ObjectiveFormulaCard[] current = valueOrEmpty(m_current.get());
      if (m_method.get() != Method.FORMULAS) return current;
      return problem(context)
          .map(
              metadata ->
                  objectiveCards(
                      synchronizeFormulaDefinitions(
                          metadata.objectiveNames(), objectiveDefinitions(current))))
          .orElse(current);
    }
  }

  static final class ConstraintFormulaCardsProvider
      implements StateProvider<ConstraintFormulaCard[]> {
    private Supplier<Method> m_method;
    private Supplier<ConstraintFormulaCard[]> m_current;

    @Override
    public void init(final StateProviderInitializer initializer) {
      m_method = initializer.computeFromValueSupplier(MethodRef.class);
      m_current = initializer.getValueSupplier(ConstraintFormulasRef.class);
      initializer.computeBeforeOpenDialog();
    }

    @Override
    public ConstraintFormulaCard[] computeState(final NodeParametersInput context) {
      final ConstraintFormulaCard[] current = valueOrEmpty(m_current.get());
      if (m_method.get() == Method.EXISTING_RESULTS) return current;
      return problem(context)
          .map(
              metadata ->
                  constraintCards(
                      synchronizeFormulaDefinitions(
                          metadata.constraints().stream()
                              .map(ProblemMetadata.Constraint::column)
                              .toList(),
                          constraintDefinitions(current))))
          .orElse(current);
    }
  }

  static final class FormulaHelp implements StateProvider<Optional<Message>> {
    @Override
    public void init(final StateProviderInitializer initializer) {
      initializer.computeBeforeOpenDialog();
    }

    @Override
    public Optional<Message> computeState(final NodeParametersInput context) {
      final String variables =
          problem(context)
              .map(metadata -> String.join(", ", metadata.evaluatorVariableNames()))
              .orElse("connect Problem Setup to see the available variables");
      return Optional.of(
          new Message(
              "Formula guide",
              "Required formula cards are created from Problem Setup. Available variables: "
                  + variables
                  + ". Use +, -, *, /, ^ and parentheses. "
                  + "Functions include abs, sqrt, exp, log, sin, cos, min, max and pow. "
                  + "Names containing spaces can be written as [variable name].",
              MessageType.INFO));
    }
  }

  @Widget(
      title = "How should candidates be evaluated?",
      description = "Choose where the objective and constraint-result values come from.")
  @ValueSwitchWidget
  @ValueReference(MethodRef.class)
  @Persistor(MethodPersistor.class)
  Method m_method = Method.BUILT_IN;

  @Widget(title = "Benchmark", description = "Included reference problem to calculate.")
  @ChoicesProvider(BenchmarkChoices.class)
  @Effect(predicate = IsBuiltIn.class, type = EffectType.SHOW)
  @Persist(configKey = EvaluationNodeModel.CFG_FUNCTION)
  String m_benchmark = "ACKLEY";

  @TextMessage(FormulaHelp.class)
  @Effect(predicate = SupportsFormulas.class, type = EffectType.SHOW)
  Void m_formulaHelp;

  @Widget(
      title = "Objective formulas",
      description =
          "One formula is created automatically for each objective declared in Problem Setup.")
  @ArrayWidget(
      elementTitle = "Objective formula", showSortButtons = false, hasFixedSize = true)
  @Effect(predicate = UsesFormulas.class, type = EffectType.SHOW)
  @ValueReference(ObjectiveFormulasRef.class)
  @ValueProvider(ObjectiveFormulaCardsProvider.class)
  @Persistor(ObjectiveFormulaCardsPersistor.class)
  ObjectiveFormulaCard[] m_objectiveFormulas = new ObjectiveFormulaCard[0];

  @Widget(
      title = "Constraint formulas",
      description =
          "One formula is created automatically for each constraint declared in Problem Setup.")
  @ArrayWidget(
      elementTitle = "Constraint formula", showSortButtons = false, hasFixedSize = true)
  @Effect(predicate = SupportsFormulas.class, type = EffectType.SHOW)
  @ValueReference(ConstraintFormulasRef.class)
  @ValueProvider(ConstraintFormulaCardsProvider.class)
  @Persistor(ConstraintFormulaCardsPersistor.class)
  ConstraintFormulaCard[] m_constraintFormulas = new ConstraintFormulaCard[0];

  @Override
  public void validate() throws InvalidSettingsException {
    if (m_method == Method.FORMULAS) {
      validateDefinitions(objectiveDefinitions(m_objectiveFormulas), "objective");
    }
    if (m_method == Method.BUILT_IN || m_method == Method.FORMULAS) {
      validateDefinitions(constraintDefinitions(m_constraintFormulas), "constraint");
    }
  }

  private static void validateDefinitions(
      final List<EvaluationNodeModel.FormulaDefinition> definitions, final String kind)
      throws InvalidSettingsException {
    final java.util.Set<String> results = new java.util.HashSet<>();
    for (EvaluationNodeModel.FormulaDefinition definition : definitions) {
      if (definition.result() == null || definition.result().isBlank()) {
        throw new InvalidSettingsException("Every " + kind + " formula needs a result.");
      }
      if (!results.add(definition.result())) {
        throw new InvalidSettingsException(
            "A result can only be calculated once: " + definition.result());
      }
      try {
        FormulaExpression.compile(definition.expression());
      } catch (IllegalArgumentException exception) {
        throw new InvalidSettingsException(
            "Invalid formula for " + definition.result() + ": " + exception.getMessage(),
            exception);
      }
    }
  }

  static List<EvaluationNodeModel.FormulaDefinition> synchronizeFormulaDefinitions(
      final List<String> required,
      final List<EvaluationNodeModel.FormulaDefinition> existing) {
    final Map<String, String> expressions = new LinkedHashMap<>();
    if (existing != null) {
      for (EvaluationNodeModel.FormulaDefinition definition : existing) {
        if (definition != null && definition.result() != null) {
          expressions.putIfAbsent(definition.result(), definition.expression());
        }
      }
    }
    return required.stream()
        .map(
            result ->
                new EvaluationNodeModel.FormulaDefinition(
                    result, expressions.getOrDefault(result, "")))
        .toList();
  }

  private static Optional<ProblemMetadata.Schema> problem(final NodeParametersInput context) {
    return context
        .getInTableSpec(1)
        .flatMap(
            spec -> {
              try {
                return Optional.of(ProblemMetadata.require(spec));
              } catch (InvalidSettingsException exception) {
                return Optional.empty();
              }
            });
  }

  private static List<EvaluationNodeModel.FormulaDefinition> objectiveDefinitions(
      final ObjectiveFormulaCard[] cards) {
    final List<EvaluationNodeModel.FormulaDefinition> definitions = new ArrayList<>();
    if (cards != null) {
      for (ObjectiveFormulaCard card : cards) {
        definitions.add(
            new EvaluationNodeModel.FormulaDefinition(
                card == null ? "" : card.m_result, card == null ? "" : card.m_expression));
      }
    }
    return List.copyOf(definitions);
  }

  private static List<EvaluationNodeModel.FormulaDefinition> constraintDefinitions(
      final ConstraintFormulaCard[] cards) {
    final List<EvaluationNodeModel.FormulaDefinition> definitions = new ArrayList<>();
    if (cards != null) {
      for (ConstraintFormulaCard card : cards) {
        definitions.add(
            new EvaluationNodeModel.FormulaDefinition(
                card == null ? "" : card.m_result, card == null ? "" : card.m_expression));
      }
    }
    return List.copyOf(definitions);
  }

  private static ObjectiveFormulaCard[] objectiveCards(
      final List<EvaluationNodeModel.FormulaDefinition> definitions) {
    return definitions.stream()
        .map(
            definition -> {
              final ObjectiveFormulaCard card = new ObjectiveFormulaCard();
              card.m_result = definition.result();
              card.m_expression = definition.expression();
              return card;
            })
        .toArray(ObjectiveFormulaCard[]::new);
  }

  private static ConstraintFormulaCard[] constraintCards(
      final List<EvaluationNodeModel.FormulaDefinition> definitions) {
    return definitions.stream()
        .map(
            definition -> {
              final ConstraintFormulaCard card = new ConstraintFormulaCard();
              card.m_result = definition.result();
              card.m_expression = definition.expression();
              return card;
            })
        .toArray(ConstraintFormulaCard[]::new);
  }

  private static ObjectiveFormulaCard[] valueOrEmpty(final ObjectiveFormulaCard[] value) {
    return value == null ? new ObjectiveFormulaCard[0] : value;
  }

  private static ConstraintFormulaCard[] valueOrEmpty(final ConstraintFormulaCard[] value) {
    return value == null ? new ConstraintFormulaCard[0] : value;
  }
}
