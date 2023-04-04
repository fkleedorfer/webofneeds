package won.utils.blend.support.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.graph.GraphFactory;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public class VariableAwareBlendedGraphs extends BlendedGraphs implements VariableAwareGraph {
    private final VariableAwareGraph leftVAG;
    private final VariableAwareGraph rightVAG;
    private final VariableAwareGraph bindingsGraphVAG;

    public VariableAwareBlendedGraphs(Graph L, Graph R,
                    VariableBindings bindings) {
        this(L, R, bindings, true);
    }

    public VariableAwareBlendedGraphs(Graph L, Graph R,
                    VariableBindings bindings, boolean addBindingMetadata) {
        super(
                        new VariableAwareGraphImpl(L, bindings::isVariable),
                        new VariableAwareGraphImpl(R, bindings::isVariable),
                        bindings, addBindingMetadata,
                        () -> new VariableAwareGraphImpl(GraphFactory.createGraphMem(), bindings::isVariable));
        this.leftVAG = (VariableAwareGraph) this.L;
        this.rightVAG = (VariableAwareGraph) this.R;
        this.bindingsGraphVAG = (VariableAwareGraph) this.bindingsGraph;
    }

    @Override
    public void resetEncounteredVariables() {
        leftVAG.resetEncounteredVariables();
        rightVAG.resetEncounteredVariables();
        bindingsGraphVAG.resetEncounteredVariables();
    }

    @Override
    public Set<Node> getEncounteredVariables() {
        return concat(
                        bindingsGraphVAG.getEncounteredVariables().stream(),
                        concat(
                                        leftVAG.getEncounteredVariables().stream(),
                                        rightVAG.getEncounteredVariables().stream()))
                                                        .collect(Collectors.toSet());
    }
}
