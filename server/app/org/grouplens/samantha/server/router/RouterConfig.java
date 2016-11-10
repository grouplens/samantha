package org.grouplens.samantha.server.router;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface RouterConfig {
    static RouterConfig getRouterConfig(Configuration routerConfig,
                                        Injector injector) {return null;}
    Router getRouter(RequestContext requestContext);
}
