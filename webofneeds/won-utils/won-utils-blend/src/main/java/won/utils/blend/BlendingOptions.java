package won.utils.blend;

import won.utils.blend.support.bindings.TemplateBindingsFilter;
import won.utils.blend.support.bindings.VariableBindingFilter;

public class BlendingOptions {
    // if true, all blending results with unbound variables are removed
    private UnboundHandlingMode unboundHandlingMode;
    // if true, and if admissible variable binding sets have been found, no smaller
    // binding sets are checked
    private boolean omitBindingSubsets;
    private boolean inferDependentVariableBindings;
    private TemplateBindingsFilter templateBindingsFilter;
    private VariableBindingFilter variableBindingFilter;

    public BlendingOptions() {
        this.unboundHandlingMode = UnboundHandlingMode.ALL_BOUND;
        this.omitBindingSubsets = false;
        this.inferDependentVariableBindings = false;
        this.templateBindingsFilter = null;
        this.variableBindingFilter = null;
    }

    public BlendingOptions(UnboundHandlingMode unboundHandlingMode, boolean omitBindingSubsets, boolean inferDependentVariableBindings,
                    TemplateBindingsFilter templateBindingsFilter,
                    VariableBindingFilter variableBindingFilter) {
        this.unboundHandlingMode = unboundHandlingMode;
        this.omitBindingSubsets = omitBindingSubsets;
        this.templateBindingsFilter = templateBindingsFilter;
        this.variableBindingFilter = variableBindingFilter;
        this.inferDependentVariableBindings = inferDependentVariableBindings;
    }

    public boolean isOmitBindingSubsets() {
        return omitBindingSubsets;
    }

    public UnboundHandlingMode getUnboundHandlingMode() {
        return unboundHandlingMode;
    }

    public boolean isInferDependentVariableBindings() {
        return inferDependentVariableBindings;
    }

    public TemplateBindingsFilter getTemplateBindingsFilter() {
        return templateBindingsFilter;
    }

    public VariableBindingFilter getVariableBindingFilter() {
        return variableBindingFilter;
    }

    public boolean hasVariableBindingFilter() {
        return this.variableBindingFilter != null;
    }

    public boolean hasTemplateBindingsFilter() {
        return this.templateBindingsFilter != null;
    }
}
