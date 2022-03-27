package won.utils.blend;

public enum UnboundHandlingMode {
    /** bl:unbound is never an option, all variables must be bound in a solution */
    ALL_BOUND,

    /**bl:unbound is always an option but may not be in a solution if there is another solution in which it's bound */
    UNBOUND_ALLOWED_IF_NO_OTHER_BINDING,

    /** bl:unbound is only an option if there is no other candidate for the variable */
    UNBOUND_ALLOWED_IF_NO_OTHER_OPTION,

    /** no restrictions */
    ALLOW_UNBOUND;

    public boolean isAllBound() {
        return this == ALL_BOUND;
    }

    public boolean isUnboundAllowedIfNoOtherBinding() {
        return this == UNBOUND_ALLOWED_IF_NO_OTHER_BINDING;
    }

    public boolean isAllowUnbound() {
        return this == ALLOW_UNBOUND;
    }

    public boolean isUnboundAllowedIfNoOtherOption(){
        return this == UNBOUND_ALLOWED_IF_NO_OTHER_OPTION;
    }

    /**
     * Informs the client whether or not bl:unbound should be considered
     * an option, <code>optionsArePresent</code> being what it is.
     * @param optionsArePresent
     * @return
     */
    public boolean isUnboundAnOption(boolean optionsArePresent){
        switch(this){
            case ALL_BOUND : return false;
            case ALLOW_UNBOUND: return true;
            case UNBOUND_ALLOWED_IF_NO_OTHER_BINDING: return true;
            case UNBOUND_ALLOWED_IF_NO_OTHER_OPTION: return !optionsArePresent;
        }
        throw new IllegalStateException("not prepared to handle this value: " + this);
    }
}
