package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Node;
import won.utils.blend.BLEND;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static won.utils.blend.algorithm.join.AlgorithmStateLogic.retainUnexplored;
import static won.utils.blend.algorithm.join.SearchNodeLogic.getPriority;

public class SearchNodeFormatter {
    private AlgorithmState state;

    public SearchNodeFormatter(AlgorithmState state) {
        this.state = state;
    }

    public static String format(AlgorithmState state, SearchNode searchNode) {
        return new SearchNodeFormatter(state).format(searchNode);
    }

    public String format(SearchNode node) {
        return new StringBuilder()
                        .append("SearchNode ").append(node.id).append("\n")
                        .append("\tpriority                : ").append(getPriority(node)).append("\n")
                        // .append("\tblended graph size : ").append(node.blendedGraphSize).append("\n")
                        .append("\tpredecessor(s)          : ")
                        .append(node.predecessors.stream().map(n -> n.id).map(Objects::toString).collect(joining(", ")))
                        .append("\n")
                        .append("\tbindings                : ")
                        .append(bindingsToString(node.bindings, "\n\t                          ")).append("\n")
                        .append("\tencountered             : ").append(node.encounteredVariablesFlat).append("\n")
                        .append("\tencountered(unexplored) : ")
                        .append(retainUnexplored(state, node.encounteredVariablesFlat)).append("\n")
                        .append("\texploring               : ").append(node.exploring).append("\n")
                        .append("\tunsatisfied shapes      : ")
                        .append(node.unsatisfiedShapesByRequiredVariable.values().stream().flatMap(
                                        Collection::stream).collect(
                                                        Collectors.toSet())).append("\n")
                        .append("\tshapes/focusNodes: ").append(node.shapeToFocusNodes).append("\n")
                        .toString();
    }

    public static String bindingsToString(VariableBindings bindings, String delimiter) {
        return bindings.getBindingsAsSet()
                        .stream()
                        .map(b -> b.getVariable() + " -> " + formatBoundNode(b))
                        .sorted()
                        .collect(joining(delimiter));
    }

    private static String formatBoundNode(VariableBinding b) {
        Node boundNode = b.getBoundNode();
        if (boundNode.equals(BLEND.unbound)){
            return "[explicitly unbound]";
        } else {
            return boundNode.toString();
        }
    }
}
