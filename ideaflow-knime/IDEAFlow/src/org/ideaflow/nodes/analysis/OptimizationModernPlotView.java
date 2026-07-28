package org.ideaflow.nodes.analysis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.page.Page;

/** Self-contained modern browser view; the PNG is embedded to avoid transient resource URLs. */
public final class OptimizationModernPlotView implements NodeView {
  private final Supplier<OptimizationPlotData> m_plotSupplier;

  public OptimizationModernPlotView(final Supplier<OptimizationPlotData> plotSupplier) {
    m_plotSupplier = plotSupplier;
  }

  @Override
  public Page getPage() {
    return Page.create().fromString(this::html).relativePath("index.html");
  }

  private String html() {
    try {
      final OptimizationPlotData plot = m_plotSupplier.get();
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      ImageIO.write(OptimizationPlotPanel.renderImage(plot, 1200, 720), "png", output);
      final String image = Base64.getEncoder().encodeToString(output.toByteArray());
      return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>%s</title>
                  <style>
                    html,body{margin:0;width:100%%;height:100%%;background:#fff}
                    body{display:flex;align-items:flex-start;justify-content:center;overflow:auto}
                    img{display:block;width:100%%;height:auto;max-width:1200px}
                  </style>
                </head>
                <body><img alt="%s" src="data:image/png;base64,%s"></body>
                </html>
                """
          .formatted(escapeHtml(plot.title()), escapeHtml(plot.title()), image);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not render the optimization plot.", exception);
    }
  }

  private static String escapeHtml(final String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  @Override
  public <D> Optional<InitialDataService<D>> createInitialDataService() {
    return Optional.empty();
  }

  @Override
  public Optional<RpcDataService> createRpcDataService() {
    return Optional.empty();
  }

  @Override
  public <D> Optional<ApplyDataService<D>> createApplyDataService() {
    return Optional.empty();
  }

  @Override
  public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {}

  @Override
  public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {}
}
