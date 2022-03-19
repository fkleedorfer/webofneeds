package won.utils.blend.algorithm.sat.shacl;

import won.utils.blend.algorithm.sat.support.Ternary;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static won.utils.blend.algorithm.sat.support.Ternary.*;

public class CheckedBindings implements Comparable<CheckedBindings> {
    public final List<VariableBinding> bindings;
    public final Ternary valid;

    public CheckedBindings(List<VariableBinding> bindings, Ternary valid) {
        this.bindings = bindings.stream().collect(Collectors.toUnmodifiableList());
        this.valid = valid;
    }

    @Override
    public int compareTo(CheckedBindings o) {
        int cmp = 0;
        cmp = this.bindings.size() - o.bindings.size();
        for (int i = 0; i < this.bindings.size() && i < o.bindings.size() && cmp == 0; i++) {
            cmp = this.bindings.get(i).compareTo(o.bindings.get(i));
        }
        if (cmp == 0) {
            return Integer.compare(this.bindings.size(), o.bindings.size());
        }
        if (cmp == 0) {
            return this.valid.compareTo(o.valid);
        }
        return cmp;
    }

    public static Collection<Ternary> getValidities(Collection<CheckedBindings> states) {
        return states.stream()
                        .map(cb -> cb.valid)
                        .collect(Collectors.toList());
    }

    public static CheckedBindings valid(List<VariableBinding> bindings) {
        return new CheckedBindings(bindings, TRUE);
    }

    public static CheckedBindings invalid(List<VariableBinding> bindings) {
        return new CheckedBindings(bindings, FALSE);
    }

    public static CheckedBindings unknown(List<VariableBinding> bindings) {
        return new CheckedBindings(bindings, UNKNOWN);
    }

    public CheckedBindings invertValidity() {
        return new CheckedBindings(bindings, not(valid));
    }

    public boolean hasOppositeValidityOf(CheckedBindings other) {
        return knownAndDifferent(this.valid, other.valid) && hasSameBindings(other);
    }

    public boolean hasSameBindings(CheckedBindings other) {
        return Objects.deepEquals(this.bindings, other.bindings);
    }

    public boolean isStrongerValidityThan(CheckedBindings other) {
        return isKnown(this.valid) && isUnknown(other.valid) && hasSameBindings(other);
    }

    public boolean hasKnownValidity() {
        return isKnown(this.valid);
    }

    public boolean isKnownInvalid() {
        return this.valid == FALSE;
    }

    public boolean isKnownValid() {
        return this.valid == TRUE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckedBindings that = (CheckedBindings) o;
        return valid == that.valid &&
                        Objects.equals(bindings, that.bindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindings, valid);
    }

    @Override
    public String toString() {
        return "CheckedBindings{" +
                        "\n\tvalid=" + valid +
                        "\n\tbindings="
                        + bindings.stream().map(Object::toString).collect(joining("\n\t\t", "\n\t\t", "")) +
                        '}';
    }
}
