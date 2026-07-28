package org.ideaflow.nodes.benchmark;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class EvaluationNodeFactory extends NodeFactory<EvaluationNodeModel> implements ModernNodeDialogFactory{
 @Override public EvaluationNodeModel createNodeModel(){return new EvaluationNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<EvaluationNodeModel> createNodeView(int i,EvaluationNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new EvaluationNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return EvaluationNodeParameters.class;}
}
