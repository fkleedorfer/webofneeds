package won.utils.blend.support.index;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class CompactBindingsIndex {
    private int numberOfVariables;
    private int[] optionsPerVariable;
    private IndexNode root;
    private int size = 0;

    public CompactBindingsIndex(int[] optionsPerVariable) {
        this.numberOfVariables = optionsPerVariable.length;
        this.optionsPerVariable = optionsPerVariable;
        this.root = null;
    }

    public void put(int[] bindings) {
        checkSize(bindings);
        if (root == null) {
            root = new LeafNode(0, bindings);
            size = 1;
            return;
        }
        root = root.put(bindings);
    }

    public void checkSize(int[] bindings) {
        if (bindings.length != numberOfVariables) {
            throw new IllegalArgumentException("specified array must be of length " + numberOfVariables);
        }
    }

    public boolean contains(int[] bindings) {
        checkSize(bindings);
        if (this.root == null) {
            return false;
        }
        checkSize(bindings);
        return root.contains(bindings);
    }

    public int size() {
        return size;
    }

    int maxElementLength() {
        if (root == null) {
            return 0;
        }
        return root.maxElementLength();
    }

    int minElementLength() {
        if (root == null) {
            return 0;
        }
        return root.minElementLength();
    }

    public String treeToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CompactBindingsIndex of size ").append(size).append("\n");
        IndexNodeVisitor toStringVisitor = new IndexNodeVisitor() {
            private ArrayDeque<IndexNode> stack = new ArrayDeque<>();

            @Override
            public void visit(TerminatorNode node) {
                sb
                                .append("T");
            }

            @Override
            public void visit(LeafNode node) {
                sb
                                .append("L(");
                if (numberOfVariables - node.variableIndex > 1) {
                    sb
                                    .append(node.variableIndex)
                                    .append("-")
                                    .append(numberOfVariables - 1);
                } else {
                    sb.append(node.variableIndex);
                }
                sb
                                .append("): ")
                                .append(Arrays.toString(node.getRemainder()));
            }

            @Override
            public void visit(InnerNode node) {
                stack.push(node);
                sb
                                .append("I(")
                                .append(node.variableIndex)
                                .append(")\n");
                visitChildren(node);
                stack.pop();
            }

            @Override
            public void visit(InnerNodeWithCommonArraySequence node) {
                stack.push(node);
                sb
                                .append("IA(")
                                .append(node.variableIndex)
                                .append("-")
                                .append(node.variableIndex + node.commonSequence.length)
                                .append("): ")
                                .append(Arrays.toString(node.commonSequence))
                                .append("\n");
                visitChildren(node);
                stack.pop();
            }

            @Override
            public void visit(InnerNodeWithCommonZeroSequence node) {
                stack.push(node);
                sb
                                .append("IZ(")
                                .append(node.variableIndex)
                                .append("-")
                                .append(node.variableIndex + node.commonSequenceLength)
                                .append("): ")
                                .append(Arrays.toString(new int[node.commonSequenceLength]))
                                .append("\n");
                visitChildren(node);
                stack.pop();
            }

            private void visitChildren(InnerNode node) {
                boolean didPrint = false;
                for (Tuple<Integer, IndexNode> childWithIndex : node.children) {
                    IndexNode child = childWithIndex.getRight();
                    if (child == null) {
                        continue;
                    }
                    didPrint = true;
                    indent();
                    sb.append(childWithIndex.getLeft()).append(": ");
                    child.visit(this);
                    sb.append("\n");
                }
                if (didPrint) {
                    sb.deleteCharAt(sb.length() - 1);
                }
            }

            private void indent() {
                sb.append("    ".repeat(stack.size()));
            }
        };
        if (root != null) {
            root.visit(toStringVisitor);
        }
        return sb.toString();
    }

    public long memorySize() {
        if (root == null) {
            return 0;
        }
        final AtomicLong size = new AtomicLong(0);
        IndexNodeVisitor visitor = new IndexNodeVisitor() {
            @Override
            public void visit(TerminatorNode node) {
                size.addAndGet(InstrumentationAgent.getObjectSize(node));
            }

            @Override
            public void visit(LeafNode node) {
                size.addAndGet(InstrumentationAgent.getObjectSize(node));
            }

            @Override
            public void visit(InnerNode node) {
                size.addAndGet(InstrumentationAgent.getObjectSize(node));
                for (Tuple<Integer, IndexNode> child : node.children) {
                    if (child.getRight() != null) {
                        child.getRight().visit(this);
                    }
                }
            }

            @Override
            public void visit(InnerNodeWithCommonArraySequence node) {
                size.addAndGet(InstrumentationAgent.getObjectSize(node));
                for (Tuple<Integer, IndexNode> child : node.children) {
                    if (child.getRight() != null) {
                        child.getRight().visit(this);
                    }
                }
            }

            @Override
            public void visit(InnerNodeWithCommonZeroSequence node) {
                size.addAndGet(InstrumentationAgent.getObjectSize(node));
                for (Tuple<Integer, IndexNode> child : node.children) {
                    if (child.getRight() != null) {
                        child.getRight().visit(this);
                    }
                }
            }
        };
        root.visit(visitor);
        return size.get();
    }

    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    abstract class IndexNode {
        protected int variableIndex = -1;

        private IndexNode() {
        }

        public IndexNode(int variableIndex) {
            if (variableIndex < 0 || variableIndex >= numberOfVariables) {
                throw new IllegalArgumentException(
                                String.format("variableIndex must be between 0 and %d (exclusive) but was %d",
                                                numberOfVariables, variableIndex));
            }
            this.variableIndex = variableIndex;
        }

        public abstract IndexNode put(int[] bindings);

        public abstract boolean contains(int[] bindings);

        public boolean isTerminator() {
            return false;
        }

        protected int getNumberOfOptions() {
            return optionsPerVariable[variableIndex];
        }

        protected int getNumberOfOptionsAt(int index) {
            return optionsPerVariable[index];
        }

        public abstract void visit(IndexNodeVisitor visitor);

        public abstract int maxElementLength();

        public abstract int minElementLength();
    }

    private interface IndexNodeVisitor {
        public void visit(TerminatorNode node);

        public void visit(LeafNode node);

        public void visit(InnerNode node);

        public void visit(InnerNodeWithCommonArraySequence node);

        public void visit(InnerNodeWithCommonZeroSequence node);
    }

    private class TerminatorNode extends IndexNode {
        public TerminatorNode() {
        }

        @Override
        public IndexNode put(int[] bindings) {
            throw new UnsupportedOperationException("Cannot put() on terminator node");
        }

        @Override
        public boolean contains(int[] bindings) {
            throw new UnsupportedOperationException("Cannot call contains() on terminator node");
        }

        @Override
        public boolean isTerminator() {
            return true;
        }

        @Override
        public void visit(IndexNodeVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public int maxElementLength() {
            return 0;
        }

        @Override
        public int minElementLength() {
            return 0;
        }

        public String toString() {
            return "T";
        }
    }

    private TerminatorNode TERMINATOR = new TerminatorNode();

    private class LeafNode extends IndexNode {
        private int[] remainder;

        public LeafNode(int variableIndex, int[] bindings) {
            super(variableIndex);
            setRemainder(variableIndex, bindings);
        }

        public void setRemainder(int variableIndex, int[] bindings) {
            int firstNonZero = IndexUtils.findFirstNonZero(bindings, variableIndex);
            if (firstNonZero == -1) {
                remainder = null; // encode: remainder is all-zeros
            } else {
                remainder = makeRemainderArray();
                System.arraycopy(bindings, variableIndex, remainder, 0, remainder.length);
            }
        }

        public int[] makeRemainderArray() {
            return new int[numberOfVariables - variableIndex];
        }

        private int[] getRemainder() {
            if (remainder == null) {
                return makeRemainderArray();
            }
            return remainder;
        }

        @Override
        public IndexNode put(int[] bindings) {
            int firstDifference = firstDifferentElement(bindings);
            if (firstDifference == -1) {
                // bindings are same as this
                return this;
            }
            if (firstDifference == variableIndex) {
                // difference at first element
                return split(this, bindings);
            }
            // difference somewhere else
            return splitWithCommonSequence(this, bindings, firstDifference);
        }

        private IndexNode splitWithCommonSequence(LeafNode leafNode, int[] bindings, int firstDifference) {
            size--;
            InnerNodeWithCommonSequence replacement = newInnerNodeWithCommonSequence(variableIndex, bindings,
                            firstDifference - variableIndex);
            replacement.put(bindings);
            replacement.put(replaceRemainder(bindings, getRemainder()));
            return replacement;
        }

        private IndexNode split(LeafNode leafNode, int[] bindings) {
            InnerNode replacement = new InnerNode(variableIndex);
            size--;
            replacement.put(bindings);
            replacement.put(replaceRemainder(bindings, getRemainder()));
            return replacement;
        }

        private int[] replaceRemainder(int[] bindings, int[] remainder) {
            int[] recreated = new int[bindings.length];
            System.arraycopy(bindings, 0, recreated, 0, variableIndex);
            System.arraycopy(remainder, 0, recreated, variableIndex, remainder.length);
            return recreated;
        }

        private int firstDifferentElement(int[] bindings) {
            if (remainder == null) {
                return IndexUtils.findFirstNonZero(bindings, variableIndex);
            } else {
                return IndexUtils.findFirstDifference(bindings, variableIndex, remainder, 0);
            }
        }

        @Override
        public boolean contains(int[] bindings) {
            return firstDifferentElement(bindings) == -1;
        }

        @Override
        public void visit(IndexNodeVisitor visitor) {
            visitor.visit(this);
        }

        public String toString() {
            return "L[" + variableIndex + "]: " + Arrays.toString(getRemainder());
        }

        @Override
        public int maxElementLength() {
            return remainder != null ? remainder.length : numberOfVariables - variableIndex;
        }

        @Override
        public int minElementLength() {
            return remainder != null ? remainder.length : numberOfVariables - variableIndex;
        }
    }

    private interface Children extends Iterable<Tuple<Integer, IndexNode>> {
        IndexNode getChildAtIndex(int index);

        void setChildAtIndex(int index, IndexNode child);

        void replaceWith(Children children);

        Iterator<Tuple<Integer, IndexNode>> iterator();

        int size();
    }

    private class ChildrenArray implements Children {
        private IndexNode[] children;

        public ChildrenArray(int size) {
            children = new IndexNode[size];
        }

        @Override
        public IndexNode getChildAtIndex(int index) {
            return children[index];
        }

        @Override
        public void setChildAtIndex(int index, IndexNode child) {
            children[index] = child;
        }

        @Override
        public void replaceWith(Children toCopy) {
            this.children = new IndexNode[toCopy.size()];
            for (Tuple<Integer, IndexNode> childWithIndex : toCopy) {
                this.children[childWithIndex.getLeft()] = childWithIndex.getRight();
            }
        }

        @Override
        public Iterator<Tuple<Integer, IndexNode>> iterator() {
            return new Iterator<Tuple<Integer, IndexNode>>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < children.length;
                }

                @Override
                public Tuple<Integer, IndexNode> next() {
                    return new Tuple(index, children[index++]);
                }
            };
        }

        @Override
        public int size() {
            return children.length;
        }
    }

    private class ChildrenMap implements Children {
        private Map<Integer, IndexNode> children;

        public ChildrenMap(int numberOfOptions) {
            this.children = new HashMap<>(Math.max(numberOfOptions / 5, 3));
        }

        @Override
        public IndexNode getChildAtIndex(int index) {
            return children.get(index);
        }

        @Override
        public void setChildAtIndex(int index, IndexNode child) {
            children.put(index, child);
        }

        @Override
        public void replaceWith(Children toCopy) {
            children.clear();
            for (Tuple<Integer, IndexNode> entry : toCopy) {
                if (entry.getRight() != null) {
                    children.put(entry.getLeft(), entry.getRight());
                }
            }
        }

        @Override
        public Iterator<Tuple<Integer, IndexNode>> iterator() {
            return children.entrySet().stream().map(e -> new Tuple<>(e.getKey(), e.getValue())).iterator();
        }

        @Override
        public int size() {
            return children.size();
        }
    }

    private class Tuple<L, R> {
        private L left;
        private R right;

        public Tuple(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public L getLeft() {
            return left;
        }

        public R getRight() {
            return right;
        }
    }

    private class InnerNode extends IndexNode {
        protected Children children;

        public InnerNode(int variableIndex) {
            super(variableIndex);
            int numberOfOptions = getNumberOfOptions();
            // this.children = new ChildrenArray(numberOfOptions + 1);
            this.children = new ChildrenMap(numberOfOptions + 1);
        }

        public InnerNode(int variableIndex, int numberOfChildren) {
            super(variableIndex);
            // this.children = new ChildrenArray(numberOfChildren);
            this.children = new ChildrenMap(numberOfChildren);
        }

        public IndexNode put(int[] bindings) {
            return putAtIndex(bindings, variableIndex);
        }

        protected IndexNode getChildAtIndex(int index) {
            return children.getChildAtIndex(index);
        }

        protected void setChildAtIndex(int index, IndexNode child) {
            children.setChildAtIndex(index, child);
        }

        protected void setChildren(Children children) {
            this.children.replaceWith(children);
        }

        protected IndexNode putAtIndex(int[] bindings, int index) {
            int optionInd = bindings[index];
            if (optionInd < 0 || optionInd > getNumberOfOptionsAt(index)) {
                throw new IllegalArgumentException(
                                String.format("optionIndex must be between 0 and %d (inclusive) but was %d",
                                                getNumberOfOptionsAt(index), optionInd));
            }
            IndexNode child = getChildAtIndex(optionInd);
            if (child == null) {
                child = newLeafOrTerminatorAtIndex(bindings, index);
                size++;
            } else if (child.isTerminator() && index == numberOfVariables - 1) {
                // put on existing element
                return this;
            } else {
                child = child.put(bindings);
            }
            setChildAtIndex(optionInd, child);
            return this;
        }

        protected IndexNode newLeafOrTerminator(int[] bindings) {
            return newLeafOrTerminatorAtIndex(bindings, variableIndex);
        }

        protected IndexNode newLeafOrTerminatorAtIndex(int[] bindings, int index) {
            IndexNode child;
            if (index >= numberOfVariables - 1) {
                child = TERMINATOR;
            } else {
                child = new LeafNode(index + 1, bindings);
            }
            return child;
        }

        public boolean contains(int[] bindings) {
            return containsAtIndex(bindings, variableIndex);
        }

        @Override
        public void visit(IndexNodeVisitor visitor) {
            visitor.visit(this);
        }

        protected boolean containsAtIndex(int[] bindings, int index) {
            int optionInd = bindings[index];
            IndexNode child = getChildAtIndex(optionInd);
            if (child == null) {
                return false;
            }
            if (child.isTerminator()) {
                return true;
            }
            return child.contains(bindings);
        }

        public String toString() {
            return "I[" + variableIndex + "]: ";
        }

        @Override
        public int maxElementLength() {
            int max = -1;
            for (Iterator<Tuple<Integer, IndexNode>> it = children.iterator(); it.hasNext();) {
                Tuple<Integer, IndexNode> cur = it.next();
                if (cur.getRight() != null) {
                    max = Math.max(max, cur.getRight().maxElementLength());
                }
            }
            return max + 1;
        }

        @Override
        public int minElementLength() {
            int min = Integer.MAX_VALUE;
            for (Tuple<Integer, IndexNode> cur : children) {
                if (cur.getRight() != null) {
                    min = Math.min(min, cur.getRight().minElementLength());
                }
            }
            return min + 1;
        }
    }

    public InnerNodeWithCommonSequence newInnerNodeWithCommonSequence(int variableIndex, int[] bindings,
                    int commonSequenceLength) {
        int firstNonZero = IndexUtils.findFirstNonZero(bindings, variableIndex, commonSequenceLength);
        if (firstNonZero == -1) {
            return new InnerNodeWithCommonZeroSequence(variableIndex, commonSequenceLength);
        } else {
            return new InnerNodeWithCommonArraySequence(variableIndex, bindings, commonSequenceLength);
        }
    }

    private abstract class InnerNodeWithCommonSequence extends InnerNode {
        public InnerNodeWithCommonSequence(int variableIndex, int commonSequenceLength) {
            super(variableIndex, optionsPerVariable[variableIndex + commonSequenceLength] + 1);
            if (commonSequenceLength < 1) {
                throw new IllegalArgumentException(
                                "Common sequence length must be at least 1, but was: " + commonSequenceLength);
            }
        }

        @Override
        public boolean contains(int[] bindings) {
            return isCommonSequenceIdentical(bindings)
                            && containsAtIndex(bindings, variableIndex + getCommonSequenceLength());
        }

        protected abstract int getCommonSequenceLength();

        protected abstract boolean isCommonSequenceIdentical(int[] bindings);

        @Override
        public IndexNode put(int[] bindings) {
            int splitIndex = findSplitIndex(bindings);
            InnerNode replacement;
            if (splitIndex == -1) {
                return putAtIndex(bindings, variableIndex + getCommonSequenceLength());
            } else if (splitIndex == variableIndex) {
                replacement = new InnerNode(variableIndex);
            } else {
                replacement = newInnerNodeWithCommonSequence(variableIndex, bindings, splitIndex - variableIndex);
            }
            InnerNode nodeWithShortenedSequence = shortenSequenceBy(splitIndex - variableIndex + 1);
            int value = getValueAt(splitIndex);
            nodeWithShortenedSequence.setChildren(this.children);
            replacement.setChildAtIndex(value, nodeWithShortenedSequence);
            replacement.put(bindings);
            return replacement;
        };

        protected abstract int getValueAt(int i);

        protected abstract InnerNode shortenSequenceBy(int i);

        protected abstract int findSplitIndex(int[] bindings);
    }

    private class InnerNodeWithCommonZeroSequence extends InnerNodeWithCommonSequence {
        int commonSequenceLength;

        public InnerNodeWithCommonZeroSequence(int variableIndex, int commonSequenceLength) {
            super(variableIndex, commonSequenceLength);
            this.commonSequenceLength = commonSequenceLength;
        }

        @Override
        protected boolean isCommonSequenceIdentical(int[] bindings) {
            return IndexUtils.findFirstNonZero(bindings, variableIndex, commonSequenceLength) == -1;
        }

        @Override
        protected int getCommonSequenceLength() {
            return commonSequenceLength;
        }

        @Override
        protected int getValueAt(int i) {
            return 0;
        }

        @Override
        protected InnerNode shortenSequenceBy(int by) {
            if (by > commonSequenceLength) {
                throw new IllegalArgumentException(String.format(
                                "Cannot shorten sequence by more than its length, %d, but was asked to shorten by %d ",
                                commonSequenceLength, by));
            } else if (by == commonSequenceLength) {
                return new InnerNode(variableIndex + by);
            }
            return new InnerNodeWithCommonZeroSequence(variableIndex + by, commonSequenceLength - by);
        }

        @Override
        protected int findSplitIndex(int[] bindings) {
            return IndexUtils.findFirstNonZero(bindings, variableIndex, commonSequenceLength);
        }

        @Override
        public void visit(IndexNodeVisitor visitor) {
            visitor.visit(this);
        }

        public String toString() {
            return "IZ[" + variableIndex + "-" + (variableIndex + commonSequenceLength) + "]: "
                            + Arrays.toString(new int[commonSequenceLength]);
        }

        @Override
        public int maxElementLength() {
            return super.maxElementLength() + commonSequenceLength;
        }

        @Override
        public int minElementLength() {
            return super.minElementLength() + commonSequenceLength;
        }
    }

    private class InnerNodeWithCommonArraySequence extends InnerNodeWithCommonSequence {
        private int[] commonSequence;

        public InnerNodeWithCommonArraySequence(int variableIndex, int[] bindings, int commonSequenceLength) {
            super(variableIndex, commonSequenceLength);
            this.commonSequence = new int[commonSequenceLength];
            int firstNonZero = IndexUtils.findFirstNonZero(bindings, variableIndex, commonSequenceLength);
            System.arraycopy(bindings, variableIndex, commonSequence, 0,
                            commonSequenceLength);
        }

        @Override
        protected boolean isCommonSequenceIdentical(int[] bindings) {
            return IndexUtils.isSubsequenceAt(bindings, commonSequence, variableIndex);
        }

        @Override
        protected int getValueAt(int i) {
            return commonSequence[i - variableIndex];
        }

        @Override
        protected InnerNode shortenSequenceBy(int by) {
            if (by > commonSequence.length) {
                throw new IllegalArgumentException(String.format(
                                "Cannot shorten sequence by more than its length, %d, but was asked to shorten by %d ",
                                commonSequence.length, by));
            } else if (by == commonSequence.length) {
                return new InnerNode(variableIndex + by);
            }
            InnerNode seqNode = newInnerNodeWithCommonSequence(by, commonSequence, commonSequence.length - by);
            seqNode.variableIndex = variableIndex + by;
            return seqNode;
        }

        @Override
        protected int findSplitIndex(int[] bindings) {
            return IndexUtils.findFirstDifference(bindings, variableIndex, commonSequence, 0);
        }

        @Override
        protected int getCommonSequenceLength() {
            return commonSequence.length;
        }

        @Override
        public void visit(IndexNodeVisitor visitor) {
            visitor.visit(this);
        }

        public String toString() {
            return "IA[" + variableIndex + "-" + (variableIndex + commonSequence.length) + "]: "
                            + Arrays.toString(commonSequence);
        }

        @Override
        public int maxElementLength() {
            return super.maxElementLength() + commonSequence.length;
        }

        @Override
        public int minElementLength() {
            return super.minElementLength() + commonSequence.length;
        }
    }
}