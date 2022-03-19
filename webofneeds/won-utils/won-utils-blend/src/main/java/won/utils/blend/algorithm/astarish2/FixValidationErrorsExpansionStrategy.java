package won.utils.blend.algorithm.astarish2;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.engine.constraint.ClassConstraint;
import org.apache.jena.shacl.engine.constraint.DatatypeConstraint;
import org.apache.jena.shacl.engine.constraint.HasValueConstraint;
import org.apache.jena.shacl.engine.constraint.NodeKindConstraint;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.sparql.path.*;
import org.apache.jena.sparql.path.eval.PathEval;
import org.apache.jena.sparql.util.Context;
import won.utils.blend.BLEND;
import won.utils.blend.algorithm.BlendingInstance;

import java.util.*;

public class FixValidationErrorsExpansionStrategy extends BaseExpansionStrategy {
    @Override
    public Set<SearchNode2> findSuccessors(BlendingInstance instance, AlgorithmState state,
                    SearchNodeInspection inspection) {
        if (blendingResultConforms(instance, state, inspection)) {
            return Collections.emptySet();
        }
        int forbiddenCount = state.forbiddenCombinations.size();
        Set<SearchNode2> successors = new HashSet<>();
        for (ReportEntry reportEntry : inspection.validationReport.getEntries()) {
            int[] newForbiddenCombination = getForbiddenCombinationFromReportEntry(reportEntry, instance, state,
                            inspection);
            if (newForbiddenCombination != null) {
                state.forbiddenCombinations.add(newForbiddenCombination);
            }
            Set<SearchNode2> newNodes = generateSuccessorsFromReportEntry(reportEntry, instance, state, inspection);
            if (newNodes.isEmpty()) {
            }
            successors.addAll(newNodes);
        }
        return successors;
    }

    private int[] getForbiddenCombinationFromReportEntry(ReportEntry reportEntry, BlendingInstance instance,
                    AlgorithmState state, SearchNodeInspection inspection) {
        if (inspection.templateBindings.isVariable(reportEntry.focusNode()) && isBoundToPath(
                        reportEntry.resultPath())) {
            Constraint constraint = reportEntry.constraint();
            if (constraint instanceof NodeKindConstraint ||
                            constraint instanceof DatatypeConstraint ||
                            constraint instanceof ClassConstraint) {
                Node actualValue = evaluatePath(inspection.blendingResult.getDataGraph(), reportEntry.focusNode(),
                                reportEntry.resultPath());
                return state.bindingsManager
                                .combinationBuilder()
                                .set(reportEntry.focusNode(), actualValue)
                                .build();
            }
        }
        return null;
    }

    private boolean isBoundToPath(Path path) {
        return path instanceof P_Link && ((P_Link) path).getNode().equals(BLEND.boundTo);
    }

    private Set<SearchNode2> generateSuccessorsFromReportEntry(ReportEntry reportEntry, BlendingInstance instance,
                    AlgorithmState state,
                    SearchNodeInspection inspection) {
        Set<SearchNode2> results = new HashSet<>();
        if (inspection.templateBindings.isVariable(reportEntry.focusNode())) {
            if (reportEntry.constraint() instanceof HasValueConstraint) {
                generateSuccessorFromExpectedTriples(reportEntry, instance, state, inspection)
                                .ifPresent(results::add);
            }
        }
        return results;
    }

