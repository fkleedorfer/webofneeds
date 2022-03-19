package won.utils.blend.algorithm.sat.shacl2.astarish;

import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;

import java.util.stream.Collectors;

public class VerbosityAwareSearchNodeFormatter implements SearchNodeFormatter {
    private AlgorithmState state;

    public VerbosityAwareSearchNodeFormatter(AlgorithmState state) {
        this.state = state;
    }

    @Override public String format(SearchNode searchNode, CompactBindingsManager bindingsManager) {
        StringBuilder sb = new StringBuilder();
        sb
                        .append("valid/globally valid : ")
                        .append(searchNode.valid)
                        .append("/")
                        .append(searchNode.globallyValid);
        if (state.verbosity.isMaximum()) {
            sb.append("\nshapeNodes  :")
                            .append(searchNode.shapes.stream().map(Shape::getShapeNode).map(Object::toString).collect(
                                            Collectors.joining(", ")))
                            .append("\nfocusNodes   :")
                            .append(searchNode.focusNodes.stream().map(Object::toString)
                                            .collect(Collectors.joining(", ")));
        }
        sb.append("\nbindings    :")
                        .append(searchNode
                                        .bindings
                                        .getBindingsAsSet()
                                        .stream()
                                        .map(b -> b.getVariable() + " -> " + b.getBoundNode())
                                        .sorted()
                                        .collect(Collectors.joining("\n\t\t\t\t", " ","")))
                        .append("\nencountered :")
                        .append(searchNode.encounteredVariables
                                        .getVariables()
                                        .stream()
                                        .map(Object::toString)
                                        .sorted()
                                        .collect(Collectors.joining("\n\t\t\t\t", " ","")));
        return sb.toString();
    }
}
