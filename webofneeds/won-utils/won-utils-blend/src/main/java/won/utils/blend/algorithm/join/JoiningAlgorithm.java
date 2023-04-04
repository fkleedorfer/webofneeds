package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.VLib;
import won.utils.blend.algorithm.BlendingAlgorithm;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.support.Ternary;
import won.utils.blend.algorithm.support.BlendingUtils;
import won.utils.blend.algorithm.support.instance.BlendingInstanceLogic;
import won.utils.blend.support.bindings.TemplateBindings;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.graph.BlendedGraphs;
import won.utils.blend.support.graph.TemplateGraphs;
import won.utils.blend.support.graph.VariableAwareGraph;
import won.utils.blend.support.graph.VariableAwareGraphImpl;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;
import static won.utils.blend.algorithm.join.AlgorithmStateLogic.*;
import static won.utils.blend.algorithm.join.SearchNodeLogic.recalculateDependentValues;

public class JoiningAlgorithm implements BlendingAlgorithm {
    @Override
    public Stream<TemplateGraphs> blend(BlendingInstance blendingInstance) {
        AlgorithmState state = initializeAlgorithmState(blendingInstance);
        state.log.debugFmt("state initialized, %d bindings possible in total", state.allPossibleBindings.size());
        initializeSearchNodes(blendingInstance, state);
        SearchNodeFormatter formatter = new SearchNodeFormatter(state);
        int iteration = 0;
        while (!state.openNodes.isEmpty() && !state.noSolution) {
            iteration++;
            logIteration(state, formatter, iteration);
            SearchNode currentNode = Collections.min(state.openNodes);
            inspectSearchNode(blendingInstance, state, formatter, currentNode);
            state.openNodes.remove(currentNode);
            state.closedNodes.add(currentNode);
        }
        Set<VariableBindings> resultingBindings = state.results
                        .stream()
                        .map(r -> r.bindings)
                        .collect(toSet());
        return generateResults(blendingInstance, state, resultingBindings);
    }

    private void logIteration(AlgorithmState state, SearchNodeFormatter formatter, int iteration) {
        if (state.log.isDebugEnabled()) {
            state.log.debugFmt("************** iteration %d **************", iteration);
            state.log.debug(() -> "unexplored variables : " + state.unexploredVariables);
            state.log.debug(() -> "search nodes         : " + state.openNodes.size());
            state.log.debug(() -> "results              : " + state.results.size());
            if (state.log.isTraceEnabled()) {
                state.log.traceFmt("all %d search nodes: ", state.openNodes.size());
                state.log.logIndented(() -> {
                    state.openNodes.stream().forEach(n -> state.log.trace(() -> formatter.format(n)));
                });
            }
        }
    }

