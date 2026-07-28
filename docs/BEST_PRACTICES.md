# Best Practices

[Main README](../README.md) · [Documentation index](README.md) · [Overview](OVERVIEW.md) · [KNIME installation](KNIME_INSTALLATION.md) · [IdeaFlow installation](INSTALLATION.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Development](DEVELOPMENT.md) · [Troubleshooting](TROUBLESHOOTING.md)


## Naming nodes and workflow areas

Rename node instances when several nodes of the same type are present, for example `Initial Evaluation` and `Offspring Evaluation`. Use annotations to clearly separate setup, initial evaluation, loop, results, and export areas.

## Keeping the data flow readable

Prefer a left-to-right reading direction, limit crossing connections, and use vertical branches for auxiliary data such as problem definitions, archives, or reference directions.

## Separating the search process from the problem

The search process should remain independent from the [optimization problem](OPTIMIZATION_PROBLEMS.md) and its evaluation function. For a custom problem, produce objective and constraint columns using KNIME nodes, then use **Finalize Evaluation** as the explicit boundary between evaluation and evolution.

## Preserving internal columns

Columns prefixed with `__if_` maintain identity, seeds, evaluation state, NFE, generation, and population management. They must be preserved unless a documented IdeaFlow node is responsible for modifying them.

## Fixing seeds and preserving parameters

Use [`Experiment Setup`](NODES.md#experiment-setup) to define replications and the master seed. Preserve the following information with the results:

- IdeaFlow version;
- KNIME version;
- benchmark or problem definition;
- operator parameters;
- budget;
- seeds;
- stopping criteria.

## Using Finalize Evaluation once per exact evaluation

An evaluation should be counted only after its objectives and constraints have been produced. Omitting **Finalize Evaluation** prevents correct NFE updates; placing it several times on the same results may distort the budget.

## Exporting reproducible data

Use [`Optimization Monitor`](NODES.md#optimization-monitor), [`Population Statistics`](NODES.md#population-statistics), [`Evolution Trace`](NODES.md#evolution-trace), and [`Export Results`](NODES.md#export-results) according to the required level of detail. Exported files should not depend on a personal path when intended for sharing.

## Validating each search configuration with a reference workflow

Every documented recipe should have an importable workflow, a known seed, and verifiable invariants. Stochastic results should be analyzed over multiple replications when conducting a scientific comparison.

---
## Related documentation

- [Optimization problems](OPTIMIZATION_PROBLEMS.md)
- [Workflow tutorial](WORKFLOW_TUTORIAL.md)
- [Sub-workflows](SUBWORKFLOWS.md)
- [IOHprofiler](https://iohprofiler.github.io/)
- [Troubleshooting](TROUBLESHOOTING.md)
