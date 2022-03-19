package won.utils.blend.support.graph.formatter;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import won.utils.blend.support.graph.TemplateGraphs;

import java.io.StringWriter;

public class DefaultTemplateGraphsFormatter implements TemplateGraphsFormatter {
    private static final DefaultTemplateGraphsFormatter instance = new DefaultTemplateGraphsFormatter();

    public static DefaultTemplateGraphsFormatter getInstance() {
        return instance;
    }

    @Override
    public String format(TemplateGraphs templateGraphs) {
        StringWriter writer = new StringWriter();
        writer.write("----------- BEGIN TEMPLATE ------\n");
        if (templateGraphs.getDataGraph() != null) {
            writer.write("........... Data Graph ..........\n");
            RDFDataMgr.write(writer, templateGraphs.getDataGraph(), Lang.TTL);
        }
        if (templateGraphs.getShapesGraph() != null) {
            writer.write("----------------------------------------------------------------------------------------\n");
            writer.write("........... Shapes Graph ..........\n");
            RDFDataMgr.write(writer, templateGraphs.getShapesGraph(), Lang.TTL);
        }
        writer.write("---------- END TEMPLATE ---------\n");
        return writer.toString();
    }
}
