package org.grouplens.samantha.modeler.space;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class SynchronizedIndexSpace implements IndexSpace {
    private static final long serialVersionUID = 1L;
    private final Map<String, ObjectKeyIndex<Object>> keyMap = new HashMap<>();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.readLock();
    private final Lock writeLock = rwl.writeLock();

    @Inject
    public SynchronizedIndexSpace() {}

    public void setSpaceState(String spaceName, SpaceMode spaceMode) {}

    public void publishSpaceVersion() {}

    public void requestKeyMap(String name) {
        writeLock.lock();
        try {
            keyMap.put(name, new ObjectKeyIndex<>());
        } finally {
            writeLock.unlock();
        }
    }

    public boolean hasKeyMap(String name) {
        readLock.lock();
        try {
            return keyMap.containsKey(name);
        } finally {
            readLock.unlock();
        }
    }

    public int getKeyMapSize(String name) {
        readLock.lock();
        try {
            return keyMap.get(name).size();
        } finally {
            readLock.unlock();
        }
    }

    public int setKey(String name, Object key) {
        writeLock.lock();
        try {
            return keyMap.get(name).setKey(key);
        } finally {
            writeLock.unlock();
        }
    }

    public boolean containsKey(String name, Object key) {
        readLock.lock();
        try {
            return keyMap.get(name).containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    public int getIndexForKey(String name, Object key) {
        readLock.lock();
        try {
            return keyMap.get(name).getIndex(key);
        } finally {
            readLock.unlock();
        }
    }

    public Object getKeyForIndex (String name, int index) {
        readLock.lock();
        try {
            return keyMap.get(name).getKey(index);
        } finally {
            readLock.unlock();
        }
    }
}
