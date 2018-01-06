/*
 * Copyright (c) [2016-2017] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.modeler.model;

import org.grouplens.samantha.server.exception.BadRequestException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
            if (keyMap.get(name).containsKey(key)) {
                return keyMap.get(name).getIndex(key);
            } else {
                return keyMap.get(name).setKey(key);
            }
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

    private void writeObject(ObjectOutputStream stream) {
        readLock.lock();
        try {
            stream.defaultWriteObject();
        } catch (IOException e) {
            throw new BadRequestException(e);
        } finally {
            readLock.unlock();
        }
    }
}
