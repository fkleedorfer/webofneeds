package won.utils.blend.support.index;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.lang.invoke.MethodHandles;
import java.util.*;

public class CompactBindingsIndexTests {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Random rnd = new Random();

    @Test
    public void testContainsOnEmptyIndex() {
        int[] optionsPerPosition = new int[] { 10, 10 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        Assertions.assertFalse(index.contains(new int[] { 3, 6 }));
    }

    @Test
    public void testShortenLengthOneCommonZeroSequence() {
        int[] optionsPerPosition = new int[] { 10, 10, 10, 10, 10, 10, 10 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        index.put(new int[] { 0, 3, 0, 7, 0, 0, 3 });
        index.put(new int[] { 1, 6, 10, 9, 4, 7, 4 });
        index.put(new int[] { 0, 4, 2, 8, 6, 0, 6 });
        index.put(new int[] { 2, 6, 0, 7, 5, 3, 5 });
        index.put(new int[] { 4, 4, 5, 6, 9, 9, 0 });
        index.put(new int[] { 2, 0, 0, 0, 0, 1, 0 });
        index.put(new int[] { 0, 5, 1, 0, 1, 9, 1 });
        index.put(new int[] { 8, 4, 0, 7, 0, 6, 3 });
        index.put(new int[] { 4, 0, 0, 6, 3, 4, 0 });
        index.put(new int[] { 0, 4, 0, 4, 0, 7, 1 });
        index.put(new int[] { 3, 0, 0, 0, 0, 4, 6 });
        index.put(new int[] { 7, 6, 3, 1, 8, 7, 2 });
        index.put(new int[] { 4, 0, 0, 0, 0, 9, 0 });
        System.out.println(index.treeToString());
        index.put(new int[] { 4, 0, 7, 0, 0, 0, 5 });
        System.out.println(index.treeToString());
        Assertions.assertTrue(index.contains(new int[] { 4, 0, 0, 6, 3, 4, 0 }));
    }

    @Test
    public void reproduceBug2() {
        int[] optionsPerPosition = new int[] { 10, 10, 10, 10, 10, 10, 10 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        index.put(new int[] { 0, 0, 0, 7, 4, 0, 7 });
        index.put(new int[] { 0, 0, 0, 4, 10, 0, 0 });
        index.put(new int[] { 0, 0, 0, 4, 0, 0, 0 });
        index.put(new int[] { 0, 0, 2, 10, 0, 0, 6 });
        index.put(new int[] { 0, 0, 6, 0, 0, 9, 0 });
        index.put(new int[] { 0, 5, 4, 0, 0, 0, 10 });
        index.put(new int[] { 0, 7, 0, 5, 0, 6, 0 });
        index.put(new int[] { 0, 3, 8, 0, 0, 2, 0 });
        index.put(new int[] { 0, 0, 0, 0, 3, 0, 6 });
        index.put(new int[] { 0, 0, 1, 7, 7, 0, 8 });
        index.put(new int[] { 5, 0, 0, 0, 10, 2, 2 });
        index.put(new int[] { 0, 0, 5, 9, 3, 0, 0 });
        index.put(new int[] { 10, 0, 8, 0, 4, 0, 0 });
        index.put(new int[] { 5, 5, 0, 0, 6, 7, 3 });
        index.put(new int[] { 2, 0, 0, 10, 0, 0, 8 });
        index.put(new int[] { 5, 0, 0, 3, 2, 0, 2 });
        index.put(new int[] { 0, 9, 6, 0, 8, 5, 9 });
        index.put(new int[] { 0, 5, 2, 7, 0, 0, 7 });
        index.put(new int[] { 0, 2, 0, 3, 0, 0, 8 });
        index.put(new int[] { 2, 2, 0, 4, 5, 0, 0 });
        index.put(new int[] { 0, 6, 0, 9, 0, 0, 5 });
        index.put(new int[] { 4, 5, 0, 0, 3, 0, 8 });
        index.put(new int[] { 0, 10, 9, 1, 6, 8, 8 });
        index.put(new int[] { 0, 4, 9, 0, 2, 1, 0 });
        index.put(new int[] { 8, 3, 4, 0, 10, 9, 6 });
        index.put(new int[] { 2, 0, 6, 0, 10, 2, 5 });
        index.put(new int[] { 0, 1, 0, 0, 9, 0, 4 });
        index.put(new int[] { 4, 6, 4, 0, 0, 9, 0 });
        index.put(new int[] { 10, 6, 0, 10, 9, 1, 0 });
        index.put(new int[] { 1, 0, 2, 0, 1, 2, 0 });
        index.put(new int[] { 0, 8, 3, 9, 0, 0, 9 });
        index.put(new int[] { 10, 7, 0, 10, 6, 9, 0 });
        index.put(new int[] { 8, 9, 0, 0, 0, 7, 7 });
        index.put(new int[] { 0, 0, 5, 0, 0, 6, 5 });
        index.put(new int[] { 9, 0, 0, 9, 0, 3, 0 });
        index.put(new int[] { 0, 0, 0, 0, 4, 0, 0 });
        index.put(new int[] { 0, 0, 0, 7, 9, 1, 9 });
        index.put(new int[] { 3, 9, 0, 0, 3, 7, 3 });
        index.put(new int[] { 0, 9, 4, 5, 6, 7, 0 });
        index.put(new int[] { 5, 4, 0, 0, 0, 10, 0 });
        index.put(new int[] { 4, 10, 5, 0, 3, 6, 10 });
        index.put(new int[] { 9, 0, 8, 6, 1, 8, 0 });
        index.put(new int[] { 7, 2, 3, 0, 5, 6, 0 });
        index.put(new int[] { 5, 0, 8, 1, 3, 9, 2 });
        index.put(new int[] { 4, 0, 0, 0, 0, 4, 0 });
        index.put(new int[] { 0, 2, 0, 0, 6, 10, 0 });
        index.put(new int[] { 1, 3, 9, 10, 4, 9, 0 });
        index.put(new int[] { 0, 0, 0, 2, 2, 0, 3 });
        index.put(new int[] { 2, 2, 7, 0, 0, 0, 0 });
        index.put(new int[] { 0, 2, 8, 0, 2, 0, 10 });
        index.put(new int[] { 0, 7, 7, 0, 10, 0, 8 });
        index.put(new int[] { 6, 4, 5, 0, 0, 6, 0 });
        index.put(new int[] { 1, 6, 0, 4, 0, 1, 3 });
        index.put(new int[] { 0, 5, 0, 3, 0, 9, 7 });
        index.put(new int[] { 7, 4, 2, 0, 7, 0, 0 });
        index.put(new int[] { 10, 5, 0, 5, 0, 4, 4 });
        index.put(new int[] { 0, 1, 0, 0, 8, 0, 8 });
        index.put(new int[] { 2, 8, 2, 0, 0, 10, 5 });
        index.put(new int[] { 0, 7, 10, 9, 4, 5, 4 });
        index.put(new int[] { 3, 9, 1, 0, 2, 0, 0 });
        index.put(new int[] { 10, 1, 0, 0, 6, 0, 0 });
        index.put(new int[] { 2, 0, 9, 10, 8, 1, 3 });
        index.put(new int[] { 5, 6, 0, 10, 0, 0, 0 });
        index.put(new int[] { 0, 4, 1, 4, 0, 5, 10 });
        index.put(new int[] { 9, 0, 0, 0, 4, 0, 0 });
        index.put(new int[] { 8, 0, 7, 0, 0, 0, 0 });
        index.put(new int[] { 6, 0, 6, 2, 9, 5, 8 });
        index.put(new int[] { 4, 0, 2, 0, 0, 2, 4 });
        index.put(new int[] { 7, 3, 5, 0, 6, 4, 1 });
        index.put(new int[] { 3, 4, 7, 0, 4, 2, 0 });
        index.put(new int[] { 0, 0, 10, 8, 0, 9, 1 });
        index.put(new int[] { 7, 5, 7, 0, 1, 7, 5 });
        index.put(new int[] { 0, 4, 2, 3, 8, 0, 1 });
        index.put(new int[] { 6, 0, 0, 9, 7, 6, 1 });
        index.put(new int[] { 6, 7, 7, 0, 0, 2, 9 });
        index.put(new int[] { 0, 5, 0, 0, 10, 4, 0 });
        index.put(new int[] { 8, 0, 0, 0, 7, 0, 8 });
        index.put(new int[] { 0, 8, 6, 4, 10, 0, 0 });
        index.put(new int[] { 0, 7, 7, 4, 0, 10, 0 });
        index.put(new int[] { 0, 2, 0, 1, 0, 0, 10 });
        index.put(new int[] { 9, 7, 4, 5, 10, 9, 9 });
        index.put(new int[] { 2, 6, 10, 0, 6, 0, 6 });
        index.put(new int[] { 1, 0, 3, 2, 1, 1, 0 });
        index.put(new int[] { 0, 0, 2, 2, 0, 7, 0 });
        index.put(new int[] { 1, 3, 3, 3, 0, 5, 9 });
        index.put(new int[] { 5, 3, 0, 4, 10, 0, 6 });
        index.put(new int[] { 0, 4, 0, 2, 8, 0, 1 });
        index.put(new int[] { 0, 0, 0, 6, 2, 10, 0 });
        index.put(new int[] { 4, 0, 0, 0, 0, 3, 10 });
        index.put(new int[] { 0, 0, 0, 0, 0, 0, 8 });
        index.put(new int[] { 6, 8, 1, 0, 7, 0, 7 });
        index.put(new int[] { 5, 9, 1, 0, 0, 2, 0 });
        index.put(new int[] { 0, 0, 0, 0, 10, 1, 0 });
        index.put(new int[] { 0, 0, 0, 2, 0, 0, 0 });
        index.put(new int[] { 0, 9, 9, 0, 0, 10, 3 });
        index.put(new int[] { 6, 6, 4, 9, 7, 1, 4 });
        index.put(new int[] { 4, 4, 0, 0, 0, 0, 0 });
        index.put(new int[] { 0, 10, 0, 0, 0, 0, 0 });
        index.put(new int[] { 9, 0, 0, 7, 0, 2, 0 });
        index.put(new int[] { 4, 0, 6, 7, 2, 0, 1 });
        index.put(new int[] { 0, 0, 10, 4, 4, 6, 3 });
        index.put(new int[] { 8, 1, 9, 7, 0, 0, 0 });
        index.put(new int[] { 5, 0, 0, 10, 9, 1, 4 });
        index.put(new int[] { 9, 10, 7, 0, 10, 5, 0 });
        index.put(new int[] { 5, 5, 4, 0, 6, 8, 4 });
        index.put(new int[] { 0, 0, 3, 4, 6, 0, 3 });
        index.put(new int[] { 9, 0, 4, 6, 7, 5, 0 });
        index.put(new int[] { 0, 0, 5, 0, 1, 6, 0 });
        index.put(new int[] { 0, 2, 5, 0, 0, 0, 10 });
        index.put(new int[] { 0, 4, 8, 0, 0, 7, 10 });
        index.put(new int[] { 0, 0, 1, 10, 1, 2, 0 });
        index.put(new int[] { 5, 0, 0, 6, 0, 0, 7 });
        index.put(new int[] { 3, 0, 0, 0, 2, 8, 0 });
        index.put(new int[] { 0, 0, 8, 8, 0, 0, 0 });
        index.put(new int[] { 5, 5, 0, 7, 4, 2, 2 });
        index.put(new int[] { 5, 0, 0, 0, 0, 10, 9 });
        index.put(new int[] { 5, 3, 0, 0, 0, 0, 0 });
        index.put(new int[] { 9, 0, 0, 9, 5, 8, 3 });
        index.put(new int[] { 3, 0, 0, 0, 2, 0, 6 });
        index.put(new int[] { 8, 5, 0, 0, 4, 0, 9 });
        index.put(new int[] { 1, 0, 0, 0, 0, 0, 2 });
        index.put(new int[] { 7, 9, 0, 0, 7, 3, 10 });
        index.put(new int[] { 0, 4, 7, 0, 0, 0, 0 });
        index.put(new int[] { 0, 0, 5, 0, 0, 0, 0 });
        index.put(new int[] { 0, 9, 0, 9, 10, 0, 0 });
        index.put(new int[] { 3, 0, 8, 8, 2, 8, 0 });
        index.put(new int[] { 0, 5, 8, 0, 0, 0, 4 });
        index.put(new int[] { 0, 0, 0, 10, 0, 1, 0 });
        index.put(new int[] { 4, 10, 10, 10, 5, 0, 3 });
        index.put(new int[] { 0, 3, 0, 0, 0, 0, 6 });
        index.put(new int[] { 7, 3, 10, 0, 0, 0, 4 });
        index.put(new int[] { 1, 2, 0, 9, 1, 5, 2 });
        index.put(new int[] { 0, 0, 0, 9, 0, 0, 4 });
        index.put(new int[] { 10, 0, 10, 0, 0, 0, 0 });
        index.put(new int[] { 1, 3, 9, 0, 0, 5, 0 });
        index.put(new int[] { 0, 0, 8, 9, 3, 0, 0 });
        index.put(new int[] { 0, 7, 0, 8, 9, 0, 7 });
        index.put(new int[] { 2, 2, 9, 10, 0, 4, 0 });
        index.put(new int[] { 0, 0, 2, 0, 10, 0, 0 });
        index.put(new int[] { 4, 1, 5, 0, 0, 1, 0 });
        index.put(new int[] { 10, 4, 0, 0, 1, 10, 0 });
        index.put(new int[] { 1, 2, 0, 3, 2, 6, 0 });
        index.put(new int[] { 9, 6, 0, 0, 1, 0, 0 });
        index.put(new int[] { 7, 0, 7, 4, 5, 8, 0 });
        index.put(new int[] { 5, 4, 2, 0, 2, 10, 9 });
        index.put(new int[] { 3, 5, 0, 2, 7, 8, 0 });
        index.put(new int[] { 0, 4, 1, 0, 9, 0, 0 });
        index.put(new int[] { 0, 4, 0, 1, 0, 6, 0 });
        index.put(new int[] { 0, 8, 0, 0, 4, 0, 6 });
        index.put(new int[] { 0, 3, 4, 1, 0, 2, 6 });
        index.put(new int[] { 0, 2, 2, 4, 0, 0, 8 });
        index.put(new int[] { 1, 8, 7, 5, 5, 0, 2 });
        index.put(new int[] { 0, 0, 8, 0, 9, 2, 2 });
        index.put(new int[] { 7, 1, 2, 8, 9, 4, 5 });
        System.out.println(index.treeToString());
        Assertions.assertFalse(index.contains(new int[] { 3, 0, 0, 2, 2, 0, 6 }));
    }

    @Test
    public void reproduceBug3() {
        int[] optionsPerPosition = new int[] { 10, 10, 10, 10, 10, 10, 10 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        index.put(new int[] { 5, 0, 2, 0, 0, 1, 0 });
        index.put(new int[] { 9, 0, 0, 0, 4, 5, 0 });
        index.put(new int[] { 7, 8, 5, 0, 1, 8, 7 });
        index.put(new int[] { 0, 0, 5, 10, 1, 4, 9 });
        index.put(new int[] { 0, 0, 9, 3, 10, 0, 10 });
        index.put(new int[] { 3, 3, 7, 0, 1, 0, 10 });
        index.put(new int[] { 10, 0, 0, 0, 0, 7, 6 });
        index.put(new int[] { 9, 9, 0, 0, 0, 1, 8 });
        index.put(new int[] { 2, 0, 0, 0, 0, 0, 3 });
        index.put(new int[] { 2, 0, 10, 0, 0, 0, 0 });
        index.put(new int[] { 10, 0, 0, 10, 5, 0, 5 });
        index.put(new int[] { 10, 1, 0, 0, 0, 9, 5 });
        index.put(new int[] { 1, 0, 4, 3, 7, 9, 0 });
        index.put(new int[] { 2, 0, 10, 0, 5, 0, 6 });
        index.put(new int[] { 0, 0, 9, 4, 7, 6, 10 });
        index.put(new int[] { 0, 0, 0, 1, 0, 1, 0 });
        index.put(new int[] { 0, 9, 4, 0, 0, 4, 4 });
        System.out.println(index.treeToString());
        checkSizeInvariant(index);
    }

    @Test
    public void testShortenCommonZeroSequenceByMoreThanOne() {
        int[] optionsPerPosition = new int[] { 10, 10, 10, 10, 10, 10, 10 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        index.put(new int[] { 0, 2, 4, 0, 0, 4, 3 });
        index.put(new int[] { 0, 9, 6, 0, 8, 4, 8 });
        index.put(new int[] { 0, 0, 0, 0, 0, 3, 10 });
        index.put(new int[] { 1, 7, 2, 0, 3, 0, 0 });
        index.put(new int[] { 1, 0, 0, 0, 0, 0, 6 });
        index.put(new int[] { 3, 0, 0, 1, 0, 0, 0 });
        index.put(new int[] { 5, 3, 0, 0, 0, 0, 0 });
        index.put(new int[] { 3, 9, 7, 1, 5, 0, 2 });
        index.put(new int[] { 0, 0, 0, 0, 1, 0, 6 });
        index.put(new int[] { 3, 0, 0, 1, 0, 6, 0 });
        index.put(new int[] { 4, 5, 3, 2, 8, 3, 10 });
        index.put(new int[] { 0, 0, 0, 0, 0, 3, 3 });
        index.put(new int[] { 10, 0, 0, 8, 6, 0, 0 });
        index.put(new int[] { 6, 0, 0, 0, 0, 0, 0 });
        index.put(new int[] { 0, 4, 3, 6, 1, 0, 7 });
        index.put(new int[] { 9, 5, 0, 4, 5, 8, 8 });
        index.put(new int[] { 0, 1, 7, 0, 9, 0, 9 });
        index.put(new int[] { 0, 2, 2, 7, 0, 0, 0 });
        index.put(new int[] { 10, 0, 0, 0, 0, 0, 0 });
        System.out.println(index.treeToString());
        Assertions.assertTrue(index.contains(new int[] { 0, 0, 0, 0, 1, 0, 6 }));
        index.put(new int[] { 0, 0, 0, 6, 0, 0, 0 });
        System.out.println(index.treeToString());
        checkSizeInvariant(index);
        Assertions.assertTrue(index.contains(new int[] { 0, 0, 0, 0, 1, 0, 6 }));
    }

    @Test
    public void splitNodeWithCommonZeros() {
        int[] optionsPerPosition = new int[] { 10, 10, 10, 10, 10, 10, 10 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        index.put(new int[] { 1, 0, 7, 9, 0, 0, 3 });
        index.put(new int[] { 0, 0, 0, 7, 0, 2, 5 });
        index.put(new int[] { 9, 1, 0, 0, 0, 0, 9 });
        index.put(new int[] { 0, 0, 4, 0, 0, 0, 0 });
        index.put(new int[] { 1, 0, 9, 0, 4, 7, 0 });
        index.put(new int[] { 2, 0, 0, 5, 4, 0, 2 });
        index.put(new int[] { 2, 9, 0, 0, 0, 0, 0 });
        index.put(new int[] { 0, 0, 1, 5, 0, 0, 8 });
        checkSizeInvariant(index);
        System.out.println(index.treeToString());
        index.put(new int[] { 1, 9, 3, 7, 0, 3, 0 });
        System.out.println(index.treeToString());
        checkSizeInvariant(index);
    }

    @Test
    public void testShortenCommonArraySequenceByMoreThanOne() {
        int[] optionsPerPosition = new int[] { 10, 10, 10, 10, 10, 10, 10 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        index.put(new int[] { 3, 0, 0, 1, 7, 3, 8 });
        index.put(new int[] { 1, 0, 8, 0, 6, 2, 8 });
        index.put(new int[] { 6, 6, 9, 4, 0, 9, 0 });
        index.put(new int[] { 0, 2, 10, 7, 6, 0, 6 });
        index.put(new int[] { 8, 5, 4, 10, 3, 7, 6 });
        index.put(new int[] { 0, 9, 7, 0, 10, 10, 8 });
        index.put(new int[] { 0, 0, 0, 0, 0, 0, 0 });
        index.put(new int[] { 7, 2, 4, 0, 0, 0, 7 });
        index.put(new int[] { 7, 7, 6, 0, 6, 4, 8 });
        index.put(new int[] { 8, 4, 4, 0, 10, 0, 0 });
        index.put(new int[] { 0, 10, 0, 0, 2, 3, 0 });
        index.put(new int[] { 1, 0, 9, 2, 10, 1, 0 });
        index.put(new int[] { 0, 10, 10, 0, 3, 0, 4 });
        index.put(new int[] { 5, 7, 4, 0, 0, 5, 7 });
        index.put(new int[] { 5, 1, 2, 5, 10, 10, 0 });
        index.put(new int[] { 0, 0, 4, 0, 1, 0, 0 });
        index.put(new int[] { 10, 9, 3, 6, 3, 0, 4 });
        index.put(new int[] { 0, 9, 6, 0, 0, 0, 2 });
        index.put(new int[] { 0, 6, 3, 0, 0, 0, 7 });
        index.put(new int[] { 10, 0, 0, 6, 2, 2, 0 });
        index.put(new int[] { 0, 0, 0, 0, 9, 0, 0 });
        index.put(new int[] { 1, 3, 0, 3, 0, 0, 9 });
        index.put(new int[] { 6, 0, 9, 0, 0, 0, 0 });
        index.put(new int[] { 0, 2, 0, 0, 10, 0, 0 });
        index.put(new int[] { 9, 7, 5, 8, 0, 0, 3 });
        index.put(new int[] { 0, 7, 7, 6, 0, 5, 0 });
        index.put(new int[] { 0, 5, 0, 1, 0, 0, 6 });
        index.put(new int[] { 0, 0, 0, 1, 3, 0, 10 });
        index.put(new int[] { 0, 2, 10, 9, 0, 10, 0 });
        index.put(new int[] { 4, 4, 0, 0, 0, 0, 0 });
        index.put(new int[] { 0, 6, 2, 0, 9, 4, 9 });
        index.put(new int[] { 9, 7, 8, 0, 0, 2, 0 });
        index.put(new int[] { 1, 0, 7, 4, 0, 0, 0 });
        index.put(new int[] { 0, 10, 3, 0, 2, 2, 0 });
        index.put(new int[] { 2, 6, 5, 7, 9, 0, 7 });
        Assertions.assertTrue(index.contains(new int[] { 9, 7, 8, 0, 0, 2, 0 }));
        System.out.println(index.treeToString());
        index.put(new int[] { 9, 0, 9, 0, 3, 6, 0 });
        System.out.println(index.treeToString());
        Assertions.assertTrue(index.contains(new int[] { 9, 7, 8, 0, 0, 2, 0 }));
    }

    @Test
    @Disabled
    public void extensiveSmokeTest() {
        boolean verboseOutput = false;
        boolean codeOutput = true;
        int[] optionsPerPosition = new int[] { 10, 10, 10, 10, 10, 10, 10 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        String oldTree = null;
        try {
            Set<List<Integer>> contents = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                oldTree = index.treeToString();
                int[] elem = makeRandomArray(optionsPerPosition, 0.4, 0.8);
                List<Integer> elemAsList = makeIntegerList(elem);
                contents.add(elemAsList);
                if (codeOutput) {
                    System.out.println(String.format("index.put(new int[] %s );",
                                    Arrays.toString(elem).replaceAll("\\[", "{").replaceAll("\\]", "}")));
                }
                int size = index.size();
                boolean alreadyPresent = index.contains(elem);
                index.put(elem);
                checkSizeInvariant(index);
                int expectedSize = alreadyPresent ? size : size + 1;
                Assertions.assertEquals(expectedSize, index.size(),
                                "expected index to be of size " + expectedSize + " but was " + index.size());
                for (int j = 0; j < 10; j++) {
                    int[] check = makeRandomArray(optionsPerPosition, 0.4, 0.8);
                    if (contents.contains(makeIntegerList(check))) {
                        Assertions.assertTrue(index.contains(check),
                                        "expected in index but isn't: " + Arrays.toString(check));
                    } else {
                        Assertions.assertFalse(index.contains(check),
                                        "not expected in index but is: " + Arrays.toString(check));
                    }
                    for (List<Integer> content : contents) {
                        int[] check2 = content.stream().mapToInt(nmb -> nmb.intValue()).toArray();
                        Assertions.assertTrue(index.contains(check2), "we already put " + Arrays.toString(check2)
                                        + " in the index but now it does not contain it");
                    }
                }
                Assertions.assertTrue(index.contains(elem));
                if (verboseOutput) {
                    System.out.println(index.treeToString());
                    System.out.println("------------------");
                }
            }
        } catch (AssertionFailedError e) {
            System.out.println("Tree before last insert:");
            System.out.println(oldTree);
            System.out.println("Tree after last insert:");
            System.out.println(index.treeToString());
            throw e;
        }

    }

    private List<Integer> makeIntegerList(int[] elem) {
        List<Integer> elemAsList = new ArrayList<>(elem.length);
        for (int j = 0; j < elem.length; j++) {
            elemAsList.add(j, elem[j]);
        }
        return elemAsList;
    }

    @Test
    public void testSplitAtStart() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        index.put(new int[] { 0, 0, 0, 0, 0 });
        Assertions.assertTrue(index.contains(new int[] { 0, 0, 0, 0, 0 }));
        index.put(new int[] { 1, 1, 1, 1, 1 });
        Assertions.assertTrue(index.contains(new int[] { 1, 1, 1, 1, 1 }));
        checkSizeInvariant(index);
    }

    @Test
    public void testSplitAtEnd() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        index.put(new int[] { 0, 0, 0, 0, 0 });
        Assertions.assertTrue(index.contains(new int[] { 0, 0, 0, 0, 0 }));
        index.put(new int[] { 0, 0, 0, 0, 1 });
        Assertions.assertTrue(index.contains(new int[] { 0, 0, 0, 0, 1 }));
        System.out.println(index.treeToString());
        checkSizeInvariant(index);
    }

    @Test
    public void testSplitWithCommonZeroSequence() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        index.put(new int[] { 0, 0, 0, 0, 0 });
        Assertions.assertTrue(index.contains(new int[] { 0, 0, 0, 0, 0 }));
        index.put(new int[] { 0, 0, 1, 1, 1 });
        Assertions.assertTrue(index.contains(new int[] { 0, 0, 1, 1, 1 }));
        checkSizeInvariant(index);
    }

    @Test
    public void testSplitCommonZeroSequence_atStart() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        index.put(new int[] { 1, 0, 0, 0, 0 });
        index.put(new int[] { 2, 0, 0, 0, 0 });
        index.put(new int[] { 2, 0, 0, 1, 1 });
        System.out.println(index.treeToString());
        Assertions.assertTrue(index.contains(new int[] { 2, 0, 0, 0, 0 }));
        Assertions.assertTrue(index.contains(new int[] { 2, 0, 0, 1, 1 }));
        Assertions.assertTrue(index.contains(new int[] { 1, 0, 0, 0, 0 }));
        index.put(new int[] { 2, 1, 0, 1, 1 });
        System.out.println(index.treeToString());
        Assertions.assertTrue(index.contains(new int[] { 2, 0, 0, 0, 0 }));
        Assertions.assertTrue(index.contains(new int[] { 2, 0, 0, 1, 1 }));
        Assertions.assertTrue(index.contains(new int[] { 2, 1, 0, 1, 1 }));
        Assertions.assertTrue(index.contains(new int[] { 1, 0, 0, 0, 0 }));
    }

    @Test
    public void putExisting() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        index.put(new int[] { 1, 2, 3, 4, 5 });
        Assertions.assertTrue(index.contains(new int[] { 1, 2, 3, 4, 5 }));
        index.put(new int[] { 1, 2, 3, 4, 5 });
        Assertions.assertTrue(index.contains(new int[] { 1, 2, 3, 4, 5 }));
        checkSizeInvariant(index);
    }

    @Test
    public void putExistingWithInnerNodeAtEnd() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        index.put(new int[] { 1, 2, 2, 4, 5 });
        index.put(new int[] { 1, 2, 2, 4, 6 });
        index.put(new int[] { 1, 2, 2, 4, 6 });
        checkSizeInvariant(index);
    }

    @Test
    public void speedTest() {
        int[] optionsPerPosition = new int[] { 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        StopWatch sw = new StopWatch();
        sw.start();
        for (int i = 0; i < 1000000; i++) {
            int[] elem = makeRandomArray(optionsPerPosition);
            index.put(elem);
        }
        logger.debug("size:" + index.size());
        for (int i = 0; i < 1000000; i++) {
            int[] elem = makeRandomArray(optionsPerPosition);
            index.contains(elem);
        }
        sw.stop();
        logger.debug("time taken: " + sw.getLastTaskTimeMillis() + " ms");
    }

    @Test
    @Disabled
    public void speedComparison() {
        int[] optionsPerPosition = new int[] { 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50 };
        CompactBindingsIndex index = new CompactBindingsIndex(optionsPerPosition);
        StopWatch sw = new StopWatch();
        sw.start();
        int ITERATIONS = 10_000_000;
        for (int i = 0; i < ITERATIONS; i++) {
            int[] elem = makeRandomArray(optionsPerPosition, 0.4, 0.8);
            index.put(elem);
        }
        sw.stop();
        //System.out.println("size: " + index.memorySize() + " bytes");
        System.out.println("index size: " + GraphLayout.parseInstance(index).totalSize() + " bytes");
        System.out.println("time taken: " + sw.getLastTaskTimeMillis() + " ms");
        sw = new StopWatch();
        sw.start();
        for (int i = 0; i < ITERATIONS; i++) {
            int[] elem = makeRandomArray(optionsPerPosition, 0.4, 0.8);
            index.contains(elem);
        }
        sw.stop();
        System.out.println("time taken: " + sw.getLastTaskTimeMillis() + " ms");
        sw = new StopWatch();
        Set<List<Integer>> index2 = new HashSet<>();
        sw.start();
        for (int i = 0; i < ITERATIONS; i++) {
            List<Integer> elem = makeRandomList(optionsPerPosition, 0.4, 0.8);
            index2.add(elem);
        }
        sw.stop();
        //System.out.println("size: " + index2.stream().mapToLong(x -> InstrumentationAgent.getObjectSize(x)).sum() + " bytes");
        System.out.println("int list size: " + GraphLayout.parseInstance(index2).totalSize() + " bytes");
        System.out.println("time taken: " + sw.getLastTaskTimeMillis() + " ms");
        sw = new StopWatch();
        sw.start();
        for (int i = 0; i < ITERATIONS; i++) {
            List<Integer> elem = makeRandomList(optionsPerPosition, 0.4, 0.8);
            index2.contains(elem);
        }
        sw.stop();
        System.out.println("time taken: " + sw.getLastTaskTimeMillis() + " ms");
        sw = new StopWatch();
        Set<List<Byte>> index3 = new HashSet<>();
        sw.start();
        for (int i = 0; i < ITERATIONS; i++) {
            List<Byte> elem = makeRandomByteList(optionsPerPosition, 0.4, 0.8);
            index3.add(elem);
        }
        sw.stop();
        //System.out.println("size: " + index2.stream().mapToLong(x -> InstrumentationAgent.getObjectSize(x)).sum() + " bytes");
        System.out.println("byte list size: " + GraphLayout.parseInstance(index3).totalSize() + " bytes");
        System.out.println("time taken: " + sw.getLastTaskTimeMillis() + " ms");
        sw = new StopWatch();
        sw.start();
        for (int i = 0; i < ITERATIONS; i++) {
            List<Byte> elem = makeRandomByteList(optionsPerPosition, 0.4, 0.8);
            index3.contains(elem);
        }
        sw.stop();
        System.out.println("time taken: " + sw.getLastTaskTimeMillis() + " ms");
    }

    private int[] makeRandomArray(int[] optionsPerPosition) {
        return makeRandomArray(optionsPerPosition, 0, 0);
    }

    private int[] makeRandomArray(int[] optionsPerPosition, double zerobias, double skew) {
        int[] rndArray = new int[optionsPerPosition.length];
        for (int i = 0; i < rndArray.length; i++) {
            double weightedBias = zerobias + ((double) i - ((double) optionsPerPosition.length / 2))
                            / (double) optionsPerPosition.length * skew;
            rndArray[i] = rnd.nextDouble() < weightedBias ? 0 : rnd.nextInt(optionsPerPosition[i] + 1);
        }
        return rndArray;
    }

    private List<Integer> makeRandomList(int[] optionsPerPosition) {
        return makeRandomList(optionsPerPosition, 0, 0);
    }

    private List<Integer> makeRandomList(int[] optionsPerPosition, double zerobias, double skew) {
        List<Integer> rndList = new ArrayList(optionsPerPosition.length);
        for (int i = 0; i < optionsPerPosition.length; i++) {
            double weightedBias = zerobias + ((double) i - ((double) optionsPerPosition.length / 2))
                            / (double) optionsPerPosition.length * skew;
            rndList.add(i, rnd.nextDouble() < weightedBias ? 0 : rnd.nextInt(optionsPerPosition[i] + 1));
        }
        return rndList;
    }

    private List<Byte> makeRandomByteList(int[] optionsPerPosition) {
        return makeRandomByteList(optionsPerPosition, 0, 0);
    }

    private List<Byte> makeRandomByteList(int[] optionsPerPosition, double zerobias, double skew) {
        List<Byte> rndList = new ArrayList(optionsPerPosition.length);
        for (int i = 0; i < optionsPerPosition.length; i++) {
            double weightedBias = zerobias + ((double) i - ((double) optionsPerPosition.length / 2))
                            / (double) optionsPerPosition.length * skew;
            rndList.add(i, rnd.nextDouble() < weightedBias ? 0 : (byte) rnd.nextInt(optionsPerPosition[i] + 1));
        }
        return rndList;
    }

    @Test
    public void testWrongArraySize() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        Assertions.assertThrows(IllegalArgumentException.class, () -> index.put(new int[] { 0, 0, 0, 0 }));
    }

    @Test
    public void testWrongArraySize2() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        Assertions.assertThrows(IllegalArgumentException.class, () -> index.put(new int[] { 0, 0, 0, 0, 0, 0 }));
    }

    @Test
    public void testWrongArrayContents1() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        index.put(new int[] { 0, 0, -1, 0, 0 });
    }

    @Test
    public void testWrongArrayContents2() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        index.put(new int[] { 0, 0, 3, 0, 0 });
    }

    @Test
    public void testWrongArrayContents3() {
        CompactBindingsIndex index = new CompactBindingsIndex(new int[] { 5, 8, 2, 4, 7 });
        index.put(new int[] { 0, 0, 3, 0, 0 });
    }

    private void checkSizeInvariant(CompactBindingsIndex index) {
        Assertions.assertEquals(index.getNumberOfVariables(), index.maxElementLength());
        Assertions.assertEquals(index.getNumberOfVariables(), index.minElementLength());
    }
}
