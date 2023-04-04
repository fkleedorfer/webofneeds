package won.utils.blend.support.stats.formatter;

import org.apache.jena.graph.Node;
import won.utils.blend.BLEND;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.stats.BlendingResultStats;
import won.utils.blend.support.stats.comparator.GroupedTemplateStatsComparator;

import java.util.Comparator;
import java.util.Set;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

public class DefaultBlendingResultStatsFormatter
                implements BlendingResultStatsFormatter {
    @Override
    public String format(BlendingResultStats blendingResultStats) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                        .append("\nBlendingResult\n")
                        .append("==============\n")
                        .append(String.format("%-20s: %d\n", "Number of results", blendingResultStats.results))
                        .append(String.format("%-20s: %d (%s)\n", "Variables",
                                        blendingResultStats.variables.size(),
                                        blendingResultStats.variables.stream()
                                                        .map(Object::toString)
                                                        .collect(joining(", "))))
                        .append(String.format("%-20s: %d (%s)\n", "Constants",
                                        blendingResultStats.constants.size(),
                                        blendingResultStats.constants.stream()
                                                        .map(Object::toString)
                                                        .collect(joining(", "))));
        if (blendingResultStats.results > 0) {
            stringBuilder.append("\nAggregated variable bindings\n")
                            .append(String.format("\n|%s|%s|\n", center("Variable", 40), center("Bound Value(s)", 40)));
            blendingResultStats.boundValuesByVariable.entrySet().stream().forEach(
                            e -> stringBuilder.append(String.format("| %-39s| %-39s\n",
                                            e.getKey(),
                                            e.getValue().stream().map(Object::toString).collect(joining(", ")))));
            stringBuilder.append("\nResults grouped by binding characteristics\n");
            stringBuilder.append(String.format("\n|%s|%s|%s|%s|%s|\n",
                            center("Count", 10),
                            center("Unbound", 10),
                            center("Bound to Constants", 20),
                            center("Bound to Variables", 20),
                            center("Templates", 20)));
            blendingResultStats.groupedTemplateStats.stream()
                            .sorted(new GroupedTemplateStatsComparator())
                            .forEach(s -> stringBuilder.append(
                                            String.format("| %-9s| %-9s| %-19s| %-19s| %-19s |\n",
                                                            s.count,
                                                            s.bindingStats.unboundCount,
                                                            s.bindingStats.boundToConstantCount,
                                                            s.bindingStats.boundToVariableCount,
                                                            s.templateNodes
                                                                            .stream()
                                                                            .map(Object::toString)
                                                                            .collect(joining(
                                                                                            ", ")))));
            stringBuilder.append("\nAll resulting bindings:\n")
                            .append("==============\n");
            blendingResultStats.allBindings
                            .stream()
                            .sorted(Comparator
                                            .comparing((VariableBindings x) -> x.getUnboundNonBlankVariables().size())
                                            .thenComparing((VariableBindings x) -> x.getVariablesBoundToVariables()
                                                            .stream().filter(not(
                                                                            Node::isBlank))
                                                            .count())
                                            .thenComparing((VariableBindings x) -> x.getVariablesBoundToConstants()
                                                            .stream().filter(not(
                                                                            Node::isBlank))
                                                            .count()))
                            .forEach(tb -> {
                                stringBuilder
                                                .append("    ")
                                                .append(tb.size())
                                                .append(String.format(
                                                                " variable bindings ( %d const, %d var, %d unbound )\n",
                                                                tb.getVariablesBoundToConstants().stream().filter(not(
                                                                                Node::isBlank)).count(),
                                                                tb.getVariablesBoundToVariables().stream().filter(not(
                                                                                Node::isBlank)).count(),
                                                                tb.getUnboundNonBlankVariables().size()));
                                tb.getBindingsAsSet().stream()
                                                .map(vb -> String.format("%-50s -> %s\n", vb.getVariable(),
                                                                formatBoundNode(vb)))
                                                .sorted()
                                                .forEach(stringBuilder::append);
                            });
            stringBuilder.append("--------------\n");
        }
        return stringBuilder.toString();
    }

    private String formatBoundNode(VariableBinding vb) {
        Node boundNode = vb.getBoundNode();
        if (boundNode.equals(BLEND.unbound)) {
            return "[explicitly unbound]";
        } else {
            return boundNode.toString();
        }
    }

    private String center(String text, int width) {
        return center(text, ' ', width);
    }

    private String center(String text, Character pad, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int padSize = (width - text.length()) / 2;
        String result = pad.toString().repeat(padSize) + text + pad.toString().repeat(padSize);
        if (result.length() < width) {
            result = result + pad;
        }
        return result;
    }
}
