# Node Reference

[Main README](../README.md) · [Documentation index](README.md) · [Overview](OVERVIEW.md) · [KNIME installation](KNIME_INSTALLATION.md) · [IdeaFlow installation](INSTALLATION.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Development](DEVELOPMENT.md) · [Troubleshooting](TROUBLESHOOTING.md)


IdeaFlow currently registers twenty-seven nodes in the [KNIME Node Repository](https://hub.knime.com/). They are grouped under the `Setup`, `Evolution`, `Evaluation`, `Results`, `Advanced`, and `Advanced / Utilities` categories.

```text
IDEAFlow
├── Setup
├── Evolution
├── Evaluation
├── Results
└── Advanced
    └── Utilities
```

<!-- Planned image: screenshot of the complete IDEAFlow category in the Node Repository. -->
<!-- ![Available IdeaFlow nodes](images/ideaflow-available-nodes.png) -->

## Setup

### Experiment Setup

**Experiment Setup** creates the experiment plan. It produces one row per independent run and associates identifiers and a deterministic seed with each run.

Main parameters:

- experiment identifier;
- problem identifier;
- algorithm or recipe name;
- number of replications;
- master seed;
- maximum evaluation budget.

**Output:** `Run plan`.

### Define Optimization Problem

**Define Optimization Problem** describes the variables, objectives, and constraints of the problem.

The dialog can be used to create the variables, objectives, and constraints described in [Optimization Problems and Benchmarks](OPTIMIZATION_PROBLEMS.md):

- continuous variables;
- integer variables;
- direct binary variables;
- numbered variable groups;
- groups of numerical values encoded using natural binary or Gray code;
- one or more objectives, each associated with a minimization or maximization direction;
- constraint-violation columns;
- optional hypervolume reference values.

**Output:** problem-definition table.

### Create Initial Population

**Create Initial Population** combines the experiment plan and problem definition to create an unevaluated population for each run.

Main parameters:

- population size;
- population identifier.

**Inputs:** `Experiment setup`, `Problem`.

**Output:** `Initial population`.

## Evolution

### Evolution Loop Start

**Evolution Loop Start** starts a native IdeaFlow evolutionary loop. During the first iteration, it receives the already evaluated initial population. During subsequent iterations, it retrieves the population returned internally by **Evolution Loop End**.

**Input:** `Initial population`.

**Output:** `Current population`.

### Select Parents

**Select Parents** samples the individuals used by the crossover operator.

Available strategies:

- tournament;
- random selection.

Tournament selection can compare a scalar fitness value or automatically use Pareto-rank and crowding columns when they are available.

Main parameters:

- objectives and directions;
- strategy;
- tournament size;
- number of parents;
- selection with or without replacement;
- constraint-violation column.

### Crossover

**Crossover** combines selected parents to produce unevaluated offspring.

Available strategies:

- `SBX`;
- `UNIFORM`;
- `ONE_POINT`;
- `ARITHMETIC`.

Main parameters:

- variables to combine;
- evaluation results to invalidate;
- crossover probability;
- SBX distribution index;
- lower and upper bounds.

### Mutation

**Mutation** modifies the decision variables of candidates and invalidates outdated evaluation results.

Available strategies:

- polynomial mutation;
- Gaussian mutation;
- bit-flip mutation;
- random-reset mutation.

Main parameters:

- variables to mutate;
- evaluation results to invalidate;
- automatic rate of `1 / number of variables` or a manual probability;
- Gaussian mutation strength;
- polynomial distribution index;
- lower and upper bounds.

### Finalize Evaluation

**Finalize Evaluation** validates the columns computed by the evaluator and serves as the single boundary for counting exact evaluations.

The node:

- checks that objective columns are present;
- aggregates constraint violations;
- marks candidates as evaluated;
- updates internal evaluation and NFE columns;
- produces a portable history of newly completed evaluations.

**Input:** `Evaluation results`.

**Outputs:** `Evaluated population`, `Evaluation history`.

### Select Survivors (Elitism)

**Select Survivors (Elitism)** selects the population passed to the next generation from the evaluated parents and offspring.

Available modes are implementation identifiers. Named methods and their publications are centralized in [Implemented algorithms](../README.md#implemented-algorithms).

- `SINGLE_OBJECTIVE`;
- `DE_PAIRWISE`;
- `NSGA_II`;
- `NSGA_III`;
- `GDE3`.

**Inputs:** `Current population`, `Evaluated children`.

**Outputs:** `Survivors`, `Rejected or replaced`.

The second output can feed [`Update Archive`](#update-archive) when a workflow uses an external archive.

### Evolution Loop End

**Evolution Loop End** receives the survivors, increments the generation, and returns the population to **Evolution Loop Start** until at least one stopping condition is met.

Available stopping conditions:

- maximum number of generations;
- maximum evaluation budget;
- optional target value for an objective.

**Input:** `Next population`.

**Final outputs:** `Final population`, `Run summary`.

The `Run summary` table notably contains the number of evaluations, the final generation, the best value, and the stopping reason.

## Evaluation

### Evaluate Benchmark

**Evaluate Benchmark** computes the objectives of built-in test functions.

Built-in problems are documented with definitions and references in [Optimization Problems and Benchmarks](OPTIMIZATION_PROBLEMS.md).

- `ACKLEY`;
- `SPHERE`;
- `ROSENBROCK`;
- `RASTRIGIN`;
- `GRIEWANK`;
- `ONEMAX`;
- `ZDT1`;
- `ZDT2`;
- `ZDT3`;
- `DTLZ2`.

Main parameters:

- decision variables;
- produced objective columns;
- selected benchmark.

A custom problem can be evaluated using any KNIME nodes, provided that the result is then passed through **Finalize Evaluation**.

## Results

### Optimization Monitor

**Optimization Monitor** passes the population through without modifying it and produces two monitoring tables:

- a summary per run and population;
- a detailed event per individual.

The summary notably contains:

- generation and NFE;
- population size;
- number of feasible individuals;
- best, mean, worst, and standard-deviation values;
- size of the non-dominated set;
- hypervolume when a reference point is provided.

**Outputs:** `Population`, `Progress summary`, `Detailed events`.

### Export Results

**Export Results** writes an optimization history in [IOHprofiler](https://iohprofiler.github.io/) format.

Produced files:

- `.info`;
- `.dat` for improvements;
- `.cdat` for the complete history, when enabled;
- an IdeaFlow TSV file preserving additional properties.

The node receives events produced by **Finalize Evaluation**, **Optimization Monitor**, or **Evolution Trace**.

## Advanced

### Decode Binary Variables

**Decode Binary Variables** transforms genes created for a binary-encoded group into numerical variables that can be used directly by an evaluator.

Variable names, bounds, bit counts, and natural-binary or [Gray-code](https://en.wikipedia.org/wiki/Gray_code) encoding are read from the table produced by **Define Optimization Problem**.

**Inputs:** `Binary population`, `Problem`.

**Output:** `Decoded population`.

### Generate Reference Directions

**Generate Reference Directions** produces normalized reference directions following the [Das-Dennis systematic approach](https://doi.org/10.1007/BF01201741).

Main parameters:

- number of objectives;
- number of divisions.

These directions can be inspected, visualized, or reused in many-objective workflows.

### Differential Evolution

**Differential Evolution** creates trial candidates for recipes listed in [Implemented algorithms](../README.md#implemented-algorithms).

Main parameters:

- continuous variables;
- fitness column and direction;
- differential mutation strategy;
- crossover strategy;
- `F`, `CR`, and p-best-rate parameters;
- bounds;
- repair method.

**Inputs:** `Current population`, optional `Archive`.

**Output:** `Trial candidates`.

### Update Archive

**Update Archive** manages two archive categories:

- `PARETO`, for non-dominated solutions;
- `FIFO_UNIQUE`, for replaced candidates used by adaptive archive-based recipes.

The node can isolate archives per run or per run/population pair and limit their size.

**Inputs:** optional previous archive, new candidates, optional current population.

**Output:** updated archive.

### Adapt DE Parameters

**Adapt DE Parameters** adds or updates individual `F` and `CR` parameters.

Available modes:

- `FIXED`;
- `JDE`;
- `SHADE`.

Advanced parameters cover self-adaptation probabilities and success-history memory size. The associated named methods are referenced in [Implemented algorithms](../README.md#implemented-algorithms).

### Reduce Population Size

**Reduce Population Size** applies a linear population-size reduction based on consumed NFE.

Main parameters:

- initial size;
- minimum size;
- evaluation budget;
- objective and direction used to remove the least competitive individuals.

### Surrogate Selection

**Surrogate Selection** ranks candidates already equipped with a prediction or acquisition score and separates:

- candidates that must be evaluated exactly;
- candidates retained as predictions.

The learning model remains external to the node and can be built using available KNIME components.

### Migrate Between Populations

**Migrate Between Populations** performs deterministic ring migration between several population identifiers within the same run.

Main parameters:

- fitness column;
- direction;
- number of migrants per island.

## Advanced / Utilities

### Pareto Ranking & Diversity

**Pareto Ranking & Diversity** computes [Pareto](https://en.wikipedia.org/wiki/Pareto_efficiency) rank and crowding distance for an evaluated population while accounting for constraints.

### Iteration Controller

**Iteration Controller** manually increments the generation and produces a table of stopping decisions. It remains available for low-level workflows or architectures using generic KNIME loops. For standard workflows, the native **Evolution Loop Start** and **Evolution Loop End** nodes combine this logic.

### Quality Indicators

**Quality Indicators** computes the size of the feasible non-dominated set and [hypervolume](https://doi.org/10.1109/4235.797969) for an arbitrary number of objectives.

### Reference Quality Indicators

**Reference Quality Indicators** compares an approximate front with a reference front.

Available indicators:

- GD;
- IGD;
- IGD+;
- additive epsilon;
- spacing.

### Population Statistics

**Population Statistics** produces one row per run, population, and generation containing population size, feasibility, best, mean, worst, and standard-deviation values.

These data can feed KNIME visualization and statistical-analysis nodes.

### Evolution Trace

**Evolution Trace** can be inserted after any stage manipulating a population. It passes the table through unchanged and produces events containing the stage name, operator name, and selected numerical values.

---
## Related documentation

- [Implemented algorithms and papers](../README.md#implemented-algorithms)
- [Optimization problems and benchmarks](OPTIMIZATION_PROBLEMS.md)
- [Workflow tutorial](WORKFLOW_TUTORIAL.md)
- [IOHprofiler](https://iohprofiler.github.io/)
- [KNIME Community Hub](https://hub.knime.com/)
