package org.ideaflow.knime;

import java.util.Arrays;
import java.util.List;
import org.ideaflow.api.ReservedColumns;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
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
import org.knime.node.parameters.updates.EffectPredicateProvider.PredicateInitializer;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/** Modern parameter definitions for the NodeModel-backed nodes. */
public final class ModernNodeParameters {
  private ModernNodeParameters() {}

  public static final class DirectionChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return List.of(
          new StringChoice("MINIMIZE", "Minimize - smaller is better"),
          new StringChoice("MAXIMIZE", "Maximize - larger is better"));
    }
  }

  public static final class TargetRuleChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return List.of(
          new StringChoice("ALL", "All targets - one individual must reach every target"),
          new StringChoice("ANY", "Any target - stop when one configured target is reached"));
    }
  }

  public static final class ProblemObjectiveChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return context
          .getInTableSpec(0)
          .map(
              spec -> {
                try {
                  return KnimeTableSupport.ProblemMetadata.require(spec).objectives().stream()
                      .map(
                          objective ->
                              new StringChoice(
                                  objective.column(),
                                  objective.column()
                                      + " ("
                                      + objective.direction().name().toLowerCase()
                                      + ")"))
                      .toList();
                } catch (InvalidSettingsException exception) {
                  return List.<StringChoice>of();
                }
              })
          .orElse(List.of());
    }
  }

  public static final class MigrationTopologyChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return List.of(
          new StringChoice("RING", "Ring - A to B, B to C, C to A"),
          new StringChoice("RANDOM", "Random destination - reproducible from the seed"),
          new StringChoice("ALL_TO_ALL", "All-to-all - send to every other population"));
    }
  }

  public static final class MigrationReplacementChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return List.of(
          new StringChoice("REPLACE_WORST", "Replace the weakest candidates"),
          new StringChoice("ADD", "Add migrants without removing candidates"));
    }
  }

  public static final class RepairChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return List.of(
          new StringChoice("REFLECT", "Reflect at the boundary"),
          new StringChoice("CLAMP", "Use the nearest boundary"),
          new StringChoice("RANDOM", "Generate a new valid value"));
    }
  }

  /** Numeric, non-reserved columns from the first table input. */
  public static final class NumericColumnChoices implements ColumnChoicesProvider {
    @Override
    public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
      return context
          .getInTableSpec(0)
          .map(
              spec -> {
                final java.util.ArrayList<DataColumnSpec> result = new java.util.ArrayList<>();
                for (DataColumnSpec column : spec) {
                  final Class<?> value = column.getType().getPreferredValueClass();
                  if (!ReservedColumns.isReserved(column.getName())
                      && (DoubleValue.class.isAssignableFrom(value)
                          || IntValue.class.isAssignableFrom(value)
                          || LongValue.class.isAssignableFrom(value))) {
                    result.add(column);
                  }
                }
                return List.copyOf(result);
              })
          .orElse(List.of());
    }
  }

  /** Every column from the first table input, for lossless sidecar exports. */
  public static final class AllColumnChoices implements ColumnChoicesProvider {
    @Override
    public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
      return context
          .getInTableSpec(0)
          .map(
              spec -> {
                final java.util.ArrayList<DataColumnSpec> result = new java.util.ArrayList<>();
                for (DataColumnSpec column : spec) result.add(column);
                return List.copyOf(result);
              })
          .orElse(List.of());
    }
  }

  private abstract static class CsvArrayPersistor implements NodeParametersPersistor<String[]> {
    private final String m_key;

    CsvArrayPersistor(final String key) {
      m_key = key;
    }

    @Override
    public String[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
      final String value = settings.getString(m_key);
      return value == null || value.isBlank()
          ? new String[0]
          : Arrays.stream(value.split(","))
              .map(String::trim)
              .filter(item -> !item.isEmpty())
              .toArray(String[]::new);
    }

    @Override
    public void save(final String[] value, final NodeSettingsWO settings) {
      settings.addString(m_key, value == null ? "" : String.join(",", value));
    }

    @Override
    public String[][] getConfigPaths() {
      return new String[][] {{m_key}};
    }
  }

  public static final class AdditionalPropertiesPersistor extends CsvArrayPersistor {
    public AdditionalPropertiesPersistor() {
      super("additional_properties");
    }
  }

  private abstract static class DefaultStringPersistor implements NodeParametersPersistor<String> {
    private final String m_key;
    private final String m_default;

    DefaultStringPersistor(final String key, final String defaultValue) {
      m_key = key;
      m_default = defaultValue;
    }

    @Override
    public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
      return settings.getString(m_key);
    }

    @Override
    public void save(final String value, final NodeSettingsWO settings) {
      settings.addString(m_key, value == null ? m_default : value);
    }

    @Override
    public String[][] getConfigPaths() {
      return new String[][] {{m_key}};
    }
  }

  private abstract static class DefaultDoublePersistor implements NodeParametersPersistor<Double> {
    private final String m_key;
    private final double m_default;

    DefaultDoublePersistor(final String key, final double defaultValue) {
      m_key = key;
      m_default = defaultValue;
    }

    @Override
    public Double load(final NodeSettingsRO settings) throws InvalidSettingsException {
      return settings.getDouble(m_key);
    }

    @Override
    public void save(final Double value, final NodeSettingsWO settings) {
      settings.addDouble(m_key, value == null ? m_default : value);
    }

    @Override
    public String[][] getConfigPaths() {
      return new String[][] {{m_key}};
    }
  }

  private abstract static class DefaultIntPersistor implements NodeParametersPersistor<Integer> {
    private final String m_key;
    private final int m_default;

    DefaultIntPersistor(final String key, final int defaultValue) {
      m_key = key;
      m_default = defaultValue;
    }

    @Override
    public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
      return settings.getInt(m_key);
    }

    @Override
    public void save(final Integer value, final NodeSettingsWO settings) {
      settings.addInt(m_key, value == null ? m_default : value);
    }

    @Override
    public String[][] getConfigPaths() {
      return new String[][] {{m_key}};
    }
  }

  private abstract static class EnumNamePersistor<E extends Enum<E>>
      implements NodeParametersPersistor<E> {
    private final String m_key;
    private final Class<E> m_type;
    private final E m_default;

    EnumNamePersistor(final String key, final Class<E> type, final E defaultValue) {
      m_key = key;
      m_type = type;
      m_default = defaultValue;
    }

    @Override
    public E load(final NodeSettingsRO settings) throws InvalidSettingsException {
      final String value = settings.getString(m_key);
      try {
        return Enum.valueOf(m_type, value);
      } catch (IllegalArgumentException exception) {
        throw new InvalidSettingsException(
            "Unsupported setting for " + m_key + ": " + value, exception);
      }
    }

    @Override
    public void save(final E value, final NodeSettingsWO settings) {
      settings.addString(m_key, (value == null ? m_default : value).name());
    }

    @Override
    public String[][] getConfigPaths() {
      return new String[][] {{m_key}};
    }
  }

  public static final class PbestRatePersistor extends DefaultDoublePersistor {
    public PbestRatePersistor() {
      super("pbest_rate", 0.2);
    }
  }

  public static final class DifferentialWeightPersistor extends DefaultDoublePersistor {
    public DifferentialWeightPersistor() {
      super("differential_weight", 0.5);
    }
  }

  public static final class InitialCrPersistor extends DefaultDoublePersistor {
    public InitialCrPersistor() {
      super("initial_cr", 0.9);
    }
  }

  public static final class TauFPersistor extends DefaultDoublePersistor {
    public TauFPersistor() {
      super("tau_f", 0.1);
    }
  }

  public static final class TauCrPersistor extends DefaultDoublePersistor {
    public TauCrPersistor() {
      super("tau_cr", 0.1);
    }
  }

  public static final class MemorySizePersistor extends DefaultIntPersistor {
    public MemorySizePersistor() {
      super("memory_size", 6);
    }
  }

  public static final class MinimumPopulationSizePersistor extends DefaultIntPersistor {
    public MinimumPopulationSizePersistor() {
      super("minimum_population_size", 4);
    }
  }

  public static final class TargetRulePersistor extends DefaultStringPersistor {
    public TargetRulePersistor() {
      super("target_rule", "ALL");
    }
  }

  public static final class BoundsRepairPersistor extends DefaultStringPersistor {
    public BoundsRepairPersistor() {
      super("bounds_repair", "REFLECT");
    }
  }

  public static final class TargetCondition implements NodeParameters {
    @Widget(title = "Objective", description = "Objective declared in Problem Setup.")
    @ChoicesProvider(ProblemObjectiveChoices.class)
    public String objective = "";

    @Widget(
        title = "Target value",
        description =
            "Stop when this value is reached according to the objective's minimize/maximize direction.")
    public double value = 0.0;
  }

  public static final class TargetConditionsPersistor
      implements NodeParametersPersistor<TargetCondition[]> {
    private static final String KEY = "target_conditions";

    @Override
    public TargetCondition[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
      final String encoded = settings.getString(KEY);
      if (encoded == null || encoded.isBlank()) return new TargetCondition[0];
      final String[] items = encoded.split(",");
      final TargetCondition[] result = new TargetCondition[items.length];
      try {
        for (int index = 0; index < items.length; index++) {
          final int separator = items[index].indexOf(':');
          if (separator < 1) throw new IllegalArgumentException();
          final TargetCondition condition = new TargetCondition();
          condition.objective =
              new String(
                  java.util.Base64.getUrlDecoder().decode(items[index].substring(0, separator)),
                  java.nio.charset.StandardCharsets.UTF_8);
          condition.value = Double.parseDouble(items[index].substring(separator + 1));
          result[index] = condition;
        }
      } catch (IllegalArgumentException exception) {
        throw new InvalidSettingsException("Invalid objective target settings.", exception);
      }
      return result;
    }

    @Override
    public void save(final TargetCondition[] value, final NodeSettingsWO settings) {
      if (value == null || value.length == 0) {
        settings.addString(KEY, "");
        return;
      }
      settings.addString(
          KEY,
          java.util.Arrays.stream(value)
              .map(
                  condition ->
                      java.util.Base64.getUrlEncoder()
                              .withoutPadding()
                              .encodeToString(
                                  condition.objective.getBytes(
                                      java.nio.charset.StandardCharsets.UTF_8))
                          + ":"
                          + Double.toString(condition.value))
              .collect(java.util.stream.Collectors.joining(",")));
    }

    @Override
    public String[][] getConfigPaths() {
      return new String[][] {{KEY}};
    }
  }

  public static final class InitialPopulation implements NodeParameters {
    @Widget(title = "Population size", description = "Initial individuals created for every run.")
    @Persist(configKey = "population_size")
    public int populationSize = 50;

    @Widget(title = "Population ID", description = "Island, species, or population identifier.")
    @Persist(configKey = "population_id")
    public String populationId = "population-0";
  }

  public static final class Selection implements NodeParameters {
    enum Strategy {
      @Label(value = "Tournament", description = "Prefer candidates that perform better.")
      TOURNAMENT,
      @Label(value = "Random", description = "Select without considering objective values.")
      RANDOM,
      @Label(
          value = "Differential Evolution donors",
          description = "Prepare target and donor vectors for DE mutation.")
      DE_DONORS
    }

    interface StrategyRef extends ParameterReference<Strategy> {}

    static final class StrategyPersistor extends EnumNamePersistor<Strategy> {
      StrategyPersistor() {
        super("selection_strategy", Strategy.class, Strategy.TOURNAMENT);
      }
    }

    static final class UsesTournament implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer.getEnum(StrategyRef.class).isOneOf(Strategy.TOURNAMENT);
      }
    }

    static final class UsesOrdinarySelection implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer.getEnum(StrategyRef.class).isOneOf(Strategy.TOURNAMENT, Strategy.RANDOM);
      }
    }

    static final class UsesDeDonors implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer.getEnum(StrategyRef.class).isOneOf(Strategy.DE_DONORS);
      }
    }

    @Widget(title = "Selection strategy", description = "Parent sampling method.")
    @ValueSwitchWidget
    @ValueReference(StrategyRef.class)
    @Persistor(StrategyPersistor.class)
    Strategy strategy = Strategy.TOURNAMENT;

    @Widget(title = "Tournament size", description = "Candidates compared in each tournament.")
    @Effect(predicate = UsesTournament.class, type = EffectType.SHOW)
    @Persist(configKey = "tournament_size")
    public int tournamentSize = 2;

    @Widget(
        title = "Parents per population",
        description = "Number of selected parent rows per run and population.")
    @Effect(predicate = UsesOrdinarySelection.class, type = EffectType.SHOW)
    @Persist(configKey = "parent_count")
    public int parentCount = 100;

    @Widget(title = "Allow duplicate selections", description = "Sample parents with replacement.")
    @Effect(predicate = UsesOrdinarySelection.class, type = EffectType.SHOW)
    @Persist(configKey = "with_replacement")
    public boolean withReplacement = true;

    @Widget(
        title = "DE p-best rate",
        description = "Top fraction eligible as p-best donors for current-to-pbest mutation.")
    @Effect(predicate = UsesDeDonors.class, type = EffectType.SHOW)
    @Persistor(PbestRatePersistor.class)
    public double pbest = 0.2;
  }

  public static final class Crossover implements NodeParameters {
    enum Strategy {
      @Label(value = "SBX", description = "Simulated binary crossover for continuous variables.")
      SBX,
      @Label(value = "Uniform", description = "Choose every value from either parent.")
      UNIFORM,
      @Label(value = "One-point", description = "Split and exchange sequences at one point.")
      ONE_POINT,
      @Label(value = "Arithmetic", description = "Blend continuous parent values.")
      ARITHMETIC,
      @Label(value = "DE binomial", description = "Binomial Differential Evolution crossover.")
      DE_BINOMIAL,
      @Label(
          value = "DE exponential",
          description = "Exponential Differential Evolution crossover.")
      DE_EXPONENTIAL
    }

    interface StrategyRef extends ParameterReference<Strategy> {}

    static final class StrategyPersistor extends EnumNamePersistor<Strategy> {
      StrategyPersistor() {
        super("strategy", Strategy.class, Strategy.SBX);
      }
    }

    static final class UsesOrdinaryCrossover implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer
            .getEnum(StrategyRef.class)
            .isOneOf(Strategy.SBX, Strategy.UNIFORM, Strategy.ONE_POINT, Strategy.ARITHMETIC);
      }
    }

    static final class UsesSbx implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer.getEnum(StrategyRef.class).isOneOf(Strategy.SBX);
      }
    }

    @Widget(title = "Strategy", description = "Recombination strategy.")
    @ValueReference(StrategyRef.class)
    @Persistor(StrategyPersistor.class)
    Strategy strategy = Strategy.SBX;

    @Widget(
        title = "Crossover probability",
        description = "Probability of recombining each parent pair.")
    @Effect(predicate = UsesOrdinaryCrossover.class, type = EffectType.SHOW)
    @Persist(configKey = "probability")
    public double probability = 0.9;

    @Widget(
        title = "SBX distribution index",
        description = "Larger values create children closer to their parents.")
    @Effect(predicate = UsesSbx.class, type = EffectType.SHOW)
    @Persist(configKey = "distribution_index")
    public double distributionIndex = 20.0;
  }

  public static final class Mutation implements NodeParameters {
    enum Strategy {
      @Label(value = "Polynomial", description = "Bounded mutation for continuous variables.")
      POLYNOMIAL,
      @Label(
          value = "Gaussian",
          description = "Add normally distributed noise to continuous variables.")
      GAUSSIAN,
      @Label(value = "Bit flip", description = "Invert binary values.")
      BIT_FLIP,
      @Label(value = "Random reset", description = "Draw a new integer or categorical value.")
      RANDOM_RESET,
      @Label(value = "DE/rand/1", description = "Random base plus one difference vector.")
      DE_RAND_1,
      @Label(value = "DE/best/1", description = "Best candidate as the base vector.")
      DE_BEST_1,
      @Label(
          value = "DE/current-to-best/1",
          description = "Move the target toward the best candidate.")
      DE_CURRENT_TO_BEST_1,
      @Label(
          value = "DE/current-to-pbest/1",
          description = "Move toward a randomly chosen top candidate; used by SHADE/L-SHADE.")
      DE_CURRENT_TO_PBEST_1
    }

    enum Adaptation {
      @Label(value = "Fixed F and CR")
      FIXED,
      @Label(value = "jDE self-adaptation")
      JDE,
      @Label(value = "SHADE success-history")
      SHADE
    }

    interface StrategyRef extends ParameterReference<Strategy> {}

    interface AdaptationRef extends ParameterReference<Adaptation> {}

    interface AutoRateRef extends BooleanReference {}

    static final class StrategyPersistor extends EnumNamePersistor<Strategy> {
      StrategyPersistor() {
        super("strategy", Strategy.class, Strategy.POLYNOMIAL);
      }
    }

    static final class AdaptationPersistor extends EnumNamePersistor<Adaptation> {
      AdaptationPersistor() {
        super("adaptation_mode", Adaptation.class, Adaptation.FIXED);
      }
    }

    static EffectPredicate ordinary(final PredicateInitializer initializer) {
      return initializer
          .getEnum(StrategyRef.class)
          .isOneOf(
              Strategy.POLYNOMIAL, Strategy.GAUSSIAN, Strategy.BIT_FLIP, Strategy.RANDOM_RESET);
    }

    static EffectPredicate differential(final PredicateInitializer initializer) {
      return initializer
          .getEnum(StrategyRef.class)
          .isOneOf(
              Strategy.DE_RAND_1, Strategy.DE_BEST_1,
              Strategy.DE_CURRENT_TO_BEST_1, Strategy.DE_CURRENT_TO_PBEST_1);
    }

    static final class UsesOrdinaryMutation implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return ordinary(initializer);
      }
    }

    static final class UsesManualRate implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return ordinary(initializer).and(initializer.getBoolean(AutoRateRef.class).isFalse());
      }
    }

    static final class UsesGaussian implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer.getEnum(StrategyRef.class).isOneOf(Strategy.GAUSSIAN);
      }
    }

    static final class UsesPolynomial implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer.getEnum(StrategyRef.class).isOneOf(Strategy.POLYNOMIAL);
      }
    }

    static final class UsesDifferentialMutation implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return differential(initializer);
      }
    }

    static final class UsesJde implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return differential(initializer)
            .and(initializer.getEnum(AdaptationRef.class).isOneOf(Adaptation.JDE));
      }
    }

    static final class UsesShade implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return differential(initializer)
            .and(initializer.getEnum(AdaptationRef.class).isOneOf(Adaptation.SHADE));
      }
    }

    @Widget(title = "Strategy", description = "Mutation strategy.")
    @ValueReference(StrategyRef.class)
    @Persistor(StrategyPersistor.class)
    Strategy strategy = Strategy.POLYNOMIAL;

    @Widget(
        title = "Automatic mutation rate",
        description = "Use one divided by the variable count.")
    @ValueReference(AutoRateRef.class)
    @Effect(predicate = UsesOrdinaryMutation.class, type = EffectType.SHOW)
    @Persist(configKey = "auto_rate")
    public boolean autoRate = true;

    @Widget(
        title = "Manual mutation probability",
        description = "Used only when automatic probability is disabled.")
    @Effect(predicate = UsesManualRate.class, type = EffectType.SHOW)
    @Persist(configKey = "mutation_rate")
    public double mutationRate = 0.1;

    @Widget(
        title = "Gaussian mutation strength",
        description = "Standard deviation as a fraction of the variable range.")
    @Effect(predicate = UsesGaussian.class, type = EffectType.SHOW)
    @Persist(configKey = "gaussian_scale")
    public double gaussianScale = 0.1;

    @Widget(
        title = "Polynomial distribution index",
        description = "Larger values make smaller local changes.")
    @Effect(predicate = UsesPolynomial.class, type = EffectType.SHOW)
    @Persist(configKey = "distribution_index")
    public double distributionIndex = 20.0;

    @Widget(
        title = "DE parameter control",
        description =
            "Use fixed F and CR, jDE self-adaptation, or SHADE success-history adaptation.")
    @ValueSwitchWidget
    @ValueReference(AdaptationRef.class)
    @Effect(predicate = UsesDifferentialMutation.class, type = EffectType.SHOW)
    @Persistor(AdaptationPersistor.class)
    Adaptation adaptation = Adaptation.FIXED;

    @Widget(
        title = "Initial or fixed F",
        description =
            "Differential weight used directly in fixed mode or as the initial adaptive value.")
    @Effect(predicate = UsesDifferentialMutation.class, type = EffectType.SHOW)
    @Persistor(DifferentialWeightPersistor.class)
    public double f = 0.5;

    @Widget(
        title = "Initial or fixed CR",
        description = "Crossover rate stored for DE Crossover; also initializes jDE or SHADE.")
    @Effect(predicate = UsesDifferentialMutation.class, type = EffectType.SHOW)
    @Persistor(InitialCrPersistor.class)
    public double cr = 0.9;

    @Widget(
        title = "jDE F adaptation probability",
        description = "Probability that an individual samples a new F.")
    @Effect(predicate = UsesJde.class, type = EffectType.SHOW)
    @Persistor(TauFPersistor.class)
    public double tauF = 0.1;

    @Widget(
        title = "jDE CR adaptation probability",
        description = "Probability that an individual samples a new CR.")
    @Effect(predicate = UsesJde.class, type = EffectType.SHOW)
    @Persistor(TauCrPersistor.class)
    public double tauCr = 0.1;

    @Widget(title = "SHADE memory size", description = "Number of successful F/CR history entries.")
    @Effect(predicate = UsesShade.class, type = EffectType.SHOW)
    @Persistor(MemorySizePersistor.class)
    public int memorySize = 6;

    @Widget(
        title = "DE bounds repair",
        description = "How out-of-range differential mutants are repaired.")
    @ChoicesProvider(RepairChoices.class)
    @Effect(predicate = UsesDifferentialMutation.class, type = EffectType.SHOW)
    @Persistor(BoundsRepairPersistor.class)
    public String repair = "REFLECT";
  }

  public static final class RankParetoSolutions implements NodeParameters {
    // Objectives, directions, and constraint accounting come from Problem Setup.
  }

  public static final class ReferenceDirections implements NodeParameters {
    @Widget(title = "Das-Dennis divisions", description = "Simplex lattice divisions.")
    @Persist(configKey = "divisions")
    public int divisions = 12;
  }

  public static final class Elitism implements NodeParameters {
    enum Mode {
      @Label(value = "Elitism", description = "Keep the best candidates for one objective.")
      SINGLE_OBJECTIVE,
      @Label(
          value = "DE competition",
          description = "Each DE child competes with its target parent.")
      DE_PAIRWISE,
      @Label(value = "NSGA-II", description = "Use Pareto rank and crowding.")
      NSGA_II,
      @Label(value = "NSGA-III", description = "Use Pareto rank and reference directions.")
      NSGA_III,
      @Label(value = "GDE3", description = "Use constraint-aware multiobjective DE competition.")
      GDE3
    }

    enum SizePolicy {
      @Label(value = "Fixed", description = "Keep the current population size.")
      FIXED,
      @Label(
          value = "Linear NFE reduction",
          description = "Reduce the population linearly as evaluations are consumed.")
      LINEAR_NFE
    }

    interface ModeRef extends ParameterReference<Mode> {}

    interface SizePolicyRef extends ParameterReference<SizePolicy> {}

    static final class ModePersistor extends EnumNamePersistor<Mode> {
      ModePersistor() {
        super("update_mode", Mode.class, Mode.NSGA_II);
      }
    }

    static final class SizePolicyPersistor extends EnumNamePersistor<SizePolicy> {
      SizePolicyPersistor() {
        super("population_size_policy", SizePolicy.class, SizePolicy.FIXED);
      }
    }

    static final class UsesNsga3 implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer.getEnum(ModeRef.class).isOneOf(Mode.NSGA_III);
      }
    }

    static final class UsesLinearReduction implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer.getEnum(SizePolicyRef.class).isOneOf(SizePolicy.LINEAR_NFE);
      }
    }

    @Widget(title = "Update mode", description = "Survivor or optimizer-state update strategy.")
    @ValueReference(ModeRef.class)
    @Persistor(ModePersistor.class)
    Mode mode = Mode.NSGA_II;

    @Widget(
        title = "NSGA-III reference divisions",
        description = "Controls the resolution of NSGA-III reference directions.")
    @Effect(predicate = UsesNsga3.class, type = EffectType.SHOW)
    @Persist(configKey = "reference_divisions")
    public int referenceDivisions = 12;

    @Widget(
        title = "Population size policy",
        description = "Keep the original size, or reduce it linearly according to NFE for L-SHADE.")
    @ValueSwitchWidget
    @ValueReference(SizePolicyRef.class)
    @Persistor(SizePolicyPersistor.class)
    SizePolicy populationSizePolicy = SizePolicy.FIXED;

    @Widget(
        title = "Final population size",
        description = "Population size at the maximum NFE. Used only with linear reduction.")
    @Effect(predicate = UsesLinearReduction.class, type = EffectType.SHOW)
    @Persistor(MinimumPopulationSizePersistor.class)
    public int minimumPopulationSize = 4;
  }

  /** Settings for the beginner-facing native evolutionary loop end. */
  public static final class OptimizationLoopEnd implements NodeParameters {
    interface TargetsRef extends ParameterReference<TargetCondition[]> {}

    static final class HasSeveralTargets implements EffectPredicateProvider {
      @Override
      public EffectPredicate init(final PredicateInitializer initializer) {
        return initializer.getArray(TargetsRef.class).hasMultipleItems();
      }
    }

    @Widget(
        title = "Objective targets",
        description =
            "Optional stopping targets. With no targets, the loop stops only at the NFE budget.")
    @ArrayWidget(
        addButtonText = "Add objective target",
        elementTitle = "Objective target",
        showSortButtons = true)
    @ValueReference(TargetsRef.class)
    @Persistor(TargetConditionsPersistor.class)
    public TargetCondition[] targets = new TargetCondition[0];

    @Widget(
        title = "When several targets are configured",
        description =
            "Choose whether any target is enough, or one individual must reach all targets.")
    @ChoicesProvider(TargetRuleChoices.class)
    @Effect(predicate = HasSeveralTargets.class, type = EffectType.SHOW)
    @Persistor(TargetRulePersistor.class)
    public String targetRule = "ALL";
  }

  public static final class TrackProgress implements NodeParameters {
    @Widget(
        title = "Recorded stage name",
        description =
            "Human-readable label stored in detailed event rows, for example evaluated population or survivors.")
    @Persist(configKey = "stage")
    public String stage = "population";
  }

  public static final class OptimizationRunAnalysis implements NodeParameters {
    @Widget(
        title = "Series column",
        description = "Algorithm, method, or stage label used to draw separate curves.")
    @Persist(configKey = "series_column")
    public String seriesColumn = "Series";

    @Widget(
        title = "Run column",
        description = "Stable identifier for one independent optimization run.")
    @Persist(configKey = "run_column")
    public String runColumn = "Run";

    @Widget(title = "Evaluation-budget column", description = "Horizontal-axis evaluation count.")
    @Persist(configKey = "nfe_column")
    public String nfeColumn = "NFE";

    @Widget(
        title = "Performance column",
        description = "Scalar value summarized for convergence and compared with the ECDF target.")
    @Persist(configKey = "performance_column")
    public String performanceColumn = "Best";

    @Widget(
        title = "Preferred direction",
        description =
            "Infer from IdeaFlow metadata, or explicitly choose whether smaller or larger values are better.")
    @ChoicesProvider(AnalysisDirectionChoices.class)
    @Persist(configKey = "performance_direction")
    public String direction = "AUTO";

    @Widget(
        title = "Per-run success target",
        description =
            "Used only for Success and First hit NFE in the Runs output; the final-fitness ECDF does not use a target.")
    @Persist(configKey = "target")
    public double target = 0.1;

    @Widget(
        title = "Carry observations forward",
        description = "Use the most recent value when runs record different NFE checkpoints.")
    @Persist(configKey = "carry_forward")
    public boolean carryForward = true;
  }

  public static final class ConvergencePlot implements NodeParameters {
    @Widget(
        title = "Series column",
        description = "Algorithm or method label used to draw separate curves.")
    @Persist(configKey = "series_column")
    public String seriesColumn = "Series";

    @Widget(
        title = "Horizontal-axis column",
        description = "Evaluation count shown on the horizontal axis.")
    @Persist(configKey = "nfe_column")
    public String nfeColumn = "NFE";

    @Widget(
        title = "Convergence value column",
        description = "Central convergence statistic, normally the median across runs.")
    @Persist(configKey = "value_column")
    public String valueColumn = "Median";

    @Widget(
        title = "Lower band column",
        description = "Lower uncertainty boundary, normally Q1. Leave blank to hide the band.")
    @Persist(configKey = "lower_column")
    public String lowerColumn = "Q1";

    @Widget(
        title = "Upper band column",
        description = "Upper uncertainty boundary, normally Q3. Leave blank to hide the band.")
    @Persist(configKey = "upper_column")
    public String upperColumn = "Q3";

    @Widget(
        title = "Logarithmic horizontal axis",
        description = "Show positive evaluation counts on a base-10 logarithmic scale.")
    @Persist(configKey = "log_x")
    public boolean logX = false;

    @Widget(
        title = "Logarithmic vertical axis",
        description = "Show positive convergence values on a base-10 logarithmic scale.")
    @Persist(configKey = "log_y")
    public boolean logY = false;
  }

  public static final class EcdfPlot implements NodeParameters {
    @Widget(
        title = "Series column",
        description = "Algorithm or method label used to draw separate curves.")
    @Persist(configKey = "series_column")
    public String seriesColumn = "Series";

    @Widget(
        title = "Fitness column",
        description = "Final performance value shown on the horizontal axis.")
    @Persist(configKey = "fitness_column")
    public String fitnessColumn = "Fitness";

    @Widget(
        title = "ECDF column",
        description =
            "Fraction of runs whose final fitness is at or below the horizontal-axis value.")
    @Persist(configKey = "ecdf_column")
    public String ecdfColumn = "ECDF";

    @Widget(
        title = "Logarithmic horizontal axis",
        description = "Show positive final-fitness values on a base-10 logarithmic scale.")
    @Persist(configKey = "log_x")
    public boolean logX = false;
  }

  public static final class AnalysisDirectionChoices implements StringChoicesProvider {
    @Override
    public List<StringChoice> computeState(final NodeParametersInput context) {
      return List.of(
          new StringChoice("AUTO", "Automatic - use IdeaFlow metadata"),
          new StringChoice("MINIMIZE", "Minimize - smaller is better"),
          new StringChoice("MAXIMIZE", "Maximize - larger is better"));
    }
  }

  public static final class CompareParetoFronts implements NodeParameters {
    // Objectives, directions, and constraint accounting come from Problem Setup.
  }

  public static final class PopulationMigration implements NodeParameters {
    @Widget(
        title = "Migrants per population",
        description =
            "Number of the strongest candidates each population sends when this node executes.")
    @Persist(configKey = "migrant_count")
    public int count = 1;

    @Widget(
        title = "Migration interval",
        description =
            "Exchange migrants every N completed generations; other passes leave populations unchanged.")
    @Persist(configKey = "migration_interval")
    public int interval = 10;

    @Widget(
        title = "Migration topology",
        description = "How migrants move between the populations in the input table.")
    @ChoicesProvider(MigrationTopologyChoices.class)
    @Persist(configKey = "migration_topology")
    public String topology = "RING";

    @Widget(
        title = "When migrants arrive",
        description =
            "Replace weak candidates to preserve population sizes, or add migrants and allow sizes to grow.")
    @ChoicesProvider(MigrationReplacementChoices.class)
    @Persist(configKey = "migration_replacement")
    public String replacement = "REPLACE_WORST";
  }

  public static final class RecordPopulation implements NodeParameters {
    @Widget(
        title = "Stage",
        description = "Semantic evolutionary stage recorded in every trace row.")
    @Persist(configKey = "stage")
    public String stage = "operator-stage";

    @Widget(
        title = "Operator",
        description = "Operator or strategy label recorded in every trace row.")
    @Persist(configKey = "operator")
    public String operator = "custom";
  }

  public static final class ExportToIohProfiler implements NodeParameters {
    @Widget(
        title = "Output directory",
        description = "Parent directory for the new IOH experiment folder.")
    @Persist(configKey = "output_directory")
    public String output = "ioh-output";

    @Widget(title = "New folder name", description = "New non-existing or empty output folder.")
    @Persist(configKey = "folder_name")
    public String folder = "ioh_data";

    @Widget(title = "Suite", description = "IOH suite identifier.")
    @Persist(configKey = "suite")
    public String suite = "unknown_suite";

    @Widget(title = "Problem ID", description = "IOH function or problem identifier.")
    @Persist(configKey = "problem_id")
    public String problemId = "1";

    @Widget(title = "Algorithm", description = "Algorithm name stored in metadata.")
    @Persist(configKey = "algorithm")
    public String algorithm = "IdeaFlow";

    @Widget(title = "Algorithm information", description = "Free-form algorithm description.")
    @Persist(configKey = "algorithm_info")
    public String info = "";

    @Widget(
        title = "Scalar performance column",
        description =
            "Select an objective for single-objective runs or hypervolume for multi-objective trajectories.")
    @ChoicesProvider(NumericColumnChoices.class)
    @Persist(configKey = "raw_y_column")
    public String rawY = "";

    @Widget(
        title = "Performance direction",
        description =
            "Automatic uses objective metadata or Hypervolume. Choose a direction for a custom scalar column.")
    @ChoicesProvider(AnalysisDirectionChoices.class)
    @Persist(configKey = "performance_direction")
    public String direction = "AUTO";

    @Widget(title = "Instance", description = "IOH problem instance number.")
    @Persist(configKey = "instance")
    public int instance = 1;

    @Widget(
        title = "Write complete .cdat log",
        description = "Also emit every event in a complete data file.")
    @Persist(configKey = "write_complete")
    public boolean complete = true;

    @Widget(
        title = "Additional sidecar columns",
        description = "Optional columns retained in the lossless companion file.")
    @ChoicesProvider(AllColumnChoices.class)
    @Persistor(AdditionalPropertiesPersistor.class)
    public String[] properties = new String[0];
  }
}
