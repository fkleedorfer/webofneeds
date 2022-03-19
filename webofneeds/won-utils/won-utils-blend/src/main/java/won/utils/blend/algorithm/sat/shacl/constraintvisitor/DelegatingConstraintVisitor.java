package won.utils.blend.algorithm.sat.shacl.constraintvisitor;

import org.apache.jena.shacl.parser.ConstraintVisitor;
import won.utils.blend.algorithm.sat.shacl.BindingExtractionState;

public abstract class DelegatingConstraintVisitor implements ResultProducingConstraintVisitor<BindingExtractionState> {
    private ResultProducingConstraintVisitor<BindingExtractionState> delegate;

    public DelegatingConstraintVisitor(ResultProducingConstraintVisitor<BindingExtractionState> delegate) {
        this.delegate = delegate;
    }

    protected ConstraintVisitor getDelegate() {
        return delegate;
    }

    @Override
    public BindingExtractionState getResult() {
        return delegate.getResult();
    }
}
