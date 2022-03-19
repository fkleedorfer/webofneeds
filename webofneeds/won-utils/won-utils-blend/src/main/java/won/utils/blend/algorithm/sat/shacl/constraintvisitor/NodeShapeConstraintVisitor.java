package won.utils.blend.algorithm.sat.shacl.constraintvisitor;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.other.G;
import org.apache.jena.shacl.engine.constraint.*;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeFunctions;
import won.utils.blend.algorithm.sat.shacl.*;
import won.utils.blend.algorithm.sat.support.Ternary;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static won.utils.blend.algorithm.sat.support.Ternary.*;

public class NodeShapeConstraintVisitor extends BindingExtractingVisitorBase {
    private NodeShapeConstraintVisitor(BindingExtractionState inputState, ExtractionContext context) {
        super(inputState, context);
    }

    public static ResultProducingConstraintVisitor<BindingExtractionState> indenting(BindingExtractionState inputState,
                    ExtractionContext context) {
        return new IndentingConstratintVisitor(new NodeShapeConstraintVisitor(inputState, context), context);
    }

    public static ResultProducingConstraintVisitor<BindingExtractionState> plain(BindingExtractionState inputState,
                    ExtractionContext context) {
        return new NodeShapeConstraintVisitor(inputState, context);
    }

    private void handleWithPropertyShapeConstraintVisitor(Constraint constraint) {
        ResultProducingConstraintVisitor<BindingExtractionState> delegate = PropertyShapeConstraintVisitor.plain(
                        inputState.setValueNodes(Set.of(inputState.node)), context);
        constraint.visit(delegate);
        outputState = delegate.getResult();
    }

    @Override
    public void visit(ClassConstraint constraint) {
        setConstraintViolated();
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        if (inputState.node.isLiteral()) {
            return;
        }
        Collection<Node> types = G.allTypesOfNodeRDFS(inputState.getBlendedData(), inputState.node);
        if (types.contains(constraint.getExpectedClass())) {
            setConstraintSatisfied();
        }
    }

