package won.utils.blend.support.graph;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import won.utils.blend.EXVAR;
import won.utils.blend.Template;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.io.TemplateIO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static won.utils.blend.Utils.*;

public class BlendedGraphsTests {
    private static final String TEST_DATA_FOLDER = "won/utils/blend/support/graph/BlendedGraphTests";

    @Test
    public void testBindings_b1()
                    throws FileNotFoundException {
        String testIdentifier = "test_b1";
        File parent = findFolder(TEST_DATA_FOLDER + "/bindings/" + testIdentifier);
        DatasetGraph leftDataset = readDatasetGraphFromFile(new File(parent, "left.trig"));
        DatasetGraph rightDataset = readDatasetGraphFromFile(new File(parent, "right.trig"));
        DatasetGraph expectedDataset = readDatasetGraphFromFile(new File(parent, "result.trig"));
        TemplateIO templateIO = new TemplateIO();
        Template leftTemplate = templateIO.fromDatasetGraph(leftDataset).stream().findAny().get();
        Template rightTemplate = templateIO.fromDatasetGraph(rightDataset).stream().findAny().get();
        Set<VariableBinding> bindings = Set.of(
                        new VariableBinding(EXVAR.uri("firstName"), NodeFactory.createLiteral("Albert")),
                        new VariableBinding(EXVAR.uri("lastName"), NodeFactory.createLiteral("Einstein")));
        BlendedGraphs blended = new BlendedGraphs(
                        leftTemplate.getTemplateGraphs().getDataGraph(),
                        rightTemplate.getTemplateGraphs().getDataGraph(),
                        bindings);
        RDFDataMgr.write(System.out, blended, Lang.TTL);
        assertGraphsAreIsomorphic(expectedDataset.getDefaultGraph(), blended, testIdentifier);
    }

    @Test
    public void testBindings_b2()
                    throws FileNotFoundException {
        String testIdentifier = "test_b2";
        File parent = findFolder(TEST_DATA_FOLDER + "/bindings/" + testIdentifier);
        DatasetGraph leftDataset = readDatasetGraphFromFile(new File(parent, "left.trig"));
        DatasetGraph rightDataset = readDatasetGraphFromFile(new File(parent, "right.trig"));
        DatasetGraph expectedDataset = readDatasetGraphFromFile(new File(parent, "result.trig"));
        TemplateIO templateIO = new TemplateIO();
        Template leftTemplate = templateIO.fromDatasetGraph(leftDataset).stream().findAny().get();
        Template rightTemplate = templateIO.fromDatasetGraph(rightDataset).stream().findAny().get();
        Set<VariableBinding> bindings = Set.of(
                        new VariableBinding(
                                        NodeFactory.createURI("http://example.org/var#transferIntent"),
                                        NodeFactory.createURI("http://example.org/ns#transferIntentAlice")));
        BlendedGraphs blended = new BlendedGraphs(
                        leftTemplate.getTemplateGraphs().getDataGraph(),
                        rightTemplate.getTemplateGraphs().getDataGraph(),
                        bindings);
        RDFDataMgr.write(System.out, blended, Lang.TTL);
        assertGraphsAreIsomorphic(expectedDataset.getDefaultGraph(), blended, testIdentifier);
    }

    @ParameterizedTest
    @MethodSource
    public void testWithoutBindings(File leftFile, File rightFile, File expectedResultFile)
                    throws FileNotFoundException {
        DatasetGraph leftDataset = readDatasetGraphFromFile(leftFile);
        DatasetGraph rightDataset = readDatasetGraphFromFile(rightFile);
        DatasetGraph expectedDataset = readDatasetGraphFromFile(expectedResultFile);
        TemplateIO templateIO = new TemplateIO();
        Template leftTemplate = templateIO.fromDatasetGraph(leftDataset).stream().findAny().get();
        Template rightTemplate = templateIO.fromDatasetGraph(rightDataset).stream().findAny().get();
        BlendedGraphs blended = new BlendedGraphs(
                        leftTemplate.getTemplateGraphs().getDataGraph(),
                        rightTemplate.getTemplateGraphs().getDataGraph(),
                        Collections.emptySet());
        RDFDataMgr.write(System.out, blended, Lang.TTL);
    }

    public static Stream<Arguments> testWithoutBindings() throws IOException {
        List<File> sortedUnittests = getSortedUnittests(TEST_DATA_FOLDER + "/nobindings");
        Arguments[] args = new Arguments[sortedUnittests.size()];
        return sortedUnittests.stream().map(unittestDir -> {
            File leftFile = new File(unittestDir, "left.trig");
            File rightFile = new File(unittestDir, "right.trig");
            File resultFile = new File(unittestDir, "result.trig");
            return arguments(leftFile, rightFile, resultFile);
        });
    }
}
