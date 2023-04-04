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

public class VariableAwareGraphImpl extends WrappedGraph implements VariableAwareGraph {
    private Set<Node> encounteredVariables = new HashSet<>();
    private Function<Node, Boolean> isVariableCheck;

    public VariableAwareGraphImpl(Graph base, Function<Node, Boolean> isVariableCheck) {
        super(base);
        this.isVariableCheck = isVariableCheck;
    }

    @Override
    public void resetEncounteredVariables() {
        encounteredVariables.clear();
    }

    @Override
    public Set<Node> getEncounteredVariables() {
        return new HashSet<>(encounteredVariables);
    }

    private Triple rememberVariables(Triple t) {
        rememberVariables(t.getSubject(), t.getPredicate(), t.getObject());
        return t;
    }

    private void rememberVariables(Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            if (isVariableCheck.apply(nodes[i])) {
                encounteredVariables.add(nodes[i]);
            }
        }
    }

    private Stream rememberVariables(Stream<Triple> tripleStream) {
        return tripleStream.map(this::rememberVariables);
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
                return rememberVariables(delegate.next());
            }

            @Override
            public void close() {
                delegate.close();
            }
        };
    }

    @Override
    public ExtendedIterator<Triple> find(Triple m) {
        return rememberVariables(super.find(rememberVariables(m)));
    }

    @Override
    public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
        rememberVariables(s, p, o);
        return rememberVariables(super.find(s, p, o));
    }

    @Override
    public boolean contains(Node s, Node p, Node o) {
        rememberVariables(s, p, o);
        return super.contains(s, p, o);
    }

    @Override
    public boolean contains(Triple t) {
        return super.contains(rememberVariables(t));
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

    @Override
    public String toString() {
        return "VariableAwareGraphImpl{" +
                        encounteredVariables.size() + " variable(s) encountered since last clear}";
    }
}