    @Override
    public void visit(DatatypeConstraint constraint) {
        setConstraintViolated(inputState);
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown(inputState);
            return;
        }
        Node valueNode = inputState.node;
        if (valueNode.isLiteral() && constraint.getDatatypeURI().equals(valueNode.getLiteralDatatypeURI())) {
            // Must be valid for the type
            if (constraint.getRDFDatatype().isValid(valueNode.getLiteralLexicalForm())) {
                setConstraintSatisfied();
            }
        }
    }

    @Override
    public void visit(NodeKindConstraint constraint) {
        setConstraintViolated();
        Node valueNode = inputState.node;
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        if (constraint.isCanBeIRI() && valueNode.isURI()) {
            setConstraintSatisfied();
            return;
        }
        if (constraint.isCanBeBlankNode() && valueNode.isBlank()) {
            setConstraintSatisfied();
            return;
        }
        if (constraint.isCanBeLiteral() && valueNode.isLiteral()) {
            setConstraintSatisfied();
            return;
        }
    }

    @Override
    public void visit(MinCount constraint) {
        throwNotSupportedException(constraint);
    }

    private void throwNotSupportedException(Constraint constraint) {
        throw new UnsupportedOperationException(
                        String.format("%s not allowed on a NodeShape", constraint.getClass().getSimpleName()));
    }

    @Override
    public void visit(MaxCount constraint) {
        throwNotSupportedException(constraint);
    }

    private void processValueRangeConstraint(ValueRangeConstraint constraint, Function<Integer, Boolean> test) {
        setConstraintViolated();
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        NodeValue nv = NodeValue.makeNode(inputState.node);
        ValueSpaceClassification vs = NodeValue.classifyValueOp(constraint.getNodeValue(), nv);
        try {
            int r = NodeValue.compare(constraint.getNodeValue(), nv);
            if (r == Expr.CMP_INDETERMINATE) {
                return;
            }
            boolean b = test.apply(r);
            if (b) {
                setConstraintSatisfied();
                return;
            }
        } catch (ExprNotComparableException ex) {
            // ignore - constraint is already marked violated
        }
    }

    @Override
    public void visit(ValueMinExclusiveConstraint constraint) {
        processValueRangeConstraint(constraint, cmp -> cmp == Expr.CMP_LESS);
    }

    @Override
    public void visit(ValueMinInclusiveConstraint constraint) {
        processValueRangeConstraint(constraint,
                        cmp -> cmp == Expr.CMP_LESS || cmp == Expr.CMP_EQUAL);
    }

    @Override
    public void visit(ValueMaxInclusiveConstraint constraint) {
        processValueRangeConstraint(constraint,
                        cmp -> cmp == Expr.CMP_GREATER || cmp == Expr.CMP_EQUAL);
    }

    @Override
    public void visit(ValueMaxExclusiveConstraint constraint) {
        processValueRangeConstraint(constraint,
                        cmp -> cmp == Expr.CMP_GREATER);
    }

    @Override
    public void visit(StrMinLengthConstraint constraint) {
        setConstraintViolated();
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        Node n = inputState.node;
        if (n.isBlank()) {
            return;
        }
        String str = NodeFunctions.str(n);
        if (str.length() >= constraint.getMinLength()) {
            setConstraintSatisfied();
        }
    }

    @Override
    public void visit(StrMaxLengthConstraint constraint) {
        setConstraintViolated();
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        Node n = inputState.node;
        if (n.isBlank()) {
            return;
        }
        String str = NodeFunctions.str(n);
        if (str.length() <= constraint.getMaxLength()) {
            setConstraintSatisfied();
        }
    }

    @Override
    public void visit(PatternConstraint constraint) {
        setConstraintViolated();
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        Node n = inputState.node;
        if (n.isBlank()) {
            return;
        }
        String str = NodeFunctions.str(n);
        String flagsStr = constraint.getFlagsStr();
        String patternString = null;
        int flags = RegexJava.makeMask(flagsStr);
        if (flagsStr != null && flagsStr.contains("q")) {
            patternString = Pattern.quote(constraint.getPattern());
        } else {
            patternString = constraint.getPattern();
        }
        Pattern pattern = Pattern.compile(patternString, flags);
        boolean b = pattern.matcher(str).find();
        if (b) {
            setConstraintSatisfied();
        }
    }

    @Override
    public void visit(StrLanguageIn constraint) {
        throw new UnsupportedOperationException("TODO implement");
    }

    @Override
    public void visit(UniqueLangConstraint constraint) {
        throw new UnsupportedOperationException("TODO implement");
    }

    @Override
    public void visit(EqualsConstraint constraint) {
        handleWithPropertyShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(DisjointConstraint constraint) {
        handleWithPropertyShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(LessThanConstraint constraint) {
        handleWithPropertyShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(LessThanOrEqualsConstraint constraint) {
        handleWithPropertyShapeConstraintVisitor(constraint);
    }

    @Override
    public void visit(ShNot constraint) {
        setConstraintViolated();
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        Shape other = constraint.getOther();
        BindingExtractionState result = (other instanceof NodeShape)
                        ? BindingSetExtractingVLib.processNodeShape(inputState, (NodeShape) other, context)
                        : BindingSetExtractingVLib.processPropertyShape(inputState, (PropertyShape) other,
                                        context);
        result = result.consolidateCheckedBindings();
        if (result.containsNoValidBindings()) {
            setConstraintSatisfied(result);
        }
    }

    @Override
    public void visit(ShAnd constraint) {
        setConstraintViolated();
        BindingExtractionState innerResults = getInnerShapeResults(constraint);
        Collection<Ternary> validities = CheckedBindings.getValidities(innerResults.checkedBindings);
        if (validities.stream().allMatch(Ternary::isTrue)) {
            setConstraintSatisfied(innerResults);
        } else if (validities.stream().allMatch(v -> v.isTrue() || v.isUnknown())) {
            setConstraintUnknown(innerResults);
        } else if (validities.stream().allMatch(Ternary::isFalse)) {
            setConstraintViolated(innerResults);
        } else if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown(innerResults);
        }
    }

    @Override
    public void visit(ShOr constraint) {
        setConstraintViolated();
        BindingExtractionState innerResults = getInnerShapeResults(constraint);
        Collection<Ternary> validities = CheckedBindings.getValidities(innerResults.checkedBindings);
        if (validities.stream().anyMatch(Ternary::isTrue)) {
            setConstraintSatisfied(innerResults);
        } else if (validities.stream().anyMatch(v -> v.isUnknown())) {
            setConstraintUnknown(innerResults);
        } else if (validities.stream().allMatch(Ternary::isFalse)) {
            setConstraintViolated(innerResults);
        } else if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown(innerResults);
        }
    }

    @Override
    public void visit(ShXone constraint) {
        setConstraintViolated();
        BindingExtractionState innerResults = getInnerShapeResults(constraint);
        Collection<Ternary> validities = CheckedBindings.getValidities(innerResults.checkedBindings);
        Map<Ternary, Long> counts = validities.stream().collect(
                        groupingBy(Function.identity(), Collectors.counting()));
        long trueCount = Optional.ofNullable(counts.get(TRUE)).orElse(Long.valueOf(0));
        long falseCount = Optional.ofNullable(counts.get(FALSE)).orElse(Long.valueOf(0));
        long unknownCount = Optional.ofNullable(counts.get(UNKNOWN)).orElse(Long.valueOf(0));
        if (trueCount == 1 && unknownCount == 0) {
            setConstraintSatisfied(innerResults);
        } else if (trueCount == 0 && unknownCount > 0) {
            setConstraintUnknown(innerResults);
        } else if (trueCount > 1 || (trueCount == 0 && unknownCount == 0)) {
            setConstraintViolated(innerResults);
        } else if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown(innerResults);
        }
    }

    private BindingExtractionState getInnerShapeResults(ConstraintOpN constraint) {
        Set<BindingExtractionState> innerResults = constraint
                        .getOthers()
                        .stream()
                        .map(other -> (other instanceof NodeShape)
                                        ? BindingSetExtractingVLib
                                                        .processNodeShape(inputState, (NodeShape) other,
                                                                        context)
                                        : BindingSetExtractingVLib.processPropertyShape(inputState,
                                                        (PropertyShape) other, context))
                        .map(s -> s.consolidateCheckedBindings())
                        .collect(Collectors.toSet());
        return inputState.addCheckedBindings(innerResults);
    }

    @Override
    public void visit(ShNode constraint) {
        setConstraintViolated();
        if (inputState.isUnboundOrUnsetVariable(inputState.node)) {
            setConstraintUnknown();
            return;
        }
        Shape other = constraint.getOther();
        BindingExtractionState result = (other instanceof NodeShape)
                        ? BindingSetExtractingVLib.processNodeShape(inputState, (NodeShape) other, context)
                        : BindingSetExtractingVLib.processPropertyShape(inputState, (PropertyShape) other, context);
        result = result.consolidateCheckedBindings();
        if (result.containsOnlyInvalidBindings()) {
            setConstraintViolated(result);
        } else if (result.containsUnknownValidityBindings()) {
            setConstraintUnknown(result);
        } else if (result.containsOnlyValidBindings()) {
            setConstraintSatisfied(result);
        }
    }

    @Override
    public void visit(QualifiedValueShape constraint) {
        throw new UnsupportedOperationException("TODO implement");
    }

    @Override
    public void visit(ClosedConstraint constraint) {
        throw new UnsupportedOperationException("TODO implement");
    }

    @Override
    public void visit(HasValueConstraint constraint) {
        setConstraintViolated();
        Node value = constraint.getValue();
        outputState = BindingUtils.applyToAllBindingOptions(inputState, value, s -> {
            Node deref = BindingUtils.dereferenceIfVariable(value, s.inheritedBindings);
            if (s.node.equals(deref)) {
                return s.valid();
            } else if (s.isUnboundOrUnsetVariable(value)) {
                return s.unknownValidity();
            }
            return s.invalid();
        }, context);
    }

    @Override
    public void visit(InConstraint constraint) {
        throw new UnsupportedOperationException("TODO implement");
    }

    @Override
    public void visit(ConstraintComponentSPARQL constraint) {
        throw new UnsupportedOperationException("TODO implement");
    }

    @Override
    public void visit(SparqlConstraint constraint) {
        setConstraintUnknown();
    }

    @Override
    public void visit(JViolationConstraint constraint) {
        throw new UnsupportedOperationException("TODO implement");
    }

    @Override
    public void visit(JLogConstraint constraint) {
        throw new UnsupportedOperationException("TODO implement");
    }
}
