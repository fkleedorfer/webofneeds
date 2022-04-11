package won.utils.blend.support.shacl.validationlistener;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.Shape;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShapeEvaluation extends ShaclEvaluation {

    public ShapeEvaluation(Shape shape, Node focusNode) {
        super(shape, focusNode);
    }

    public ShapeEvaluation(Shape shape, Node focusNode, Boolean valid,
                    Set<Node> encounteredVariables,
                    Set<ShaclEvaluation> subEvaluations) {
        super(shape, focusNode, valid, encounteredVariables, subEvaluations);
    }

    @Override
    public ShaclEvaluation setValid(boolean valid) {
        return new ShapeEvaluation(shape, focusNode, valid, encounteredVariables, subEvaluations);
    }

    @Override
    public ShaclEvaluation addEncounteredVariables(Set<Node> encounteredVariables) {
        return new ShapeEvaluation(shape, focusNode, valid,
                        Stream.concat(encounteredVariables.stream(), this.encounteredVariables.stream()).collect(
                                        Collectors.toSet()), subEvaluations);
    }

    @Override
    public ShaclEvaluation addEncounteredVariable(Node encounteredVariable) {
        return addEncounteredVariables(Collections.singleton(encounteredVariable));
    }

    @Override
    public ShaclEvaluation addSubEvaluation(ShaclEvaluation subEvaluation) {
        return new ShapeEvaluation(shape, focusNode, valid, encounteredVariables,
                        Stream.concat(this.subEvaluations.stream(), Stream.of(subEvaluation)).collect(
                                        Collectors.toSet()));
    }

    public String toString() {
        return "ShapeEvaluation{ "
                        + "shape=" + shape
                        + ", focusNode=" + focusNode
                        + ", valid=" + valid
                        + ", encounteredVars=" + encounteredVariables
                        + ", subEvaluations=" + subEvaluations + "}";
    }

    public String toPrettyString() {
        return toPrettyString("");
    }

    public String toPrettyString(String linePrefix){
        StringBuilder sb = new StringBuilder();
        sb.append(linePrefix).append("ShapeEvaluation { \n")
        .append(linePrefix).append( "    shape      : ").append(shape).append("\n")
        .append(linePrefix).append( "    focusNode  : ").append(focusNode).append("\n")
        .append(linePrefix).append( "    valid      : ").append(valid).append("\n")
        .append(linePrefix).append( "    encounteredVars : ");
        if (encounteredVariables.isEmpty()) {
            sb.append("[none]\n");
        } else {
            sb.append("\n").append(encounteredVariables.stream().map(Object::toString)
                                        .sorted()
                                        .collect(Collectors.joining("\n" + linePrefix + "        ",
                                                        linePrefix + "        ",
                                                        "")))
                            .append("\n");

        }
        sb.append(linePrefix).append("    subEvaluations  : ");
        if (subEvaluations.isEmpty()) {
            sb.append("[none]\n");
        } else {
            sb.append("\n").append(
                            subEvaluations.stream()
                                            .map(ev -> ev.toPrettyString(linePrefix + "        "))
                                            .collect(Collectors.joining()))
                            .append("\n");
        }
        sb.append(linePrefix).append("}\n");
        return sb.toString();
    }

    public Optional<Boolean> isValid() {
        return Optional.ofNullable(valid);
    }
}
