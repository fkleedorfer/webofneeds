package won.utils.blend.algorithm.sat.shacl2.astarish;

import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;

import java.util.stream.Collectors;

public class DefaultSearchNodeFormatter implements SearchNodeFormatter {
    @Override
    public String format(SearchNode searchNode, CompactBindingsManager bindingsManager) {
        StringBuilder sb = new StringBuilder();
        sb
                        .append("valid: ").append(searchNode.valid)
                        .append(", globallyValid: ").append(searchNode.globallyValid)
                        .append("\nshapeNodes  :")
                        .append(searchNode.shapes.stream().map(Shape::getShapeNode).map(Object::toString).collect(
                                        Collectors.joining(", ")))
                        .append("\nfocusNodes   :")
                        .append(searchNode.focusNodes.stream().map(Object::toString).collect(Collectors.joining(", ")))
                        .append("\nbindings    :")
                        .append(searchNode.bindings)
                        .append("\nencountered :")
                        .append(searchNode.encounteredVariables);
        return sb.toString();
    }
}
