package won.utils.blend.support.shacl.validationlistener;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.engine.constraint.*;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.ValidationListener;
import org.apache.jena.sparql.path.Path;
import won.utils.blend.support.graph.VariableAwareGraph;

import java.util.*;
import java.util.function.Function;

public class VariableAwareValidationListener implements ValidationListener {

    private final VariableAwareGraph variableAwareGraph;
    private final ArrayDeque<ShaclEvaluation> evaluationStack = new ArrayDeque<>();
    private final Set<ShaclEvaluation> finishedEvaluations = new HashSet<>();


    public VariableAwareValidationListener(VariableAwareGraph variableAwareGraph) {
        this.variableAwareGraph = variableAwareGraph;
    }

    @Override public void onBeginValidateShape(ValidationContext vCxt, Shape shape, Node focusNode) {
        variableAwareGraph.resetEncounteredVariables();
        ShapeEvaluation eval = new ShapeEvaluation(shape, focusNode);
        evaluationStack.push(eval);
    }

    @Override public void onValueNodesDeterminedForPropertyShape(ValidationContext vCxt, Shape shape, Node focusNode,
                    Path path, Set<Node> valueNodes) {
        modifyCurrentShapeEvaluation(e -> e.addEncounteredVariables(variableAwareGraph.getEncounteredVariables()));
        variableAwareGraph.resetEncounteredVariables();
    }

    @Override public void onEndValidateShape(ValidationContext vCxt, Shape shape, Node focusNode) {
        ShaclEvaluation evalShape =  evaluationStack.pop();
        if (evaluationStack.isEmpty()) {
            finishedEvaluations.add(evalShape);
        } else {
            ShaclEvaluation evalParent = evaluationStack.pop();
            evalParent = ((ShapeEvaluation) evalParent).addSubEvaluation(evalShape);
            evalParent = evalParent.setValid(evalParent.isValid().orElse(true) && evalShape.isValid().get());
            evaluationStack.push(evalParent);
        }
    }

    @Override public void onBeginValidateConstraint(ValidationContext vCxt, Shape shape, Node focusNode, Path path,
                    Set<Node> pathNodes, Constraint c) {
        ConstraintEvaluation evalConstraint = new ConstraintEvaluation(shape, c, focusNode);
        evaluationStack.push(evalConstraint);
    }

    @Override public void onEndValidateConstraint(ValidationContext vCxt, Shape shape, Node focusNode, Path path,
                    Set<Node> pathNodes, Constraint c) {
        ShaclEvaluation evalConstraint = evaluationStack.pop();
        if (!evalConstraint.isValid().isPresent()){
            evalConstraint = evalConstraint.setValid(true);
        }
        ShaclEvaluation evalParent = evaluationStack.pop();
        evalConstraint = evalConstraint.addEncounteredVariables(variableAwareGraph.getEncounteredVariables());
        evalParent = evalParent.addSubEvaluation(evalConstraint);
        evalParent = evalParent.setValid(evalParent.isValid().orElse(true) && evalConstraint.isValid().get());
        evaluationStack.push(evalParent);
    }

    private void modifyCurrentShapeEvaluation(Function<ShaclEvaluation, ShaclEvaluation> modifier) {
        ShaclEvaluation curEval = evaluationStack.pop();
        Objects.requireNonNull(curEval);
        ShaclEvaluation result = modifier.apply(curEval);
        Objects.requireNonNull(result);
        evaluationStack.push(result);
    }

    @Override public void onViolatedConstraintTerm(ValidationContext vCxt, Shape shape, Node focusNode,
                    ConstraintTerm constraintTerm, Path path, Node term) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedConstraintDataTerm(ValidationContext vCxt, Shape shape,
                    ConstraintDataTerm constraintDataTerm, Node focusNode, Path path, Node term) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedClosedConstraint(ValidationContext vCxt, Shape shape,
                    ClosedConstraint closedConstraint, Node focusNode, Path path, Node p, Node o) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedConstraintEntity(ValidationContext vCxt, Shape shape, Node focusNode,
                    ConstraintEntity constraintEntity, Path path, Node pathNode) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedDisjointConstraint(ValidationContext vCxt, Shape shape,
                    DisjointConstraint disjointConstraint, Node focusNode, Path path, Node pathNode,
                    Set<Node> compareNodes) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedEqualsConstraintNotInCompareNodes(ValidationContext vCxt, Shape shape,
                    EqualsConstraint equalsConstraint, Node focusNode, Path path, Node pathNode) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedEqualsConstraintNotInPathNodes(ValidationContext vCxt, Shape shape,
                    EqualsConstraint equalsConstraint, Node focusNode, Path path, Node compareNode) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedHasValueConstraint(ValidationContext vCxt, Shape shape,
                    HasValueConstraint hasValueConstraint, Node focusNode, Node expectedValue) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedLessThanConstraint(ValidationContext vCxt, Shape shape,
                    LessThanConstraint lessThanConstraint, Node focusNode, Path path, Node pathNode, Node compareNode) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedLessThanOrEqualsConstraint(ValidationContext vCxt, Shape shape,
                    LessThanOrEqualsConstraint lessThanOrEqualsConstraint, Node focusNode, Path path,
                    Set<Node> pathNodes, Set<Node> compareNodes, Node pathNode, Node compareNode) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedQualifiedValueShapeConstraintTooFew(ValidationContext vCxt, Shape shape,
                    QualifiedValueShape qualifiedValueShape, Node focusNode, Path path, Set<Node> valueNodes,
                    int minCount, int count) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedQualifiedValueShapeConstraintTooMany(ValidationContext vCxt, Shape shape,
                    QualifiedValueShape qualifiedValueShape, Node focusNode, Path path, Set<Node> valueNodes,
                    int maxCount, int count) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedSparqlValidationAsk(ValidationContext vCxt, Shape shape,
                    Constraint reportConstraint, Node focusNode, Path path, Node valueNode) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedSparqlValidationSelect(ValidationContext vCxt, Shape shape,
                    Constraint reportConstraint, Node focusNode, Path rPath, Node value) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedUniqueLangConstraint(ValidationContext vCxt, Shape shape,
                    UniqueLangConstraint uniqueLangConstraint, Node focusNode, Path path, String duplicateTag) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedConstraintOpNodeShape(ValidationContext vCxt, Shape shape,
                    ConstraintOp constraintOp, Node focusNode) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    @Override public void onViolatedConstraintOpPropertyShape(ValidationContext vCxt, Shape shape,
                    ConstraintOp constraintOp, Node focusNode, Path path, Node pathNode) {
        modifyCurrentShapeEvaluation(e -> e.setValid(false));
    }

    public Set<ShaclEvaluation> getEvaluations() {
        return Collections.unmodifiableSet(this.finishedEvaluations);
    }
}
