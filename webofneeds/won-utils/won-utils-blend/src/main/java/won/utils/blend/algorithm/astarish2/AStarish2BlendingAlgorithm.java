package won.utils.blend.algorithm.astarish2;

import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import won.utils.blend.BLEND;
import won.utils.blend.algorithm.BlendingAlgorithm;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.support.BindingChecks;
import won.utils.blend.algorithm.support.BlendingUtils;
import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;
import won.utils.blend.support.bindings.TemplateBindings;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.graph.TemplateGraphs;

import java.util.BitSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static won.utils.blend.algorithm.support.BindingChecks.areBindingsAcceptedByBlendingOptions;

public class AStarish2BlendingAlgorithm implements BlendingAlgorithm {
    private ExpansionStrategy expansionStrategy;

    public AStarish2BlendingAlgorithm(ExpansionStrategy expansionStrategy) {
        this.expansionStrategy = expansionStrategy;
    }

    public AStarish2BlendingAlgorithm() {
        this.expansionStrategy = new StandardExpansionStrategy();
        // new AllValuesOfNextVariableExpansionStrategy();
        // new FixValidationErrorsExpansionStrategy();
    }

    @Override
    public Stream<TemplateGraphs> blend(final BlendingInstance instance) {
        return evaluate(instance);
    }

    Stream<TemplateGraphs> evaluate(BlendingInstance instance) {
        AlgorithmState state = initializeAlgorithmState(instance);
        addInitialSearchNode(state);
        while (!state.open.isEmpty()) {
            SearchNodeInspection inspection = initializeSearchNodeInspection(state);
            makeTemplateBindings(instance, state, inspection);
            if (ignoredForIllegalBindings(instance, state, inspection)) {
                continue;
            }
            blend(instance, inspection);
            evaluateResult(instance, state, inspection);
            calculateGraphSize(inspection);
            calculateScore(state, inspection);
            decideAcceptability(instance, state, inspection);
            acceptAndPruneResults(instance, state, inspection);
            close(state, inspection);
            expand(instance, state, inspection);
            pruneOpenNodes(state);
        }
        removeSuboptimalResults(state);
        return generateResults(instance, state);
    }

    private SearchNodeInspection initializeSearchNodeInspection(
                    AlgorithmState state) {
        SearchNodeInspection inspection = new SearchNodeInspection(state.open.poll());
        state.nodesInspected++;
        return inspection;
    }

    private void addInitialSearchNode(AlgorithmState state) {
        state.open.add(new SearchNode2(0, state.bindingsManager.getZeroBindings(),
                        new BitSet(state.bindingsManager.getNumberOfVariables()), 0, Integer.MAX_VALUE));
    }

    private AlgorithmState initializeAlgorithmState(BlendingInstance instance) {
        // CompactBindingsManager bindingsManager = new
        // CompactBindingsManager(BlendingUtils.findAdmissibleVariableBindings(
        // instance));
        CompactBindingsManager bindingsManager = new CompactBindingsManager(
                        BlendingUtils.findAdmissibleVariableBindings2(
                                        instance));
        AlgorithmState state = new AlgorithmState(bindingsManager);
        return state;
    }

    private Stream<TemplateGraphs> generateResults(BlendingInstance instance, AlgorithmState state) {
        return state.results.stream().map(n -> recreateResult(instance, state, n).get());
    }

    private boolean ignoredForIllegalBindings(BlendingInstance instance, AlgorithmState state,
                    SearchNodeInspection inspection) {
        if (isBindingsRejected(instance, inspection.templateBindings)) {
            state.closed.add(inspection.node);
            return true;
        }
        return false;
    }

    private void close(AlgorithmState state, SearchNodeInspection inspection) {
        state.closed.add(inspection.node);
    }

    private void acceptAndPruneResults(BlendingInstance instance, AlgorithmState state,
                    SearchNodeInspection inspection) {
        if (inspection.acceptable) {
            if (inspection.score > state.maxScore) {
                state.maxScore = inspection.score;
                state.minGraphSize = inspection.graphSize;
                removeSuboptimalResults(state);
            } else if (inspection.score == state.maxScore && inspection.graphSize < state.minGraphSize) {
                state.minGraphSize = inspection.graphSize;
                removeSuboptimalResults(state);
            }
            if (inspection.score == state.maxScore && inspection.graphSize == state.minGraphSize) {
                state.results.add(toAcceptedNode(instance, inspection));
            }
        }
    }

