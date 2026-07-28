package org.ideaflow.knime;

import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.node.parameters.NodeParameters;

/** Adds a KNIME modern default dialog to a NodeModel-backed factory. */
public interface ModernNodeDialogFactory extends NodeDialogFactory {
    Class<? extends NodeParameters> modernParametersClass();

    @Override
    default NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, modernParametersClass());
    }
}
