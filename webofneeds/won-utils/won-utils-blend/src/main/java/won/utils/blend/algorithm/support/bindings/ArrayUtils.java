package won.utils.blend.algorithm.support.bindings;

public class ArrayUtils {
    /**
     * Merges the two arrays. The specified arrays must not contain duplicate
     * entries.
     * 
     * @param left
     * @param right
     * @return
     */
    public static int[] mergeArrays(int[] left, int[] right) {
        int[] buffer = new int[right.length];
        int bufferind = -1;
        for (int i = 0; i < right.length; i++) {
            boolean foundIt = false;
            for (int j = 0; j < left.length; j++) {
                if (left[j] == right[i]) {
                    foundIt = true;
                    break;
                }
            }
            if (!foundIt) {
                bufferind++;
                buffer[bufferind] = right[i];
            }
        }
        int[] merged = new int[left.length + bufferind + 1];
        System.arraycopy(left, 0, merged, 0, left.length);
        if (bufferind >= 0) {
            System.arraycopy(buffer, 0, merged, left.length, bufferind + 1);
        }
        return merged;
    }

    /**
     * Combines arrays, using the nonzero value in each position if there is one.
     * Arrays must be same length.
     * 
     * @param left
     * @param right
     * @return
     */
    public static int[] combineArrays(int[] left, int[] right) {
        if (left.length != right.length) {
            throw new IllegalArgumentException("Arrays must be of same length");
        }
        int[] combined = new int[left.length];
        for (int i = 0; i < left.length; i++) {
            int lv = left[i];
            int rv = right[i];
            if (lv == 0) {
                combined[i] = rv;
            } else if (rv == 0) {
                combined[i] = lv;
            } else {
                if (lv != rv) {
                    throw new IllegalArgumentException("Arrays cannot be combined: conflict at position " + i);
                }
                combined[i] = lv;
            }
        }
        return combined;
    }
}
