package org.ideaflow.nodes.archive;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class ExternalArchiveNodeFactory extends NodeFactory<ExternalArchiveNodeModel> implements ModernNodeDialogFactory{
 @Override public ExternalArchiveNodeModel createNodeModel(){return new ExternalArchiveNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<ExternalArchiveNodeModel> createNodeView(int i,ExternalArchiveNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new ExternalArchiveNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.ExternalArchive.class;}
}
