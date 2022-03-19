package won.utils.blend;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.RdfUtils;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class Utils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String NUMBERED_TEST_DIRECTORY_PATTERN = "test_(\\d+)";
    private static final String RESULT_OUTPUT_DIR = "target/testresults/";

    public static Graph readGraphFromClasspathResource(String path) {
        Graph result = GraphFactory.createJenaDefaultGraph();
        RDFDataMgr.read(result, MethodHandles.lookup().lookupClass().getClassLoader().getResourceAsStream(path),
                        Lang.TTL);
        return result;
    }

    public static List<File> getSortedUnittests(String testParentFolder) {
        File unitTestTopDir = findFolder(testParentFolder);
        File[] unitTestDirs = unitTestTopDir
                        .listFiles((FileFilter) new RegexFileFilter(NUMBERED_TEST_DIRECTORY_PATTERN));
        Pattern sortpattern = Pattern.compile(NUMBERED_TEST_DIRECTORY_PATTERN);
        List<File> sortedTests = Arrays.stream(unitTestDirs).sorted((f1, f2) -> {
            Matcher m1 = sortpattern.matcher(f1.getName());
            Matcher m2 = sortpattern.matcher(f2.getName());
            m1.matches();
            m2.matches();
            int num1 = Integer.parseInt(m1.group(1));
            int num2 = Integer.parseInt(m2.group(1));
            return num1 - num2;
        }).collect(Collectors.toList());
        return sortedTests;
    }

    public static File findFolder(String testParentFolder) {
        URL folder = MethodHandles.lookup().lookupClass().getClassLoader().getResource(testParentFolder);
        File unitTestTopDir = new File(folder.getPath());
        return unitTestTopDir;
    }

    public static void writeTestResultToFile(DatasetGraph resultAsDataset, String outfile)
                    throws FileNotFoundException {
        File outputFile = new File(outfile);
        outputFile.getParentFile().mkdirs();
        RDFDataMgr.write(new FileOutputStream(outputFile), DatasetFactory.wrap(resultAsDataset), RDFFormat.TRIG_PRETTY);
        logger.info("wrote test result to " + outputFile.getAbsolutePath());
    }

    public static String getOutputFileName(Class<?> callingTestClass, String testIdentifier) {
        return RESULT_OUTPUT_DIR + callingTestClass.getSimpleName() + "/actualResult_" + testIdentifier + ".trig";
    }

    public static DatasetGraph readDatasetGraphFromFile(File inputLeftFile) throws FileNotFoundException {
        DatasetGraph left = DatasetGraphFactory.create();
        RDFDataMgr.read(left, new FileInputStream(inputLeftFile), Lang.TRIG);
        return left;
    }

    public static File getFileWithSuffixIfExists(File file, String suffix)  {
        File withSuffix = new File(file.getParentFile(), file.getName().replaceFirst("\\.", suffix + "."));
        if (withSuffix.exists() && withSuffix.canRead()){
            return withSuffix;
        }
        return file;
    }

    public static void assertGraphsAreIsomorphic(Graph expectedResult, Graph actualResult, String testIdentifier) {
        if (!expectedResult.isIsomorphicWith(actualResult)) {
            System.err.println("Test failed: " + testIdentifier);
            RdfUtils.Pair<Graph> diff = RdfUtils.diff(expectedResult, actualResult);
            Graph onlyInExpected = diff.getFirst();
            Graph onlyInActual = diff.getSecond();
            if (!onlyInExpected.isEmpty()) {
                System.err.println("\nThese expected triples are missing from the actual data:\n");
                RDFDataMgr.write(System.err, onlyInExpected, Lang.TTL);
            }
            if (!onlyInActual.isEmpty()) {
                System.err.println("\nThese unexpected triples should not be in actual data:\n");
                RDFDataMgr.write(System.err, onlyInActual, Lang.TTL);
            }
            System.err.println("\nExpected data:\n");
            RDFDataMgr.write(System.err, expectedResult, Lang.TTL);
            System.err.println("\nActual data:\n");
            RDFDataMgr.write(System.err, actualResult, Lang.TTL);
            Assertions.fail(testIdentifier + ": resulting graph differs from expected");
        }
    }

    public static void assertDatasetsEqual(DatasetGraph expectedResult, DatasetGraph actualResult,
                    String testIdentifier) {
        actualResult.getDefaultGraph().getPrefixMapping()
                        .setNsPrefixes(expectedResult.getDefaultGraph().getPrefixMapping());
        Iterator<Node> graphNodes = actualResult.listGraphNodes();
        while (graphNodes.hasNext()) {
            Node graphNode = graphNodes.next();
            Graph expectedGraph = expectedResult.getGraph(graphNode);
            if (expectedGraph == null) {
                Assertions.fail(String.format("Unexpected graph %s found in actual result"));
            }
            assertGraphsAreIsomorphic(expectedGraph, actualResult.getGraph(graphNode), testIdentifier);
        }
        graphNodes = expectedResult.listGraphNodes();
        while (graphNodes.hasNext()) {
            Node graphNode = graphNodes.next();
            if (!actualResult.containsGraph(graphNode)) {
                Assertions.fail(String.format("Expected graph %s not found in actual result", graphNode));
            }
        }
    }
}
