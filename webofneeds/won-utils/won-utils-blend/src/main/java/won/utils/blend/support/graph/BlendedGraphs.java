package won.utils.blend.support.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NiceIterator;
import org.apache.jena.vocabulary.RDF;
import won.utils.blend.BLEND;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.*;
import java.util.stream.Collectors;

import static won.utils.blend.algorithm.sat.shacl.BindingUtils.*;

public class BlendedGraphs extends Union {
    private Map<Node, Node> bindings;
    private final Graph bindingsGraph;
    private final boolean addBindingMetadata;

    public BlendedGraphs(Graph L, Graph R, Iterable<VariableBinding> bindings) {
        super(L, R);
        this.addBindingMetadata = false;
        this.bindingsGraph = null;
        throw new UnsupportedOperationException("dont use!");
    }

    public BlendedGraphs(Graph L, Graph R, VariableBindings bindings) {
        this(L, R, bindings, true);
    }

    public BlendedGraphs(Graph L, Graph R, VariableBindings bindings, boolean addBindingMetadata) {
        super(L, R);
        this.bindings = new HashMap<>();
        for (VariableBinding binding : bindings.getBindingsAsSet()) {
            this.bindings.put(binding.getVariable(), binding.getBoundNode());
        }
        this.addBindingMetadata = addBindingMetadata;
        if (this.addBindingMetadata) {
            this.bindingsGraph = GraphFactory.createGraphMem();
            for (Node variable : this.bindings.keySet()) {
                Node boundTo = this.bindings.get(variable);
                if (boundTo.equals(BLEND.unbound)) {
                    continue;
                }
                bindingsGraph.add(variable, BLEND.boundTo, boundTo);
            }
            bindings.getVariables().forEach(var -> bindingsGraph.add(var, RDF.type.asNode(), BLEND.Variable));
        } else {
            this.bindingsGraph = null;
        }
    }

    @Override
    public void performAdd(Triple t) {
        throw new UnsupportedOperationException("add(Triple) not allowed for " + getClass().getSimpleName());
    }

    @Override
    public void performDelete(Triple t) {
        throw new UnsupportedOperationException("delete(Triple) not allowed for " + getClass().getSimpleName());
    }

    @Override
    public boolean graphBaseContains(Triple t) {
        return unblendTriple(t).stream().anyMatch(u -> super.graphBaseContains(u)) || bindingsGraph.contains(t);
    }

    @Override
    protected ExtendedIterator<Triple> _graphBaseFind(Triple t) {
        if (addBindingMetadata) {
            Set<Triple> unblended = unblendTriple(t);
            Set<Triple> found = new HashSet<>();
            for (Triple u : unblended) {
                found.addAll(super._graphBaseFind(u).mapWith(this::blendTriple).toSet());
            }
            return bindingsGraph.find(t).andThen(new NiceIterator<Triple>() {
                Iterator<Triple> it = found.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Triple next() {
                    return it.next();
                }
            });
        } else {
            Set<Triple> unblended = unblendTriple(t);
            Set<Triple> found = new HashSet<>();
            for (Triple u : unblended) {
                found.addAll(super._graphBaseFind(u).mapWith(this::blendTriple).toSet());
            }
            return new NiceIterator<Triple>() {
                Iterator<Triple> it = found.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Triple next() {
                    return it.next();
                }
            };
        }
    }

    private Triple blendTriple(Triple t) {
        Triple blended = new Triple(
                        dereferenceIfVariable(t.getSubject(), bindings),
                        dereferenceIfVariable(t.getPredicate(), bindings),
                        dereferenceIfVariable(t.getObject(), bindings));
        return blended;
    }

    private Set<Triple> unblendTriple(Triple t) {
        Set<Node> subjects = unblendNode(t.getSubject());
        Set<Node> predicates = unblendNode(t.getPredicate());
        Set<Node> objects = unblendNode(t.getObject());
        Set<Triple> unblended = subjects
                        .stream()
                        .flatMap(s -> predicates
                                        .stream()
                                        .flatMap(p -> objects
                                                        .stream()
                                                        .map(o -> new Triple(s, p, o))))
                        .collect(Collectors.toSet());
        return unblended;
    }

    private Set<Node> unblendNode(Node node) {
        if (node == Node.ANY) {
            return Set.of(node);
        }
        if (isBoundVariable(node, bindings)){
            return Collections.emptySet();
        }
        Set<Node> nodes = getVariablesBoundToNode(node, bindings);
        nodes.add(node);
        return nodes;
    }

}
