# Best Practices

[Documentation index](README.md) · [Node reference](NODES.md) · [Examples](../ideaflow-knime/examples/EXAMPLES.md)

## Keep the workflow readable

Use a left-to-right flow and annotations for setup, initial evaluation, generation operators, stopping, analysis, and export. Rename repeated Evaluation nodes as `Initial Evaluation` and `Offspring Evaluation`.

## Define the problem once

Use one Problem Setup node as the source of run settings, variables, objectives, constraints, directions, and bounds. Connect the same setup table to every Evaluation node and preserve its attached metadata on population tables.

## Preserve IdeaFlow columns

Columns prefixed with `__if_` carry identity, seed, state, NFE, generation, and population information. Do not filter or rename them unless a documented IdeaFlow node owns that transformation.

## Evaluate candidates through Evaluation

Even when formulas or external nodes calculate the objective, use Evaluation to validate declared results, aggregate constraint violations, mark candidates as evaluated, update NFE, and emit history events. Do not manually modify evaluation-state or NFE columns.

## Respect the strict budget

The evaluation budget belongs to Problem Setup and Optimization Loop End treats it as a ceiling. Choose population sizes so a useful number of complete generations fits within the budget.

## Keep archives separate from survivors

A DE archive is donor memory, not an active population. Feed it through the dedicated loop archive ports. Do not concatenate it into the active population because that would change survivor selection and evaluation accounting.

## Make experiments reproducible

Record the IdeaFlow and KNIME versions, seed, problem definition, population size, operators, stopping criteria, and evaluation budget. Fix seeds in external nodes as well.

## Use the appropriate analysis output

Use Track Progress for compact per-generation summaries, Record Population for detailed snapshots, Optimization Run Analysis for repeated runs, and Export to IOHprofiler for IOH-compatible files.

## Share portable workflows

Use relative or workflow-local resources. Do not publish personal paths, credentials, generated output, or data that recipients cannot access. Re-import and execute exported workflows in the supported KNIME version.
