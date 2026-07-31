# Node Reference

[Documentation index](README.md) · [Overview](OVERVIEW.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Troubleshooting](TROUBLESHOOTING.md)

IdeaFlow registers 19 nodes under nine categories in the KNIME Node Repository.

```text
IDEAFlow
├── Setup
├── Loops
├── Operators
├── Evaluation
├── Multi-Objective
├── Multiple Populations
├── Analysis
└── Export
```

The Problem Setup metadata travels with IdeaFlow population tables. Downstream nodes use it to discover variables, bounds, objectives, directions, constraints, seeds, and the evaluation budget without asking for the same information again.

## Setup

### Problem Setup

Defines one reproducible run and its optimization problem. Configure the experiment name, seed, maximum evaluations, direct float/integer/binary variables, optional binary-encoded numerical groups, objectives, directions, optional hypervolume reference values, and constraints.

**Output:** `Problem setup`.

Use standard KNIME loops or Components when several runs or seeds are required.

### Initial Population

Creates a deterministic unevaluated population from Problem Setup. Its main settings are population size and population ID.

**Input:** `Problem setup`. **Output:** `Initial population`.

## Loops

### Optimization Loop Start

Starts the native IdeaFlow optimization loop after initial evaluation. On the first pass it emits the connected population and archive. Later passes retrieve both tables retained by Optimization Loop End. When no initial archive is connected, it produces an empty archive with the population schema.

**Inputs:** `Initial population`, optional `Initial archive`.
**Outputs:** `Current population`, `Current archive`.

### Optimization Loop End

Returns the active population and optional DE archive to Loop Start until the run reaches its strict NFE budget or configured objective targets. Only the active population affects stopping. A complete next generation is not started if it would exceed the budget.

**Inputs:** `Next population`, optional `Progress summary`, optional `Next archive`.
**Outputs:** `Final population`, `Run summary`, `Convergence history`, `Final archive`.

The archive must have exactly the active-population schema. Connect Track Progress's summary output when convergence history is required.

## Operators

### Selection

Provides tournament and random parent selection, or prepares target and donor vectors for Differential Evolution. DE mode supports a configurable p-best fraction and can sample its second difference donor from the optional archive.

**Inputs:** `Population`, optional `DE archive`. **Output:** `Selected candidates`.

### Crossover

Provides SBX, uniform, one-point, arithmetic, DE binomial, and DE exponential crossover. It reads variables and bounds from Problem Setup and invalidates evaluation state when offspring are created.

**Input:** `Candidates`. **Output:** `Offspring`.

### Mutation

Provides polynomial, Gaussian, bit-flip, random-reset, DE/rand/1, DE/best/1, DE/current-to-best/1, and DE/current-to-pbest/1 mutation. Differential mutation supports fixed F/CR, jDE self-adaptation, SHADE success-history adaptation, and bounds repair.

**Input:** `Candidates`. **Output:** `Mutated candidates`.

### Elitism

Combines evaluated parents and children using single-objective elitism, pairwise DE competition, NSGA-II, NSGA-III, or GDE3. Population size can remain fixed or decrease linearly with NFE for L-SHADE. The second output contains rejected or replaced candidates and can serve as the next DE archive.

**Inputs:** `Current population`, `Evaluated children`.
**Outputs:** `Survivors`, `Rejected or replaced`.

## Evaluation

### Evaluation

Evaluates candidates in one of three ways:

- a built-in benchmark;
- objective and constraint formulas;
- numeric result columns produced upstream by Python, simulations, databases, machine-learning models, or other KNIME nodes.

The node decodes binary-encoded variables, validates all declared results, calculates constraint violation, advances NFE, and emits one history event per newly evaluated candidate.

**Inputs:** `Candidates`, `Problem setup`.
**Outputs:** `Evaluated population`, `Evaluation history`.

## Multi-Objective

### Rank Pareto Solutions

Adds constraint-aware Pareto rank and crowding distance using the objectives and constraints declared in Problem Setup.

**Input:** `Population`. **Output:** `Ranked population`.

### Reference Directions

Creates normalized Das-Dennis reference directions. The number of objectives comes from Problem Setup and the dialog selects the number of divisions.

**Input:** `Problem or population`. **Output:** `Reference directions`.

### Compare Pareto Fronts

Compares each feasible nondominated approximation with a known reference front using GD, IGD, IGD+, epsilon, and spacing.

**Inputs:** `Front found by the algorithm`, `Known reference front`.
**Output:** `Comparison results`.

## Multiple Populations

### Population Migration

Exchanges strong candidates between population IDs using ring, reproducible-random, or all-to-all routing. Migrants may replace weak destination candidates or be added. Adaptive optimizer memory remains local to the destination population.

**Input:** `Combined populations`. **Output:** `Populations after migration`.

## Analysis

### Track Progress

Passes the population through unchanged while producing scalar or Pareto summaries and optional per-individual events. Summary rows retain series, run, seed, and problem identity.

**Input:** `Population`.
**Outputs:** `Population`, `Progress summary`, `Detailed events`.

### Optimization Run Analysis

Combines convergence histories from repeated runs, aligns NFE checkpoints, and produces convergence statistics, a final-fitness empirical CDF, and one result row per run.

**Input:** `Optimization histories`.
**Outputs:** `Convergence`, `Final-fitness ECDF`, `Runs`.

### Convergence Plot

Displays median performance by NFE and an interquartile band when quartile columns are present.

**Input:** `Convergence` from Optimization Run Analysis.

### ECDF Plot

Displays exact empirical-CDF steps for final fitness by series.

**Input:** `Final-fitness ECDF` from Optimization Run Analysis.

### Record Population

Records objectives, constraints, indicators, and optimizer state at a chosen workflow point while passing the population through unchanged.

**Input:** `Population`. **Outputs:** `Population`, `Recorded rows`.

## Export

### Export to IOHprofiler

Writes IOHprofiler `.info`, improvement `.dat`, and optional complete `.cdat` files. An optional escaped IdeaFlow TSV sidecar retains additional source columns.

**Input:** an optimization event history from Evaluation, Track Progress, Record Population, or a compatible table.
**Output:** `Export summary`.

## Typical connections

A simple generational workflow uses:

```text
Problem Setup -> Initial Population -> Evaluation -> Optimization Loop Start
                                                      |
                        Selection -> Crossover -> Mutation -> Evaluation
                                                      |
current population -------------------------------> Elitism
                                                      |
                           Track Progress -> Optimization Loop End
```

A SHADE or L-SHADE workflow additionally connects Loop Start's current archive to Selection and returns Elitism's replaced candidates to Loop End's archive input.
