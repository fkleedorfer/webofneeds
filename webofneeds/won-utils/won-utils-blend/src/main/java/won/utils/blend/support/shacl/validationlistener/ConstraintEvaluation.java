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

public class ConstraintEvaluation extends ShaclEvaluation {
    private final Constraint constraint;

    public ConstraintEvaluation(Shape shape, Constraint constraint, Node focusNode) {
        super(shape, focusNode);
        this.constraint = constraint;
    }

    public ConstraintEvaluation(Shape shape, Constraint constraint, Node focusNode, Boolean valid,
                    Set<Node> encounteredVariables,
                    Set<ShaclEvaluation> subEvaluations) {
        super(shape, focusNode, valid, encounteredVariables, subEvaluations);
        this.constraint = constraint;
    }


    @Override public ShaclEvaluation setValid(boolean valid) {
        return new ConstraintEvaluation(shape, constraint, focusNode, valid, encounteredVariables, subEvaluations);
    }

    @Override public ShaclEvaluation addEncounteredVariables(Set<Node> encounteredVariables) {
        return new ConstraintEvaluation(shape, constraint, focusNode, valid,
                        Stream.concat(encounteredVariables.stream(), this.encounteredVariables.stream()).collect(
                                        Collectors.toSet()), subEvaluations);
    }

    @Override public ShaclEvaluation addEncounteredVariable(Node encounteredVariable) {
        return addEncounteredVariables(Collections.singleton(encounteredVariable));
    }

    @Override public ShaclEvaluation addSubEvaluation(ShaclEvaluation subEvaluation) {
        return new ConstraintEvaluation(shape, constraint, focusNode, valid, encounteredVariables,
                        Stream.concat(this.subEvaluations.stream(), Stream.of(subEvaluation)).collect(
                                        Collectors.toSet()));
    }

    public String toString() {
        return "ConstraintEvaluation{ "
                        + "shape=" + shape
                        + ", focusNode=" + focusNode
                        + ", constraint=" + constraint
                        + ", valid=" + valid
                        + ", encounteredVars=" + encounteredVariables
                        + ", subEvaluations=" + subEvaluations + "}";
    }

    @Override public String toPrettyString() {
        return toPrettyString("");
    }

    @Override public String toPrettyString(String linePrefix){
        StringBuilder sb = new StringBuilder();
        sb.append(linePrefix).append("ConstraintEvaluation { \n")
        .append(linePrefix).append( "    constraint : ").append(constraint).append("\n")
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

    @Override public Optional<Boolean> isValid() {
        return Optional.ofNullable(valid);
    }
}
