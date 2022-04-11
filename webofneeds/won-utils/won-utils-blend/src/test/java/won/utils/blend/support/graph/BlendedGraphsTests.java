package won.utils.blend.support.graph;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.other.G;
import org.apache.jena.sparql.core.DatasetGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import won.utils.blend.BLEND;
import won.utils.blend.EXVAR;
import won.utils.blend.Template;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.io.TemplateIO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
        Node varFirstName = EXVAR.uri("firstName");
        Node varLastName = EXVAR.uri("lastName");
        VariableBindings bindings = new VariableBindings(Set.of(varFirstName, varLastName), Set.of(
                        new VariableBinding(varFirstName, NodeFactory.createLiteral("Albert")),
                        new VariableBinding(varLastName, NodeFactory.createLiteral("Einstein"))));
        BlendedGraphs blended = new BlendedGraphs(
                        leftTemplate.getTemplateGraphs().getDataGraph(),
                        rightTemplate.getTemplateGraphs().getDataGraph(),
                        bindings);
        assertTrue(G.allSP(blended, varFirstName, BLEND.boundTo).stream().collect(Collectors.toUnmodifiableSet()).contains(NodeFactory.createLiteral("Albert")));
        assertTrue(G.allSP(blended, NodeFactory.createLiteral("Albert"), BLEND.boundTo).stream().count() == 0);

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
        VariableBindings bindings = new VariableBindings(
                        Set.of(NodeFactory.createURI("http://example.org/var#transferIntent")),
                        Set.of(
                                        new VariableBinding(
                                                        NodeFactory.createURI("http://example.org/var#transferIntent"),
                                                        NodeFactory.createURI(
                                                                        "http://example.org/ns#transferIntentAlice"))));
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
                        new VariableBindings(Set.of()));
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
