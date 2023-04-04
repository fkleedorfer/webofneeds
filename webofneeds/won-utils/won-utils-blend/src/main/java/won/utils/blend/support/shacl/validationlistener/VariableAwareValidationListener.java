package won.utils.blend.support.shacl.validationlistener;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.validation.HandlerBasedValidationListener;
import org.apache.jena.shacl.validation.ValidationListener;
import org.apache.jena.shacl.validation.event.*;
import won.utils.blend.support.graph.VariableAwareGraph;

import java.util.*;
import java.util.function.Function;

import static org.apache.jena.shacl.validation.event.EventPredicates.isOfType;

public class VariableAwareValidationListener implements ValidationListener {
    private HandlerBasedValidationListener delegate;
    private final VariableAwareGraph variableAwareGraph;
    private final ArrayDeque<ShaclEvaluation> evaluationStack = new ArrayDeque<>();
    private final Set<ShaclEvaluation> finishedEvaluations = new HashSet<>();

    @Override public void onValidationEvent(ValidationEvent e) {
        delegate.onValidationEvent(e);
    }

    public VariableAwareValidationListener(VariableAwareGraph variableAwareGraph) {
        this.variableAwareGraph = variableAwareGraph;
        this.delegate =
                        HandlerBasedValidationListener
                                        .builder()
                                        .forEventType(FocusNodeValidationStartedEvent.class)
                                        .addHandler(c -> c
                                            .iff(EventPredicates.isOfType(FocusNodeValidationStartedEvent.class))
                                            .handle(e -> {
                                                variableAwareGraph.resetEncounteredVariables();
                                                ShapeEvaluation eval = new ShapeEvaluation(e.getShape(), e.getFocusNode());
                                                evaluationStack.push(eval);
                                            })
                                        )
                                        .forEventType(ValueNodesDeterminedForPropertyShapeEvent.class)
                                        .addSimpleHandler(e -> {
                                            modifyCurrentShapeEvaluation(eval -> eval.addEncounteredVariables(
                                                            variableAwareGraph.getEncounteredVariables()));
                                            variableAwareGraph.resetEncounteredVariables();
                                        })
                                        .forEventType(FocusNodeValidationFinishedEvent.class)
                                        .addSimpleHandler(e -> {
                                            ShaclEvaluation evalShape = evaluationStack.pop();
                                            if (evaluationStack.isEmpty()) {
                                                finishedEvaluations.add(evalShape);
                                            } else {
                                                ShaclEvaluation evalParent = evaluationStack.pop();
                                                evalParent = ((ShapeEvaluation) evalParent).addSubEvaluation(evalShape);
                                                evalParent = evalParent.setValid(
                                                                evalParent.isValid().orElse(true) && evalShape.isValid()
                                                                                .get());
                                                evaluationStack.push(evalParent);
                                            }
                                        })
                                        .forEventType(ConstraintEvaluationForNodeShapeStartedEvent.class)
                                        .addSimpleHandler(e -> {
                                            ConstraintEvaluation constraintEvals = new ConstraintEvaluation(
                                                            e.getShape(),
                                                            e.getConstraint(), e.getFocusNode(),
                                                            Set.of());
                                            evaluationStack.push(constraintEvals);
                                        })
                                        .forEventTypes(ConstraintEvaluationForNodeShapeFinishedEvent.class,
                                                        ConstraintEvaluationForPropertyShapeFinishedEvent.class)
                                        .addSimpleHandler(e -> {
                                            ShaclEvaluation constraintEvals = evaluationStack.pop();
                                            if (constraintEvals.getSubEvaluations().size() == 1) {
                                                // if there was only one constraint evaluation, pull it up
                                                ShaclEvaluation sub = constraintEvals.getSubEvaluations().stream()
                                                                .findFirst().get();
                                                sub = sub.addEncounteredVariables(
                                                                constraintEvals.getEncounteredVariables());
                                                constraintEvals = sub;
                                            }
                                            if (constraintEvals.isValid().isEmpty()) {
                                                constraintEvals = constraintEvals.setValid(true);
                                            }
                                            ShaclEvaluation parent = evaluationStack.pop();
                                            parent = parent.setValid(
                                                            parent.isValid().orElse(true) && constraintEvals.isValid()
                                                                            .get());
                                            parent = parent.addSubEvaluation(constraintEvals);
                                            evaluationStack.push(parent);
                                        })
                                        .forEventType(ConstraintEvaluationForPropertyShapeStartedEvent.class)
                                        .addSimpleHandler(e -> {
                                            ConstraintEvaluation constraintEvals = new ConstraintEvaluation(
                                                            e.getShape(),
                                                            e.getConstraint(), e.getFocusNode(),
                                                            e.getValueNodes());
                                            evaluationStack.push(constraintEvals);
                                        })
                                        .forEventType(ConstraintEvaluatedEvent.class)
                                        .addSimpleHandler(e -> {
                                            Set<Node> encountered = getEncounteredVariablesAndReset();
                                            addToParent(new ConstraintEvaluation(e.getShape(), e.getConstraint(),
                                                            e.getFocusNode(),
                                                            e.getValueNodes(), e.isValid(), encountered,
                                                            Set.of()));
                                        })
                                        .build();
    }

    private void modifyCurrentShapeEvaluation(Function<ShaclEvaluation, ShaclEvaluation> modifier) {
        ShaclEvaluation curEval = evaluationStack.pop();
        Objects.requireNonNull(curEval);
        ShaclEvaluation result = modifier.apply(curEval);
        Objects.requireNonNull(result);
        evaluationStack.push(result);
    }

    private void addToParent(ShaclEvaluation evaluation) {
        ShaclEvaluation evalParent = evaluationStack.pop();
        evalParent = evalParent.addSubEvaluation(evaluation);
        evalParent = evalParent.setValid(evalParent.isValid().orElse(true) && evaluation.isValid().get());
        evaluationStack.push(evalParent);
    }

    private Set<Node> getEncounteredVariablesAndReset() {
        Set<Node> encountered = variableAwareGraph.getEncounteredVariables();
        variableAwareGraph.resetEncounteredVariables();
        return encountered;
    }

    public Set<ShaclEvaluation> getEvaluations() {
        return Collections.unmodifiableSet(this.finishedEvaluations);
    }
}
