package won.utils.blend.algorithm.sat.shacl;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.Shapes;
import won.utils.blend.Template;

public class TemplateWithGlobalData {
    public final Template template;
    public final Shapes shapes;
    public final Graph data;

    public TemplateWithGlobalData(Template template, Shapes shapes, Graph data) {
        this.template = template;
        this.shapes = shapes;
        this.data = data;
    }
}
