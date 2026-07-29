# IdeaFlow Example Workflows

[Main README](../../README.md) · [Documentation index](../../docs/README.md) · [Overview](../../docs/OVERVIEW.md) · [Node reference](../../docs/NODES.md) · [Optimization problems](../../docs/OPTIMIZATION_PROBLEMS.md) · [Best practices](../../docs/BEST_PRACTICES.md)

This directory contains importable [KNIME Analytics Platform](https://www.knime.com/knime-analytics-platform) workflows that demonstrate how IdeaFlow nodes can be assembled into complete optimization methods.

## Importing an example

1. Install the IdeaFlow extension in KNIME.
2. In the KNIME Explorer, choose **Import KNIME Workflow...**.
3. Select one of the `.knwf` files from this directory.
4. Choose a destination inside the active KNIME workspace.
5. Open the imported workflow, inspect its configuration, and execute it.

The exported files do not contain execution data. Each example therefore starts from its configured seed and performs a complete run after import.

## Workflow catalogue

| # | Workflow | Search method | Optimization problem | Main feature demonstrated |
| ---: | --- | --- | --- | --- |
| 1 | [Binary OneMax Generational GA](<1 Binary OneMax Generational GA.knwf>) | Generational genetic algorithm | OneMax | Binary variables and full generational replacement |
| 2 | [Real-Valued Ackley GA](<2 Real-Valued Ackley GA.knwf>) | Real-valued genetic algorithm | Ackley | SBX, polynomial mutation, and elitist survivor selection |
| 3 | [Binary-Encoded Ackley GA](<3 Binary-Encoded Ackley GA.knwf>) | Binary-encoded genetic algorithm | Ackley | Searching a real domain through a binary genotype |
| 4 | [NSGA-II on ZDT1](<4 NSGA-II on ZDT1.knwf>) | NSGA-II | ZDT1 | Pareto ranking, crowding distance, and two-objective visualization |
| 5 | [NSGA-II on Constrained ZDT1](<5 NSGA-II on Constrained ZDT1.knwf>) | Constraint-aware NSGA-II | Constrained ZDT1 variant | Built-in objectives with formula-based constraints |
| 6 | [NSGA-III on DTLZ2](<6 NSGA-III on DTLZ2.knwf>) | NSGA-III | Three-objective DTLZ2 | Reference-direction survivor selection |
| 7 | [GDE3 on ZDT3](<7 GDE3 on ZDT3.knwf>) | GDE3 | ZDT3 | Multi-objective Differential Evolution |
| 8 | [Conventional DE on Sphere](<8 Conventional DE on Sphere.knwf>) | DE/rand/1/bin | Sphere | Fixed DE parameters and pairwise competition |
| 9 | [jDE on Rastrigin](<9 jDE on Rastrigin.knwf>) | jDE/rand/1/bin | Rastrigin | Individual-level self-adaptation of `F` and `CR` |
| 10 | [SHADE on Rosenbrock](<10 SHADE on Rosenbrock.knwf>) | SHADE parameter-adaptation variant | Rosenbrock | Success-history adaptation and logarithmic convergence analysis |
| 11 | [L-SHADE on Griewank](<11 L-SHADE on Griewank.knwf>) | L-SHADE-style variant | Griewank | Success-history adaptation and linear population reduction |
| 12 | [Homogeneous Multi-Population Island Model](<12 Homogeneous Multi-Population Island Model.knwf>) | Four L-SHADE-style islands | Griewank | Multiple populations, ring migration, and per-island analysis |

## Shared workflow structure

Most examples use the following pattern:

```text
Problem Setup -> Initial Population -> Initial Evaluation -> Optimization Loop Start ->
-> Selection -> Mutation/Crossover -> Evaluation ->
-> Elitism -> Track Progress -> Optimization Loop End
```

`Problem Setup` is connected both to `Initial Population` and to every `Evaluation` node. It supplies the variable schema, objective directions, constraints, seed, and maximum number of exact evaluations. The first `Evaluation` node evaluates the initial population. The second evaluates only the new candidates created inside the loop.

`Optimization Loop End` receives the next active population and the summary produced by `Track Progress`. No example enables an objective-target condition, so the exact-evaluation budget declared in `Problem Setup` is the stopping condition.

The two main operator orders are intentional:

- GA workflows use `Selection -> Crossover -> Mutation` because crossover combines selected parents and mutation perturbs the resulting offspring.
- DE workflows use `Selection -> Mutation -> Crossover` because selection stores target and donor vectors, mutation creates a mutant vector, and DE crossover combines the mutant with its corresponding target.

## 1. Binary OneMax Generational GA

[Import the workflow](<1 Binary OneMax Generational GA.knwf>)

This example maximizes the number of one-bits in a string of length `100`. The global optimum is therefore `100`, reached when every decision variable equals one.

| Setting | Value |
| --- | --- |
| Variables | `100` direct binary variables, `x0` through `x99` |
| Objective | Maximize `fitness` with the built-in `ONEMAX` evaluation |
| Population | `100` individuals |
| Budget and seed | `10,000` evaluations, seed `42` |
| Parent selection | Tournament selection, size `2`, `100` parents, with replacement |
| Crossover | Uniform crossover, probability `0.9` |
| Mutation | Bit-flip mutation with automatic rate |
| Replacement | Full generational replacement |

Tournament selection introduces moderate selection pressure while still allowing weaker individuals to reproduce. Uniform crossover decides independently which parent's value is used at each bit, which is a natural match for a position-independent problem such as OneMax. The automatic bit-flip rate scales with the genotype length instead of requiring a manually maintained probability.

There is deliberately no `Elitism` node. The evaluated offspring become the complete population of the next generation. This demonstrates a conventional generational GA and also means that the best individual is not explicitly protected from one generation to the next. `Track Progress` records the best value in each current population, so the history can also reveal temporary losses between generations.

The final convergence history is connected to `Export to IOHprofiler`, configured for the PBO suite, OneMax problem `1`, instance `1`, and complete-data output. Select an output directory before using that branch on another machine.

## 2. Real-Valued Ackley GA

[Import the workflow](<2 Real-Valued Ackley GA.knwf>)

This workflow searches the standard continuous Ackley domain. Its main purpose is to show the real-valued GA operator pair normally used in evolutionary optimization.

| Setting | Value |
| --- | --- |
| Variables | `30` real variables in `[-32.768, 32.768]` |
| Objective | Minimize `fitness` with built-in `ACKLEY` |
| Population | `100` individuals |
| Budget and seed | `100,000` evaluations, seed `42` |
| Parent selection | Tournament size `2`, with replacement |
| Crossover | SBX, probability `0.9`, distribution index `20` |
| Mutation | Polynomial mutation, automatic rate, distribution index `20` |
| Survivor update | Single-objective elitism, fixed population size |

Simulated Binary Crossover creates children around the two real-valued parents while respecting the declared bounds. A distribution index of `20` favors children reasonably close to their parents. Polynomial mutation uses the same local-search bias and an automatic per-variable rate.

Unlike the OneMax example, the current population and evaluated offspring are both connected to `Elitism`. The node keeps the best `100` candidates under the minimizing objective, so good parents survive when their children are worse.

The convergence branch aggregates `Best` by NFE, carries values forward when necessary, and displays the median with `Q1` and `Q3`. The NFE axis is logarithmic, which makes early and late search behavior visible in the same plot.

## 3. Binary-Encoded Ackley GA

[Import the workflow](<3 Binary-Encoded Ackley GA.knwf>)

This example solves the same continuous Ackley problem through a binary representation. It demonstrates the difference between the genotype manipulated by the GA and the real-valued phenotype evaluated by the benchmark.

| Setting | Value |
| --- | --- |
| Logical variables | `30` values in `[-32.768, 32.768]` |
| Encoding | `10` bits per logical variable, `300` bits in total |
| Objective | Minimize `fitness` with built-in `ACKLEY` |
| Population | `100` individuals |
| Budget and seed | `100,000` evaluations, seed `1` |
| Parent selection | Tournament size `2`, with replacement |
| Crossover | One-point crossover, probability `0.9` |
| Mutation | Bit-flip mutation with automatic rate |
| Survivor update | Single-objective elitism |

`Initial Population` creates the encoded gene columns. One-point crossover and bit-flip mutation operate on those columns without needing real-valued bounds. `Evaluation` reconstructs each logical value from its ten-bit code before calculating Ackley.

Ten bits provide `1,024` representable values per dimension. The encoding therefore discretizes the continuous domain: it can approach the analytical optimum but cannot represent every real number. This workflow is useful for comparing representation effects while leaving the benchmark and survivor policy unchanged.

## 4. NSGA-II on ZDT1

[Import the workflow](<4 NSGA-II on ZDT1.knwf>)

ZDT1 is a two-objective minimization problem with a continuous Pareto front. This workflow shows the complete NSGA-II data flow, including the rank information used during mating selection.

| Setting | Value |
| --- | --- |
| Variables | `30` real variables in `[0, 1]` |
| Objectives | Minimize `f1` and `f2` with built-in `ZDT1` |
| Population | `100` individuals |
| Budget and seed | `25,000` evaluations, seed `42` |
| Parent selection | Tournament size `2`, with replacement |
| Variation | SBX `0.9`, index `20`; polynomial mutation with automatic rate, index `20` |
| Survivor update | `NSGA-II`, fixed population size |

The initial evaluated population passes through `Rank Pareto Solutions` before entering the loop. Tournament selection can therefore compare Pareto rank first and crowding distance second. Parents and offspring are then combined by the `NSGA-II` mode of `Elitism`, which fills the next population front by front and uses crowding distance when the last accepted front does not fit completely.

The survivors are ranked again before progress tracking and loop feedback. The final population is connected to a KNIME Scatter Plot so `f1` and `f2` can be displayed as an approximation of the Pareto front.

## 5. NSGA-II on Constrained ZDT1

[Import the workflow](<5 NSGA-II on Constrained ZDT1.knwf>)

This workflow adds two constraints to the previous ZDT1 configuration while keeping the same NSGA-II search operators.

| Setting | Value |
| --- | --- |
| Base problem | `30` real variables in `[0, 1]`; minimize built-in `ZDT1` objectives `f1` and `f2` |
| Constraints | `x0 >= 0.2` and `x0 <= 0.8` |
| Population | `100` individuals |
| Budget and seed | `25,000` evaluations, seed `42` |
| Variation | Tournament selection, SBX, and polynomial mutation |
| Survivor update | Constraint-aware `NSGA-II` |

The built-in benchmark owns the two objective equations. The `Evaluation` dialog therefore exposes only the two constraint formulas. Both formula cards contain `x0`: one produces `lower_x0`, checked against the lower threshold, and the other produces `upper_x0`, checked against the upper threshold.

`Evaluation` converts these results into a total constraint violation. Feasible candidates are preferred over infeasible candidates; when both candidates are infeasible, the smaller violation is preferred. Pareto rank, tournament selection, and NSGA-II survivor selection all receive the resulting constraint-aware population.

The comparison with workflow 4 is intentional: changing the problem definition and constraint formulas is sufficient, while the evolutionary operators remain the same.

## 6. NSGA-III on DTLZ2

[Import the workflow](<6 NSGA-III on DTLZ2.knwf>)

This example extends the data flow to three objectives and uses reference directions to maintain a well-distributed approximation.

| Setting | Value |
| --- | --- |
| Variables | `12` real variables in `[0, 1]` |
| Objectives | Minimize `f1`, `f2`, and `f3` with built-in `DTLZ2` |
| Population | `100` individuals |
| Budget and seed | `50,000` evaluations, seed `1` |
| Variation | Tournament selection; SBX probability `1.0`, index `30`; polynomial mutation with automatic rate, index `20` |
| Survivor update | `NSGA-III`, `12` reference divisions |

NSGA-II's crowding distance becomes less discriminating as the number of objectives grows. NSGA-III instead associates candidates with structured reference directions and uses niche occupancy when the final Pareto front must be truncated. With three objectives and twelve divisions, the Das-Dennis construction yields `91` reference directions.

The higher SBX distribution index of `30` makes offspring more local around their parents, while a crossover probability of `1.0` applies SBX to every selected pair. The workflow ranks the population before tournament selection and after survivor selection. Inspect the final population's three objective columns or add a three-dimensional visualization downstream of `Optimization Loop End`.

## 7. GDE3 on ZDT3

[Import the workflow](<7 GDE3 on ZDT3.knwf>)

GDE3 adapts Differential Evolution to constrained and multi-objective search. ZDT3 is useful here because its Pareto front is disconnected.

| Setting | Value |
| --- | --- |
| Variables | `30` real variables in `[0, 1]` |
| Objectives | Minimize `f1` and `f2` with built-in `ZDT3` |
| Population | `100` individuals |
| Budget and seed | `25,000` evaluations, seed `1` |
| Donor selection | One DE donor set per target |
| Mutation | `DE/rand/1`, fixed `F = 0.5`, reflected bounds repair |
| Crossover | DE binomial crossover with fixed `CR = 0.9` |
| Survivor update | `GDE3`, fixed population size |

Selection keeps each target row and stores distinct donor vectors in its IdeaFlow state. `DE/rand/1` forms a mutant from a random base plus one scaled difference. Binomial crossover then combines that mutant with the corresponding target and forces at least one mutant coordinate into every trial.

In GDE3 competition, a dominating member of a parent-trial pair is retained; when neither dominates, both may enter the intermediate pool. The survivor node then applies non-dominated sorting and crowding-based truncation to restore the population size. The survivors are ranked for monitoring, and the final `f1`/`f2` table feeds a Scatter Plot.

## 8. Conventional DE on Sphere

[Import the workflow](<8 Conventional DE on Sphere.knwf>)

This is the simplest single-objective Differential Evolution example and a useful baseline for the adaptive variants that follow.

| Setting | Value |
| --- | --- |
| Variables | `30` real variables in `[-5.12, 5.12]` |
| Objective | Minimize `fitness` with built-in `SPHERE` |
| Population | `100` individuals |
| Budget and seed | `30,000` evaluations, seed `10` |
| Mutation and crossover | `DE/rand/1/bin`, fixed `F = 0.5`, fixed `CR = 0.9` |
| Bounds repair | Reflection |
| Survivor update | Pairwise DE competition |

Each target receives its own donors, mutant, and trial. Pairwise competition compares the trial only with the target that generated it. The better value survives because Sphere is minimized. This one-to-one replacement preserves the population size and is the conventional selection rule for single-objective DE.

Reflection returns an out-of-bounds mutant to the feasible domain without simply clipping every violation to a boundary. `Optimization Run Analysis` summarizes best fitness against NFE and feeds a convergence plot with a logarithmic NFE axis.

## 9. jDE on Rastrigin

[Import the workflow](<9 jDE on Rastrigin.knwf>)

jDE keeps the `DE/rand/1/bin` structure but allows each individual to carry and probabilistically update its own control parameters.

| Setting | Value |
| --- | --- |
| Variables | `30` real variables in `[-5.12, 5.12]` |
| Objective | Minimize `fitness` with built-in `RASTRIGIN` |
| Population | `100` individuals |
| Budget and seed | `100,000` evaluations, seed `10` |
| Initial parameters | `F = 0.5`, `CR = 0.9` |
| Adaptation | jDE with `tau_F = 0.1` and `tau_CR = 0.1` |
| Survivor update | Pairwise DE competition |

Before mutation, each target has a ten-percent chance of drawing a new `F` and a separate ten-percent chance of drawing a new `CR`. These values remain attached to the candidate state. Successful trials carry their parameters into the next generation through pairwise competition, allowing useful settings to persist without a global schedule.

Rastrigin's many local minima make it a stronger adaptation test than Sphere. The analysis branch plots median best fitness with quartiles against NFE.

The included CSV Writer still points to `C:\Users\Ion\Desktop\test.csv`. Remove that diagnostic node or replace the destination with a workflow-relative path before publishing or running the workflow on another machine.

## 10. SHADE on Rosenbrock

[Import the workflow](<10 SHADE on Rosenbrock.knwf>)

This workflow demonstrates SHADE's success-history parameter adaptation on the curved Rosenbrock valley.

| Setting | Value |
| --- | --- |
| Variables | `10` real variables in `[-5, 10]` |
| Objective | Minimize `fitness` with built-in `ROSENBROCK` |
| Population | `100` individuals |
| Budget and seed | `50,000` evaluations, seed `1` |
| Mutation | Current-to-best/1 with SHADE adaptation |
| Initial memory | `F = 0.5`, `CR = 0.5`, memory size `100` |
| Survivor update | Pairwise DE competition |

The mutation node samples `F` and `CR` from its success-history memories. Pairwise competition marks successful trials and records their objective improvement. On the following generation, successful `F` values update memory through an improvement-weighted Lehmer mean, while successful `CR` values use an improvement-weighted arithmetic mean.

The convergence plot uses a logarithmic fitness axis, which is useful when progress spans several orders of magnitude.

**Current recipe status:** the exported workflow uses `current-to-best/1`, not canonical `current-to-pbest/1`, and the `Rejected or replaced` output from `Elitism` is not connected to the `Replaced DE parents` input of `Optimization Loop End`. It therefore demonstrates SHADE parameter adaptation but not the complete canonical SHADE mutation/archive mechanism. For canonical SHADE, select `current-to-pbest/1` and connect those two named ports.

## 11. L-SHADE on Griewank

[Import the workflow](<11 L-SHADE on Griewank.knwf>)

This workflow adds linear population-size reduction to success-history parameter adaptation.

| Setting | Value |
| --- | --- |
| Variables | `10` real variables in `[-600, 600]` |
| Objective | Minimize `fitness` with built-in `GRIEWANK` |
| Initial population | `100` individuals |
| Budget and seed | `50,000` evaluations, seed `1` |
| Mutation | Current-to-best/1 with SHADE adaptation and memory size `6` |
| Survivor update | Pairwise DE competition |
| Size policy | Linear reduction by NFE to a minimum of `4` individuals |

The survivor node calculates the target population size from the consumed fraction of the evaluation budget. Early generations retain more individuals for exploration; later generations use a smaller population to spend more evaluations on focused search. DE selection always emits one target row per member of the current population, so trial population size follows the reduction automatically.

The replaced-parent output is connected to the loop's archive feedback port. However, the configured `current-to-best/1` mutation does not consume the p-best archive donor prepared by Selection.

**Current recipe status:** to represent canonical L-SHADE, change Mutation to `current-to-pbest/1`. The archive feedback and linear population reduction are already wired.

## 12. Homogeneous Multi-Population Island Model

[Import the workflow](<12 Homogeneous Multi-Population Island Model.knwf>)

This example runs the same L-SHADE-style configuration on four named populations and periodically exchanges candidates between them.

| Setting | Value |
| --- | --- |
| Problem | `10`-dimensional built-in `GRIEWANK` in `[-600, 600]` |
| Populations | `population-0` through `population-3`, `45` individuals each |
| Combined initial size | `180` individuals |
| Global budget and seed | `40,000` evaluations, seed `1` |
| Per-population method | SHADE adaptation, current-to-best/1, linear reduction to `4` |
| Migration | One migrant every `10` generations |
| Topology | Ring |
| Replacement | Replace worst recipient member |

Four `Initial Population` nodes receive the same problem definition but assign different population identifiers. `Concatenate` places all rows in one table. IdeaFlow nodes group rows by run and population, so Selection, Mutation, Elitism, and adaptive state updates still operate independently inside each island.

After survivor selection, `Population Migration` sends one candidate from each island to the next island in a ring every ten generations. Replacing the worst recipient member limits population growth. Imported candidates keep their decision values, while each receiving island retains its own adaptive optimizer memory.

`Optimization Run Analysis` groups the progress history by population. Four Row Filter branches isolate the four series and feed separate convergence plots, making island behavior easy to compare.

As in workflow 11, change Mutation from `current-to-best/1` to `current-to-pbest/1` if the islands are intended to implement canonical L-SHADE. One additional unconnected Convergence Plot remains on the canvas and can be removed before final publication.

## Choosing an example to modify

- Start with workflow 1 when experimenting with binary decision variables or generational replacement.
- Start with workflow 2 for a conventional real-valued GA.
- Compare workflows 2 and 3 to study how representation changes the same optimization problem.
- Start with workflow 4 for two-objective Pareto optimization.
- Use workflow 5 as the pattern for adding constraint formulas to a built-in benchmark.
- Use workflow 6 when more than two objectives require reference-direction diversity.
- Start with workflow 8 before introducing adaptive DE parameters.
- Compare workflows 8, 9, 10, and 11 to isolate fixed, individual-level, success-history, and population-reduction choices.
- Use workflow 12 as the basis for homogeneous island models and migration experiments.

When adapting a workflow, change one algorithmic decision at a time and keep the problem, budget, seed, and monitoring configuration fixed. This makes the effect of the change easier to interpret.

## Publication checks

Before treating these files as release examples:

- remove or reconfigure the personal CSV path in workflow 9;
- correct and re-export the SHADE-family workflows if their titles are intended to denote canonical SHADE and L-SHADE;
- validate every import and complete execution in the minimum supported KNIME version;
- remove the temporary `testing/` source directory once the exported workflows no longer need to be inspected;
- keep the `.knwf` exports free of execution data, credentials, and machine-specific paths.

---

## Related documentation

- [Concepts and workflow architecture](../../docs/OVERVIEW.md)
- [Complete node reference](../../docs/NODES.md)
- [Optimization problems and benchmark definitions](../../docs/OPTIMIZATION_PROBLEMS.md)
- [Workflow construction tutorial](../../docs/WORKFLOW_TUTORIAL.md)
- [Workflow organization and reproducibility](../../docs/BEST_PRACTICES.md)
