package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Node;
import org.junit.jupiter.api.Test;
import won.utils.blend.EX;
import won.utils.blend.EXVAR;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BindingCombinatorTest {
    @Test
    public void testSimple() {
        Node varA = EXVAR.uri("a"), varB = EXVAR.uri("b");
        Node valA1 = EX.uri("a1"), valA2 = EX.uri("a2"), vala3 = EX.uri("a3"), valA4 = EX.uri("a4");
        Node valB1 = EX.uri("b1"), valB2 = EX.uri("b2"), valB3 = EX.uri("b3");
        Set<Node> vars = Set.of(varA, varB);
        Map<Node, Set<Node>> options = Map.of(varA, Set.of(valA1, valA2, vala3, valA4), varB,
                        Set.of(valB1, valB2, valB3));
        VariableBindings bindings = new VariableBindings(vars);
        List<VariableBindings> allcombinations = BindingCombinator
                        .allCombinationsAsStream(node -> options.get(node), vars, bindings)
                        .collect(Collectors.toList());
        System.out.println(allcombinations.stream().map(Objects::toString).collect(Collectors.joining("\n")));
        assertEquals(12, allcombinations.size());
    }
}
