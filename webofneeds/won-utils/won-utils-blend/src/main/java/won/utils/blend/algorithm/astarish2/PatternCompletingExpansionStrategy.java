package won.utils.blend.algorithm.astarish2;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.other.G;
import won.utils.blend.BLEND;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PatternCompletingExpansionStrategy extends BaseExpansionStrategy {
    @Override
    public Set<SearchNode2> findSuccessors(BlendingInstance instance, AlgorithmState state,
                    SearchNodeInspection inspection) {
        int[] bindings = inspection.node.bindings;
        BitSet decided = inspection.node.decided;
        Set<SearchNode2> expanded = new HashSet<>();
        for (int varIndex = 0; varIndex < bindings.length; varIndex++) {
            if (!decided.get(varIndex)) {
                continue;
            }
            Optional<VariableBinding> bindingOpt = state.bindingsManager.getBinding(bindings, varIndex);
            if (bindingOpt.isPresent() && !instance.blendingResultEvaluator
                            .hasReportEntriesForVariable(inspection.validationReport, bindingOpt.get().getVariable())) {
                expanded = expandForBoundValueAsSubject(state, inspection, expanded, bindingOpt.get());
                expanded = expandForBoundValueAsObject(state, inspection, expanded, bindingOpt.get());
            }
        }
        return expanded;
    }

    private Set<SearchNode2> expandForBoundValueAsSubject(AlgorithmState state, SearchNodeInspection inspection,
                    Set<SearchNode2> expanded, VariableBinding binding) {
        Iterator<Triple> triples = G.find(inspection.blendingResult.getDataGraph(), binding.getBoundNode(), null, null);
        Map<Node, Set<Triple>> groupedByPredicate = StreamSupport
                        .stream(Spliterators.spliterator(triples, Long.MAX_VALUE, Spliterator.IMMUTABLE), false)
                        .filter(t -> !t.getPredicate().equals(BLEND.boundTo))
                        .collect(Collectors.groupingBy(t -> t.getPredicate(), Collectors.toSet()));
        for (Map.Entry<Node, Set<Triple>> e : groupedByPredicate.entrySet()) {
            Set<Triple> samePredicate = e.getValue();
            if (samePredicate.size() == 2) {
                Node variable = null;
                Node boundValue = null;
                for (Triple t : samePredicate) {
                    Node n = t.getObject();
                    if (inspection.templateBindings.isVariable(n) && variable == null) {
                        variable = n;
                    } else {
                        boundValue = n;
                    }
                }
                if (variable != null
                                && !state.bindingsManager.isBoundVariable(inspection.node.bindings, variable)
                                && state.bindingsManager.isAdmissibleBinding(variable, boundValue)) {
                    if (expanded.isEmpty()) {
                        expanded.add(new SearchNode2(inspection.score, inspection.node.bindings,
                                        inspection.node.decided, inspection.node.problems, inspection.graphSize));
                    }
                    expanded = addBindingsToExpandedNodes(expanded, state, inspection, variable, boundValue);
                }
            }
        }
        return expanded;
    }

    private Set<SearchNode2> expandForBoundValueAsObject(AlgorithmState state, SearchNodeInspection inspection,
                    Set<SearchNode2> expanded, VariableBinding binding) {
        Iterator<Triple> triples = G.find(inspection.blendingResult.getDataGraph(), null, null, binding.getBoundNode());
        Map<Node, Set<Triple>> groupedByPredicate = StreamSupport
                        .stream(Spliterators.spliterator(triples, Long.MAX_VALUE, Spliterator.IMMUTABLE), false)
                        .filter(t -> !t.getPredicate().equals(BLEND.boundTo))
                        .collect(Collectors.groupingBy(t -> t.getPredicate(), Collectors.toSet()));
        for (Map.Entry<Node, Set<Triple>> e : groupedByPredicate.entrySet()) {
            Set<Triple> samePredicate = e.getValue();
            if (samePredicate.size() == 2) {
                Node variable = null;
                Node boundValue = null;
                for (Triple t : samePredicate) {
                    Node n = t.getSubject();
                    if (inspection.templateBindings.isVariable(n) && variable == null) {
                        variable = n;
                    } else {
                        boundValue = n;
                    }
                }
                if (variable != null
                                && !state.bindingsManager.isBoundVariable(inspection.node.bindings, variable)
                                && state.bindingsManager.isAdmissibleBinding(variable, boundValue)) {
                    if (expanded.isEmpty()) {
                        expanded.add(new SearchNode2(inspection.score, inspection.node.bindings,
                                        inspection.node.decided, inspection.node.problems, inspection.graphSize));
                    }
                    expanded = addBindingsToExpandedNodes(expanded, state, inspection, variable, boundValue);
                }
            }
        }
        return expanded;
    }

    private Set<SearchNode2> addBindingsToExpandedNodes(Set<SearchNode2> expanded,
                    AlgorithmState state, SearchNodeInspection inspection, Node variable, Node boundValue) {
        return expanded
                        .stream()
                        .map(n -> {
                            int[] newBindings = state.bindingsManager
                                            .copyAndSetBinding(n.bindings, variable, boundValue);
                            BitSet newDecided = (BitSet) n.decided.clone();
                            newDecided.set(state.bindingsManager.getIndexOfVariable(variable));
                            float score = estimateScore(inspection, state.bindingsManager, inspection.node.bindings,
                                            inspection.node.decided);
                            return new SearchNode2(score, newBindings, newDecided, n.problems, n.graphSize - 1);
                        }).collect(Collectors.toSet());
    }
}