    private void inspectSearchNode(BlendingInstance blendingInstance, AlgorithmState state,
                    SearchNodeFormatter formatter,
                    SearchNode currentNode) {
        state.log.debug(() -> "inspecting node \n" + formatter.format(currentNode));
        if (blendingInstance.blendingOptions.getUnboundHandlingMode().isUnboundAllowedIfNoOtherBinding()) {
            Set<Node> unbound = currentNode.bindings.getUnboundNonBlankVariables();
            if (!unbound.isEmpty()) {
                if (isAllUnboundVariablesBoundOrSubsetUnboundInResult(state, unbound)) {
                    state.log.debug(() -> "omitting node because the unboundHandlingMode only allows an unbound variable "
                                    + "in the result if there is no result in which that variable is bound, "
                                    + "and this node violates the condition");
                    return;
                }
            }
        }
        /*
         * if (currentNode.blendedGraphSize > 0) { if (state.smallestBlendedGraphSize <
         * currentNode.blendedGraphSize) { state.log.
         * debugFmt("omitting node because the size of its blended graph (%d) is bigger than the smallest one we've seen so far (%d)"
         * , currentNode.blendedGraphSize, state.smallestBlendedGraphSize); return; }
         * state.smallestBlendedGraphSize = currentNode.blendedGraphSize; }
         */
        Map<Boolean, Set<Node>> exploredUnexplored = currentNode.encounteredVariablesFlat.stream()
                        .collect(groupingBy(n -> isExploredVariable(
                                        state, n), toSet()));
        Set<Node> varsToJoinOn = exploredUnexplored.getOrDefault(true, Collections.emptySet());
        varsToJoinOn.removeAll(currentNode.exploring); // we're still exploring these vars, don't join on them!
        Set<Node> varsToExplore = new HashSet<>();
        varsToExplore.addAll(exploredUnexplored.getOrDefault(false, Collections.emptySet()));
        varsToExplore.addAll(currentNode.exploring);
        // we have to decide whether to explore or to join. we choose the smaller
        // portion as we can expect less complexity
        if (!varsToExplore.isEmpty()) {
            if (varsToJoinOn.isEmpty() || varsToJoinOn.size() > varsToExplore.size()) {
                // only unexplored vars left among encountered - explore
                state.log.logIndented(() -> explore(blendingInstance, state, currentNode, varsToExplore));
                return;
            }
        }
        if (!varsToJoinOn.isEmpty()) {
            if (!varsToExplore.isEmpty()) {
                // however: if we also have unexplored variables, we have to remember that we
                // need to explore them
                // in each joined node
                currentNode.exploring.addAll(varsToExplore);
            }
            boolean atLeastOneJoinResult = state.log
                            .logIndented(() -> join(blendingInstance, state, currentNode, varsToJoinOn));
            state.log.debug(() -> "node consumed by join");
            return;
        }
        state.log.logIndented(() -> outerJoin(blendingInstance, state, currentNode));
        if (currentNode.bindings.isAllVariablesDecided()) {
            if (state.results.contains(currentNode)) {
                state.log.debug(() -> "ignoring duplicate result");
                return;
            }
            if (blendingInstance.blendingOptions.getUnboundHandlingMode().isAllBound()) {
                if (!currentNode.bindings.isAllNonBlankVariablesBound()) {
                    state.log.debug(() -> "not accepting node as final result because not all variables are bound, which is required by binding options");
                    return;
                }
            }
            state.log.debug(() -> "all satisfied or all bound - evaluating globally");
            addResultIfValid(blendingInstance, state, currentNode);
        } else {
            state.log.debug(() -> "nothing else to do with this search node - setting all unbound variables to explicitly unbound and trying again");
            SearchNode copy = new SearchNode(state, currentNode);
            copy.bindings.setUndecidedToExplicitlyUnbound();
            AlgorithmStateLogic.addToOpenNodes(state, copy);
        }
    }

    private void addResultIfValid(BlendingInstance blendingInstance, AlgorithmState state, SearchNode currentNode) {
        validateSearchNodeGlobally(blendingInstance, state, currentNode);
        if (currentNode.invalid) {
            state.log.debugFmt("node failed final test on shape %s, ignoring", currentNode.invalidShape);
        } else {
            state.log.debug(() -> "accepting node as final result!");
            AlgorithmStateLogic.addResult(state, currentNode);
            if (state.log.isDebugEnabled()) {
                logBlendedGraphs(blendingInstance, state, currentNode);
            }
        }
    }

    private void outerJoin(BlendingInstance blendingInstance, AlgorithmState state, SearchNode currentNode) {
        if (state.log.isDebugEnabled()) {
            state.log.debug(() -> "calculating outer join with open nodes");
        }
        Set<SearchNode> joinCandidates = new HashSet<>();
        state.log.logIndented(() -> {
            for (SearchNode other : state.openNodes) {
                if (!other.encounteredVariablesFlat.isEmpty()) {
                    continue;
                }
                if (!other.bindings.conflictsWithAllowOverrideUnbound(currentNode.bindings)) {
                    Optional<SearchNode> outerJoined = SearchNodeLogic.join(currentNode, other, true);
                    if (outerJoined.isPresent()) {
                        joinCandidates.add(outerJoined.get());
                    }
                }
            }
            if (joinCandidates.size() == 0) {
                state.log.debug(() -> " no outer join candidates");
            } else {
                state.log.debugFmt("outer join results in %d candidates", joinCandidates.size());
            }
        });
        for (SearchNode node : joinCandidates) {
            AlgorithmStateLogic.addToOpenNodes(state, node);
        }
    }

