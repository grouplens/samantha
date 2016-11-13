package org.grouplens.samantha.server.router;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class HashBucketRouterConfig implements RouterConfig {
    private final List<String> predHashAttrs;
    private final List<String> recHashAttrs;
    private final Integer numPredBuckets;
    private final Integer numRecBuckets;
    private final Configuration predName2range;
    private final Configuration recName2range;

    private HashBucketRouterConfig(List<String> predHashAttrs, List<String> recHashAttrs,
                            Integer numPredBuckets, Integer numRecBuckets,
                            Configuration predName2range, Configuration recName2range) {
        this.predHashAttrs = predHashAttrs;
        this.recHashAttrs = recHashAttrs;
        this.numPredBuckets = numPredBuckets;
        this.numRecBuckets = numRecBuckets;
        this.predName2range = predName2range;
        this.recName2range = recName2range;
    }

    public static RouterConfig getRouterConfig(Configuration routerConfig,
                                               Injector injector) {
        Configuration predictorConfig = routerConfig.getConfig("predictorConfig");
        Configuration recommenderConfig = routerConfig.getConfig("recommenderConfig");
        return new HashBucketRouterConfig(
                predictorConfig.getStringList("hashAttrs"),
                recommenderConfig.getStringList("hashAttrs"),
                predictorConfig.getInt("numBuckets"),
                recommenderConfig.getInt("numBuckets"),
                predictorConfig.getConfig("name2range"),
                recommenderConfig.getConfig("name2range")
        );
    }

    public Router getRouter(RequestContext requestContext) {
        return new HashBucketRouter(predHashAttrs, recHashAttrs, numPredBuckets, numRecBuckets,
                predName2range, recName2range);
    }
}
