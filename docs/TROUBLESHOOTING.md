# Troubleshooting

[Main README](../README.md) · [Documentation index](README.md) · [Overview](OVERVIEW.md) · [KNIME installation](KNIME_INSTALLATION.md) · [IdeaFlow installation](INSTALLATION.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Development](DEVELOPMENT.md) · [Troubleshooting](TROUBLESHOOTING.md)


## A node cannot be configured

Verify that preceding nodes are configured and that the expected columns exist in their output schemas. Some dialogs can only offer variable or objective columns after upstream nodes have been configured.

## A variable or objective cannot be found

Verify the names defined in [`Define Optimization Problem`](NODES.md#define-optimization-problem) and those produced by the evaluation. Names are sensitive to spelling differences. A column declared as an objective must exist before [`Finalize Evaluation`](NODES.md#finalize-evaluation).

## Candidates remain unevaluated

Verify that:

- the evaluation function produces the expected columns;
- [`Finalize Evaluation`](NODES.md#finalize-evaluation) is placed immediately after evaluation;
- objectives are selected in its dialog;
- produced values are numerical and valid.

## NFE does not increase correctly

NFE is updated by [`Finalize Evaluation`](NODES.md#finalize-evaluation). Verify that it is executed once for each newly evaluated batch and that an already finalized population is not finalized a second time without a new evaluation.

## The loop does not stop

Verify the configuration of [`Evolution Loop End`](NODES.md#evolution-loop-end): maximum number of generations, evaluation budget, and optional target. The budget can only be reached if NFE is updated correctly.

## Select Survivors produces an error

Verify that:

- the first input contains the current population;
- the second contains evaluated offspring;
- objectives and directions are consistent;
- the selected mode matches the number of objectives;
- run and population identifiers are preserved;
- columns specific to DE or multi-objective optimization are available when required by the mode.

## Results change despite using a fixed seed

Verify that all random operations use IdeaFlow seeds and that no external node introduces an uncontrolled generator. Parallelization, an external model, or a changing data source may also modify the result.

## The external archive remains empty

The second output of [`Select Survivors (Elitism)`](NODES.md#select-survivors-elitism) must feed [`Update Archive`](NODES.md#update-archive). When the workflow requires a reusable archive, preserve its output between generations and reconnect it to the node that consumes it, such as [`Differential Evolution`](NODES.md#differential-evolution).

## A sub-workflow does not receive the correct columns

Compare the schemas of `Container Input (Table)` and `Container Output (Table)` ports with those expected by the calling workflow. Also verify that internal `__if_` columns have not been filtered out.

## The extension no longer loads after an update

For a [manual installation](INSTALLATION.md#manual-installation-using-a-jar-file), remove older JAR versions from `dropins`, place the compatible version there, and restart KNIME. Also verify the KNIME version and Java environment being used.

---
## Related documentation

- [Node reference](NODES.md)
- [IdeaFlow installation](INSTALLATION.md)
- [KNIME installation](KNIME_INSTALLATION.md)
- [Workflow tutorial](WORKFLOW_TUTORIAL.md)
- [KNIME documentation](https://docs.knime.com/)