    private void logBlendedGraphs(BlendingInstance blendingInstance, AlgorithmState state, SearchNode currentNode) {
        StringWriter serialized = new StringWriter();
        if (!state.log.isTraceEnabled()) {
            return;
        }
        Graph leftGraph = blendingInstance.leftTemplate.getTemplateGraphs().getDataGraph();
        Graph rightGraph = blendingInstance.rightTemplate.getTemplateGraphs().getDataGraph();
        Graph blended = new BlendedGraphs(leftGraph, rightGraph, currentNode.bindings);
        RDFDataMgr.write(serialized, blended, Lang.TTL);
        state.log.traceFmt("blended: \n%s", serialized.toString());
    }

    private AlgorithmState initializeAlgorithmState(BlendingInstance instance) {
        Set<VariableBinding> allBindings = BlendingUtils.getAllPossibleBindings(instance);
        BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
        Shapes shapes = instanceLogic.getShapes();
        VariableBindings initialBindings = instanceLogic.getFixedBindings();
        AlgorithmState state = new AlgorithmState(instance, shapes, allBindings, initialBindings,
                        instance.blendingOptions.getVerbosity());
        return state;
    }

    /**
     * @param blendingInstance
     * @param state
     * @param node
     * @param joinVars
     * @return true if at least one join was successful
     */
    private boolean join(BlendingInstance blendingInstance, AlgorithmState state,
                    SearchNode node, Set<Node> joinVars) {
        if (state.log.isDebugEnabled()) {
            state.log.debug(() -> "joining on " + joinVars);
        }
        return state.log.logIndented(() -> {
            if (joinVars.isEmpty()) {
                return false;
            }
            if (state.openNodes.isEmpty()) {
                return false;
            }
            Set<SearchNode> newNodes = new HashSet<>();
            Set<SearchNode> usedNodes = new HashSet<>();
            Set<SearchNode> intermediateNodes = new HashSet<>();
            for (Node var : joinVars) {
                Set<SearchNode> joinNodes = newNodes.isEmpty() ? Collections.singleton(node) : newNodes;
                Set<SearchNode> joinedNodes = new HashSet<>();
                Set<SearchNode> candidates = Stream.concat(state.openNodes.stream(), state.closedNodes.stream())
                                .filter(n -> !n.invalid)
                                .filter(n -> n.bindings.isAlreadyDecidedVariable(var)) // we don't know what we need
                                                                                       // yet...
                                .collect(toSet());
                Set<SearchNode> predecessors = candidates.stream().flatMap(n -> n.predecessors.stream())
                                .collect(toSet());
                candidates.removeAll(predecessors);
                // TODO CONTINUE HERE: optionally, set an undecided var to bl:unbound on a
                // cloned node.
                for (SearchNode candidate : candidates) {
                    if (candidate.bindings.isAlreadyDecidedVariable(var)) {
                        for (SearchNode toJoin : joinNodes) {
                            Optional<SearchNode> joinedOpt = SearchNodeLogic.join(candidate, toJoin);
                            if (joinedOpt.isPresent()) {
                                SearchNode joined = joinedOpt.get();
                                joinedNodes.add(joined);
                                intermediateNodes.add(joined);
                                usedNodes.add(candidate);
                            }
                        }
                    }
                }
                newNodes = joinedNodes;
            }
            intermediateNodes.removeAll(newNodes);
            Set<SearchNode> validJoinNodes = new HashSet<>();
            for (SearchNode newNode : newNodes) {
                calculateDependentVariableBindings(blendingInstance, state, newNode);
                validateSearchNode(blendingInstance, state, newNode);
                if (AlgorithmStateLogic.addToOpenNodes(state, newNode)) {
                    validJoinNodes.add(newNode);
                }
            }
            Set<SearchNode> removeFromOpenNodes = new HashSet<>();
            for (SearchNode validJoinNode : validJoinNodes) {
                removeFromOpenNodes.addAll(getPredecessorsFromSet(validJoinNode, usedNodes));
            }
            state.openNodes.removeAll(removeFromOpenNodes);
            if (validJoinNodes.isEmpty()) {
                state.log.debug(() -> "no valid join results");
            } else {
                if (state.log.isDebugEnabled()) {
                    SearchNodeFormatter formatter = new SearchNodeFormatter(state);
                    state.log.debugFmt(
                                    "join results in %d valid nodes, closing these %d nodes that are now predecessors:",
                                    validJoinNodes.size(),
                                    removeFromOpenNodes.size());
                    removeFromOpenNodes.forEach(n -> recalculateDependentValues(n, state));
                    state.log.logIndented(
                                    () -> removeFromOpenNodes.forEach(n -> state.log.debug(() -> formatter.format(n))));
                    if (state.log.isTraceEnabled()) {
                        state.log.trace(() -> "intermediate join nodes:");
                        intermediateNodes.forEach(n -> recalculateDependentValues(n, state));
                        state.log.logIndented(() -> intermediateNodes
                                        .forEach(n -> state.log.trace(() -> formatter.format(n))));
                    }
                }
            }
            state.closedNodes.addAll(intermediateNodes);
            state.closedNodes.addAll(removeFromOpenNodes);
            return !validJoinNodes.isEmpty();
        });
    }

