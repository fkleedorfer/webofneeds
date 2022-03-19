package won.utils.blend.algorithm.sat.shacl2;

import org.apache.jena.graph.Node;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.astarish.AlgorithmState;
import won.utils.blend.algorithm.support.instance.BlendingInstanceLogic;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class BindingCombinator {
    private final BlendingInstance instance;
    private final AlgorithmState state;
    private final BlendingInstanceLogic instanceLogic;

    public BindingCombinator(BlendingInstance instance, AlgorithmState state) {
        this.instance = instance;
        this.state = state;
        this.instanceLogic = new BlendingInstanceLogic(instance);
    }

    public <T> Stream<T> allCombinations(Set<Node> variables, Function<VariableBindings, T> fun) {
        return allCombinations(variables, new VariableBindings(new BlendingInstanceLogic(instance).getVariables()),
                        fun);
    }

    public <T> Stream<T> allCombinations(Set<Node> variables, VariableBindings bindings,
                    Function<VariableBindings, T> fun) {
        if (variables.isEmpty()) {
            if (!state.forbiddenBindings.isForbiddenBindings(bindings.getBindingsAsSet())) {
                return Stream.of(fun.apply(bindings));
            }
        } else {
            Optional<Node> varOpt = variables.stream()
                            .filter(not(bindings::isAlreadyDecidedVariable))
                            .findFirst();
            if (varOpt.isEmpty()) {
                if (!state.forbiddenBindings.isForbiddenBindings(bindings.getBindingsAsSet())) {
                    return Stream.of(fun.apply(bindings));
                }
            }
            Node var = varOpt.get();
            Stream.Builder<T> streamBuilder = Stream.builder();
            for (Node candidate : state.bindingsManager.getBindingOptions(var)) {
                VariableBindings nextBindings = new VariableBindings(bindings);
                if (nextBindings.addBindingIfPossible(var, candidate)) {
                    state.log.info(() -> "trying binding " + var + " -> " + candidate);
                    Set nextVars = new HashSet(variables);
                    nextVars.remove(var);
                    ((Stream<T>) allCombinations(nextVars, nextBindings, fun))
                                    .forEach(t -> streamBuilder.accept(t));
                }
            }
            return streamBuilder.build();
        }
        return Stream.empty();
    }
}
