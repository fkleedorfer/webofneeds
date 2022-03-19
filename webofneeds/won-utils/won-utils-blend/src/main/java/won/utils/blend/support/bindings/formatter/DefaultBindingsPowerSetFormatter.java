package won.utils.blend.support.bindings.formatter;

import org.apache.jena.graph.Node;
import won.utils.blend.support.bindings.TemplateBindings;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultBindingsPowerSetFormatter implements BindingsPowerSetFormatter {
    private static final DefaultBindingsPowerSetFormatter instance = new DefaultBindingsPowerSetFormatter();

    public static DefaultBindingsPowerSetFormatter getInstance() {
        return instance;
    }

    @Override
    public String format(Set<TemplateBindings> bindingsSet) {
        StringWriter writer = new StringWriter();
        writer.write(bindingsSet.size() + " Bindings\n");
        Set<Node> variables = bindingsSet.stream().flatMap(b -> b.getVariables().stream()).collect(Collectors.toSet());
        Map<Node, Set<Node>> bindingsByVariable = bindingsSet.stream()
                        .flatMap(b -> b.getBindings().getBindingsAsSet().stream())
                        .collect(Collectors.toMap(v -> v.getVariable(), v -> Set.of(v.getBoundNode()), (l, r) -> {
                            Set<Node> both = new HashSet<>(l);
                            both.addAll(r);
                            return both;
                        }));
        bindingsByVariable.entrySet().forEach(e -> {
            writer.write(e.getKey().toString());
            writer.write(" => ");
            writer.write(e.getValue().stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]")));
            writer.write("\n");
        });
        writer.write("All bindings");
        writer.write(bindingsSet.toString());
        return writer.toString();
    }
}