    private Set<SearchNode> getPredecessorsFromSet(SearchNode head, Set<SearchNode> candidates) {
        Map<Boolean, Set<SearchNode>> foundInSet = head.predecessors.stream()
                        .collect(Collectors.groupingBy(candidates::contains, toSet()));
        Set<SearchNode> ret = foundInSet.getOrDefault(true, new HashSet<>());
        ret.addAll(foundInSet.getOrDefault(false, new HashSet<>()).stream()
                        .flatMap(n -> getPredecessorsFromSet(n, candidates).stream()).collect(toSet()));
        return ret;
    }

    private void explore(BlendingInstance instance, AlgorithmState state, SearchNode node, Set<Node> toExplore) {
        if (state.log.isDebugEnabled()) {
            state.log.debug(() -> "exploring " + toExplore);
        }
        int[] nodesAdded = new int[] { 0 };
        BindingCombinator.allCombinationsAsStream(var -> state.bindingOptionsProvider.apply(var),
                        toExplore,
                        node.bindings)
                        .forEach(bindings -> {
                            state.log.logIndented(() -> {
                                if (state.log.isDebugEnabled()) {
                                    var addedBindings = bindings.getBindingsAsSet();
                                    addedBindings.removeAll(node.bindings.getBindingsAsSet());
                                    state.log.debug(() -> "Exploring with new bindings: " + addedBindings);
                                }
                                SearchNode newNode = new SearchNode(state, node);
                                newNode.bindings.setAll(bindings);
                                calculateDependentVariableBindings(instance, state, newNode);
                                validateSearchNode(instance, state, newNode);
                                if (AlgorithmStateLogic.addToOpenNodes(state, newNode)) {
                                    nodesAdded[0]++;
                                }
                                ;
                            });
                        });
        state.unexploredVariables.removeAll(node.encounteredVariablesFlat);
        state.exploredVariables.addAll(node.encounteredVariablesFlat);
        if (state.log.isDebugEnabled()) {
            state.log.debugFmt("Exploring %s produces %d new nodes", toExplore, nodesAdded[0]);
        }
    }

