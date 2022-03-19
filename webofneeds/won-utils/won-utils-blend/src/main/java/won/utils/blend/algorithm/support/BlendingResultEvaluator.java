package won.utils.blend.algorithm.support;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.Path;
import won.utils.blend.BLEND;
import won.utils.blend.support.bindings.TemplateBindings;
import won.utils.blend.support.graph.TemplateGraphs;

import java.util.stream.Collectors;

public class BlendingResultEvaluator {
    private BlendingBackground blendingBackground;

    public BlendingResultEvaluator(BlendingBackground blendingBackground) {
        this.blendingBackground = blendingBackground;
    }

    public ValidationReport validateWithBackground(TemplateGraphs templateGraphs) {
        return validateWithBackground(templateGraphs.getShapesGraph(), templateGraphs.getDataGraph());
    }

    private ValidationReport validateWithBackground(Graph shapesGraph,
                    Graph dataGraph) {
        Graph actualDataGraph = blendingBackground.combineWithBackgroundData(dataGraph);
        if (actualDataGraph == null) {
            return null;
        }
        Graph actualShapesGraph = blendingBackground.combineWithBackgroundShapes(shapesGraph);
        if (actualShapesGraph == null) {
            return null;
        }
        Shapes actualShapes = Shapes.parse(actualShapesGraph);
        ValidationReport report = ShaclValidator.get().validate(actualShapes, actualDataGraph);
        return report;
    }

    public boolean conformsToTemplateShapes(TemplateGraphs result, Node variable) {
        ValidationReport report = validateWithBackground(result);
        return !report.getEntries()
                        .stream()
                        .anyMatch(e -> e.focusNode().equals(variable));
    }

    public boolean conformsToTemplateShapes(TemplateGraphs result, TemplateBindings bindings) {
        ValidationReport report = validateWithBackground(result);
        return !(report.getEntries()
                        .stream()
                        .filter(e -> !reportCanBeIgnored(e, bindings))
                        .count() == 0);
    }

    public int getNumberOfBoundVariablesWithEntries(ValidationReport report, TemplateBindings bindings) {
        return report.getEntries()
                        .stream()
                        .filter(e -> !reportCanBeIgnored(e, bindings))
                        .map(e -> e.focusNode())
                        .collect(Collectors.toSet())
                        .size();
    }

    private boolean reportCanBeIgnored(ReportEntry e, TemplateBindings bindings) {
        return bindings.isVariable(e.focusNode())
                        && isReportAboutBoundToProperty(e)
                        && !bindings.hasConstantBindingForVariableAllowTransitive(e.focusNode());
    }

    private boolean isReportAboutBoundToProperty(ReportEntry e) {
        return pathStartsWithBoundTo(e.resultPath());
    }

    private boolean pathStartsWithBoundTo(Path path) {
        if (path instanceof P_Link && ((P_Link) path).getNode().equals(BLEND.boundTo)) {
            return true;
        }
        if (path instanceof P_Seq) {
            return pathStartsWithBoundTo(((P_Seq) path).getLeft());
        }
        return false;
    }

    public int getNumberOfReportEntriesForBoundVariables(TemplateGraphs result, TemplateBindings bindings) {
        ValidationReport report = validateWithBackground(result);
        return (int) report.getEntries()
                        .stream()
                        .filter(e -> !reportCanBeIgnored(e, bindings))
                        .count();
    }

    public boolean hasReportEntriesForVariable(ValidationReport validationReport, Node variable) {
        return validationReport.getEntries()
                        .stream()
                        .anyMatch(e -> e.focusNode().equals(variable));
    }
}
