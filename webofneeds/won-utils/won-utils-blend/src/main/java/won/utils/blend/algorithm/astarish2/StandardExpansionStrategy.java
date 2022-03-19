package won.utils.blend.algorithm.astarish2;

public class StandardExpansionStrategy extends CombinedExpansionStrategy {
    public StandardExpansionStrategy() {
        super(new PatternCompletingExpansionStrategy(),
                        new FixValidationErrorsExpansionStrategy(),
                        new AllValuesOfNextVariableExpansionStrategy());
    }
}
