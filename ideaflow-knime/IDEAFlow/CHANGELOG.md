# Changelog

## 0.1.0 - SNAPSHOT

- Added Optimization Run Analysis, which aligns repeated-run checkpoints and emits plot-ready convergence, target
  ECDF, and per-run result tables. Track Progress summaries now retain series, seed, and problem identity so called
  workflows require no manual labeling or seed-column nodes.
- Added dedicated Convergence Plot and ECDF Plot nodes. They group curves by `Series` directly and remove the need
  for Pivoting, Sorter, Missing Value, and generic Line Plot nodes.
- Repeated-run analysis now treats the seed as part of trajectory identity, preventing called-workflow iterations
  with a stable run ID from being collapsed into a single run.
- Changed the ECDF output and dedicated ECDF Plot to the ordinary final-fitness ECDF: fitness is on the horizontal
  axis and the vertical axis is the fraction of runs with final fitness at or below that value.
- Added conditional visibility to every mode-dependent modern dialog. Selection, Crossover, Mutation, Elitism,
  adaptive DE control, Evaluation, Problem Setup, and loop-target settings now show only fields used by the chosen
  strategy. Classic-workbench fallbacks disable their irrelevant controls dynamically.
- Renamed Optimization Monitor to Track Progress and kept it as an optional analysis node.
- Removed the redundant Iteration Controller and Population Statistics nodes.
- Removed the redundant Quality Indicators node; Track Progress already provides nondominated size and
  hypervolume, while Reference Quality Indicators remains available for reference-front comparisons.
- Expanded Migrate Between Populations with ring, reproducible-random, and all-to-all routing plus replace-worst
  or add-migrants arrival behavior.
- Reorganized the Node Repository into real Setup, Loops, Operators, Evaluation, Multi-Objective, Adaptation,
  Multiple Populations, Analysis, Export, and Advanced subcategories.
- Renamed Parents Selection to **Selection** and distributed Differential Evolution across the same Selection,
  Mutation, and Crossover nodes used by ordinary GAs. Selection prepares hidden target/donor data, Mutation applies
  the chosen differential formula and bounds repair, and Crossover applies binomial/exponential mixing before
  restoring the normal population schema. Removed the standalone Differential Evolution node.
- Replaced the many visible internal `__if_*` columns with one versioned `IdeaFlow state` cell. Population tables
  now expose only problem values, relevant feasibility/ranking results, and NFE. Removed the workflow iteration
  counter entirely; evaluation progress, deterministic random scopes, monitoring, stopping, and export use NFE.
- Simplified Pareto Ranking & Diversity, Optimization Monitor, and Quality Indicators by deriving objectives,
  directions, aggregate constraints, and hypervolume references from Problem Setup. Monitor retains only its stage
  label; Ranking and Quality Indicators require no dialog settings.
- Unified all exact-evaluation paths in Evaluation and removed Finalize Custom Evaluation. The modern dialog now
  offers built-in benchmark, guided formula cards, and upstream-result modes. The safe formula language validates
  declared results and variable names before execution and supports standard arithmetic and mathematical functions.
- Integrated natural-binary decoding into Evaluation. Encoded benchmark workflows no longer need a separate decoder;
  evaluated output preserves the decoded numerical columns, and Decode Binary Variables was removed from the catalog.
- Combined Experiment Setup and Define Optimization Problem into one modern **Problem Setup** source node.
- Problem Setup emits one combined table containing run settings and the problem definition.
- Removed replicate generation. Each execution creates one deterministic run; repeated runs and seed sweeps now use
  standard KNIME loops or Components.
- Removed the standalone Experiment Setup node from the public extension.
- Renamed the beginner-facing nodes to Problem Setup, Initial Population, Evaluation, Selection, and Elitism.
- Removed the algorithm/recipe field and reduced Initial Population to one Problem Setup input.
- Moved configuration-time problem metadata into the established KNIME table-support source unit so existing Eclipse
  PDE workspaces resolve it reliably during incremental builds.
