package org.ideaflow.nodes.initialization;
import org.knime.core.node.defaultnodesettings.*;
final class PopulationInitializationNodeDialog extends DefaultNodeSettingsPane{
    PopulationInitializationNodeDialog(){
        addDialogComponent(new DialogComponentNumber(new SettingsModelIntegerBounded(PopulationInitializationNodeModel.CFG_POPULATION_SIZE,50,1,1000000),"Population size",1));
        addDialogComponent(new DialogComponentString(new SettingsModelString(PopulationInitializationNodeModel.CFG_POPULATION_ID,"population-0"),"Population ID"));
    }
}
