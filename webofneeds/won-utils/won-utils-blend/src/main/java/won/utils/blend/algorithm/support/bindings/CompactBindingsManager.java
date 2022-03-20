package won.utils.blend.algorithm.support.bindings;

import org.apache.jena.graph.Node;
import won.utils.blend.BLEND;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CompactBindingsManager {
    private int[] optionCountPerVariable;
    private int numberOfVariables;
    private List<VariableBinding> admissibleBindingsSortedByVariable;
    private Node[] variables;

    public CompactBindingsManager(Set<VariableBinding> admissibleBindings) {
        initialize(admissibleBindings);
    }

    public CompactBindingsManager removeOptions(Set<VariableBinding> toRemove) {
        Set<VariableBinding> newAdmissibleBindings = new HashSet<>(admissibleBindingsSortedByVariable);
        newAdmissibleBindings.removeAll(toRemove);
        Set<Node> newVariables = newAdmissibleBindings.stream().map(b -> b.getVariable()).collect(Collectors.toSet());
        for (Node oldVariable : variables) {
            if (!newVariables.contains(oldVariable)) {
                newAdmissibleBindings.add(new VariableBinding(oldVariable, BLEND.unbound));
            }
        }
        return new CompactBindingsManager(newAdmissibleBindings);
    }

    public int[] getZeroBindings() {
        return new int[numberOfVariables];
    }

    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    public Set<int[]> allBindingsOfVariableIncludingUnbound(int[] bindings, int variableIndex) {
        Set<int[]> result = new HashSet<>();
        if (variableIndex < 0 || variableIndex > numberOfVariables) {
            throw new IndexOutOfBoundsException("Variable index " + variableIndex + " is out of bounds. There are "
                            + numberOfVariables + " variables, indexing is 0-based");
        }
        for (int i = 0; i <= optionCountPerVariable[variableIndex]; i++) {
            int[] cur = new int[numberOfVariables];
            System.arraycopy(bindings, 0, cur, 0, bindings.length);
            cur[variableIndex] = i;
            result.add(cur);
        }
        return result;
    }

    public Stream<int[]> addOneBinding(int[] currentBindings) {
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<int[]>(Long.MAX_VALUE,
                        Spliterator.ORDERED
                                        | Spliterator.NONNULL
                                        | Spliterator.DISTINCT
                                        | Spliterator.IMMUTABLE) {
            private int varIndex = -1;
            private int bindingIndex = 1;
            private boolean finished = false;
            private int firstUnboundVariableIndex = findFirstUnboundVariable(currentBindings);
            private int lastUnboundVariableIndex = findLastUnboundVariable(currentBindings);

            @Override
            public boolean tryAdvance(Consumer<? super int[]> action) {
                if (finished) {
                    return false;
                }
                if (firstUnboundVariableIndex == -1) {
                    finished = true;
                    return false;
                }
                int[] nextCombination = new int[numberOfVariables];
                System.arraycopy(currentBindings, 0, nextCombination, 0, currentBindings.length);
                if (varIndex == -1) {
                    varIndex = firstUnboundVariableIndex;
                } else {
                    if (varIndex > lastUnboundVariableIndex) {
                        finished = true;
                        return false;
                    }
                }
                nextCombination[varIndex] = bindingIndex;
                action.accept(nextCombination);
                bindingIndex++;
                if (bindingIndex > optionCountPerVariable[varIndex]) {
                    varIndex = findNextUnboundVariable(currentBindings, varIndex);
                    bindingIndex = 1;
                }
                return true;
            }

            private int findNextUnboundVariable(int[] currentBindings, int variableIndex) {
                for (int i = variableIndex + 1; i < currentBindings.length; i++) {
                    if (currentBindings[i] == 0) {
                        return i;
                    }
                }
                return Integer.MAX_VALUE;
            }

            private int findFirstUnboundVariable(int[] currentBindings) {
                for (int i = 0; i < currentBindings.length; i++) {
                    if (currentBindings[i] == 0) {
                        return i;
                    }
                }
                return -1;
            }

            private int findLastUnboundVariable(int[] currentBindings) {
                for (int i = currentBindings.length - 1; i >= 0; i--) {
                    if (currentBindings[i] == 0) {
                        return i;
                    }
                }
                return -1;
            }
        }, false);
    }

    public Set<VariableBinding> toBindings(int[] bindingsArray) {
        Set<VariableBinding> binding = new HashSet<>();
        int offset = 0;
        for (int i = 0; i < numberOfVariables; i++) {
            int varBinding = bindingsArray[i];
            if (varBinding > 0) {
                int position = offset + varBinding - 1;
                binding.add(admissibleBindingsSortedByVariable.get(position));
            }
            offset += optionCountPerVariable[i];
        }
        return binding;
    }

    private Node getOptionValue(int variable, int optionIndex) {
        int offset = 0;
        for (int i = 0; i < numberOfVariables; i++) {
            if (optionIndex > 0 && variable == i) {
                int position = offset + optionIndex - 1;
                return admissibleBindingsSortedByVariable.get(position).getBoundNode();
            }
            offset += optionCountPerVariable[i];
        }
        return null;
    }

    public CompactVariableBindings fromBindings(Set<VariableBinding> bindings) {
        return new CompactVariableBindings(arrayFromBindings(bindings), this);
    }

    private int[] arrayFromBindings(Set<VariableBinding> bindings) {
        int[] bindingsArray = new int[numberOfVariables];
        Map<Integer, Node> valuesMap = new HashMap<>();
        for (VariableBinding binding : bindings) {
            Node variable = binding.getVariable();
            int varInd = getIndexOfVariable(variable);
            valuesMap.put(varInd, binding.getBoundNode());
        }
        int offset = 0;
        for (int varInd = 0; varInd < numberOfVariables; varInd++) {
            Node boundNode = valuesMap.get(varInd);
            int optionCount = optionCountPerVariable[varInd];
            if (boundNode != null) {
                for (int optionInd = 0; optionInd < optionCount; optionInd++) {
                    Node optionValue = admissibleBindingsSortedByVariable.get(offset + optionInd).getBoundNode();
                    if (boundNode.equals(optionValue)) {
                        bindingsArray[varInd] = optionInd + 1;
                        break;
                    }
                }
            }
            offset += optionCount;
        }
        return bindingsArray;
    }

    public Set<VariableBinding> getOptions(int varIndex) {
        Set<VariableBinding> bindings = new HashSet<>();
        int offset = 0;
        for (int i = 0; i < numberOfVariables; i++) {
            if (i == varIndex) {
                bindings.addAll(admissibleBindingsSortedByVariable.subList(offset, offset + optionCountPerVariable[i]));
            }
            offset += optionCountPerVariable[i];
        }
        return bindings;
    }

    public int[] getOptionCountPerVariable() {
        int[] ret = new int[this.optionCountPerVariable.length];
        System.arraycopy(this.optionCountPerVariable, 0, ret, 0, this.optionCountPerVariable.length);
        return ret;
    }

    public Optional<VariableBinding> getBinding(int[] bindingsArray, int varIndex) {
        int offset = 0;
        for (int i = 0; i < numberOfVariables; i++) {
            if (i == varIndex) {
                int varBinding = bindingsArray[i];
                if (varBinding > 0) {
                    int position = offset + bindingsArray[varIndex] - 1;
                    return Optional.of(admissibleBindingsSortedByVariable.get(position));
                } else {
                    return Optional.empty();
                }
            }
            offset += optionCountPerVariable[i];
        }
        throw new ArrayIndexOutOfBoundsException("Could not obtain binding at index " + varIndex
                        + " in bindings array of length " + bindingsArray.length);
    }

    private void initialize(Set<VariableBinding> admissibleBindings) {
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
        optionCountPerVariable = countBindingsPerVariable(admissibleBindingsSortedByVariable);
        numberOfVariables = optionCountPerVariable.length;
        variables = new Node[numberOfVariables];
        int offset = 0;
        for (int varInd = 0; varInd < numberOfVariables; varInd++) {
            variables[varInd] = admissibleBindingsSortedByVariable.get(offset).getVariable();
            offset += optionCountPerVariable[varInd];
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

    public boolean isValidVariableIndex(int nextVarIndex) {
        return nextVarIndex < numberOfVariables;
    }

    public int countBindings(int[] bindings) {
        int cnt = 0;
        for (int i = 0; i < bindings.length; i++) {
            if (bindings[i] > 0) {
                cnt++;
            }
        }
        return cnt;
    }

    public boolean isAdmissibleBinding(Node variable, Node boundValue) {
        return admissibleBindingsSortedByVariable.stream()
                        .anyMatch(vb -> vb.getVariable().equals(variable) && vb.getBoundNode().equals(boundValue));
    }

    public int[] copyAndSetBinding(int[] bindings, Node variable, Node boundNode) {
        int offset = 0;
        for (int varIndex = 0; varIndex < numberOfVariables; varIndex++) {
            if (admissibleBindingsSortedByVariable.get(offset).getVariable().equals(variable)) {
                int[] result = new int[bindings.length];
                System.arraycopy(bindings, 0, result, 0, bindings.length);
                if (boundNode == null) {
                    result[varIndex] = 0;
                    return result;
                }
                for (int bindingIndex = 0; bindingIndex < optionCountPerVariable[varIndex]; bindingIndex++) {
                    VariableBinding binding = admissibleBindingsSortedByVariable.get(offset + bindingIndex);
                    if (binding.getVariable().equals(variable) && binding.getBoundNode().equals(boundNode)) {
                        result[varIndex] = bindingIndex + 1;
                        return result;
                    }
                }
            } else {
                offset += optionCountPerVariable[varIndex];
            }
        }
        throw new IllegalArgumentException(
                        "Could not set binding " + variable + " -> " + boundNode + " in binding array" + bindings);
    }

    public int[] copyAndRemoveBinding(int[] bindings, Node variable) {
        return copyAndSetBinding(bindings, variable, null);
    }

    /**
     * Returns the index of the variable or -1 if there is no admissible binding for
     * the variable.
     * 
     * @param variableNode
     * @return
     */
    public int getIndexOfVariable(Node variableNode) {
        for (int i = 0; i < variables.length; i++) {
            if (variables[i].equals(variableNode)) {
                return i;
            }
        }
        throw new IllegalArgumentException(String.format("Specified variable %s not in variables %s", variableNode,
                        Arrays.toString(variables)));
    }

    public CompactVariables getCompactVariables(Collection<Node> variableNodes) {
        return new CompactVariables(getIndicesOfVariables(variableNodes), this);
    }

    private int[] getIndicesOfVariables(Collection<Node> variableNodes) {
        int[] indices = new int[variableNodes.size()];
        int i = 0;
        for (Node variableNode : variableNodes) {
            indices[i++] = getIndexOfVariable(variableNode);
        }
        return indices;
    }

    public int getNumberOfOptionsForVariable(int varIndex) {
        return optionCountPerVariable[varIndex] - 1;
    }

    public CombinationBuilder combinationBuilder() {
        return new CombinationBuilder();
    }

    public boolean isBoundVariable(int[] bindings, Node variable) {
        return isBoundVariable(bindings, getIndexOfVariable(variable));
    }

    public boolean isBoundVariable(int[] bindings, int varIndex) {
        return bindings[varIndex] > 0;
    }

    public Set<Node> getVariables() {
        return Arrays.stream(variables).collect(Collectors.toSet());
    }

    public Set<Node> getVariables(int[] variableIndices) {
        Set<Node> result = new HashSet<>();
        for (int i = 0; i < variableIndices.length; i++) {
            result.add(variables[variableIndices[i]]);
        }
        return result;
    }

    /**
     * Returns true iff the bindings overlap but are not the same.
     * 
     * @param leftBindings
     * @param rightBindings
     * @return
     */
    public boolean isBindingsOverlap(int[] leftBindings, int[] rightBindings) {
        if (leftBindings.length != rightBindings.length) {
            throw new IllegalArgumentException("Cannot compare bindings of different lengths");
        }
        boolean valuesDistinct = false;
        boolean valuesShared = false;
        for (int i = 0; i < leftBindings.length; i++) {
            int left = leftBindings[i];
            int right = rightBindings[i];
            if (left != right) {
                if (left == 0 || right == 0) {
                    valuesDistinct = true;
                } else {
                    return false; // conflict - neither left nor right are 0 but different
                }
            } else if (left != 0) {
                valuesShared = true;
            }
        }
        return valuesShared && valuesDistinct;
    }

    public boolean isBindingsConflict(int[] leftBindings, int[] rightBindings) {
        if (leftBindings.length != rightBindings.length) {
            throw new IllegalArgumentException("Cannot compare bindings of different lengths");
        }
        for (int i = 0; i < leftBindings.length; i++) {
            int left = leftBindings[i];
            int right = rightBindings[i];
            if (left != right && left != 0 && right != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isAllUnbound(int[] bindings) {
        for (int i = 0; i < bindings.length; i++) {
            if (bindings[i] > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isAnyVariableBound(int[] bindings, int[] variables) {
        for (int i = 0; i < variables.length; i++) {
            if (bindings[variables[i]] > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyVariableBound(int[] bindings, Set<Node> variables) {
        return isAnyVariableBound(bindings, getIndicesOfVariables(variables));
    }

    public CompactVariables removeFromVariablesIfBound(int[] variables, int[] bindings) {
        int[] newVars = new int[variables.length];
        int newVarInd = 0;
        for (int i = 0; i < variables.length; i++) {
            if (bindings[variables[i]] == 0) {
                newVars[newVarInd++] = variables[i];
            }
        }
        int[] actualNewVars = new int[newVarInd];
        System.arraycopy(newVars, 0, actualNewVars, 0, newVarInd);
        return new CompactVariables(actualNewVars, this);
    }

    public CompactVariables removeFromVariablesIfBound(int[] variables, Set<VariableBinding> bindings) {
        return removeFromVariablesIfBound(variables, this.arrayFromBindings(bindings));
    }

    public class CombinationBuilder {
        private int[] combination;

        private CombinationBuilder() {
            this.combination = new int[numberOfVariables];
            Arrays.fill(this.combination, -1);
        }

        public CombinationBuilder set(Node variable, Node value) {
            this.combination = copyAndSetBinding(this.combination, variable, value);
            return this;
        }

        public int[] build() {
            return this.combination;
        }
    }

    public boolean isAdmissibleVariableBindings(CompactVariableBindings bindings) {
        return this.admissibleBindingsSortedByVariable.containsAll(bindings.getBindingsAsSet().stream()
                        .filter(b -> !b.getBoundNode().equals(BLEND.unbound)).collect(
                                        Collectors.toSet()));
    }

    public boolean isAdmissibleVariables(CompactVariables variables) {
        Set<Node> vars = variables.getVariables();
        return Arrays.stream(this.variables).collect(Collectors.toSet()).containsAll(vars);
    }

    public CompactVariableBindings withThisManager(CompactVariableBindings bindings) {
        if (bindings.hasManager(this)) {
            return bindings;
        }
        return fromBindings(bindings.getBindingsAsSet());
    }

    public CompactVariables withThisManager(CompactVariables variables) {
        if (variables.hasManager(this)) {
            return variables;
        }
        return getCompactVariables(variables.getVariables());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CompactBindingsManager that = (CompactBindingsManager) o;
        return admissibleBindingsSortedByVariable.equals(that.admissibleBindingsSortedByVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(admissibleBindingsSortedByVariable);
    }

    public Set<Node> getBindingOptions(Node variable) {
        int index = getIndexOfVariable(variable);
        return getOptions(index).stream().map(vb -> vb.getBoundNode()).collect(Collectors.toSet());
    }
}
