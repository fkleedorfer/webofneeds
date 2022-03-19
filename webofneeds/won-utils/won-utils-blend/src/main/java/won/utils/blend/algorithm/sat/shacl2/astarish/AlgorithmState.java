package won.utils.blend.algorithm.sat.shacl2.astarish;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.sat.shacl2.ForbiddenBindings;
import won.utils.blend.algorithm.sat.shacl2.forbiddenbindings.IntListForbiddenBindings;
import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;

import java.util.*;
import java.util.function.Supplier;

public class AlgorithmState {
    public final Queue<SearchNode> open = new PriorityQueue<>();
    public final Set<SearchNode> closed = new HashSet<>();
    public final Set<SearchNode> results = new HashSet<>();
    public final Map<Node, List<SearchNode>> searchNodesByBoundNode = new HashMap<>();
    public final Set<Node> allOptionsExplored = new HashSet<>();
    public final Set<SearchNode> validFragments = new HashSet<>();
    public CompactBindingsManager bindingsManager;
    public final IndentedWriter debugWriter = IndentedWriter.clone(IndentedWriter.stdout);
    public final ForbiddenBindings forbiddenBindings;
    public final Shapes shapes;
    public final Verbosity verbosity;
    public final Map<Node, Set<Shape>> shapesImpliedByVariables = new HashMap<>();
    public final Log log = new Log();
    int nodesInspected = 0;

    public AlgorithmState(CompactBindingsManager bindingsManager, Shapes shapes, Verbosity verbosity) {
        this.bindingsManager = bindingsManager;
        this.shapes = shapes;
        this.forbiddenBindings = new IntListForbiddenBindings(bindingsManager);
        this.verbosity = verbosity;
    }

    public class Log {
        public void trace(Supplier<String> messageSupplier){
            if (verbosity.isMaximum()) {
                debugWriter.println(messageSupplier.get());
            }
        }

        public void info(Supplier<String> messageSupplier) {
            if (verbosity.isMediumOrHigher()){
                debugWriter.println(messageSupplier.get());
            }
        }
    }
}
