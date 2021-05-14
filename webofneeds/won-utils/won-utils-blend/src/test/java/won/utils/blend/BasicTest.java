package won.utils.blend;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class BasicTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ParameterizedTest
    @MethodSource
    public void testOk(File inputLeft, File inputRight) throws IOException {
        DatasetGraph left = DatasetGraphFactory.create();
        DatasetGraph right = DatasetGraphFactory.create();
        RDFDataMgr.read(left, new FileInputStream(inputLeft), Lang.TRIG);
        RDFDataMgr.read(right, new FileInputStream(inputRight), Lang.TRIG);
        Blendable leftBlendable = BlendableLoader.load(left).stream().findFirst().get();
        Blendable rightBlendable = BlendableLoader.load(right).stream().findFirst().get();
        Graph globalShapesGraph = readGraphFromClasspathResource("shapes/valueflows-shapes-notargets.ttl");
        Graph globalDataGraph = readGraphFromClasspathResource("ontologies/valueflows-and-required.ttl");
        Set<Blendable> results = new Blender(globalShapesGraph, globalDataGraph).blend(leftBlendable, rightBlendable);
        /*
         * for (Blendable result : results) {
         * System.out.println("========== result ============");
         * System.out.println("data:"); RDFDataMgr.write(System.out, result.dataGraph,
         * Lang.TTL); System.out.println("shapes:"); RDFDataMgr.write(System.out,
         * result.dataGraph, Lang.TTL); }
         */
        logger.debug("results: {}", results.size());
    }

    private Graph readGraphFromClasspathResource(String path) {
        Graph result = GraphFactory.createJenaDefaultGraph();
        RDFDataMgr.read(result, getClass().getClassLoader().getResourceAsStream(path), Lang.TTL);
        return result;
    }

    public static Stream<Arguments> testOk() throws IOException {
        URL folder = BasicTest.class.getClassLoader().getResource("basic/");
        File folderFile = new File(folder.getPath());
        File[] leftFiles = folderFile.listFiles((FileFilter) new RegexFileFilter("test\\d+_left\\.trig"));
        File[] rightFiles = folderFile.listFiles((FileFilter) new RegexFileFilter("test\\d+_right\\.trig"));
        Arguments[] args = new Arguments[leftFiles.length];
        for (int i = 0; i < leftFiles.length; i++) {
            args[i] = arguments(leftFiles[i], rightFiles[i]);
        }
        return Stream.of(args);
    }
}
