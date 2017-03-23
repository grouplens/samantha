package org.grouplens.samantha.ephemeral;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;

import java.util.*;

public final class TopNAccumulator<R> {
    private final int n;
    private DoubleArrayList scores;
    private ArrayList<R> items;

    // The index of the empty space to use.  Once the accumulator is at capacity, this will be the
    // index of the last-removed item.
    private int slot;
    // The current size of the accumulator.
    private int size;
    // The number of items added to the accumulator.
    private int count;
    private IntHeapPriorityQueue heap;

    /**
     * Create a new accumulator to accumulate the top <var>n</var> IDs.
     *
     * @param n The number of IDs to retain.
     */
    public TopNAccumulator(int n) {
        this.n = n;

        slot = 0;
        size = 0;
        count = 0;

        // heap must have n+1 slots to hold extra item before removing smallest
        heap = new IntHeapPriorityQueue(n + 1, new SlotComparator());

        // item lists are lazy-allocated
    }

    /**
     * Find a good initial size to minimize the overhead when up to <em>n</em> items are added to a
     * list.
     *
     * @param maxSize The maximum number of items expected.
     * @return A size in the range [10,25] that, when used as the initial size of an array
     *         list, minimizes the overhead when {@code maxSize} items have been added.
     */
    private static int findInitialSize(int maxSize) {
        int best = 10;
        int overhead = maxSize;
        for (int i = 10; i <= 25; i++) {
            int cap = i;
            while (cap < maxSize) {
                cap *= 2;
            }
            int ovh = maxSize - cap;
            if (ovh < overhead) {
                overhead = ovh;
                best = i;
            }
        }
        return best;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void put(R item, double score) {
        assert slot <= n;
        assert heap.size() == size;

        if (items == null) {
            int isize = findInitialSize(n + 1);

            scores = new DoubleArrayList(isize);
            items = new ArrayList<R>(isize);
        }

        assert items.size() == scores.size();

        /*
         * Store the new item. The slot shows where the current item is, and
         * then we deal with it based on whether we're oversized.
         */
        if (slot == items.size()) {
            items.add(item);
            scores.add(score);
        } else {
            items.set(slot, item);
            scores.set(slot, score);
        }
        heap.enqueue(slot);

        if (size == n) {
            // already at capacity, so remove and reuse smallest item
            slot = heap.dequeue();
        } else {
            // we have free space, so increment the slot and size
            slot += 1;
            size += 1;
        }

        count++;
    }

    public R max() {
        return finishList().get(0);
    }

    public List<R> finishList() {
        assert size == heap.size();

        List<R> result = new ArrayList<>(n);
        while (!heap.isEmpty()) {
            result.add(items.get(heap.dequeueInt()));
        }
        clear();
        return Lists.reverse(result);
    }

    public Map<R, Double>  finishMap() {
        assert size == heap.size();

        Map<R, Double> result = new HashMap<>();
        while (!heap.isEmpty()) {
            int i = heap.dequeueInt();
            result.put(items.get(i), scores.getDouble(i));
        }
        clear();
        return result;
    }

    public int count() {
        return this.count;
    }

    private void clear() {
        size = 0;
        slot = 0;
        count = 0;
        items = null;
        scores = null;
    }

    /**
     * Compare two positions by comparing their scores.
     */
    private class SlotComparator extends AbstractIntComparator {
        @Override
        public int compare(int i, int j) {
            return Doubles.compare(scores.getDouble(i), scores.getDouble(j));
        }
    }
}
