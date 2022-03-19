package won.utils.blend.algorithm.sat.shacl.constraintvisitor;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.other.G;
import org.apache.jena.shacl.engine.constraint.*;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.ConstraintVisitor;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprNotComparableException;
import org.apache.jena.sparql.expr.NodeValue;
import won.utils.blend.algorithm.sat.shacl.BindingExtractionState;
import won.utils.blend.algorithm.sat.shacl.BindingUtils;
import won.utils.blend.algorithm.sat.shacl.CheckedBindings;
import won.utils.blend.algorithm.sat.shacl.ExtractionContext;
import won.utils.blend.algorithm.sat.support.Ternary;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;

import static won.utils.blend.algorithm.sat.shacl.ProcessingStepMessages.handlingPropShapeConstrintAsNodeShapeConstraintMsg;

public class PropertyShapeConstraintVisitor extends BindingExtractingVisitorBase implements ConstraintVisitor {
    private PropertyShapeConstraintVisitor(BindingExtractionState inputState, ExtractionContext context) {
        super(inputState, context);
    }

    public static ResultProducingConstraintVisitor<BindingExtractionState> indenting(BindingExtractionState inputState,
                    ExtractionContext context) {
        return new IndentingConstratintVisitor(new PropertyShapeConstraintVisitor(inputState, context), context);
    }

    public static ResultProducingConstraintVisitor<BindingExtractionState> plain(BindingExtractionState inputState,
                    ExtractionContext context) {
        return new PropertyShapeConstraintVisitor(inputState, context);
    }

