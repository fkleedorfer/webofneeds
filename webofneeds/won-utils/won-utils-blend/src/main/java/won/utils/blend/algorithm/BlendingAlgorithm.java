package won.utils.blend.algorithm;

import won.utils.blend.support.graph.TemplateGraphs;

import java.util.stream.Stream;

public interface BlendingAlgorithm {
    Stream<TemplateGraphs> blend(BlendingInstance blendingInstanceContext);
}
