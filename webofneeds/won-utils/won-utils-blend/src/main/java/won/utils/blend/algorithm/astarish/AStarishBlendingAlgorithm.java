package won.utils.blend.algorithm.astarish;

import won.utils.blend.algorithm.BlendingAlgorithm;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.support.BindingChecks;
import won.utils.blend.algorithm.support.BlendingUtils;
import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;
import won.utils.blend.support.bindings.TemplateBindings;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.graph.TemplateGraphs;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static won.utils.blend.algorithm.support.BindingChecks.areBindingsAcceptedByBlendingOptions;

public class AStarishBlendingAlgorithm implements BlendingAlgorithm {
    @Override
    public Stream<TemplateGraphs> blend(final BlendingInstance instance) {
        return new AStarExecution(instance).evaluate();
    }

    private class AStarExecution {
        private final Queue<SearchNode> open = new PriorityQueue<>();
        private final Set<SearchNode> closed = new HashSet<>();
        private final Set<SearchNode> results = new HashSet<>();
        private final CompactBindingsManager bindingsManager;
        private final BlendingInstance instance;
        private int maxScore;

        private AStarExecution(BlendingInstance instance) {
            this.instance = instance;
            this.bindingsManager = new CompactBindingsManager(BlendingUtils.findAdmissibleVariableBindings(instance));
        }

        private Stream<TemplateGraphs> evaluate() {
            open.add(new SearchNode(0, bindingsManager.getZeroBindings(), -1));
            while (!open.isEmpty()) {
                SearchNode toExpand = open.poll();
                Set<VariableBinding> bindings = bindingsManager.toBindings(toExpand.bindings);
                TemplateBindings templateBindings = new TemplateBindings(instance.leftTemplate, instance.rightTemplate,
                                bindings);
                if (isBindingsRejected(templateBindings)) {
                    closed.add(toExpand);
                    continue;
                }
                TemplateGraphs blendingResult = instance.blendingOperations.blendWithGivenBindings(
                                instance.leftTemplate.getTemplateGraphs(),
                                instance.rightTemplate.getTemplateGraphs(),
                                templateBindings);
                long numEntries = instance.blendingResultEvaluator
                                .getNumberOfReportEntriesForBoundVariables(blendingResult, templateBindings);
                int score = bindings.size();
                boolean nodeIsAcceptable = numEntries == 0;
                if (nodeIsAcceptable) {
                    if (score > maxScore) {
                        maxScore = score;
                        removeSuboptimalResults();
                    }
                    if (score == maxScore) {
                        results.add(toExpand);
                    }
                }
                closed.add(toExpand);
                if (shouldExpand(toExpand, nodeIsAcceptable)) {
                    expand(toExpand, score);
                }
                pruneOpenNodes();
            }
            removeSuboptimalResults();
            return results.stream().map(n -> recreateResult(instance, n).get());
        }

        private void removeSuboptimalResults() {
            results.removeIf(n -> countBindings(n.bindings) < maxScore);
        }

        private void pruneOpenNodes() {
            open.removeIf(n -> n.score < maxScore);
        }

        private boolean shouldExpand(SearchNode toExpand, boolean nodeIsAcceptable) {
            return nodeIsAcceptable || isInitialNode(toExpand);
        }

        private boolean isInitialNode(SearchNode toExpand) {
            return toExpand.varIndex == -1;
        }

        private boolean isBindingsRejected(TemplateBindings templateBindings) {
            if (BindingChecks.doBindingsContainLongVariableChain(templateBindings)) {
                return true;
            }
            if (!areBindingsAcceptedByBlendingOptions(templateBindings, instance.blendingOptions)) {
                return true;
            }
            return false;
        }

        private Optional<TemplateGraphs> recreateResult(BlendingInstance instance, SearchNode node) {
            Set<VariableBinding> bindings = bindingsManager.toBindings(node.bindings);
            TemplateBindings templateBindings = new TemplateBindings(instance.leftTemplate, instance.rightTemplate,
                            bindings);
            if (isBindingsRejected(templateBindings)) {
                return Optional.empty();
            }
            return Optional.ofNullable(instance.blendingOperations.blendWithGivenBindings(
                            instance.leftTemplate.getTemplateGraphs(),
                            instance.rightTemplate.getTemplateGraphs(),
                            templateBindings));
        }

        private boolean expand(SearchNode toExpand, int score) {
            int nextVarIndex = toExpand.varIndex + 1;
            if (!bindingsManager.isValidVariableIndex(nextVarIndex)) {
                return false;
            }
            boolean nodesOpened = false;
            for (int[] newBindings : bindingsManager
                            .allBindingsOfVariableIncludingUnbound(toExpand.bindings, nextVarIndex)) {
                int nextScore = countBindings(newBindings) + bindingsManager.getNumberOfVariables() - nextVarIndex - 1;
                if (nextScore < maxScore) {
                    continue;
                }
                SearchNode newSearchNode = new SearchNode(nextScore, newBindings, nextVarIndex);
                if (closed.contains(newSearchNode)) {
                    continue;
                }
                AtomicBoolean foundIt = new AtomicBoolean(false);
                if (open.removeIf(n -> {
                    if (n.equals(newSearchNode)) {
                        foundIt.set(true);
                        return newSearchNode.score < n.score;
                    }
                    return false;
                })) {
                    nodesOpened = true;
                    open.add(newSearchNode);
                } else if (!foundIt.get()) {
                    nodesOpened = true;
                    open.add(newSearchNode);
                }
            }
            return nodesOpened;
        }

        private int countBindings(int[] bindings) {
            int cnt = 0;
            for (int i = 0; i < bindings.length; i++) {
                if (bindings[i] > 0) {
                    cnt++;
                }
            }
            return cnt;
        }
    }
}
