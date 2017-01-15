package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;

import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

public class ESBasedJoinExpander implements EntityExpander {
    final private String elasticSearchIndex;
    final private ElasticSearchService elasticSearchService;
    final private List<Configuration> configList;

    public ESBasedJoinExpander(ElasticSearchService elasticSearchService,
                               String elasticSearchIndex,
                               List<Configuration> configList) {
        this.elasticSearchService = elasticSearchService;
        this.configList = configList;
        this.elasticSearchIndex = elasticSearchIndex;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new ESBasedJoinExpander(injector.instanceOf(ElasticSearchService.class),
                expanderConfig.getString("elasticSearchIndex"),
                expanderConfig.getConfigList("expandFields"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                  RequestContext requestContext) {
        for (Configuration config : configList) {
            String type = config.getString("type");
            List<String> keys = config.getStringList("keys");
            Map<Map<String, String>, SearchHit> keyVals = new HashMap<>();
            for (ObjectNode entity : initialResult) {
                Map<String, String> keyVal = IOUtilities.getKeyValueFromEntity(entity, keys);
                if (! keyVals.containsKey(keyVal) && keyVal.size() > 0) {
                    keyVals.put(keyVal, null);
                }
            }
            BoolQueryBuilder queryBuilder = boolQuery();
            for (Map<String, String> keyVal : keyVals.keySet()) {
                BoolQueryBuilder singleQuery = boolQuery();
                for (Map.Entry<String, String> entry : keyVal.entrySet()) {
                   singleQuery.must(QueryBuilders.termQuery(entry.getKey(), entry.getValue()));
                }
                queryBuilder.should(singleQuery);
            }
            List<String> entityFields = config.getStringList("fields");
            SearchResponse response = elasticSearchService.search(elasticSearchIndex,
                    type, queryBuilder, entityFields);
            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                Map<String, SearchHitField> hitFields = hit.getFields();
                Map<String, String> keyVal = new HashMap<>(keys.size());
                for (String key : keys) {
                    if (hitFields.containsKey(key)) {
                        //for some reason, this (String) is necessary for some environments/compilers
                        keyVal.put(key, (String) hitFields.get(key).getValue());
                    }
                }
                keyVals.put(keyVal, hit);
            }
            for (ObjectNode entity : initialResult) {
                Map<String, String> keyVal = IOUtilities.getKeyValueFromEntity(entity,
                        keys);
                if (keyVals.containsKey(keyVal) && keyVal.size() > 0) {
                    ExpanderUtilities.parseEntityFromSearchHit(entityFields,
                            null, keyVals.get(keyVal), entity);
                } else {
                    Logger.warn("Can not find the key {} while joining: {}", keyVal.toString(),
                            entity.toString());
                }
            }
        }
        return initialResult;
    }
}
