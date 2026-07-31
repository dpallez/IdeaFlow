# Frequently Asked Questions

[Documentation index](README.md) · [Node reference](NODES.md) · [Troubleshooting](TROUBLESHOOTING.md)

## Can IdeaFlow be used without programming?

Yes. The included nodes and example workflows can be configured graphically. Java is only required when developing the extension or adding a new strategy or node.

## Can I define a custom optimization problem?

Yes. Problem Setup defines the variables, objectives, and constraints. Evaluation can calculate formulas directly or validate result columns produced by Python, R, simulations, databases, machine-learning models, or other KNIME nodes.

## Does IdeaFlow support multiple objectives?

Yes. It provides constraint-aware Pareto ranking, NSGA-II, NSGA-III, GDE3, Das-Dennis reference directions, hypervolume tracking, and reference-front indicators.

## Are algorithms separate nodes?

No. Algorithms are assembled from reusable operators. For example, a DE workflow combines DE donor selection, a differential mutation strategy, DE crossover, Evaluation, and pairwise DE competition. The examples show complete configurations.

## How does SHADE's archive cross the loop boundary?

Optimization Loop Start emits a current archive and Selection accepts it as an optional input. Elitism's rejected or replaced candidates can be returned through Optimization Loop End's optional archive input. The archive schema must exactly match the active population schema.

## Can Python or R be used in a workflow?

Yes. Preserve IdeaFlow's internal columns, create the objective and constraint result columns declared in Problem Setup, and pass the resulting table through Evaluation in upstream-column mode.

## Can results be exported?

Yes. Export to IOHprofiler writes standard IOH files and an optional IdeaFlow TSV sidecar. Standard KNIME writer nodes can save any visible table output.

## Are executions reproducible?

IdeaFlow derives stochastic operations from the configured run seed. Full reproducibility also requires fixed external data, versions, resources, and seeds in non-IdeaFlow nodes.

## How do I run several independent repetitions?

Use standard KNIME loops or Components around the workflow and vary the Problem Setup seed. Feed the resulting convergence histories to Optimization Run Analysis.

## Can workflows be shared?

Yes. Share the `.knwf` file and document the required KNIME and IdeaFlow versions. Avoid credentials, personal paths, and unavailable external resources.