    private void calculateDependentVariableBindings(BlendingInstance instance, AlgorithmState state,
                    SearchNode newNode) {
        if (!instance.blendingOptions.isInferDependentVariableBindings()) {
            return;
        }
        VariableBindings bindingsBeforeCalculatingImplied = null;
        if (state.log.isDebugEnabled()) {
            bindingsBeforeCalculatingImplied = new VariableBindings(newNode.bindings);
            logBlendedGraphs(instance, state, newNode);
        }
        int bindingsCount;
        do {
            bindingsCount = newNode.bindings.sizeExcludingExplicitlyUnbound();
            BlendedGraphs blended = new BlendedGraphs(instance.leftTemplate.getTemplateGraphs().getDataGraph(),
                            instance.rightTemplate.getTemplateGraphs().getDataGraph(), newNode.bindings, false);
            Map<List<Node>, List<Triple>> sp = blended.stream()
                            .collect(Collectors.groupingBy(t -> List.of(t.getSubject(), t.getPredicate()), toList()));
            Map<List<Node>, List<Triple>> po = blended.stream()
                            .collect(Collectors.groupingBy(t -> List.of(t.getPredicate(), t.getObject()), toList()));
            sp.entrySet().stream().forEach(e -> {
                List<Triple> evidence = e.getValue();
                if (evidence.size() != 2) {
                    return;
                }
                Triple leftTriple = evidence.get(0);
                Triple rightTriple = evidence.get(1);
                Node leftObject = leftTriple.getObject();
                Node rightObject = rightTriple.getObject();
                if (newNode.bindings.isVariable(leftObject)) {
                    newNode.bindings.addBindingIfPossible(leftObject, rightObject);
                } else if (newNode.bindings.isVariable(rightObject)) {
                    newNode.bindings.addBindingIfPossible(rightObject, leftObject);
                }
            });
            /*
             * po seems bad, it unifies the quantity nodes in the vf example
             * po.entrySet().stream().forEach(e -> { List<Triple> evidence = e.getValue();
             * if (evidence.size() != 2) { return; } Triple leftTriple = evidence.get(0);
             * Triple rightTriple = evidence.get(1); Node leftSubject =
             * leftTriple.getSubject(); Node rightSubject = rightTriple.getSubject(); if
             * (newNode.bindings.isVariable(leftSubject)) {
             * newNode.bindings.addBindingIfPossible(leftSubject, rightSubject); } else if
             * (newNode.bindings.isVariable(rightSubject)) {
             * newNode.bindings.addBindingIfPossible(rightSubject, leftSubject); } });
             */
        } while (newNode.bindings.sizeExcludingExplicitlyUnbound() > bindingsCount);
        if (state.log.isDebugEnabled()) {
            var impliedBindings = newNode.bindings.getBindingsAsSet();
            impliedBindings.removeAll(bindingsBeforeCalculatingImplied.getBindingsAsSet());
            if (!impliedBindings.isEmpty()) {
                state.log.debug(() -> "added implied bindings: " + impliedBindings);
                logBlendedGraphs(instance, state, newNode);
            }
        }
    }

