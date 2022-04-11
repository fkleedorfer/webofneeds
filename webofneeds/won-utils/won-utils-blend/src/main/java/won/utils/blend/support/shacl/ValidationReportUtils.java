package won.utils.blend.support.shacl;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ValidationReport;

import java.io.StringWriter;

public class ValidationReportUtils {
    public static String toString(ValidationReport generateReport) {
        StringWriter out = new StringWriter();
        RDFDataMgr.write(out, generateReport.getModel(), Lang.TTL);
        return out.toString();
    }
}
