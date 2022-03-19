package won.utils.blend.algorithm.sat.shacl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import won.utils.blend.algorithm.sat.support.Ternary;

import static won.utils.blend.algorithm.sat.support.Ternary.*;

public class TernaryTests {
    @Test
    public void testOf() {
        Assertions.assertEquals(TRUE, Ternary.of(true));
        Assertions.assertEquals(FALSE, Ternary.of(false));
        Assertions.assertEquals(UNKNOWN, Ternary.of(null));
    }

    @Test
    public void testAnd() {
        Assertions.assertEquals(TRUE, Ternary.and(TRUE, TRUE));
        Assertions.assertEquals(FALSE, Ternary.and(TRUE, FALSE));
        Assertions.assertEquals(UNKNOWN, Ternary.and(TRUE, UNKNOWN));
        Assertions.assertEquals(FALSE, Ternary.and(FALSE, TRUE));
        Assertions.assertEquals(FALSE, Ternary.and(FALSE, FALSE));
        Assertions.assertEquals(FALSE, Ternary.and(FALSE, UNKNOWN));
        Assertions.assertEquals(UNKNOWN, Ternary.and(UNKNOWN, TRUE));
        Assertions.assertEquals(FALSE, Ternary.and(UNKNOWN, FALSE));
        Assertions.assertEquals(UNKNOWN, Ternary.and(UNKNOWN, UNKNOWN));
    }

    @Test
    public void testOr() {
        Assertions.assertEquals(TRUE, Ternary.or(TRUE, TRUE));
        Assertions.assertEquals(TRUE, Ternary.or(TRUE, FALSE));
        Assertions.assertEquals(TRUE, Ternary.or(TRUE, UNKNOWN));
        Assertions.assertEquals(TRUE, Ternary.or(FALSE, TRUE));
        Assertions.assertEquals(FALSE, Ternary.or(FALSE, FALSE));
        Assertions.assertEquals(UNKNOWN, Ternary.or(FALSE, UNKNOWN));
        Assertions.assertEquals(TRUE, Ternary.or(UNKNOWN, TRUE));
        Assertions.assertEquals(UNKNOWN, Ternary.or(UNKNOWN, FALSE));
        Assertions.assertEquals(UNKNOWN, Ternary.or(UNKNOWN, UNKNOWN));
    }

    @Test
    public void testXor() {
        Assertions.assertEquals(FALSE, Ternary.xor(TRUE, TRUE));
        Assertions.assertEquals(TRUE, Ternary.xor(TRUE, FALSE));
        Assertions.assertEquals(UNKNOWN, Ternary.xor(TRUE, UNKNOWN));
        Assertions.assertEquals(TRUE, Ternary.xor(FALSE, TRUE));
        Assertions.assertEquals(FALSE, Ternary.xor(FALSE, FALSE));
        Assertions.assertEquals(UNKNOWN, Ternary.xor(FALSE, UNKNOWN));
        Assertions.assertEquals(UNKNOWN, Ternary.xor(UNKNOWN, TRUE));
        Assertions.assertEquals(UNKNOWN, Ternary.xor(UNKNOWN, FALSE));
        Assertions.assertEquals(UNKNOWN, Ternary.xor(UNKNOWN, UNKNOWN));
    }

    @Test
    public void testSame() {
        Assertions.assertEquals(true, Ternary.same(TRUE, TRUE));
        Assertions.assertEquals(false, Ternary.same(TRUE, FALSE));
        Assertions.assertEquals(false, Ternary.same(TRUE, UNKNOWN));
        Assertions.assertEquals(false, Ternary.same(FALSE, TRUE));
        Assertions.assertEquals(true, Ternary.same(FALSE, FALSE));
        Assertions.assertEquals(false, Ternary.same(FALSE, UNKNOWN));
        Assertions.assertEquals(false, Ternary.same(UNKNOWN, TRUE));
        Assertions.assertEquals(false, Ternary.same(UNKNOWN, FALSE));
        Assertions.assertEquals(true, Ternary.same(UNKNOWN, UNKNOWN));
    }

    @Test
    public void testDifferent() {
        Assertions.assertEquals(false, Ternary.different(TRUE, TRUE));
        Assertions.assertEquals(true, Ternary.different(TRUE, FALSE));
        Assertions.assertEquals(true, Ternary.different(TRUE, UNKNOWN));
        Assertions.assertEquals(true, Ternary.different(FALSE, TRUE));
        Assertions.assertEquals(false, Ternary.different(FALSE, FALSE));
        Assertions.assertEquals(true, Ternary.different(FALSE, UNKNOWN));
        Assertions.assertEquals(true, Ternary.different(UNKNOWN, TRUE));
        Assertions.assertEquals(true, Ternary.different(UNKNOWN, FALSE));
        Assertions.assertEquals(false, Ternary.different(UNKNOWN, UNKNOWN));
    }
}