    private void validateSearchNode(BlendingInstance instance, AlgorithmState state, SearchNode searchNode) {
        searchNode.encounteredVariables.clear();
        searchNode.encounteredVariablesFlat.clear();
        VariableAwareGraph data = ShaclValidator.blendDataGraphs(instance, searchNode.bindings);
        Set<Node> decidedVars = searchNode.bindings.getDecidedVariables();
        Set<ShapeFocusNode> checkButDontCollectEncountered = AlgorithmStateLogic.getApplicableShapes(state,
                        searchNode.bindings);
        Set<Shape> checkAndCollectEncountered = new HashSet<>();
        checkAndCollectEncountered.addAll(searchNode.untestedShapes);
        checkAndCollectEncountered.addAll(searchNode.unsatisfiedShapesByRequiredVariable
                        .entrySet()
                        .stream()
                        .filter(e -> decidedVars.contains(e.getKey()))
                        .map(Map.Entry::getValue)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));
        checkButDontCollectEncountered
                        .removeIf(
                                        shapeFocusNode -> checkAndCollectEncountered.stream()
                                                        .anyMatch(shape -> shapeFocusNode.shape
                                                                        .equals(shape.getShapeNode())
                                                                        && searchNode.shapeToFocusNodes.getOrDefault(
                                                                                        shape.getShapeNode(),
                                                                                        Collections.emptySet())
                                                                                        .contains(shapeFocusNode.focusNode)));
        for (ShapeFocusNode shapeFocusNode : checkButDontCollectEncountered) {
            evaluateShapeForSearchNodeOnFocusNodes(instance, state, data, searchNode,
                            state.shapes.getShape(shapeFocusNode.shape), false,
                            Collections.singleton(shapeFocusNode.focusNode));
            if (searchNode.invalid) {
                state.log.debugFmt("shape %s->%s, required for bound variables, is violated", shapeFocusNode.shape,
                                shapeFocusNode.focusNode);
                return;
            }
        }
        for (Shape shape : checkAndCollectEncountered) {
            evaluateShapeForSearchNode(instance, state, data, searchNode, shape, true);
            if (searchNode.invalid) {
                state.log.debugFmt("shape %s->%s of search node is violated", shape.getShapeNode(),
                                searchNode.shapeToFocusNodes.get(shape));
                return;
            }
        }
    }

    private void validateSearchNodeGlobally(BlendingInstance instance, AlgorithmState state, SearchNode searchNode) {
        VariableAwareGraph data = ShaclValidator.blendDataGraphs(instance, searchNode.bindings);
        for (Shape shape : state.shapes.getTargetShapes()) {
            state.log.finerTraceFmt("validating shape %s", shape.getShapeNode());
            Set<BindingValidationResult> validationResults = ShaclValidator.validateShapeForFocusNodesOnData(instance,
                            state,
                            data, searchNode.bindings, shape, VLib.focusNodes(data, shape).stream().collect(toSet()));
            if (validationResults.isEmpty()) {
                throw new IllegalStateException("No validation results for shape " + shape.getShapeNode() + "!");
            }
            Ternary valid = validationResults.stream().map(r -> r.valid).reduce((l, r) -> l.and(r)).get();
            Set<Set<Node>> encounteredVariables = validationResults
                            .stream()
                            .map(r -> new HashSet<>(r.encounteredVariables))
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toUnmodifiableSet());
            searchNode.untestedShapes.remove(shape);
            if (valid.isFalse()) {
                searchNode.invalid = true;
                searchNode.invalidShape = shape;
                return;
            }
            if (valid.isTrue()) {
                for (Node variable : searchNode.bindings.getDecidedVariables()) {
                    SearchNodeLogic.removeUnsatisfiedShapeByVariable(searchNode, shape, variable);
                }
                searchNode.satisfiedShapes.add(shape);
            }
        }
    }

    private void initializeSearchNodes(BlendingInstance blendingInstance, AlgorithmState state) {
        VariableAwareGraph data = ShaclValidator.blendDataGraphs(blendingInstance,
                        new BlendingInstanceLogic(blendingInstance).getFixedBindings());
        Set<SearchNode> initialNodes = generateInitialSearchNodes(state, data);
        Set<Node> potentiallyUnsatisfiableShapes = initialNodes.stream()
                        .flatMap(n -> n.shapeToFocusNodes.keySet().stream()).collect(toSet());
        state.log.debugFmt("generated %d initial search nodes", initialNodes.size());
        Set<SearchNode> effectiveNodes = new HashSet<>();
        for (SearchNode node : initialNodes) {
            Shape shape = node.untestedShapes.stream().findFirst().get();
            evaluateShapeForSearchNode(blendingInstance, state, data, node, shape, true);
            AlgorithmStateLogic.recordBoundVarsToShapesToCheck(state, node);
            recalculateDependentValues(node, state);
            if (node.encounteredVariables.isEmpty()) {
                // special case: no variables encountered when evaluating the shape.
                // Two possibilities:
                // a) all focus nodes valid - we do not need to consider this shape further, it
                // will always be valid
                // b) at least one invalid node: no bindings can change this result, we can stop
                // the algorithm
                if (!node.satisfiedShapes.isEmpty()) {
                    state.log.debugFmt("no variables encountered while validating shape %s - excluding from search",
                                    shape.toString());
                    state.closedAsIneffective.add(node);
                    potentiallyUnsatisfiableShapes.remove(shape.getShapeNode());
                }
            } else {
                effectiveNodes.add(node);
                potentiallyUnsatisfiableShapes.remove(shape.getShapeNode());
            }
        }
        if (!potentiallyUnsatisfiableShapes.isEmpty()) {
            state.noSolution = true;
            effectiveNodes.clear();
            state.log.infoFmt("shapes %s cannot be satisfied, aborting search", potentiallyUnsatisfiableShapes);
            return;
        }
        AlgorithmStateLogic.addSearchNodes(state, effectiveNodes);
    }

    private void evaluateShapeForSearchNode(BlendingInstance blendingInstance, AlgorithmState state,
                    VariableAwareGraph data, SearchNode searchNode, Shape shape,
                    boolean collectEncounteredVariables) {
        Set<Node> focusNodes = searchNode.shapeToFocusNodes.getOrDefault(shape.getShapeNode(), Collections.emptySet());
        evaluateShapeForSearchNodeOnFocusNodes(blendingInstance, state, data, searchNode, shape,
                        collectEncounteredVariables, focusNodes);
    }

    private void evaluateShapeForSearchNodeOnFocusNodes(BlendingInstance blendingInstance, AlgorithmState state,
                    VariableAwareGraph data,
                    SearchNode searchNode, Shape shape, boolean collectEncounteredVariables, Set<Node> focusNodes) {
        Set<BindingValidationResult> validationResults = ShaclValidator.validateShapeForFocusNodesOnData(
                        blendingInstance, state,
                        data, searchNode.bindings, shape, focusNodes);
        if (validationResults.isEmpty()) {
            throw new IllegalStateException("No validation results for shape " + shape.getShapeNode() + "!");
        }
        Ternary valid = validationResults.stream().map(r -> r.valid).reduce((l, r) -> l.and(r)).get();
        Set<Set<Node>> encounteredVariables = validationResults
                        .stream()
                        .map(r -> new HashSet<>(r.encounteredVariables))
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        if (collectEncounteredVariables) {
            searchNode.encounteredVariables.addAll(encounteredVariables);
        }
        searchNode.untestedShapes.remove(shape);
        if (valid.isFalse()) {
            searchNode.invalid = true;
            searchNode.invalidShape = shape;
            return;
        }
        if (valid.isUnkown()) {
            if (collectEncounteredVariables) {
                for (Node encounteredVariable : encounteredVariables.stream().flatMap(Set::stream)
                                .collect(toSet())) {
                    SearchNodeLogic.addUnsatisfiedShapeByVariable(searchNode, shape, encounteredVariable);
                }
            }
            return;
        }
        if (valid.isTrue()) {
            for (Node variable : searchNode.bindings.getDecidedVariables()) {
                SearchNodeLogic.removeUnsatisfiedShapeByVariable(searchNode, shape, variable);
            }
            searchNode.satisfiedShapes.add(shape);
        }
    }

    private Set<SearchNode> generateInitialSearchNodes(AlgorithmState state, Graph data) {
        return state.shapes
                        .getTargetShapes()
                        .stream()
                        .filter(not(Shape::deactivated))
                        .flatMap(shape -> VLib.focusNodes(data, shape).stream()
                                        .map(focusNode -> SearchNodeLogic.forInitialShapeAndBindings(state, shape,
                                                        focusNode, state.initialBindings, state.allVariables)))
                        .collect(toSet());
    }

    private Stream<TemplateGraphs> generateResults(BlendingInstance instance, AlgorithmState state,
                    Set<VariableBindings> bindingsSet) {
        return bindingsSet.stream().map(bs -> recreateResult(instance, state, bs).get());
    }

    private Optional<TemplateGraphs> recreateResult(BlendingInstance instance, AlgorithmState state,
                    VariableBindings bindings) {
        TemplateBindings templateBindings = new TemplateBindings(instance.leftTemplate, instance.rightTemplate,
                        bindings);
        try {
            return Optional.ofNullable(instance.blendingOperations.blendWithGivenBindings(
                            instance.leftTemplate.getTemplateGraphs(),
                            instance.rightTemplate.getTemplateGraphs(),
                            templateBindings));
        } catch (Exception e) {
            if (state.log.isDebugEnabled()) {
                state.log.debugFmt("error while recreating result: %s", e.getMessage());
                state.log.debug(() -> "bindings: "
                                + new SearchNodeFormatter(state).bindingsToString(bindings, "\n             "));
            }
            throw e;
        }
    }
}