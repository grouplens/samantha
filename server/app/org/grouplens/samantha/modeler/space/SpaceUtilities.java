package org.grouplens.samantha.modeler.space;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SpaceUtilities {
    static void fillReadWriteLocks(List<Lock> readLocks,
                                   List<Lock> writeLocks,
                                   int curSize, int size) {
        if (curSize < size) {
            for (int i=curSize; i<size; i++) {
                ReentrantReadWriteLock nrwl = new ReentrantReadWriteLock();
                readLocks.add(nrwl.readLock());
                writeLocks.add(nrwl.writeLock());
            }
        }
    }
}
