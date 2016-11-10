package org.grouplens.samantha.modeler.space;

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
