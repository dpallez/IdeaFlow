package org.ideaflow.nodes.benchmark;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class BenchmarkEvaluatorNodeFactory extends NodeFactory<BenchmarkEvaluatorNodeModel> implements ModernNodeDialogFactory{
 @Override public BenchmarkEvaluatorNodeModel createNodeModel(){return new BenchmarkEvaluatorNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<BenchmarkEvaluatorNodeModel> createNodeView(int i,BenchmarkEvaluatorNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new BenchmarkEvaluatorNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return BenchmarkEvaluatorNodeParameters.class;}
}
