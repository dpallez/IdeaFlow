package org.ideaflow.nodes.migration;
import org.ideaflow.knime.*;import org.knime.core.node.*;
public final class PopulationMigrationNodeFactory extends NodeFactory<PopulationMigrationNodeModel> implements ModernNodeDialogFactory{
 @Override public PopulationMigrationNodeModel createNodeModel(){return new PopulationMigrationNodeModel();}@Override protected int getNrNodeViews(){return 0;}@Override public NodeView<PopulationMigrationNodeModel> createNodeView(int i,PopulationMigrationNodeModel m){return null;}@Override protected boolean hasDialog(){return true;}@Override protected NodeDialogPane createNodeDialogPane(){return new PopulationMigrationNodeDialog();}@Override public Class<? extends org.knime.node.parameters.NodeParameters> modernParametersClass(){return ModernNodeParameters.PopulationMigration.class;}
}
