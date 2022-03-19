package won.utils.blend.algorithm.support;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.engine.constraint.ClassConstraint;
import org.apache.jena.shacl.engine.constraint.ConstraintOp1;
import org.apache.jena.shacl.engine.constraint.ConstraintOpN;
import org.apache.jena.shacl.engine.constraint.ConstraintTerm;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shacl.validation.ReportItem;
import org.apache.jena.shacl.validation.VLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.utils.blend.BLEND;
import won.utils.blend.Template;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.support.bindings.TemplateBindings;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.graph.TemplateGraphs;
import won.utils.blend.support.graph.formatter.DefaultTemplateGraphsFormatter;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BlendingUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static Graph combineGraphsIfPresent(Graph left, Graph right) {
        Graph result;
        if (left == null) {
            result = right;
        } else if (right == null) {
            result = left;
        } else {
            result = new Union(left, right);
        }
        return result;
    }

    public static Set<VariableBinding> getAllPossibleBindings(BlendingInstance instance) {
        Set<VariableBinding> allBindings = new HashSet<>();
        addAllPossibleBindings(instance.leftTemplate, instance.rightTemplate, allBindings);
        addAllPossibleBindings(instance.rightTemplate, instance.leftTemplate, allBindings);
        return allBindings;
    }

    private static void addAllPossibleBindings(Template left, Template right, Set<VariableBinding> allBindings) {
        for (Node var : left.getVariables()) {
            for (Node target : right.getConstants()) {
                allBindings.add(new VariableBinding(var, target));
            }
            for (Node target : right.getVariables()) {
                allBindings.add(new VariableBinding(var, target, true));
            }
            allBindings.add(new VariableBinding(var, BLEND.unbound));
        }
    }

    public static Set<VariableBinding> findAdmissibleVariableBindings(
                    BlendingInstance instanceContext) {
        Set<VariableBinding> admissibleBindings = findAdmissibleVariableBindings(instanceContext,
                        instanceContext.leftTemplate, instanceContext.rightTemplate);
        admissibleBindings.addAll(findAdmissibleVariableBindings(instanceContext, instanceContext.rightTemplate,
                        instanceContext.leftTemplate));
        return admissibleBindings;
    }

    private static Set<VariableBinding> findAdmissibleVariableBindings(BlendingInstance instanceContext,
                    Template source, Template target) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(target);
        Set<VariableBinding> resultingCombinations = new HashSet<>();
        Set<Node> sourceVariables = source.getVariables();
        Set<Node> targetConstants = target.getConstants();
        for (Node sourceVariable : sourceVariables) {
            for (Node targetConstant : targetConstants) {
                TemplateBindings bindingsToCheck = new TemplateBindings(source, target);
                bindingsToCheck.addBinding(new VariableBinding(sourceVariable, targetConstant));
                TemplateGraphs result = instanceContext.blendingOperations.blendWithGivenBindings(
                                source.getTemplateGraphs(), target.getTemplateGraphs(),
                                bindingsToCheck);
                if (instanceContext.blendingResultEvaluator.conformsToTemplateShapes(result, sourceVariable)) {
                    resultingCombinations.add(new VariableBinding(sourceVariable, targetConstant));
                    logger.debug("admissible: binding variable {} to  constant {}", sourceVariable, targetConstant);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("not admissible: binding variable {} to  constant {}", sourceVariable,
                                        targetConstant);
                        logger.debug("===============================================");
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
                }
            }
        }
        for (Node sourceVariable : sourceVariables) {
            for (Node targetVariable : target.getVariables()) {
                if (sourceVariable.toString().equals(targetVariable.toString())) {
                    continue;
                }
                resultingCombinations.add(new VariableBinding(sourceVariable, targetVariable, true));
                // Note: currently unsure how to filter out bindings to variables
                // there is no way of deciding in general that the intersection of potential
                // focus nodes of two shapes is empty (hence the two variables cannot be
                // unified)
                // we could have any number of heuristics (e.g. different node kinds) to filter,
                // but is this the way to go?
            }
        }
        return resultingCombinations;
    }

    public static Set<VariableBinding> findAdmissibleVariableBindings2(
                    BlendingInstance instanceContext) {
        Set<VariableBinding> admissibleBindings = findAdmissibleVariableBindings2(instanceContext,
                        instanceContext.leftTemplate, instanceContext.rightTemplate);
        admissibleBindings.addAll(findAdmissibleVariableBindings2(instanceContext, instanceContext.rightTemplate,
                        instanceContext.leftTemplate));
        return admissibleBindings;
    }

    public static Set<VariableBinding> findAdmissibleVariableBindings2(BlendingInstance instance,
                    Template source, Template target) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(target);
        Set<VariableBinding> resultingCombinations = new HashSet<>();
        Set<Node> sourceVariables = source.getVariables();
        Set<Node> targetConstants = target.getConstants();
        Shapes sourceShapes = Shapes.parse(instance.blendingBackground
                        .combineWithBackgroundShapes(source.getTemplateGraphs().getShapesGraph()));
        ValidationReport sourceReport = instance.blendingResultEvaluator
                        .validateWithBackground(source.getTemplateGraphs());
        resultingCombinations
                        .addAll(getBindingsFromReport(targetConstants, sourceReport, sourceShapes, source, target,
                                        instance));
        for (Node sourceVariable : sourceVariables) {
            for (Node targetVariable : target.getVariables()) {
                if (sourceVariable.toString().equals(targetVariable.toString())) {
                    continue;
                }
                resultingCombinations.add(new VariableBinding(sourceVariable, targetVariable, true));
                // Note: currently unsure how to filter out bindings to variables
                // there is no way of deciding in general that the intersection of potential
                // focus nodes of two shapes is empty (hence the two variables cannot be
                // unified)
                // we could have any number of heuristics (e.g. different node kinds) to filter,
                // but is this the way to go?
            }
        }
        return resultingCombinations;
    }

    private static Set<VariableBinding> getBindingsFromReport(Set<Node> targetConstants,
                    ValidationReport report, Shapes shapes, Template sourceTemplate, Template targetTemplate,
                    BlendingInstance instance) {
        Set<VariableBinding> allowedBindings = new HashSet<>();
        for (ReportEntry reportEntry : report.getEntries()) {
            if (targetConstants.isEmpty()) {
                return Collections.emptySet();
            }
            selectAllowedValueFromReportEntry(targetConstants, sourceTemplate, targetTemplate, instance, shapes,
                            allowedBindings,
                            reportEntry);
        }
        return allowedBindings;
    }

    private static void selectAllowedValueFromReportEntry(Set<Node> targetConstants, Template sourceTemplate,
                    Template targetTemplate, BlendingInstance instance, Shapes shapes,
                    Set<VariableBinding> allowedBindings,
                    ReportEntry reportEntry) {
        Constraint constraint = reportEntry.constraint();
        if (ClassConstraint.class.isAssignableFrom(constraint.getClass())) {
            Node sourceVar2 = reportEntry.value();
            if (sourceVar2 != null && sourceTemplate.isVariable(sourceVar2)) {
                Set<VariableBinding> newBindings = targetConstants.stream().filter(targetConstant -> {
                    ValidationContext targetContext = makeTargetValidationContext(targetTemplate, instance, shapes);
                    ReportItem item = ((ClassConstraint) constraint).validate(targetContext,
                                    targetContext.getDataGraph(), targetConstant);
                    return item == null;
                }).map(t -> new VariableBinding(sourceVar2, t))
                                .collect(Collectors.toSet());
                allowedBindings.addAll(newBindings);
            }
        } else if (ConstraintOpN.class.isAssignableFrom(constraint.getClass())) {
            for (Shape shape : ((ConstraintOpN) constraint).getOthers()) {
                if (shape.isNodeShape()) {
                    Node focusNode = reportEntry.focusNode();
                    if (sourceTemplate.isVariable(focusNode)) {
                        Set<VariableBinding> newBindings = targetConstants.stream().filter(targetConstant -> {
                            ValidationContext targetCtx = makeTargetValidationContext(targetTemplate, instance,
                                            shapes);
                            VLib.validateShape(targetCtx,
                                            targetCtx.getDataGraph(), shape, targetConstant);
                            return !targetCtx.hasViolation();
                        }).map(t -> new VariableBinding(focusNode, t))
                                        .collect(Collectors.toSet());
                        allowedBindings.addAll(newBindings);
                    }
                }
            }
        } else if (ConstraintOp1.class.isAssignableFrom(constraint.getClass())) {
            Shape shape = ((ConstraintOp1) constraint).getOther();
            if (shape.isNodeShape()) {
                Node focusNode = reportEntry.focusNode();
                if (sourceTemplate.isVariable(focusNode)) {
                    Set<VariableBinding> newBindings = targetConstants.stream().filter(targetConstant -> {
                        ValidationContext targetCtx = makeTargetValidationContext(targetTemplate, instance,
                                        shapes);
                        VLib.validateShape(targetCtx,
                                        targetCtx.getDataGraph(), shape, targetConstant);
                        return !targetCtx.hasViolation();
                    }).map(t -> new VariableBinding(focusNode, t))
                                    .collect(Collectors.toSet());
                    allowedBindings.addAll(newBindings);
                }
            }
        } else if (ConstraintTerm.class.isAssignableFrom(constraint.getClass())) {
            Node focusNode = reportEntry.focusNode();
            if (sourceTemplate.isVariable(focusNode)) {
                Set<VariableBinding> newBindings = targetConstants.stream().filter(targetConstant -> {
                    ValidationContext targetCtx = makeTargetValidationContext(targetTemplate, instance,
                                    shapes);
                    ReportItem entry = ((ConstraintTerm) constraint).validate(targetCtx, targetConstant);
                    return entry == null;
                }).map(t -> new VariableBinding(focusNode, t))
                                .collect(Collectors.toSet());
                allowedBindings.addAll(newBindings);
            }
        } else {
            System.out.println("not using constraint " + constraint + " of shape " + reportEntry.source()
                            + " on focus node " + reportEntry.focusNode());
        }
    }

    private static ValidationContext makeTargetValidationContext(
                    Template targetTemplate, BlendingInstance instance, Shapes shapes) {
        Graph targetData = instance.blendingBackground
                        .combineWithBackgroundData(targetTemplate.getTemplateGraphs().getDataGraph());
        ValidationContext targetCtx = ValidationContext
                        .create(shapes, targetData);
        return targetCtx;
    }
}
