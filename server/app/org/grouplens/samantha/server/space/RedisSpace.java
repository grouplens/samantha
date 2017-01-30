package org.grouplens.samantha.server.space;

import org.grouplens.samantha.modeler.space.SpaceMode;
import org.grouplens.samantha.server.common.RedisService;

abstract public class RedisSpace {
    protected SpaceMode spaceMode;
    protected String spaceVersion;
    protected String spaceName;
    protected SpaceType spaceType;
    protected String spaceIdentifier;
    protected final RedisService redisService;

    public RedisSpace(RedisService redisService) {
        this.redisService = redisService;
    }

    synchronized public void publishSpaceVersion() {
        redisService.set(spaceName + "_" + spaceType.get(), SpaceMode.DEFAULT.get(), spaceVersion);
        redisService.del(spaceName + "_" + spaceType.get(), SpaceMode.BUILDING.get());
        spaceMode = SpaceMode.DEFAULT;
    }
}