    @Override
    public void visit(ClassConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    private void handleWithNodeShapeConstraintVisitor(Constraint constraint) {
        this.outputState = this.inputState.valueNodes
                        .stream()
                        .map(n -> {
                            context.indentedWriter.println(handlingPropShapeConstrintAsNodeShapeConstraintMsg(n,
                                            constraint.getClass()));
                            ResultProducingConstraintVisitor<BindingExtractionState> delegate = NodeShapeConstraintVisitor
                                            .indenting(
                                                            inputState.setNodeAndResetValueNodes(n), context);
                            constraint.visit(delegate);
                            return delegate.getResult();
                        })
                        .reduce((l, r) -> l.addCheckedBindings(r)).orElse(this.inputState);
    }

    @Override
    public void visit(DatatypeConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(NodeKindConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(MinCount constraint) {
        setConstraintViolated();
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        if (inputState.containsUnboundOrUnsetVariable(inputState.valueNodes)) {
            setConstraintUnknown();
            return;
        }
        Set<Node> valueNodes = inputState.valueNodes;
        int count = valueNodes.size();
        int minCount = constraint.getMinCount();
        if (minCount >= 0 && count >= minCount) {
            setConstraintSatisfied();
        }
    }

    @Override
    public void visit(MaxCount constraint) {
        setConstraintViolated();
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        if (inputState.containsUnboundOrUnsetVariable(inputState.valueNodes)) {
            setConstraintUnknown();
            return;
        }
        Set<Node> valueNodes = inputState.valueNodes;
        int count = valueNodes.size();
        int maxCount = constraint.getMaxCount();
        if (maxCount >= 0 && count <= maxCount) {
            setConstraintSatisfied();
        }
    }

    @Override
    public void visit(ValueMinExclusiveConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(ValueMinInclusiveConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(ValueMaxInclusiveConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(ValueMaxExclusiveConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(StrMinLengthConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(StrMaxLengthConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(PatternConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(StrLanguageIn constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(UniqueLangConstraint constraint) {
        throw new UnsupportedOperationException("TODO: implement!");
    }

    @Override
    public void visit(EqualsConstraint constraint) {
        handlePairwiseConstraint(constraint,
                        (state, pathAndCompareNodes) -> Ternary.of(
                                        pathAndCompareNodes.pathNodes.containsAll(pathAndCompareNodes.compareNodes)
                                                        && pathAndCompareNodes.compareNodes
                                                                        .size() == pathAndCompareNodes.pathNodes
                                                                                        .size()));
    }

    private void handlePairwiseConstraint(ConstraintPairwise constraint,
                    BiFunction<BindingExtractionState, PathAndCompareNodes, Ternary> test) {
        setConstraintViolated();
        context.indentedWriter.println(
                        String.format("comparing value nodes with values of property %s", constraint.getValue()));
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        if (inputState.containsUnboundOrUnsetVariable(inputState.valueNodes)) {
            setConstraintUnknown();
            return;
        }
        Set<Node> compareNodes = G.allSP(inputState.getBlendedData(), inputState.node, constraint.getValue());
        context.indentedWriter.incIndent();
        compareNodes.stream().forEach(n -> context.indentedWriter.println(n));
        context.indentedWriter.decIndent();
        Set<Node> pathNodes = inputState.valueNodes;
        BindingExtractionState result = BindingUtils.applyToAllBindingOptions(inputState, compareNodes, s -> {
            Set<Node> valueNodes = BindingUtils.dereferenceIfVariableToSet(compareNodes, s.inheritedBindings);
            PathAndCompareNodes pathAndValueNodes = new PathAndCompareNodes(pathNodes, valueNodes);
            Ternary valid = test.apply(s, pathAndValueNodes);
            context.indentedWriter.incIndent();
            context.indentedWriter.println("valid: " + valid);
            context.indentedWriter.decIndent();
            return s.setValidity(valid);
        }, context);
        result.consolidateCheckedBindings();
        Collection<Ternary> validities = CheckedBindings.getValidities(result.checkedBindings);
        if (validities.stream().allMatch(Ternary::isTrue)) {
            setConstraintSatisfied(result);
        } else if (validities.stream().allMatch(v -> v.isTrue() || v.isUnknown())) {
            setConstraintUnknown(result);
        } else if (validities.stream().allMatch(Ternary::isFalse)) {
            setConstraintViolated(result);
        } else if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown(result);
        }
    }

    private static class PathAndCompareNodes {
        public final Set<Node> pathNodes;
        public final Set<Node> compareNodes;

        public PathAndCompareNodes(Set<Node> pathNodes, Set<Node> compareNodes) {
            this.pathNodes = pathNodes;
            this.compareNodes = compareNodes;
        }
    }

    @Override
    public void visit(DisjointConstraint constraint) {
        handlePairwiseConstraint(constraint,
                        (state, pathAndValueNodes) -> Ternary.of(
                                        pathAndValueNodes.pathNodes
                                                        .stream()
                                                        .noneMatch(n -> pathAndValueNodes.compareNodes
                                                                        .contains(n))));
    }

    @Override
    public void visit(LessThanConstraint constraint) {
        handlePairwiseConstraint(constraint,
                        (state, pathAndValueNodes) -> {
                            if (pathAndValueNodes.pathNodes.stream().anyMatch(state::isUnboundOrUnsetVariable)) {
                                return Ternary.UNKNOWN;
                            }
                            if (pathAndValueNodes.compareNodes.stream().anyMatch(state::isUnboundOrUnsetVariable)) {
                                return Ternary.UNKNOWN;
                            }
                            boolean test = pathAndValueNodes.pathNodes.stream()
                                            .allMatch(p -> pathAndValueNodes.compareNodes
                                                            .stream().allMatch(c -> {
                                                                try {
                                                                    int cmp = compare(p, c);
                                                                    return cmp == Expr.CMP_LESS;
                                                                } catch (ExprNotComparableException e) {
                                                                    return false;
                                                                }
                                                            }));
                            return Ternary.of(test);
                        });
    }

    @Override
    public void visit(LessThanOrEqualsConstraint constraint) {
        handlePairwiseConstraint(constraint,
                        (state, pathAndValueNodes) -> {
                            if (pathAndValueNodes.pathNodes.stream().anyMatch(state::isUnboundOrUnsetVariable)) {
                                return Ternary.UNKNOWN;
                            }
                            if (pathAndValueNodes.compareNodes.stream().anyMatch(state::isUnboundOrUnsetVariable)) {
                                return Ternary.UNKNOWN;
                            }
                            boolean test = pathAndValueNodes.pathNodes.stream()
                                            .allMatch(p -> pathAndValueNodes.compareNodes
                                                            .stream().allMatch(c -> {
                                                                try {
                                                                    int cmp = compare(p, c);
                                                                    return cmp == Expr.CMP_LESS
                                                                                    || cmp == Expr.CMP_EQUAL;
                                                                } catch (ExprNotComparableException e) {
                                                                    return false;
                                                                }
                                                            }));
                            return Ternary.of(test);
                        });
    }

    @Override
    public void visit(ShNot constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(ShAnd constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(ShOr constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(ShXone constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(ShNode constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(QualifiedValueShape constraint) {
        throw new UnsupportedOperationException("TODO: implement!");
    }

    @Override
    public void visit(ClosedConstraint constraint) {
        throw new UnsupportedOperationException("TODO: implement!");
    }

    @Override
    public void visit(HasValueConstraint constraint) {
        handleWithNodeShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(InConstraint constraint) {
        throw new UnsupportedOperationException("TODO: implement!");
    }

    @Override
    public void visit(ConstraintComponentSPARQL constraint) {
        throw new UnsupportedOperationException("TODO: implement!");
    }

    @Override
    public void visit(SparqlConstraint constraint) {
        throw new UnsupportedOperationException("TODO: implement!");
    }

    @Override
    public void visit(JViolationConstraint constraint) {
        throw new UnsupportedOperationException("TODO: implement!");
    }

    @Override
    public void visit(JLogConstraint constraint) {
        throw new UnsupportedOperationException("TODO: implement!");
    }

    protected int compare(Node n1, Node n2) {
        NodeValue nv1 = NodeValue.makeNode(n1);
        NodeValue nv2 = NodeValue.makeNode(n2);
        try {
            return NodeValue.compare(nv1, nv2);
        } catch (ExprNotComparableException ex) {
            // Known to be not a Expr compare constant value.
            return -999;
        }
    }
}
