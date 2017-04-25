/*
 * Copyright (c) [2016-2017] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.server.retriever;

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
    final private String queryKey;
    final private List<String> defaultMatchFields;
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
                                        Injector injector, Configuration config,
                                        String queryKey, List<String> defaultMatchFields) {
        super(config);
        this.injector = injector;
        this.retrieveFields = retrieveFields;
        this.retrieveType = retrieveType;
        this.elasticSearchIndex = elasticSearchIndex;
        this.elasticSearchReqKey = elasticSearchReqKey;
        this.elasticSearchScoreName = elasticSearchScoreName;
        this.setScrollKey = setScrollKey;
        this.queryKey = queryKey;
        this.defaultMatchFields = defaultMatchFields;
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
        return new ESQueryBasedRetrieverConfig(retrieverConfig.getString("elasticSearchIndex"),
                elasticSearchScoreName, retrieverConfig.getString("retrieveType"),
                retrieveFields, retrieverConfig.getString("elasticSearchReqKey"),
                retrieverConfig.getString("setScrollKey"),
                injector, retrieverConfig, retrieverConfig.getString("queryKey"),
                retrieverConfig.getStringList("defaultMatchFields"));
    }

    public Retriever getRetriever(RequestContext requestContext) {
        ElasticSearchService elasticSearchService = injector
                .instanceOf(ElasticSearchService.class);
        List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext, expandersConfig, injector);
        return new ESQueryBasedRetriever(elasticSearchService, expanders, elasticSearchScoreName,
                elasticSearchReqKey, setScrollKey, elasticSearchIndex, retrieveType,
                retrieveFields, config, defaultMatchFields, queryKey);
    }
}