- Simplified Problem Setup: Seed label, no separate problem name, all settings visible, zero-based generated groups,
  Float/Integer/Binary type labels, and natural-binary-only encoded variables.
- Made Selection, Crossover, Mutation, and Elitism automatically consume variables, bounds, objectives,
  directions, and constraint accounting from Problem Setup; removed the corresponding manual dialog fields and
  advanced-setting toggles.
- Simplified Evolution Loop End to stop only at the Problem Setup NFE budget or optional objective targets. Its
  modern dialog supports multiple objective/value cards and ANY/ALL combination rules; directions are automatic.
- Made Elitism align compatible parent and child tables by column name and preserve columns found on either branch,
  instead of requiring identical column order and count.
- Fixed run-level NFE propagation through Elitism. Every survivor now receives the newest evaluated NFE, preventing
  the stopping counter from stalling when a converged population retains only older parents.
- Added versioned configuration-time Problem Definition metadata. Initial populations, binary decoding, objectives,
  constraints, bounds, and evaluation-history schemas now propagate without executing upstream nodes.
- Combined benchmark/formula calculation, external-result validation, constraint accounting, NFE accounting, and
  history creation in Evaluation.
- Reorganized the flat node list into beginner-facing Setup, Evolution, Evaluation, and Results categories plus a separate Advanced toolbox.
- Renamed the public catalog around familiar EA vocabulary.
- Added native Evolution Loop Start/End nodes with NFE and target stopping, final-population output, and per-run stop summaries.
- Added Optimization Monitor, combining pass-through monitoring, scalar population statistics, feasibility counts, Pareto-set size, optional hypervolume, and detailed event output.
- Replaced free-text variable/objective/constraint lists in the principal modern dialogs with input-schema-driven multi-column selectors while preserving the existing NodeModel settings format.
- Moved specialist tuning controls into the modern dialog's Advanced section and rewrote labels around user actions rather than internal implementation terms.
- Added friendly displayed labels for crossover, mutation, survivor selection, DE, archive, adaptation, repair, and direction choices while retaining stable persisted strategy IDs.
- Added a modern KNIME table view to Optimization Monitor for inspecting per-run progress without adding another table-view node.

- Added public problem, population, run-state, and event contracts.
- Added public strategy SPI and service discovery.
- Added deterministic scoped random streams and family-neutral Ask/Tell interfaces.
- Added reusable multi-objective, hypervolume, DE, SHADE/L-SHADE, and island kernels.
- Added the complete generic experiment, initialization, GA variation, evaluation, ranking, DE, adaptive update, archive, scheduling, stopping, surrogate, metric, migration, trace, and IOH export catalog.
- Added workflow-visible reference directions and versioned GA, NSGA-II, NSGA-III, DE, GDE3, and L-SHADE conformance recipes.
- Removed the 1.x factories, global registries, custom loop state, and obsolete chart implementation after replacing their capabilities with generic nodes and standard KNIME loops/plots.
- Added Tycho feature and p2 update-site packaging, architecture/publication documentation, migration guidance, and mathematical/IOH regression tests.
- Migrated Problem Definition to KNIME's declarative modern dialog API while preserving its factory ID, settings keys, and output contract.
- Added settings-compatible modern embedded dialogs to every remaining IdeaFlow node, including native choice switches, numeric inputs, booleans, and field descriptions; classic dialogs remain only as classic-workbench fallback.
- Fixed modern choice-field schema generation by using string-compatible choice providers, and corrected legacy node-description categories for statistics, reference indicators, and surrogate coordination.
- Replaced Problem Definition's parallel comma-separated fields with structured modern cards for direct groups, binary-encoded vectors, objectives, and constraints, including conditional settings and a live generated-column summary.
- Added representation metadata, automatic encoded-gene initialization, and metadata-driven natural-binary decoding.
