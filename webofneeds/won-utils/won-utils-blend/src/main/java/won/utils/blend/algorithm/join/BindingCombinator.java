package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Node;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class BindingCombinator {
    public static Stream<VariableBindings> allCombinationsAsStream(
                    Function<Node, Set<Node>> optionsForVariableProvider,
                    Set<Node> variables,
                    VariableBindings existingBindings) {
        if (variables.isEmpty()) {
            return Stream.empty();
        }
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<VariableBindings>(Long.MAX_VALUE,
                        Spliterator.ORDERED
                                        | Spliterator.NONNULL
                                        | Spliterator.DISTINCT
                                        | Spliterator.IMMUTABLE) {
            private boolean finished = false;
            Map<Node, Iterator> optionIteratorByVariable = new HashMap<>();
            Map<Node, Node> combination = new HashMap<>();
            List<Node> variablesAsList = new ArrayList<>(variables);
            Iterator<Node> variablesIterator = variables.iterator();
            Iterator<Node> optionsForCurrentVariableIterator = null;
            Node currentVariable = variablesIterator.next();

            @Override
            public boolean tryAdvance(Consumer<? super VariableBindings> action) {
                if (finished) {
                    return false;
                }
                if (optionIteratorByVariable.isEmpty()) {
                    initialize();
                }
                VariableBindings validBindings = null;
                while (validBindings == null && !finished) {
                    prepareNextCombination();
                    if (!finished) {
                        validBindings = makeValidBindingsFromNextCombination();
                    }
                }
                if (validBindings != null) {
                    action.accept(validBindings);
                }
                return !finished;
            }

            private VariableBindings makeValidBindingsFromNextCombination() {
                VariableBindings result = new VariableBindings(existingBindings);
                for (Map.Entry<Node, Node> binding : combination.entrySet()) {
                    if (!result.addBindingIfPossible(binding.getKey(), binding.getValue())) {
                        return null;
                    }
                }
                return result;
            }

            private void prepareNextCombination() {
                boolean forceAdvance = true;
                for (int i = 0; i < variablesAsList.size(); i++) {
                    Node var = variablesAsList.get(i);
                    Node option = combination.get(var);
                    if (option == null || forceAdvance) {
                        Iterator<Node> optionIt = optionIteratorByVariable.get(var);
                        if (!optionIt.hasNext()) {
                            optionIt = optionsForVariableProvider.apply(var).iterator();
                            optionIteratorByVariable.put(var, optionIt);
                            if (i == variablesAsList.size() - 1) {
                                finished = true;
                                return;
                            }
                            forceAdvance = true;
                        } else {
                            forceAdvance = false;
                        }
                        Node nextOption = optionIt.next();
                        combination.put(var, nextOption);
                    }
                }
            }

            private void initialize() {
                for (Node var : variablesAsList) {
                    Iterator<Node> optionIt = optionsForVariableProvider.apply(var).iterator();
                    optionIteratorByVariable.put(var, optionIt);
                }
            }
        }, false);
    }
}