    private Optional<SearchNode2> generateSuccessorFromExpectedTriples(ReportEntry reportEntry,
                    BlendingInstance instance,
                    AlgorithmState state, SearchNodeInspection inspection) {
        Node focusVar = reportEntry.focusNode();
        Path lastPathElement = getLastPathElement(reportEntry.resultPath());
        if (lastPathElement instanceof P_Inverse) {
            if (((P_Inverse) lastPathElement).getSubPath() instanceof P_Link) {
                P_Link link = (P_Link) ((P_Inverse) lastPathElement).getSubPath();
                if (link.getNode().equals(BLEND.boundTo)) {
                    Node expectedtargetVariable = ((HasValueConstraint) reportEntry.constraint()).getValue();
                    if (!inspection.templateBindings.isVariable(expectedtargetVariable)) {
                        return Optional.empty();
                    }
                    Path focusVarBoundToPath = PathFactory.pathLink(BLEND.boundTo);
                    Node focusVarValue = evaluatePath(inspection.blendingResult.getDataGraph(), focusVar,
                                    focusVarBoundToPath);
                    Path pathToTargetValue = getPathWithoutLastElement(reportEntry.resultPath());
                    Node targetVarValue = evaluatePath(inspection.blendingResult.getDataGraph(), focusVar,
                                    pathToTargetValue);
                    Node actualTargetVariable = evaluatePath(inspection.blendingResult.getDataGraph(), focusVar,
                                    reportEntry.resultPath());
                    SearchNode2 expandedNode = inspection.node;
                    if (targetVarValue == null) {
                        // the focus var does not have the required path at all, it can't be a legal
                        // value
                        int[] forbiddenCombination = state.bindingsManager.combinationBuilder()
                                        .set(focusVar, focusVarValue)
                                        .build();
                        state.forbiddenCombinations.add(forbiddenCombination);
                    }
                    if (actualTargetVariable != null) {
                        // the focus var has the required path, but the wrong variable is bound to it
                        int[] forbiddenCombination = state.bindingsManager.combinationBuilder()
                                        .set(focusVar, focusVarValue)
                                        .set(actualTargetVariable, targetVarValue)
                                        .build();
                        state.forbiddenCombinations.add(forbiddenCombination);
                    }
                    if (state.bindingsManager.isAdmissibleBinding(expectedtargetVariable, targetVarValue)) {
                        expandedNode = bindVariable(expandedNode, state, inspection, expectedtargetVariable,
                                        targetVarValue);
                    }
                    if (!expandedNode.equals(inspection.node)) {
                        return Optional.of(expandedNode);
                    }
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private Node evaluatePath(Graph dataGraph, Node focusNode, Path pathToExpectedValue) {
        Iterator<Node> it = PathEval.eval(dataGraph, focusNode, pathToExpectedValue, Context.emptyContext);
        if (!it.hasNext()) {
            return null;
        }
        return it.next();
    }

    private Path getPathWithoutLastElement(Path pathToCopy) {
        if (!(pathToCopy instanceof P_Seq)) {
            return null;
        }
        P_Seq seqPath = (P_Seq) pathToCopy;
        if (seqPath.getRight() instanceof P_Seq) {
            return new P_Seq(seqPath.getLeft(), getPathWithoutLastElement(seqPath.getRight()));
        } else {
            return seqPath.getLeft();
        }
    }

    private Path getLastPathElement(Path path) {
        if (path instanceof P_Seq) {
            return getLastPathElement(((P_Seq) path).getRight());
        }
        return path;
    }

    private SearchNode2 bindVariable(SearchNode2 node, AlgorithmState state,
                    SearchNodeInspection inspection, Node expectedVariable, Node expectedValue) {
        int[] newBindings = state.bindingsManager.copyAndSetBinding(node.bindings, expectedVariable, expectedValue);
        BitSet newDecided = (BitSet) node.decided.clone();
        newDecided.set(state.bindingsManager.getIndexOfVariable(expectedVariable));
        float score = estimateScore(inspection, state.bindingsManager, newBindings, newDecided);
        int problems = inspection.validationReport.getEntries().size() - 1;
        SearchNode2 result = new SearchNode2(score, newBindings, newDecided, node.problems - 1, inspection.graphSize);
        return result;
    }

    private SearchNode2 unbindVariable(SearchNode2 node, AlgorithmState state,
                    SearchNodeInspection inspection, Node variable) {
        int[] newBindings = state.bindingsManager.copyAndRemoveBinding(node.bindings, variable);
        BitSet newDecided = (BitSet) node.decided.clone();
        newDecided.clear(state.bindingsManager.getIndexOfVariable(variable));
        float score = estimateScore(inspection, state.bindingsManager, newBindings, newDecided);
        int problems = inspection.validationReport.getEntries().size() - 1;
        SearchNode2 result = new SearchNode2(score, newBindings, newDecided, node.problems - 1, inspection.graphSize);
        return result;
    }

    public boolean blendingResultConformsIgnoringUnboundVariables(BlendingInstance instance, AlgorithmState state,
                    SearchNodeInspection inspection) {
        return instance.blendingResultEvaluator
                        .getNumberOfBoundVariablesWithEntries(inspection.validationReport,
                                        inspection.templateBindings) == 0;
    }

    public boolean blendingResultConforms(BlendingInstance instance, AlgorithmState state,
                    SearchNodeInspection inspection) {
        return inspection.validationReport.conforms();
    }
}
