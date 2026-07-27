package org.ideaflow.spi;

import java.util.Map;
import org.ideaflow.api.OptimizationEvent;

public interface ExportMapping extends Strategy {
    Map<String, String> map(OptimizationEvent event);
}
