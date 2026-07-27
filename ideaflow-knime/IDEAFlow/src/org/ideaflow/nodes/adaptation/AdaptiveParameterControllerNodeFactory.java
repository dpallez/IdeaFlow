package org.ideaflow.nodes.adaptation;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class AdaptiveParameterControllerNodeFactory extends NodeFactory<AdaptiveParameterControllerNodeModel> implements ModernNodeDialogFactory{
 @Override public AdaptiveParameterControllerNodeModel createNodeModel(){return new AdaptiveParameterControllerNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<AdaptiveParameterControllerNodeModel> createNodeView(int i,AdaptiveParameterControllerNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new AdaptiveParameterControllerNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.AdaptiveParameterController.class;}
}
