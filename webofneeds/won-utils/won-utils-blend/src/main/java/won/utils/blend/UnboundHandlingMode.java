package won.utils.blend;

public enum UnboundHandlingMode {
    ALL_BOUND,
    UNBOUND_ALLOWED_IF_NO_OTHER_BINDING,
    ALLOW_UNBOUND;

    public boolean isAllBound() {
        return this == ALL_BOUND;
    }

    public boolean isUnboundAllowedIfNoOtherBinding(){
        return this == UNBOUND_ALLOWED_IF_NO_OTHER_BINDING;
    }

    public boolean isAllowUnbound(){
        return this == ALLOW_UNBOUND;
    }
}
