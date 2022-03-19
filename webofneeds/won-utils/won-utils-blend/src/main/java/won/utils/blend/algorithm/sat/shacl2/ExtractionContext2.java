package won.utils.blend.algorithm.sat.shacl2;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.shacl.Shapes;

public class ExtractionContext2 {
    public final IndentedWriter indentedWriter;
    public final ForbiddenBindings forbiddenBindings;
    public final Shapes shapes;

    public ExtractionContext2(Shapes shapes, ForbiddenBindings forbiddenBindings, IndentedWriter indentedWriter) {
        this.indentedWriter = indentedWriter;
        this.forbiddenBindings = forbiddenBindings;
        this.shapes = shapes;
    }
}
