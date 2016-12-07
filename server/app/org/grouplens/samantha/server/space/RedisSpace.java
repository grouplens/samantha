package org.grouplens.samantha.server.space;

import org.grouplens.samantha.modeler.space.SpaceMode;
import org.grouplens.samantha.server.common.RedisService;

abstract public class RedisSpace {
    protected SpaceMode spaceMode;
    protected String spaceVersion;
    protected String spaceName;
    protected String spaceIdentifier;
    protected final RedisService redisService;

    public RedisSpace(RedisService redisService) {
        this.redisService = redisService;
    }

    synchronized public void setSpaceState(String spaceName, SpaceMode spaceMode) {
        spaceVersion = redisService.get(spaceName, spaceMode.get());
        if (spaceVersion == null) {
            spaceVersion = redisService.incre(spaceName, "version").toString();
            redisService.set(spaceName, spaceMode.get(), spaceVersion);
        }
        this.spaceMode = spaceMode;
        this.spaceName = spaceName;
        this.spaceIdentifier = RedisService.composeKey(spaceName, spaceVersion);
    }

    synchronized public void publishSpaceVersion() {
        redisService.set(spaceName, SpaceMode.DEFAULT.get(), spaceVersion);
        redisService.del(spaceName, SpaceMode.BUILDING.get());
        spaceMode = SpaceMode.DEFAULT;
    }
}
