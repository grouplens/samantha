package org.grouplens.samantha.server.space;

import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.server.common.RedisService;

import javax.inject.Inject;

public class RedisIndexSpace extends RedisSpace implements IndexSpace {
    private static final long serialVersionUID = 1L;

    @Inject
    public RedisIndexSpace(RedisService redisService) {
        super(redisService);
    }

    public void requestKeyMap(String name) {
        redisService.set(spaceIdentifier, name, "-1");
    }

    public boolean hasKeyMap(String name) {
        String value = redisService.get(spaceIdentifier, name);
        if (value == null) {
            return false;
        } else {
            return true;
        }
    }

    public int setKey(String name, Object key) {
        Long index = redisService.incre(spaceIdentifier, name);
        redisService.set(spaceIdentifier, RedisService.composeKey(name, (String) key), index.toString());
        String idxStr = RedisService.composeKey(RedisService.composeKey(name, "index"), index.toString());
        redisService.set(spaceIdentifier, idxStr, (String) key);
        return index.intValue();
    }

    public boolean containsKey(String name, Object key) {
        String value = redisService.get(spaceIdentifier, RedisService.composeKey(name, (String) key));
        if (value != null) {
            return true;
        } else {
            return false;
        }
    }

    public int getIndexForKey(String name, Object key) {
        return Integer.parseInt(redisService.get(spaceIdentifier, RedisService.composeKey(name, (String) key)));
    }

    public Object getKeyForIndex(String name, int index) {
        String idxStr = RedisService.composeKey(RedisService.composeKey(name, "index"),
                Integer.valueOf(index).toString());
        return redisService.get(spaceIdentifier, idxStr);
    }

    public int getKeyMapSize(String name) {
        return Integer.parseInt(redisService.get(spaceIdentifier, name));
    }
}
