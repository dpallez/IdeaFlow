package org.ideaflow.nodes.io;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class ExportToIohProfilerNodeFactory extends NodeFactory<ExportToIohProfilerNodeModel> implements ModernNodeDialogFactory{
 @Override public ExportToIohProfilerNodeModel createNodeModel(){return new ExportToIohProfilerNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<ExportToIohProfilerNodeModel> createNodeView(int i,ExportToIohProfilerNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new ExportToIohProfilerNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.ExportToIohProfiler.class;}
}
