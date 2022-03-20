package won.utils.blend.algorithm.join;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.support.bindings.options.BindingOptionsProvider;
import won.utils.blend.algorithm.support.bindings.options.CachingBindingOptionsProvider;
import won.utils.blend.algorithm.support.bindings.options.InstanceBindingOptionProvider;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.graph.VariableAwareGraph;

import java.util.*;
import java.util.function.Supplier;

public class AlgorithmState {
    public BlendingInstance blendingInstance;
    public boolean noSolution = false;
    public final VariableBindings initialBindings;
    public final Set<Node> exploredVariables = new HashSet<>();
    public final Set<Node> unexploredVariables = new HashSet<>();
    public final Set<SearchNode> openNodes = new HashSet<>();
    public final Set<SearchNode> results = new HashSet<>();
    public final Map<Shape, Set<Set<Node>>> requiredVariablesByShapes = new HashMap<>();
    public final Set<VariableBinding> allPossibleBindings;
    public final BindingOptionsProvider bindingOptionsProvider;
    public final Set<Node> allVariables;
    public final Shapes shapes;
    public final Verbosity verbosity;
    private final IndentedWriter debugWriter = IndentedWriter.clone(IndentedWriter.stdout);
    public final Log log = new Log();
    public final Set<SearchNode> closedAsIneffective = new HashSet<>();
    public Set<SearchNode> closedNodes = new HashSet<>();
    public final Set<Node> boundVariablesInResult = new HashSet<>();
    public final Set<Node> unboundVariablesInResult = new HashSet<>();

    public AlgorithmState(BlendingInstance instance, Shapes shapes, Set<VariableBinding> allBindings,
                    VariableBindings initialBindings,
                    Verbosity verbosity) {
        this.blendingInstance = instance;
        this.shapes = shapes;
        this.verbosity = verbosity;
        this.initialBindings = new VariableBindings(initialBindings);
        this.allPossibleBindings = Collections.unmodifiableSet(allBindings);
        this.allVariables = new HashSet<>(initialBindings.getVariables());
        this.unexploredVariables.addAll(this.allVariables);
        this.bindingOptionsProvider = new CachingBindingOptionsProvider(new InstanceBindingOptionProvider(instance));
    }

    public static enum Verbosity {
        ERROR, INFO, DEBUG, TRACE, FINER_TRACE;
    }

    public class Log {
        public <R> R logIndented(Supplier<R> supplier) {
            debugWriter.incIndent();
            try {
                return supplier.get();
            } finally {
                debugWriter.decIndent();
            }
        }

        public void logIndented(Runnable runnable) {
            debugWriter.incIndent();
            try {
                runnable.run();
            } finally {
                debugWriter.decIndent();
            }
        }

        public boolean meetsVerbosity(Verbosity verbosity) {
            return verbosity.compareTo(AlgorithmState.this.verbosity) <= 0;
        }

        private void log(Verbosity messageVerbosity, Supplier<String> messageSupplier) {
            if (messageVerbosity.compareTo(verbosity) <= 0) {
                debugWriter.println(messageSupplier.get());
            }
        }

        private void logFmt(Verbosity messageVerbosity, String format, Object... arguments) {
            if (messageVerbosity.compareTo(verbosity) <= 0) {
                debugWriter.println(String.format(format, arguments));
            }
        }

        public void debug(Supplier<String> messageSupplier) {
            log(Verbosity.DEBUG, messageSupplier);
        }

        public void debugFmt(String format, Object... arguments) {
            logFmt(Verbosity.DEBUG, format, arguments);
        }

        public void trace(Supplier<String> messageSupplier) {
            log(Verbosity.TRACE, messageSupplier);
        }

        public void traceFmt(String format, Object... arguments) {
            logFmt(Verbosity.TRACE, format, arguments);
        }

        public void finerTrace(Supplier<String> messageSupplier) {
            log(Verbosity.FINER_TRACE, messageSupplier);
        }

        public void finerTraceFmt(String format, Object... arguments) {
            logFmt(Verbosity.FINER_TRACE, format, arguments);
        }

        public void info(Supplier<String> messageSupplier) {
            log(Verbosity.INFO, messageSupplier);
        }

        public void infoFmt(String format, Object... arguments) {
            logFmt(Verbosity.INFO, format, arguments);
        }

        public void error(Supplier<String> messageSupplier) {
            log(Verbosity.ERROR, messageSupplier);
        }

        public void errorFmt(String format, Object... arguments) {
            logFmt(Verbosity.ERROR, format, arguments);
        }

        public boolean isDebugEnabled() {
            return meetsVerbosity(Verbosity.DEBUG);
        }

        public boolean isInfoEnabled() {
            return meetsVerbosity(Verbosity.INFO);
        }

        public boolean isTraceEnabled() {
            return meetsVerbosity(Verbosity.TRACE);
        }

        public boolean isFinerTraceEnabled() {
            return meetsVerbosity(Verbosity.FINER_TRACE);
        }

        public boolean isErrorEnabled() {
            return meetsVerbosity(Verbosity.ERROR);
        }
    }
}
