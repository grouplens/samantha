package org.grouplens.samantha.server.retriever;

import com.typesafe.config.ConfigRenderOptions;

import org.grouplens.samantha.server.common.ElasticSearchService;

import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class ESQueryBasedRetrieverConfig implements RetrieverConfig {
    final Injector injector;
    final Configuration elasticSearchSetting;
    final Configuration elasticSearchMapping;
    final String defaultElasticSearchReq;
    final String elasticSearchIndex;
    final String elasticSearchScoreName;
    final String elasticSearchReqKey;
    final String retrieveType;
    final String setScrollKey;
    final List<String> retrieveFields;
    final List<Configuration> expandersConfig;

    private ESQueryBasedRetrieverConfig(Configuration elasticSearchMapping,
                                        String elasticSearchIndex,
                                        String elasticSearchScoreName,
                                        String retrieveType,
                                        List<String> retrieveFields,
                                        String elasticSearchReqKey,
                                        String setScrollKey,
                                        List<Configuration> expandersConfig,
                                        Configuration elasticSearchSetting,
                                        String defaultElasticSearchReq,
                                        Injector injector) {
        this.injector = injector;
        this.retrieveFields = retrieveFields;
        this.retrieveType = retrieveType;
        this.elasticSearchMapping = elasticSearchMapping;
        this.elasticSearchIndex = elasticSearchIndex;
        this.elasticSearchReqKey = elasticSearchReqKey;
        this.elasticSearchScoreName = elasticSearchScoreName;
        this.elasticSearchSetting = elasticSearchSetting;
        this.setScrollKey = setScrollKey;
        this.expandersConfig = expandersConfig;
        this.defaultElasticSearchReq = defaultElasticSearchReq;
    }

    static public RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector)
            throws ConfigurationException {
        ElasticSearchService elasticSearchService = injector
                .instanceOf(ElasticSearchService.class);
        String elasticSearchIndex = retrieverConfig.getString("elasticSearchIndex");
        Configuration settingConfig = retrieverConfig.getConfig("elasticSearchSetting");
        if (!elasticSearchService.existsIndex(elasticSearchIndex).isExists()) {
            elasticSearchService.createIndex(elasticSearchIndex, settingConfig);
        }
        String elasticSearchScoreName = retrieverConfig.getString("elasticSearchScoreName");
        List<String> retrieveFields = retrieverConfig.getStringList("retrieveFields");
        Configuration mappingConfig = retrieverConfig.getConfig("elasticSearchMapping");
        for (String typeName : mappingConfig.subKeys()) {
            if (!elasticSearchService.existsType(elasticSearchIndex, typeName)
                    .isExists()) {
                elasticSearchService.putMapping(elasticSearchIndex, typeName,
                        mappingConfig.getConfig(typeName));
            }
        }
        List<Configuration> expandersConfig = ExpanderUtilities.getEntityExpandersConfig(retrieverConfig);
        String defaultReq = null;
        if (retrieverConfig.asMap().containsKey("defaultElasticSearchReq")) {
            defaultReq = retrieverConfig.getConfig("defaultElasticSearchReq")
                    .underlying().root().render(ConfigRenderOptions.concise());
        }
        return new ESQueryBasedRetrieverConfig(mappingConfig,
                retrieverConfig.getString("elasticSearchIndex"), elasticSearchScoreName,
                retrieverConfig.getString("retrieveType"),
                retrieveFields, retrieverConfig.getString("elasticSearchReqKey"),
                retrieverConfig.getString("setScrollKey"), expandersConfig,
                settingConfig, defaultReq, injector);
    }

    public Retriever getRetriever(RequestContext requestContext) {
        ElasticSearchService elasticSearchService = injector
                .instanceOf(ElasticSearchService.class);
        List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext, expandersConfig, injector);
        return new ESQueryBasedRetriever(elasticSearchService, expanders, this);
    }
}
