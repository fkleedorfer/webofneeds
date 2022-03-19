package won.utils.blend.algorithm.sat.shacl.constraintvisitor;

import won.utils.blend.algorithm.sat.shacl.BindingExtractionState;
import won.utils.blend.algorithm.sat.shacl.ExtractionContext;

public abstract class BindingExtractingVisitorBase implements ResultProducingConstraintVisitor<BindingExtractionState> {
    protected BindingExtractionState inputState;
    protected BindingExtractionState outputState;
    protected ExtractionContext context;

    public BindingExtractingVisitorBase(
                    BindingExtractionState inputState, ExtractionContext context) {
        this.inputState = inputState;
        this.context = context;
    }

    @Override
    public BindingExtractionState getResult() {
        return this.outputState;
    }

    protected void setConstraintSatisfied(BindingExtractionState forState) {
        this.outputState = this.inputState.valid(forState);
    }

    protected void setConstraintUnknown(BindingExtractionState forState) {
        this.outputState = this.inputState.unknownValidity(forState);
    }

    protected void setConstraintViolated(BindingExtractionState forState) {
        this.outputState = this.inputState.invalid(forState);
    }

    protected void setConstraintViolated() {
        setConstraintViolated(inputState);
    }

    protected void setConstraintUnknown() {
        setConstraintUnknown(inputState);
    }

    protected void setConstraintSatisfied() {
        setConstraintSatisfied(inputState);
    }
}
