package won.utils.blend.algorithm.bruteforce;

import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.utils.blend.algorithm.BlendingAlgorithm;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.support.BindingChecks;
import won.utils.blend.algorithm.support.BlendingUtils;
import won.utils.blend.support.bindings.TemplateBindings;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.graph.TemplateGraphs;
import won.utils.blend.support.graph.formatter.DefaultTemplateGraphsFormatter;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static won.utils.blend.algorithm.support.BindingChecks.areBindingsAcceptedByBlendingOptions;

public class BruteForceBlendingAlgorithm implements BlendingAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Stream<TemplateGraphs> blend(BlendingInstance instanceContext) {
        Set<VariableBinding> admissibleBindings = BlendingUtils.findAdmissibleVariableBindings(instanceContext);
        if (instanceContext.blendingOptions.hasVariableBindingFilter()) {
            admissibleBindings = admissibleBindings
                            .stream()
                            .filter(instanceContext.blendingOptions.getVariableBindingFilter()::accept)
                            .collect(Collectors.toSet());
        }
        return generateAllBlendingResults(instanceContext, admissibleBindings);
    }

    private Stream<TemplateGraphs> generateAllBlendingResults(BlendingInstance instanceContext,
                    Set<VariableBinding> admissibleBindings) {
        BindingsPowersetGenerator bindingsPowersetGenerator = new BindingsPowersetGenerator(admissibleBindings);
        Set<TemplateGraphs> results = new HashSet<>();
        Set<TemplateBindings> acceptedBindings = new HashSet<>();
        return bindingsPowersetGenerator.getBindingsPowersetAsStream()
                        .map(bindings -> new TemplateBindings(instanceContext.leftTemplate,
                                        instanceContext.rightTemplate, bindings))
                        .filter(not(BindingChecks::doBindingsContainLongVariableChain))
                        .filter(b -> areBindingsAcceptedByBlendingOptions(b, instanceContext.blendingOptions))
                        .map(bindings -> {
                            if (instanceContext.blendingOptions.isOmitBindingSubsets()
                                            && acceptedBindings.stream()
                                                            .anyMatch(a -> a.getBindings().size() > bindings
                                                                            .getBindings().size())) {
                                // stop searching in binding sets smaller than already accepted solutions
                                bindingsPowersetGenerator.stopStream();
                            }
                            TemplateGraphs result = instanceContext.blendingOperations.blendWithGivenBindings(
                                            instanceContext.leftTemplate.getTemplateGraphs(),
                                            instanceContext.rightTemplate.getTemplateGraphs(),
                                            bindings);
                            if (instanceContext.blendingResultEvaluator.conformsToTemplateShapes(result, bindings)) {
                                acceptedBindings.add(bindings);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("===============");
                                    logger.debug("admissible binding for blending result: {}", bindings);
                                    logger.debug("result data:");
                                    System.out.println(DefaultTemplateGraphsFormatter.getInstance().format(result));
                                }
                                return result;
                            } else {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("===============================================");
                                    logger.debug("Rejecting binding in final check:{}, ", bindings);
                                    logger.debug("---------------- SHACL REPORT ----------------");
                                    ValidationReport validationReport = instanceContext.blendingResultEvaluator
                                                    .validateWithBackground(result);
                                    if (validationReport != null) {
                                        ShLib.printReport(validationReport);
                                    }
                                    logger.debug("--------------- BLENDING RESULT --------------");
                                    logger.debug(DefaultTemplateGraphsFormatter.getInstance().format(result));
                                    logger.debug("===============================================");
                                }
                                return null;
                            }
                        }).filter(Objects::nonNull);
    }
}
