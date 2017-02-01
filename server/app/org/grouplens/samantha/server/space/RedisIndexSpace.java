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
        if (spaceMode.equals(SpaceMode.DEFAULT)) {
            spaceVersion = redisService.get(spaceName + "_" + SpaceType.INDEX.get(), spaceMode.get());
        }
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
        String watchKey = RedisService.composeKey(name, (String) key);
        String value = redisService.get(spaceIdentifier, watchKey);
        if (value != null) {
            return Integer.parseInt(value);
        }
        int index = Integer.parseInt(redisService.get(spaceIdentifier, name));
        index += 1;
        String idxStr = RedisService.composeKey(name + "_IDX_", Integer.valueOf(index).toString());
        redisService.watch(spaceIdentifier, watchKey);
        redisService.multi(false);
        redisService.setWithoutLock(spaceIdentifier, watchKey, Integer.valueOf(index).toString());
        redisService.increWithoutLock(spaceIdentifier, name);
        redisService.setWithoutLock(spaceIdentifier, idxStr, (String) key);
        List<Object> resps = redisService.exec();
        if (resps.get(0) == null) {
            index = getIndexForKey(name, key);
        }
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
