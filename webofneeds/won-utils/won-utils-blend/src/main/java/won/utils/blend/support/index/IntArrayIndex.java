package won.utils.blend.support.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntArrayIndex {
    private final int arrayLength;
    private int size = 0;
    private IntArrayIndexNode root = new IntArrayIndexNode(0);

    public IntArrayIndex(int arrayLength) {
        this.arrayLength = arrayLength;
    }

    public int size() {
        return size;
    }

    public int sizeExpensive() {
        return root.sizeExpensive();
    }

    public void put(int[] arr) {
        checkLength(arr);
        root.add(arr);
    }

    public void checkLength(int[] arr) {
        if (arr.length != arrayLength) {
            throw new IllegalArgumentException(String.format("Invalid array length - expected: %d, provided: %d",
                            this.arrayLength, arr.length));
        }
    }

    public boolean contains(int[] arr) {
        checkLength(arr);
        return root.contains(arr);
    }

    public List<int[]> toList() {
        return root.toList();
    }

    private class IntArrayIndexNode {
        private int index = 0;
        private Map<Integer, IntArrayIndexNode> children = new HashMap<>();

        public IntArrayIndexNode(int index) {
            this.index = index;
        }

        public int size() {
            return size;
        }

        public int sizeExpensive() {
            if (children.isEmpty()) {
                return 1;
            }
            return children.values().stream().mapToInt(IntArrayIndexNode::sizeExpensive).sum();
        }

        public boolean add(int[] arr) {
            int value = arr[index];
            boolean isNew = false;
            IntArrayIndexNode child = children.get(value);
            if (child == null) {
                isNew = true;
                child = new IntArrayIndexNode(index + 1);
                children.put(value, child);
            }
            if (index < arrayLength - 1) {
                return child.add(arr);
            } else {
                if (isNew) {
                    size++;
                }
                return isNew;
            }
        }

        public boolean contains(int[] arr) {
            IntArrayIndexNode child = children.get(arr[index]);
            if (child == null) {
                return false;
            }
            if (index < arrayLength - 1) {
                return child.contains(arr);
            }
            return true;
        }

        public List<int[]> toList() {
            return toList(new int[arrayLength]);
        }

        private List<int[]> toList(int[] arr) {
            List<int[]> ret = new ArrayList<>();
            if (children.isEmpty()) {
                return List.of(arr);
            }
            int childIndex = 0;
            for (Map.Entry<Integer, IntArrayIndexNode> entry : children.entrySet()) {
                int value = entry.getKey();
                if (childIndex > 0) {
                    int[] copy = new int[arrayLength];
                    System.arraycopy(arr, 0, copy, 0, arrayLength);
                    arr = copy;
                }
                arr[index] = value;
                ret.addAll(entry.getValue().toList(arr));
                childIndex++;
            }
            return ret;
        }
    }
}
