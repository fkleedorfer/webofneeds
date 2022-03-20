package won.utils.blend.algorithm.support.instance;

import org.apache.jena.graph.Graph;
import won.utils.blend.BlendingOptions;
import won.utils.blend.Template;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.support.graph.TemplateGraphs;

public class OverviewBlendingInstanceFormatter implements BlendingInstanceFormatter {
    @Override
    public String format(BlendingInstance instance) {
        StringBuilder sb = new StringBuilder();
        sb.append("Left Template:\n");
        formatInstanceTemplate(instance.leftTemplate, sb);
        sb.append("Right Template:\n");
        formatInstanceTemplate(instance.rightTemplate, sb);
        sb.append("Options:\n");
        formatBlendingOptions(instance.blendingOptions, sb);
        return sb.toString();
    }

    private void formatBlendingOptions(BlendingOptions blendingOptions, StringBuilder sb) {
        sb
                        .append("\t")
                        .append("omitBindingSubsets          :").append(blendingOptions.isOmitBindingSubsets())
                        .append("\n")
                        .append("\t").append("unboundHandlingMode         :")
                        .append(blendingOptions.getUnboundHandlingMode()).append("\n")
                        .append("\t").append("templateBindingsFilter      :")
                        .append(blendingOptions.hasTemplateBindingsFilter()).append("\n")
                        .append("\t").append("variableBindingsFilter      :")
                        .append(blendingOptions.hasVariableBindingFilter()).append("\n");
    }

    private void formatInstanceTemplate(Template template, StringBuilder sb) {
        TemplateGraphs templateGraphs = template.getTemplateGraphs();
        sb
                        .append("\t").append("variables      : ").append(template.getVariables()).append("\n")
                        .append("\t").append("constants      : ").append(template.getConstants()).append("\n")
                        .append("\t").append("data           : ")
                        .append(getSizeIfPresent(templateGraphs.getDataGraph()))
                        .append(" triples").append("\n")
                        .append("\t").append("shapes         : ")
                        .append(getSizeIfPresent(templateGraphs.getShapesGraph())).append(" triples").append("\n")
                        .append("\t").append("blendingConfig : ")
                        .append(getSizeIfPresent(templateGraphs.getBlendingConfigGraph())).append(" triples")
                        .append("\n")
                        .append("\t").append("bindingsGraph  : ")
                        .append(getSizeIfPresent(templateGraphs.getBindingsGraph())).append(" triples").append("\n");
    }

    private int getSizeIfPresent(Graph graph) {
        return graph == null ? 0 : graph.size();
    }
}
