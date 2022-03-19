package won.utils.blend.algorithm.support;

import won.utils.blend.BlendingOptions;
import won.utils.blend.support.bindings.TemplateBindings;

public class BindingChecks {
    public static boolean doBindingsContainLongVariableChain(TemplateBindings b) {
        throw new UnsupportedOperationException("Delete all this!");
    }

    public static boolean areBindingsAcceptedByBlendingOptions(TemplateBindings bindings,
                    BlendingOptions blendingOptions) {
        if (blendingOptions.getUnboundHandlingMode().isAllBound()) {
            return ! bindings.hasUnboundVariables();
        }
        if (blendingOptions.hasTemplateBindingsFilter()) {
            boolean accept = blendingOptions.getTemplateBindingsFilter().acceptBindings(bindings);
            if (!accept) {
                return false;
            }
        }
        return true;
    }
}
