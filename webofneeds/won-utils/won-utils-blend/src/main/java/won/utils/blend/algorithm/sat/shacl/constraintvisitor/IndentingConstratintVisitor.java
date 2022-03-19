package won.utils.blend.algorithm.sat.shacl.constraintvisitor;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.shacl.engine.constraint.*;
import won.utils.blend.algorithm.sat.shacl.BindingExtractionState;
import won.utils.blend.algorithm.sat.shacl.ExtractionContext;

public class IndentingConstratintVisitor extends DelegatingConstraintVisitor {
    private IndentedWriter out;

    public IndentingConstratintVisitor(ResultProducingConstraintVisitor<BindingExtractionState> delegate,
                    ExtractionContext context) {
        super(delegate);
        this.out = context.indentedWriter;
    }

    @Override
    public void visit(ClassConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(DatatypeConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(NodeKindConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(MinCount constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(MaxCount constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ValueMinExclusiveConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ValueMinInclusiveConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ValueMaxInclusiveConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ValueMaxExclusiveConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(StrMinLengthConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(StrMaxLengthConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(PatternConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(StrLanguageIn constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(UniqueLangConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(EqualsConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(DisjointConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(LessThanConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(LessThanOrEqualsConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ShNot constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ShAnd constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ShOr constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ShXone constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ShNode constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(QualifiedValueShape constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ClosedConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(HasValueConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(InConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(ConstraintComponentSPARQL constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(SparqlConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(JViolationConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }

    @Override
    public void visit(JLogConstraint constraint) {
        out.incIndent();
        getDelegate().visit(constraint);
        out.decIndent();
    }
}
