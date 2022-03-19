package won.utils.blend.algorithm.bruteforce;

import org.apache.jena.graph.Node;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class BindingsPowersetGenerator {
    private Set<VariableBinding> admissibleBindings;
    private int[] currentCombination, bindingsCountPerVariable;
    private boolean[] boundVariables;
    private int numberOfVariables;
    private List<VariableBinding> admissibleBindingsSortedByVariable;

    public BindingsPowersetGenerator(Set<VariableBinding> admissibleBindings) {
        this.admissibleBindings = admissibleBindings;
        initialize();
    }

    public Stream<Set<VariableBinding>> getBindingsPowersetAsStream() {
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<Set<VariableBinding>>(calculateSize(),
                        Spliterator.SIZED
                                        | Spliterator.ORDERED
                                        | Spliterator.NONNULL
                                        | Spliterator.DISTINCT
                                        | Spliterator.IMMUTABLE) {
            @Override
            public boolean tryAdvance(Consumer<? super Set<VariableBinding>> action) {
                if (boundVariables == null) {
                    return false;
                }
                action.accept(makeVariableBindingSetForCombination());
                makeNextBindingCombination();
                return true;
            }
        }, false);
    }

    public void stopStream() {
        this.boundVariables = null;
    }

    private long calculateSize() {
        long size = 1;
        for (int i = 0; i < bindingsCountPerVariable.length; i++) {
            size *= (bindingsCountPerVariable[i] + 1);
            if (size < 0) {
                return Long.MAX_VALUE;
            }
        }
        return size;
    }

    private Set<VariableBinding> makeVariableBindingSetForCombination() {
        Set<VariableBinding> binding = new HashSet<>();
        int offset = 0;
        for (int i = 0; i < numberOfVariables; i++) {
            if (boundVariables[i]) {
                // currentCombination == bindingsCountPerVariable[i] encodes not using the
                // variable
                int position = offset + currentCombination[i];
                binding.add(admissibleBindingsSortedByVariable.get(position));
            }
            offset += bindingsCountPerVariable[i];
        }
        return binding;
    }

    private void initialize() {
        admissibleBindingsSortedByVariable = new ArrayList<>();
        admissibleBindingsSortedByVariable.addAll(admissibleBindings);
        admissibleBindingsSortedByVariable.sort(new Comparator<VariableBinding>() {
            @Override
            public int compare(VariableBinding o1, VariableBinding o2) {
                int cmp = o1.getVariable().toString().compareTo(o2.getVariable().toString());
                if (cmp == 0) {
                    cmp = o1.getBoundNode().toString().compareTo(o2.getBoundNode().toString());
                }
                return cmp;
            }
        });
        bindingsCountPerVariable = countBindingsPerVariable(admissibleBindingsSortedByVariable);
        numberOfVariables = bindingsCountPerVariable.length;
        currentCombination = new int[numberOfVariables];
        boundVariables = new boolean[numberOfVariables];
        for (int i = 0; i < numberOfVariables; i++) {
            boundVariables[i] = true;
        }
    }

    private int[] countBindingsPerVariable(
                    List<VariableBinding> admissibleBindingsSortedByVariable) {
        List<Integer> bindingsCountPerVariable = new ArrayList<>();
        Node lastVariable = null;
        int bindingsCountForLastVariable = 0;
        for (int i = 0; i < admissibleBindingsSortedByVariable.size(); i++) {
            VariableBinding currentBinding = admissibleBindingsSortedByVariable.get(i);
            if (lastVariable == null) {
                lastVariable = currentBinding.getVariable();
            } else if (!lastVariable.equals(currentBinding.getVariable())) {
                bindingsCountPerVariable.add(bindingsCountForLastVariable);
                bindingsCountForLastVariable = 0;
                lastVariable = currentBinding.getVariable();
            }
            bindingsCountForLastVariable++;
        }
        bindingsCountPerVariable.add(bindingsCountForLastVariable);
        int[] bindingsCountPerVariableArray = new int[bindingsCountPerVariable.size()];
        for (int i = 0; i < bindingsCountPerVariableArray.length; i++) {
            bindingsCountPerVariableArray[i] = bindingsCountPerVariable.get(i);
        }
        return bindingsCountPerVariableArray;
    }

    /**
     * Advances the combination array to the next combination. If the current state
     * of the array represents the last combination, the array is set to null;
     */
    private void makeNextCombination() {
        if (boundVariables == null) {
            return;
        }
        for (int varIndex = 0; varIndex < bindingsCountPerVariable.length; varIndex++) {
            if (boundVariables[varIndex]) {
                int bindingsCount = bindingsCountPerVariable[varIndex];
                if (currentCombination[varIndex] + 1 > bindingsCount - 1) {
                    currentCombination[varIndex] = 0;
                } else {
                    currentCombination[varIndex]++;
                    return;
                }
            }
        }
        // we have iterated through all possibilities, set array to null
        currentCombination = null;
    }

    private void makeNextVariableSelection() {
        if (boundVariables == null) {
            return;
        }
        // find out how many zeros we use (how many vars are unbound)
        int numZeroes = 0;
        for (int i = 0; i < numberOfVariables; i++) {
            if (!boundVariables[i]) {
                numZeroes++;
            }
        }
        if (numZeroes == 0) {
            // put fir
            boundVariables[numberOfVariables - 1] = false;
            return;
        }
        int seenZeroes = 0;
        // change the boundVariables array to make next selection
        for (int i = 0; i < numberOfVariables; i++) {
            if (!boundVariables[i]) {
                // found a 0
                seenZeroes++;
                if (i > 0) {
                    // not at left end - might be movable
                    if (boundVariables[i - 1]) {
                        // can be moved left, so move
                        boundVariables[i - 1] = false;
                        boundVariables[i] = true;
                        return;
                    }
                }
            }
            if (seenZeroes == numZeroes && seenZeroes == i + 1) {
                // numZeroes 0 at left side
                if (numZeroes >= numberOfVariables - 1) {
                    // we have exhausted all combinations, signal by setting null
                    boundVariables = null;
                    return;
                } else {
                    // add a zero, put all 0s to right side
                    for (int k = 0; k < numberOfVariables; k++) {
                        boundVariables[k] = k < (numberOfVariables - numZeroes - 1);
                    }
                    return;
                }
            }
        }
        boundVariables = null;
    }

    private void makeNextBindingCombination() {
        makeNextCombination();
        if (currentCombination == null) {
            currentCombination = new int[numberOfVariables];
            makeNextVariableSelection();
        }
    }
}
