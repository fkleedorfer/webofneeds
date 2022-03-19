package won.utils.blend.algorithm.sat.shacl2.astarish;

public enum Verbosity {
    SILENT, MEDIUM, MAXIMUM;

    public boolean isMaximum() {
        return this == MAXIMUM;
    }

    public boolean isSilent() {
        return this == SILENT;
    }

    public boolean isMediumOrHigher() {
        return this == MEDIUM || this == MAXIMUM;
    }

}
