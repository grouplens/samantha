package org.grouplens.samantha.server.router;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class BasicRouterConfig implements RouterConfig {
    private final String recommenderKey;
    private final String predictorKey;

    private BasicRouterConfig(String recommenderKey, String predictorKey) {
        this.recommenderKey = recommenderKey;
        this.predictorKey = predictorKey;
    }

    public static RouterConfig getRouterConfig(Configuration routerConfig,
                                        Injector injector) {
        return new BasicRouterConfig(routerConfig.getString("recommenderKey"),
                routerConfig.getString("predictorKey"));
    }

    public Router getRouter(RequestContext requestContext) {
        return new BasicRouter(recommenderKey, predictorKey);
    }
}
