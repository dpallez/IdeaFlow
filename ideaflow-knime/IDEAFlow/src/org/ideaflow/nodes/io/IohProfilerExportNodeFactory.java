package org.ideaflow.nodes.io;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class IohProfilerExportNodeFactory extends NodeFactory<IohProfilerExportNodeModel> implements ModernNodeDialogFactory{
 @Override public IohProfilerExportNodeModel createNodeModel(){return new IohProfilerExportNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<IohProfilerExportNodeModel> createNodeView(int i,IohProfilerExportNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new IohProfilerExportNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.IohProfilerExport.class;}
}
