package won.utils.blend.algorithm.sat.shacl;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.shacl.Shapes;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.Set;

public class ExtractionContext {
    public final IndentedWriter indentedWriter;
    public final Set<VariableBinding> forbiddenBindings;
    public final Shapes shapes;

    public ExtractionContext(Shapes shapes, Set<VariableBinding> forbiddenBindings, IndentedWriter indentedWriter) {
        this.indentedWriter = indentedWriter;
        this.forbiddenBindings = forbiddenBindings;
        this.shapes = shapes;
    }
}
