package org.ideaflow.nodes.benchmark;
import org.knime.core.node.defaultnodesettings.*;
final class BenchmarkEvaluatorNodeDialog extends DefaultNodeSettingsPane{
 BenchmarkEvaluatorNodeDialog(){addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(BenchmarkEvaluatorNodeModel.CFG_METHOD,"BUILT_IN"),"Evaluation method","BUILT_IN","FORMULAS","EXISTING_RESULTS"));addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(BenchmarkEvaluatorNodeModel.CFG_FUNCTION,"ACKLEY"),"Benchmark","ACKLEY","SPHERE","ROSENBROCK","RASTRIGIN","GRIEWANK","ONEMAX","ZDT1","ZDT2","ZDT3","DTLZ2"));}
}
