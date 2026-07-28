package org.ideaflow.nodes.benchmark;
import org.knime.core.node.defaultnodesettings.*;
final class EvaluationNodeDialog extends DefaultNodeSettingsPane{
 EvaluationNodeDialog(){addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(EvaluationNodeModel.CFG_METHOD,"BUILT_IN"),"Evaluation method","BUILT_IN","FORMULAS","EXISTING_RESULTS"));addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(EvaluationNodeModel.CFG_FUNCTION,"ACKLEY"),"Benchmark","ACKLEY","SPHERE","ROSENBROCK","RASTRIGIN","GRIEWANK","ONEMAX","ZDT1","ZDT2","ZDT3","DTLZ2"));}
}
