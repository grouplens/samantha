package org.grouplens.samantha.server.space;

import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceMode;
import org.grouplens.samantha.server.common.RedisService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class RedisIndexSpace extends RedisSpace implements IndexSpace {
    private static final long serialVersionUID = 1L;

    @Inject
    public RedisIndexSpace(RedisService redisService) {
        super(redisService);
    }

    synchronized public void setSpaceState(String spaceName, SpaceMode spaceMode) {
        if (spaceVersion == null) {
            spaceVersion = redisService.incre(spaceName, SpaceType.INDEX.get()).toString();
            redisService.set(spaceName + "_" + SpaceType.INDEX.get(), spaceMode.get(), spaceVersion);
        }
        this.spaceMode = spaceMode;
        this.spaceName = spaceName;
        this.spaceType = SpaceType.INDEX;
        this.spaceIdentifier = RedisService.composeKey(spaceName + "_" + spaceType.get(), spaceVersion);
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
        String value = redisService.get(spaceIdentifier, RedisService.composeKey(name, (String) key));
        if (value != null) {
            return Integer.parseInt(value);
        }
        String watchKey = RedisService.composeKey(name, (String) key);
        redisService.watchKey(watchKey);
        redisService.multi();
        int index = redisService.incre(spaceIdentifier, name).intValue();
        redisService.set(spaceIdentifier, RedisService.composeKey(name, (String) key), Integer.valueOf(index).toString());
        List<Object> resps = redisService.exec();
        if (resps.get(0) == null) {
            index = getIndexForKey(name, key);
        }
        String idxStr = RedisService.composeKey(RedisService.composeKey(name, "index"), Integer.valueOf(index).toString());
        redisService.set(spaceIdentifier, idxStr, (String) key);
        return index;
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
