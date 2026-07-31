# Troubleshooting

[Documentation index](README.md) · [Node reference](NODES.md) · [Installation](INSTALLATION.md)

## A node cannot be configured

Configure upstream nodes first so KNIME can provide their output schema. IdeaFlow dialogs obtain variables, objectives, and constraints from Problem Setup metadata.

## A variable or objective cannot be found

Check the exact names in Problem Setup and preserve the attached table metadata. An objective or constraint result column must exist when Evaluation uses formula or upstream-column evaluation.

## Candidates remain unevaluated or NFE does not increase

Pass every newly evaluated batch through Evaluation. Confirm that all declared objective and constraint results are numeric and valid. Do not remove IdeaFlow's state column or edit NFE manually.

## The loop does not stop

Optimization Loop End reads the maximum-evaluations budget from the active population. Verify that the population was created from Problem Setup and passed through Evaluation. Check optional objective targets and remember that the loop stops before a complete generation that would exceed the budget.

## Elitism reports incompatible inputs

The first input must contain evaluated parents and the second evaluated children from the same run, population, and problem schema. Choose an update mode appropriate for the number of objectives and use DE-produced children for pairwise DE or GDE3 modes.

## The SHADE archive remains empty

Connect Elitism's `Rejected or replaced` output to Optimization Loop End's `Next archive` input. Connect Loop Start's `Current archive` output to Selection's archive input. Both population and archive must have exactly the same schema.

## Selection cannot prepare DE donors

DE donor mode requires direct float variables and at least four active individuals per population. Use the optional archive for SHADE/L-SHADE and ensure it was produced from the same population schema.

## Results differ despite a fixed seed

Check external nodes, data sources, parallel execution, and their own random settings. IdeaFlow's deterministic seed cannot control randomness introduced outside the extension.

## A called workflow loses IdeaFlow information

Ensure callable-workflow input and output schemas preserve the state column, all `__if_` columns, and table metadata. Avoid column filters around Component or Call Workflow boundaries unless the required columns are explicitly retained.

## The extension does not load after an update

Remove older manually installed plugin versions, install the p2 update-site ZIP through KNIME's extension mechanism, restart KNIME, and check the KNIME log. IdeaFlow currently targets KNIME 5.11 and Java 21.
