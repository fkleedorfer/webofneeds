package won.utils.blend.support.index;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static won.utils.blend.support.index.IndexUtils.findFirstDifference;
import static won.utils.blend.support.index.IndexUtils.findFirstNonZero;

public class IndexUtilsTests {
    @Test
    public void testFindFirstDifference() {
        Assertions.assertEquals(3,
                        findFirstDifference(new int[] { 0, 0, 0, 0, 0, 0 }, 0, new int[] { 0, 0, 0, 1, 0, 0 }, 0));
    }

    @Test
    public void testFindFirstDifferenceDifferentStartingPoints() {
        Assertions.assertEquals(1,
                        findFirstDifference(new int[] { 0, 0, 0, 0, 0, 0 }, 0, new int[] { 0, 0, 0, 1, 0, 0 }, 2));
    }

    @Test
    public void testFindFirstNonZero_lengthMoreThanArrayLength() {
        Assertions.assertEquals(-1, findFirstNonZero(new int[] { 0, 0, 0, 0, 0, 0 }, 0, 100));
    }

    @Test
    public void testFindFirstNonZero_lengthLessThanArrayLength() {
        Assertions.assertEquals(-1, findFirstNonZero(new int[] { 0, 0, 0, 0, 0, 0 }, 0, 3));
    }

    @Test
    public void testFindFirstNonZero_firstElement() {
        Assertions.assertEquals(0, findFirstNonZero(new int[] { 1, 0, 0, 0, 0, 0 }, 0, 3));
    }

    @Test
    public void testFindFirstNonZero_firstElementAfterLength() {
        Assertions.assertEquals(-1, findFirstNonZero(new int[] { 0, 0, 0, 1, 0, 0 }, 0, 3));
    }

    @Test
    public void testFindFirstNonZero_within() {
        Assertions.assertEquals(3, findFirstNonZero(new int[] { 0, 0, 0, 1, 0, 0 }, 2, 2));
    }

    @Test
    public void testFindFirstNonZero_within_FirstMatch() {
        Assertions.assertEquals(2, findFirstNonZero(new int[] { 0, 0, 1, 1, 0, 0 }, 2, 2));
    }
}
