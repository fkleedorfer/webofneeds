package won.utils.blend;

import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.core.DatasetGraph;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.utils.blend.algorithm.BlendingAlgorithm;
import won.utils.blend.algorithm.join.AlgorithmState;
import won.utils.blend.algorithm.join.JoiningAlgorithm;
import won.utils.blend.algorithm.support.BlendingBackground;
import won.utils.blend.algorithm.support.BlendingOperations;
import won.utils.blend.support.io.TemplateIO;
import won.utils.blend.support.stats.BindingResultStatsAccumulator;
import won.utils.blend.support.stats.formatter.DefaultBlendingResultStatsFormatter;
import won.utils.blend.support.uuid.SequentialUUIDSource;
import won.utils.blend.support.uuid.UUIDSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static won.utils.blend.Utils.*;

public class BlendingTests {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ParameterizedTest
    @MethodSource
    public void testFocused(File inputLeftFile, File inputRightFile, File expectedOutputFile) throws IOException {
        testOkAllBound(inputLeftFile, inputRightFile, expectedOutputFile);
    }

    @ParameterizedTest
    @MethodSource
    public void testOkAllBound(File inputLeftFile, File inputRightFile, File expectedOutputFile) throws IOException {
        BlendingOptions allBoundOptions = new BlendingOptions(
                        UnboundHandlingMode.ALL_BOUND,
                        true,
                        false,
                        null,
                        null, AlgorithmState.Verbosity.TRACE);
        testOk(inputLeftFile, inputRightFile, expectedOutputFile, "_allbound", allBoundOptions);
    }

    @ParameterizedTest
    @MethodSource
    @Disabled // unrestricted, it just takes too long
    public void testOkAllowUnbound(File inputLeftFile, File inputRightFile, File expectedOutputFile)
                    throws IOException {
        BlendingOptions allowUnboundOptions = new BlendingOptions(
                        UnboundHandlingMode.ALLOW_UNBOUND,
                        true,
                        false,
                        null,
                        null, AlgorithmState.Verbosity.ERROR);
        testOk(inputLeftFile, inputRightFile, expectedOutputFile, "_allowUnbound", allowUnboundOptions);
    }

    @ParameterizedTest
    @MethodSource
    public void testOkUnboundAllowedIfNoOtherBinding(File inputLeftFile, File inputRightFile, File expectedOutputFile)
                    throws IOException {
        BlendingOptions allowUnboundIfNoOtherBindingsOptions = new BlendingOptions(
                        UnboundHandlingMode.UNBOUND_ALLOWED_IF_NO_OTHER_BINDING,
                        true,
                        true,
                        null,
                        null,
                        AlgorithmState.Verbosity.FINER_TRACE);
        testOk(inputLeftFile, inputRightFile, expectedOutputFile, "_unboundAllowedIfNoOtherBindings",
                        allowUnboundIfNoOtherBindingsOptions);
    }

    private void testOk(File inputLeftFile, File inputRightFile, File expectedOutputFile, String testSuffix,
                    BlendingOptions blendingOptions) throws FileNotFoundException {
        DatasetGraph left = readDatasetGraphFromFile(getFileWithSuffixIfExists(inputLeftFile, testSuffix));
        DatasetGraph right = readDatasetGraphFromFile(getFileWithSuffixIfExists(inputRightFile, testSuffix));
        DatasetGraph expectedResultGraph = readDatasetGraphFromFile(
                        getFileWithSuffixIfExists(expectedOutputFile, testSuffix));
        UUIDSource uuidSource = new SequentialUUIDSource();
        TemplateIO templateIO = new TemplateIO(uuidSource);
        Template leftTemplate = templateIO.fromDatasetGraph(left).stream().findFirst().get();
        Template rightTemplate = templateIO.fromDatasetGraph(right).stream().findFirst().get();
        Graph globalShapesGraph = readGraphFromClasspathResource("shapes/valueflows-shapes-notargets.ttl");
        Graph globalDataGraph = readGraphFromClasspathResource("ontologies/valueflows-and-required.ttl");
        BlendingAlgorithm blendingAlgorithm = new JoiningAlgorithm();
        logger.info("blending starts");
        Set<Template> results = new Blender(
                        blendingAlgorithm,
                        new BlendingOperations(new TemplateIO(uuidSource)),
                        new BlendingBackground(globalShapesGraph, globalDataGraph))
                                        .blend(leftTemplate, rightTemplate, blendingOptions)
                                        .collect(Collectors.toUnmodifiableSet());
        logger.info("blending done");
        DatasetGraph actualResultGraph = templateIO.toDatasetGraph(results);
        String testIdentifier = inputLeftFile.getParentFile().getName();
        writeTestResultToFile(actualResultGraph, getOutputFileName(getClass(), testIdentifier + testSuffix));
        String stats = new DefaultBlendingResultStatsFormatter()
                        .format(BindingResultStatsAccumulator.accumulate(leftTemplate, rightTemplate, results));
        try {
            assertDatasetsEqual(expectedResultGraph, actualResultGraph, testIdentifier);
        } catch (AssertionFailedError e) {
            Set<Template> expectedResult = templateIO.fromDatasetGraph(expectedResultGraph);
            System.err.println("expected result statistics: ");
            System.err.println(new DefaultBlendingResultStatsFormatter().format(
                            BindingResultStatsAccumulator.accumulate(leftTemplate, rightTemplate, expectedResult)));
            System.err.println("actual result statistics: ");
            System.err.println(stats);
            throw e;
        }
        System.out.println(stats);
    }

    public static Stream<Arguments> testOkUnboundAllowedIfNoOtherBinding() throws IOException {
        return testOkAllBound();
    }

    public static Stream<Arguments> testOkAllowUnbound() throws IOException {
        return testOkAllBound();
    }

    public static Stream<Arguments> testOkAllBound() throws IOException {
        List<File> sortedUnittests = getSortedUnittests("won/utils/blend/BlendingTests");
        Arguments[] args = new Arguments[sortedUnittests.size()];
        return sortedUnittests.stream().map(unittestDir -> {
            File leftFile = new File(unittestDir, "left.trig");
            File rightFile = new File(unittestDir, "right.trig");
            File resultFile = new File(unittestDir, "result.trig");
            return arguments(leftFile, rightFile, resultFile);
        });
    }

    public static Stream<Arguments> testFocused() throws IOException {
        List<File> sortedUnittests = getSortedUnittests("focusedTests/");
        Arguments[] args = new Arguments[sortedUnittests.size()];
        return sortedUnittests.stream().map(unittestDir -> {
            File leftFile = new File(unittestDir, "left.trig");
            File rightFile = new File(unittestDir, "right.trig");
            File resultFile = new File(unittestDir, "result.trig");
            return arguments(leftFile, rightFile, resultFile);
        });
    }
}
