package won.utils.blend.support.index;

public abstract class IndexUtils {
    public static boolean isSubsequenceAt(int[] bindings, int[] commonSequence, int variableIndex) {
        for (int i = 0; i < commonSequence.length; i++) {
            if (bindings[variableIndex + i] != commonSequence[i]) {
                return false;
            }
        }
        return true;
    }

    public static int findFirstNonZero(int[] array, int startingAt) {
        return findFirstNonZero(array, startingAt, -1);
    }

    public static int findFirstNonZero(int[] array, int startingAt, int length) {
        int to = length == -1 ? array.length : Math.min(startingAt + length, array.length);
        int i;
        for (i = startingAt; i < to; i++) {
            if (array[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    public static int findFirstDifference(int[] array1, int[] array2) {
        return findFirstDifference(array1, 0, array2, 0);
    }

    /**
     * Returns the first index in array1 at which it differs from array2, when
     * starting to compare array1 at start1 and array2 at start2.
     *
     * @param array1
     * @param start1
     * @param array2
     * @param start2
     * @return
     */
    public static int findFirstDifference(int[] array1, int start1, int[] array2, int start2) {
        int i = start1, j = start2;
        while (i < array1.length && j < array2.length) {
            if (array1[i] != array2[j]) {
                return i;
            }
            i++;
            j++;
        }
        return -1;
    }
}
