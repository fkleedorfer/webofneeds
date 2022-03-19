package won.utils.blend.support.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.WrappedGraph;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NiceIterator;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class VariableAwareGraph extends WrappedGraph {
    private Set<Node> encounteredVariables = new HashSet<>();
    private Function<Node, Boolean> isVariableCheck;

    public VariableAwareGraph(Graph base, Function<Node, Boolean> isVariableCheck) {
        super(base);
        this.isVariableCheck = isVariableCheck;
    }

    public void reset() {
        encounteredVariables.clear();
    }

    public Set<Node> getEncounteredVariables() {
        return new HashSet<>(encounteredVariables);
    }

    private void rememberVariables(Triple t) {
        rememberVariables(t.getSubject(), t.getPredicate(), t.getObject());
    }

    private void rememberVariables(Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            if (isVariableCheck.apply(nodes[i])) {
                encounteredVariables.add(nodes[i]);
            }
        }
    }

    private Stream rememberVariables(Stream<Triple> tripleStream) {
        return tripleStream.map(t -> {
            rememberVariables(t);
            return t;
        });
    }

    private ExtendedIterator<Triple> rememberVariables(ExtendedIterator<Triple> delegate) {
        return new NiceIterator<Triple>() {
            @Override
            public void remove() {
                delegate.remove();
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Triple next() {
                Triple x = delegate.next();
                rememberVariables(x);
                return x;
            }

            @Override
            public void close() {
                delegate.close();
            }
        };
    }

    @Override
    public ExtendedIterator<Triple> find(Triple m) {
        rememberVariables(m);
        return rememberVariables(super.find(m));
    }

    @Override
    public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
        rememberVariables(s, p, o);
        return rememberVariables(super.find(s, p, o));
    }

    @Override
    public boolean contains(Node s, Node p, Node o) {
        rememberVariables(s, p, o);
        if (super.contains(s, p, o)) {
            rememberVariables(s, p, o);
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(Triple t) {
        rememberVariables(t);
        if (super.contains(t)) {
            rememberVariables(t);
            return true;
        }
        return false;
    }

    @Override
    public Stream<Triple> stream(Node s, Node p, Node o) {
        rememberVariables(s, p, o);
        return rememberVariables(super.stream(s, p, o));
    }

    @Override
    public Stream<Triple> stream() {
        return rememberVariables(super.stream());
    }

    @Override
    public ExtendedIterator<Triple> find() {
        return rememberVariables(super.find());
    }
}
