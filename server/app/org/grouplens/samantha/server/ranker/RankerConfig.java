package org.grouplens.samantha.server.ranker;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface RankerConfig {
    Ranker getRanker(RequestContext requestContext);
    static RankerConfig getRankerConfig(Configuration rankerConfig,
                                        Injector injector) {return null;}
}
