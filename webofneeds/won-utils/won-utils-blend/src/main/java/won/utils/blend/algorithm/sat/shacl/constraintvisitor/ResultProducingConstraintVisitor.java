package won.utils.blend.algorithm.sat.shacl.constraintvisitor;

import org.apache.jena.shacl.parser.ConstraintVisitor;

public interface ResultProducingConstraintVisitor<T> extends ConstraintVisitor {
    public T getResult();
}
