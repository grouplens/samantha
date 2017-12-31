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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.Serializable;

public class ObjectKeyIndex<K> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Object2IntOpenHashMap<K> key2idx;
    private ObjectArrayList<K> keyList;

    public ObjectKeyIndex() {
        this.key2idx = new Object2IntOpenHashMap<>();
        this.keyList = new ObjectArrayList<>();
    }

    public int getIndex(K key) {
        return key2idx.getInt(key);
    }

    public K getKey(int idx) {
        return keyList.get(idx);
    }

    public boolean containsKey(K key) {
        return key2idx.containsKey(key);
    }

    public int size() {
        return keyList.size();
    }

    public int setKey(K key) {
        if (key2idx.containsKey(key)) {
            return key2idx.getInt(key);
        } else {
            int idx = keyList.size();
            key2idx.put(key, idx);
            keyList.add(key);
            return idx;
        }
    }
}
