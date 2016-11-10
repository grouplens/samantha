package org.grouplens.samantha.server.space;

import org.grouplens.samantha.modeler.space.IndexSpace;

public class RedisBasedIndexSpace implements IndexSpace {
    private static final long serialVersionUID = 1L;

    public void setSpaceName(String spaceName) {}

    public void requestKeyMap(String name) {

    }

    public boolean hasKeyMap(String name) {
        return false;
    }

    public int setKey(String name, Object key) {
        return 0;
    }

    public boolean containsKey(String name, Object key) {
        return false;
    }

    public int getIndexForKey(String name, Object key) {
        return 0;
    }

    public Object getKeyForIndex(String name, int index) {
        return null;
    }

    public int getKeyMapSize(String name) {
        return 0;
    }
}
