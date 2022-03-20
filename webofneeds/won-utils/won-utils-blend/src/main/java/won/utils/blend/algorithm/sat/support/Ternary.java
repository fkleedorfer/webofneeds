package won.utils.blend.algorithm.sat.support;

public enum Ternary {
    TRUE, UNKNOWN, FALSE;
    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }

    public boolean isUnkown() {
        return this == UNKNOWN;
    }

    public boolean isTrueOrUnknown() {
        return isTrue() || isUnknown();
    }

    public boolean isFalseOrUnknown() {
        return isFalse() || isUnknown();
    }

    public Ternary or(Ternary other) {
        return or(this, other);
    }

    public Ternary and(Ternary other) {
        return and(this, other);
    }

    public Ternary xor(Ternary other) {
        return xor(this, other);
    }

    public boolean isKnown() {
        return isKnown(this);
    }

    public boolean isUnknown() {
        return isUnknown(this);
    }

    public boolean isSameAs(Ternary other) {
        return same(this, other);
    }

    public boolean isDifferentFrom(Ternary other) {
        return different(this, other);
    }

    public boolean isKnownAndSameAs(Ternary other) {
        return knownAndSame(this, other);
    }

    public boolean isKnownAndDifferent(Ternary other) {
        return knownAndDifferent(this, other);
    }

    public static Ternary of(Boolean value) {
        if (value == null) {
            return UNKNOWN;
        }
        if (value) {
            return TRUE;
        }
        return FALSE;
    }

    public static Ternary not(Ternary o) {
        switch (o) {
            case TRUE:
                return FALSE;
            case FALSE:
                return TRUE;
            case UNKNOWN:
                return UNKNOWN;
            default:
                throw new IllegalArgumentException("Cannot handle Ternary " + o);
        }
    }

    public static boolean isKnown(Ternary o) {
        return o != UNKNOWN;
    }

    public static boolean isUnknown(Ternary o) {
        return o == UNKNOWN;
    }

    public static boolean same(Ternary l, Ternary r) {
        return l == r;
    }

    public static boolean different(Ternary l, Ternary r) {
        return l != r;
    }

    public static boolean knownAndSame(Ternary l, Ternary r) {
        return isKnown(l) && same(l, r);
    }

    public static boolean knownAndDifferent(Ternary l, Ternary r) {
        return isKnown(l) && isKnown(r) && different(l, r);
    }

    public static Ternary and(Ternary l, Ternary r) {
        switch (l) {
            case FALSE:
                return FALSE;
            case UNKNOWN:
                return r == FALSE ? FALSE : UNKNOWN;
            case TRUE:
                return r;
        }
        throw makeException("and", l, r);
    }

    private static RuntimeException makeException(String function, Ternary l, Ternary r) {
        return new IllegalArgumentException(String.format("Cannot compute ternary '%s(%s, %s)'", function, l, r));
    }

    public static Ternary or(Ternary l, Ternary r) {
        switch (l) {
            case TRUE:
                return TRUE;
            case UNKNOWN:
                return r == TRUE ? TRUE : UNKNOWN;
            case FALSE:
                return r;
        }
        throw makeException("or", l, r);
    }

    public static Ternary xor(Ternary l, Ternary r) {
        if (l == UNKNOWN || r == UNKNOWN) {
            return UNKNOWN;
        }
        return (l == TRUE ^ r == TRUE) ? TRUE : FALSE;
    }
}
