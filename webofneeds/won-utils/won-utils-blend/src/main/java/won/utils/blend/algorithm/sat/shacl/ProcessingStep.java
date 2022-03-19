package won.utils.blend.algorithm.sat.shacl;

class ProcessingStep {
    public final String description;

    public ProcessingStep(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "step:  " + description;
    }
}
