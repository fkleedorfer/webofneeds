package won.utils.blend.support.shacl.validationlistener;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.Shape;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class ShaclEvaluation {
    protected final Shape shape;
    protected final Node focusNode;
    protected final Boolean valid;
    protected final Set<Node> encounteredVariables;
    protected final Set<ShaclEvaluation> subEvaluations;

    public ShaclEvaluation(Shape shape, Node focusNode) {
        this.shape = shape;
        this.focusNode = focusNode;
        this.valid = null;
        this.encounteredVariables = Collections.emptySet();
        this.subEvaluations = Collections.emptySet();
    }

    public ShaclEvaluation(Shape shape, Node focusNode, Boolean valid,
                    Set<Node> encounteredVariables,
                    Set<ShaclEvaluation> subEvaluations) {
        this.shape = shape;
        this.focusNode = focusNode;
        this.valid = valid;
        this.encounteredVariables = Collections.unmodifiableSet(new HashSet<>(encounteredVariables));
        this.subEvaluations = Collections.unmodifiableSet(new HashSet<>(subEvaluations));
    }

    public abstract ShaclEvaluation setValid(boolean valid);

    public abstract ShaclEvaluation addEncounteredVariables(Set<Node> encounteredVariables);

    public abstract ShaclEvaluation addEncounteredVariable(Node encounteredVariable);

    public abstract ShaclEvaluation addSubEvaluation(ShaclEvaluation subEvaluation);

    public abstract String toPrettyString();

    public abstract String toPrettyString(String linePrefix);

    Optional<Boolean> isValid() {
        return Optional.ofNullable(this.valid);
    }

    public Shape getShape() {
        return shape;
    }

    public Node getFocusNode() {
        return focusNode;
    }

    public Set<Node> getEncounteredVariables() {
        return encounteredVariables;
    }

    public Set<ShaclEvaluation> getSubEvaluations() {
        return subEvaluations;
    }
}
