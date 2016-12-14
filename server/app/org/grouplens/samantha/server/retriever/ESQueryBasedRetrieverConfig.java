package org.grouplens.samantha.server.retriever;

import com.typesafe.config.ConfigRenderOptions;

import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.common.ElasticSearchService;

import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class ESQueryBasedRetrieverConfig extends AbstractComponentConfig implements RetrieverConfig {
    final private Injector injector;
    final private String defaultElasticSearchReq;
    final private String elasticSearchIndex;
    final private String elasticSearchScoreName;
    final private String elasticSearchReqKey;
    final private String retrieveType;
    final private String setScrollKey;
    final private List<String> retrieveFields;

    private ESQueryBasedRetrieverConfig(String elasticSearchIndex,
                                        String elasticSearchScoreName,
                                        String retrieveType,
                                        List<String> retrieveFields,
                                        String elasticSearchReqKey,
                                        String setScrollKey,
                                        String defaultElasticSearchReq,
                                        Injector injector, Configuration config) {
        super(config);
        this.injector = injector;
        this.retrieveFields = retrieveFields;
        this.retrieveType = retrieveType;
        this.elasticSearchIndex = elasticSearchIndex;
        this.elasticSearchReqKey = elasticSearchReqKey;
        this.elasticSearchScoreName = elasticSearchScoreName;
        this.setScrollKey = setScrollKey;
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
        String defaultReq = null;
        if (retrieverConfig.asMap().containsKey("defaultElasticSearchReq")) {
            defaultReq = retrieverConfig.getConfig("defaultElasticSearchReq")
                    .underlying().root().render(ConfigRenderOptions.concise());
        }
        return new ESQueryBasedRetrieverConfig(retrieverConfig.getString("elasticSearchIndex"),
                elasticSearchScoreName, retrieverConfig.getString("retrieveType"),
                retrieveFields, retrieverConfig.getString("elasticSearchReqKey"),
                retrieverConfig.getString("setScrollKey"),
                defaultReq, injector, retrieverConfig);
    }

    public Retriever getRetriever(RequestContext requestContext) {
        ElasticSearchService elasticSearchService = injector
                .instanceOf(ElasticSearchService.class);
        List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext, expandersConfig, injector);
        return new ESQueryBasedRetriever(elasticSearchService, expanders, elasticSearchScoreName,
                elasticSearchReqKey, defaultElasticSearchReq, setScrollKey, elasticSearchIndex, retrieveType,
                retrieveFields, config);
    }
}
