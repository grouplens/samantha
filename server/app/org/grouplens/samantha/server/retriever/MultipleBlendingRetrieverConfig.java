package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class MultipleBlendingRetrieverConfig implements RetrieverConfig {
    private final List<String> retrieverNames;
    private final List<String> itemAttrs;
    private final Integer maxHits;
    private final Injector injector;
    private final Configuration config;

    private MultipleBlendingRetrieverConfig(List<String> retrieverNames, List<String> itemAttrs, Integer maxHits,
                                            Injector injector, Configuration config) {
        this.injector = injector;
        this.retrieverNames = retrieverNames;
        this.itemAttrs = itemAttrs;
        this.maxHits = maxHits;
        this.config = config;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        return new MultipleBlendingRetrieverConfig(retrieverConfig.getStringList("retrieverNames"),
                retrieverConfig.getStringList("itemAttrs"),
                retrieverConfig.getInt("maxHits"), injector, retrieverConfig);
    }

    public Retriever getRetriever(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        List<Retriever> retrievers = new ArrayList<>(retrieverNames.size());
        for (String name : retrieverNames) {
            retrievers.add(configService.getRetriever(name, requestContext));
        }
        return new MultipleBlendingRetriever(retrievers, itemAttrs, maxHits, config);
    }
}