    private SearchNode2 toAcceptedNode(BlendingInstance instance, SearchNodeInspection inspection) {
        return new SearchNode2(
                        inspection.score,
                        inspection.node.bindings,
                        inspection.node.decided,
                        instance.blendingResultEvaluator.getNumberOfBoundVariablesWithEntries(
                                        inspection.validationReport, inspection.templateBindings),
                        inspection.graphSize);
    }

    private void decideAcceptability(BlendingInstance instance, AlgorithmState state, SearchNodeInspection inspection) {
        if (inspection.node.isInitialNode()) {
            inspection.acceptable = false;
        } else {
            int varsWithProblems = instance.blendingResultEvaluator
                            .getNumberOfBoundVariablesWithEntries(inspection.validationReport,
                                            inspection.templateBindings);
            inspection.acceptable = varsWithProblems == 0;
        }
    }

    private void calculateScore(AlgorithmState data, SearchNodeInspection inspection) {
        int boundToConstants = (int) inspection.bindings.stream()
                        .filter(b -> inspection.templateBindings.isConstant(b.getBoundNode())).count();
        int boundToVariables = inspection.bindings.size() - boundToConstants;
        inspection.score = -0.5f * boundToVariables + boundToConstants;
    }

    private void calculateGraphSize(SearchNodeInspection inspection) {
        ExtendedIterator<Triple> it = inspection.blendingResult.getDataGraph().find();
        int size = 0;
        while (it.hasNext()) {
            if (!it.next().predicateMatches(BLEND.boundTo)) {
                size++;
            }
        }
        inspection.graphSize = size;
    }

    private void evaluateResult(BlendingInstance instance, AlgorithmState state, SearchNodeInspection inspection) {
        inspection.validationReport = instance.blendingResultEvaluator
                        .validateWithBackground(inspection.blendingResult);
    }

    private void blend(BlendingInstance instance, SearchNodeInspection inspection) {
        inspection.blendingResult = instance.blendingOperations.blendWithGivenBindings(
                        instance.leftTemplate.getTemplateGraphs(),
                        instance.rightTemplate.getTemplateGraphs(),
                        inspection.templateBindings);
    }

    private void makeTemplateBindings(BlendingInstance instance, AlgorithmState state,
                    SearchNodeInspection inspection) {
        inspection.bindings = state.bindingsManager.toBindings(inspection.node.bindings);
        inspection.templateBindings = new TemplateBindings(instance.leftTemplate, instance.rightTemplate,
                        inspection.bindings);
    }

    private void removeSuboptimalResults(AlgorithmState state) {
        state.results.removeIf(n -> n.score < state.maxScore || n.graphSize > state.minGraphSize);
    }

    private void pruneOpenNodes(AlgorithmState data) {
        int openSize = data.open.size();
        if (data.open.removeIf(n -> n.score < data.maxScore || n.containsAnyCombination(data.forbiddenCombinations))) {
            data.nodesPruned += openSize - data.open.size();
        }
    }

    private boolean isBindingsRejected(BlendingInstance instance, TemplateBindings templateBindings) {
        if (BindingChecks.doBindingsContainLongVariableChain(templateBindings)) {
            return true;
        }
        if (!areBindingsAcceptedByBlendingOptions(templateBindings, instance.blendingOptions)) {
            return true;
        }
        return false;
    }

    private Optional<TemplateGraphs> recreateResult(BlendingInstance instance, AlgorithmState state, SearchNode2 node) {
        Set<VariableBinding> bindings = state.bindingsManager.toBindings(node.bindings);
        TemplateBindings templateBindings = new TemplateBindings(instance.leftTemplate, instance.rightTemplate,
                        bindings);
        if (isBindingsRejected(instance, templateBindings)) {
            return Optional.empty();
        }
        return Optional.ofNullable(instance.blendingOperations.blendWithGivenBindings(
                        instance.leftTemplate.getTemplateGraphs(),
                        instance.rightTemplate.getTemplateGraphs(),
                        templateBindings));
    }

    private boolean expand(BlendingInstance instance, AlgorithmState state, SearchNodeInspection inspection) {
        boolean nodesOpened = false;
        for (SearchNode2 expanded : expansionStrategy.findSuccessors(instance, state, inspection)) {
            if (state.closed.contains(expanded)) {
                continue;
            }
            AtomicBoolean foundIt = new AtomicBoolean(false);
            if (state.open.removeIf(n -> {
                if (n.equals(expanded)) {
                    foundIt.set(true);
                    return expanded.score < n.score;
                }
                return false;
            })) {
                nodesOpened = true;
                state.open.add(expanded);
            } else if (!foundIt.get()) {
                nodesOpened = true;
                state.open.add(expanded);
            }
        }
        return nodesOpened;
    }
}
