package won.utils.blend.algorithm.sat.shacl2.astarish;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.BlendingAlgorithm;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.ShaclValidator;
import won.utils.blend.algorithm.sat.shacl2.astarish.expand.JoinAndFallbackToExploreStrategy;
import won.utils.blend.algorithm.sat.shacl2.forbiddenbindings.IntListForbiddenBindings;
import won.utils.blend.algorithm.support.BlendingUtils;
import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;
import won.utils.blend.algorithm.support.bindings.CompactVariableBindings;
import won.utils.blend.algorithm.support.bindings.CompactVariables;
import won.utils.blend.algorithm.support.instance.BlendingInstanceLogic;
import won.utils.blend.algorithm.support.instance.OverviewBlendingInstanceFormatter;
import won.utils.blend.support.bindings.TemplateBindings;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.graph.TemplateGraphs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AStarishBlendingAlgorithm implements BlendingAlgorithm {
    private ExpansionStrategy expansionStrategy;
    private SearchInitializer searchInitializer;

    public AStarishBlendingAlgorithm(ExpansionStrategy expansionStrategy) {
        this.expansionStrategy = expansionStrategy;
    }

    public AStarishBlendingAlgorithm() {
        this.expansionStrategy = new JoinAndFallbackToExploreStrategy();
        this.searchInitializer = new StandardSearchInitializer();
    }

    @Override
    public Stream<TemplateGraphs> blend(final BlendingInstance instance) {
        return evaluate(instance);
    }

    Stream<TemplateGraphs> evaluate(BlendingInstance instance) {
        AlgorithmState state = initializeAlgorithmState(instance);
        state.debugWriter.incIndent();
        state.log.info(() -> new OverviewBlendingInstanceFormatter().format(instance));
        state.log.info(() -> "generating initial search states from empty bindings");
        addInitialSearchNodes(instance, state);
        state.log.info(() -> "generated " + state.open.size() + " search nodes, starting search");
        while (!state.open.isEmpty()) {
            state.debugWriter.incIndent();
            SearchNode node = null;
            {
                SearchNode nextNode = state.open.poll();
                state.log.info(() -> "inspecting node:");
                state.debugWriter.incIndent();
                state.log.info(() -> new VerbosityAwareSearchNodeFormatter(state).format(nextNode,
                                state.bindingsManager));
                state.debugWriter.decIndent();
                node = update(state, nextNode);
                if (node == null) {
                    close(state, nextNode);
                    state.debugWriter.decIndent();
                    continue;
                }
            }
            if (node.valid.isUnknown() && node.encounteredVariables.isEmpty()) {
                state.log.info(() -> "unknown validity, node might lead to acceptable result - must check");
                node = checkValidity(instance, state, node);
            }
            if (node.valid.isTrue()) {
                node = checkGlobalValidity(instance, state, node);
            }
            if (node.globallyValid.isTrue()) {
                state.log.info(() -> "node is an acceptable result");
                acceptResult(state, node);
            } else if (node.valid.isUnkown() && node.encounteredVariables.size() > 0) {
                state.log.info(() -> "additional variables encountered, node might lead to acceptable result, expanding");
                expand(instance, state, node);
            } else if (node.valid.isTrue() && !node.bindings.isEmpty()) {
                state.log.info(() -> "valid node, no additional variables encountered - adding to intermediate results");
                saveIntermediateResult(state, node);
            } else {
                close(state, node);
            }
            state.debugWriter.decIndent();
        }
        Set<VariableBindings> resultingBindings =
                        state.results
                                        .stream()
                                        .map(r -> r.bindings.getVariableBindings())
                                        .collect(Collectors.toSet());
        return generateResults(instance, resultingBindings);
    }

    private SearchNode checkValidity(BlendingInstance instance, AlgorithmState state, SearchNode node) {
        return ShaclValidator.validateSearchNode(instance, state, node);
    }

    private SearchNode checkGlobalValidity(BlendingInstance instance, AlgorithmState state, SearchNode node) {
        return ShaclValidator.validateSearchNodeGlobally(instance, state, node);
    }

    private SearchNode update(AlgorithmState state, SearchNode node) {
        CompactVariableBindings bindings = node.bindings;
        CompactVariables variables = node.encounteredVariables;
        if (!state.bindingsManager.isAdmissibleVariableBindings(bindings)) {
            state.log.trace(() -> "cannot update search node: bindings no longer admissible");
            return null;
        }
        if (!state.bindingsManager.isAdmissibleVariables(variables)) {
            state.log.trace(() -> "cannot update search node: variables no longer admissible");
            return null;
        }
        bindings = state.bindingsManager.withThisManager(bindings);
        variables = state.bindingsManager.withThisManager(variables);
        SearchNode updated = new SearchNode(node.shapes, node.focusNodes, bindings, variables, node.valid,
                        node.globallyValid);
        return updated;
    }

    private void saveIntermediateResult(AlgorithmState state, SearchNode node) {
        for (Node boundNode : node.bindings.getVariableBindings().getDecidedVariables()){
            state.searchNodesByBoundNode.compute(boundNode, (n, list) -> {
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(node);
                return list;
            });
        }
    }


    private void acceptResult(AlgorithmState state, SearchNode node) {
        state.log.info(() -> "Result accepted ");
        state.debugWriter.incIndent();
        state.log.info(() -> new VerbosityAwareSearchNodeFormatter(state).format(node, state.bindingsManager));
        state.debugWriter.decIndent();
        state.results.add(node);
    }

    private void addInitialSearchNodes(BlendingInstance instance, AlgorithmState state) {
        Set<SearchNode> initialSearchNodes = searchInitializer.initializeSearch(instance, state);
        collectShapesImpliedByVariables(state, initialSearchNodes);
        state.open.addAll(initialSearchNodes);
    }

    private void collectShapesImpliedByVariables(AlgorithmState state, Set<SearchNode> initialSearchNodes) {
        for (SearchNode node : initialSearchNodes) {
            if (!node.encounteredVariables.isEmpty()) {
                for (Shape shape : node.shapes) {
                    for (Node variable : node.encounteredVariables.getVariables()) {
                        state.shapesImpliedByVariables.compute(variable, (n, shapeSet) -> {
                            if (shapeSet == null) {
                                shapeSet = new HashSet<>();
                            }
                            shapeSet.add(shape);
                            return shapeSet;
                        });
                    }
                }
            }
        }
    }

    private AlgorithmState initializeAlgorithmState(BlendingInstance instance) {
        // CompactBindingsManager bindingsManager = new
        // CompactBindingsManager(BlendingUtils.findAdmissibleVariableBindings(
        // instance));
        CompactBindingsManager bindingsManager = new CompactBindingsManager(
                        BlendingUtils.getAllPossibleBindings(instance));
        AlgorithmState state = new AlgorithmState(bindingsManager, new BlendingInstanceLogic(instance).getShapes(),
                        Verbosity.MAXIMUM);
        return state;
    }

    private Stream<TemplateGraphs> generateResults(BlendingInstance instance, Set<VariableBindings> bindingsSet) {
        return bindingsSet.stream().map(bs -> recreateResult(instance, bs).get());
    }

    private Optional<TemplateGraphs> recreateResult(BlendingInstance instance, VariableBindings bindings) {
        TemplateBindings templateBindings = new TemplateBindings(instance.leftTemplate, instance.rightTemplate,
                        bindings.getBindingsAsSet());
        return Optional.ofNullable(instance.blendingOperations.blendWithGivenBindings(
                        instance.leftTemplate.getTemplateGraphs(),
                        instance.rightTemplate.getTemplateGraphs(),
                        templateBindings));
    }

    private void close(AlgorithmState state, SearchNode node) {
        state.log.trace(() -> "closing search node");
        state.closed.add(node);
    }

    private boolean expand(BlendingInstance instance, AlgorithmState state, SearchNode node) {
        boolean nodesOpened = false;
        Set<SearchNode> expandedSet = expansionStrategy.findSuccessors(instance, state, node);
        state.log.info(() -> String.format("expanded node into %d nodes", expandedSet.size()));
        for (SearchNode expanded : expandedSet) {
            if (state.closed.stream().anyMatch(c -> c.equalsIgnoreValidity(expanded))) {
                state.log.info(() -> "Node omitted - already closed identical node");
                continue;
            }
            if (((IntListForbiddenBindings) state.forbiddenBindings).isForbiddenBindings(
                            expanded.bindings.indexList())) {
                state.log.info(() -> "Node omitted - bindings are forbidden");
                continue;
            }
            AtomicBoolean foundIt = new AtomicBoolean(false);
            if (state.open.removeIf(n -> {
                if (n.equals(expanded)) {
                    foundIt.set(true);
                    return expanded.encounteredVariables.size() < n.encounteredVariables.size();
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
        state.log.info(() -> String.format("open list contains %d nodes after expansion", state.open.size()));
        return nodesOpened;
    }
}
