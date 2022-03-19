package won.utils.blend.algorithm.support.bindings;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArrayUtilsTests {
    @Test
    public void testMerge(){
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.mergeArrays(new int[]{1,2}, new int[]{1,2,3,4})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.mergeArrays(new int[]{1,2}, new int[]{3,4})));
        assertTrue(Arrays.equals(new int[]{1,2,4,3}, ArrayUtils.mergeArrays(new int[]{1,2}, new int[]{4,3})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.mergeArrays(new int[]{1,2,3,4}, new int[]{3,4})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.mergeArrays(new int[]{1,2,3,4}, new int[]{4,3})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.mergeArrays(new int[]{1,2,3,4}, new int[]{2,3})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.mergeArrays(new int[]{1,2,3,4}, new int[]{3,2})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.mergeArrays(new int[]{1,2,3,4}, new int[]{})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.mergeArrays(new int[]{}, new int[]{1,2,3,4})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.mergeArrays(new int[]{1,2,3,4}, new int[]{1,2,3,4})));
    }

    @Test
    public void testCombine(){
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.combineArrays(new int[]{1,2,0,0}, new int[]{0,0,3,4})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.combineArrays(new int[]{1,2,0,0}, new int[]{1,2,3,4})));
        assertTrue(Arrays.equals(new int[]{1,2,3,4}, ArrayUtils.combineArrays(new int[]{1,2,3,4}, new int[]{0,0,3,4})));
        assertTrue(Arrays.equals(new int[]{0,0,3,4}, ArrayUtils.combineArrays(new int[]{0,0,0,0}, new int[]{0,0,3,4})));
        assertThrows(IllegalArgumentException.class, () -> ArrayUtils.combineArrays(new int[]{1,2,3,4}, new int[]{4,4,4,4}));
        assertThrows(IllegalArgumentException.class, () -> ArrayUtils.combineArrays(new int[]{1,2,3,4}, new int[]{4,4,4}));
    }

}
