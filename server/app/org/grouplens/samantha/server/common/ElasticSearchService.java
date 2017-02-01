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

package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;


import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.action.search.*;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.grouplens.samantha.server.config.ConfigKey;
import com.typesafe.config.ConfigRenderOptions;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.grouplens.samantha.server.exception.ConfigurationException;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F;
import play.libs.Json;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

@Singleton
public class ElasticSearchService {
    private final int scrollTimeout = 60000;
    private final int defaultSize = 500;
    private final int defaultFrom = 0;
    private final String cfgClusterName;
    private final String cfgHost;
    private final Integer cfgPort;

    private Client client = null;

    @Inject
    private ElasticSearchService(Configuration configuration, ApplicationLifecycle lifecycle) {
        cfgClusterName = configuration.getString(ConfigKey.ELASTICSEARCH_CLUSTER_NAME.get());
        cfgHost = configuration.getString(ConfigKey.ELASTICSEARCH_HOST.get());
        cfgPort = configuration.getInt(ConfigKey.ELASTICSEARCH_PORT.get());
        startUp();
        lifecycle.addStopHook(() -> {
            shutDown();
            return F.Promise.pure(null);
        });
    }

    protected void startUp() {
        Logger.info("Starting SearchService");
        {
            Logger.debug("ElasticSearch settings:");
            Logger.debug("* cluster.name={}", cfgClusterName);
            Logger.debug("* host={}", cfgHost);
            Logger.debug("* port={}", cfgPort);
        }

        final Settings settings = Settings.settingsBuilder()
                .put("cluster.name", cfgClusterName).build();
        try {
            client = TransportClient.builder().settings(settings).build().addTransportAddress(
                    new InetSocketTransportAddress(InetAddress.getByName(cfgHost), cfgPort));
        } catch (UnknownHostException e) {
            throw new ConfigurationException(e);
        }
        Logger.info("Connected to an elasticsearch client");
    }

    protected void shutDown() {
        if (client != null) {
            client.close();
        }
    }

    public IndicesExistsResponse existsIndex(String index) {
        return client.admin().indices().prepareExists(index)
                .execute().actionGet();
    }

    public TypesExistsResponse existsType(String index, String type) {
        return client.admin().indices().prepareTypesExists(index)
                .setTypes(type).execute().actionGet();
    }

    public CreateIndexResponse createIndex(String index, Configuration setting) {
        return client.admin().indices().prepareCreate(index)
                .setSource(setting.underlying().root()
                        .render(ConfigRenderOptions.concise()))
                .execute().actionGet();
    }

    public PutMappingResponse bulkPutMapping(String index, Configuration mapping) {
        return client.admin().indices().preparePutMapping(index)
                .setSource(mapping.underlying().root().
                        render(ConfigRenderOptions.concise())).execute().actionGet();
    }

    public PutMappingResponse putMapping(String index, String type,
                                         Configuration mapping) {
        return client.admin().indices().preparePutMapping(index).setType(type)
                .setSource(mapping.underlying().root().
                        render(ConfigRenderOptions.concise())).execute().actionGet();
    }

    public MultiSearchResponse bulkSearch(String index, String type, JsonNode query) {
        if (!query.isArray()) {
            ArrayNode array = Json.newArray();
            array.add(query);
            query = array;
        }
        MultiSearchRequestBuilder builder = client.prepareMultiSearch();
        for (JsonNode request : query) {
            builder.add(client.prepareSearch(index)
                    .setTypes(type)
                    .setQuery(request.toString()));
        }
        return builder.execute().actionGet();
    }

    public SearchResponse search(String index, String type, QueryBuilder query) {
        return client.prepareSearch(index)
                .setTypes(type)
                .setQuery(query)
                .execute().actionGet();
    }

    public SearchResponse search(String index, String type, QueryBuilder query, List<String> fields) {
        SearchRequestBuilder builder = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(query);
        for (String field : fields) {
            builder.addField(field);
        }
        return builder.execute().actionGet();
    }

    public SearchResponse search(String index, String type, JsonNode query) {
        return client.prepareSearch(index)
                .setTypes(type)
                .setQuery(query.toString())
                .execute().actionGet();
    }

    public SearchResponse search(String index, String type, JsonNode query, Set<String> fields,
                                 Integer size, Integer from) {
        SearchRequestBuilder builder = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(query.toString());
        for (String field : fields) {
            builder.addField(field);
        }
        builder.setTrackScores(true);
        if (size == null) {
            size = defaultSize;
        }
        if (from == null) {
            from = defaultFrom;
        }
        builder.setSize(size);
        builder.setFrom(from);
        return builder.execute().actionGet();
    }

    public SearchResponse search(String index, String type, JsonNode query, List<String> fields,
                                 boolean setScroll, String scrollId, Integer size, Integer from) {
        if (scrollId != null) {
            return client.prepareSearchScroll(scrollId)
                    .setScroll(new TimeValue(scrollTimeout))
                    .execute().actionGet();
        } else {
            SearchRequestBuilder builder = client.prepareSearch(index)
                    .setTypes(type)
                    .setQuery(query.toString());
            if (setScroll) {
                builder.setScroll(new TimeValue(scrollTimeout));
            }
            for (String field : fields) {
                builder.addField(field);
            }
            builder.setTrackScores(true);
            if (size == null) {
                size = defaultSize;
            }
            builder.setSize(size);
            if (from == null) {
                from = defaultFrom;
            }
            builder.setFrom(from);
            return builder.execute().actionGet();
        }
    }

    public void resetScroll(String scrollId) {
        client.prepareClearScroll().addScrollId(scrollId).execute().actionGet();
    }

    public BulkResponse bulkIndex(String index, String type, JsonNode documents) {
        if (!documents.isArray()) {
            ArrayNode array = Json.newArray();
            array.add(documents);
            documents = array;
        }
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (JsonNode document : documents) {
            bulkRequest.add(client.prepareIndex(index, type)
                    .setSource(document.toString()));
        }
        return bulkRequest.execute().actionGet();
    }

    public IndexResponse index(String index, String type, JsonNode document) {
        return client.prepareIndex(index, type)
                .setSource(document.toString())
                .execute().actionGet();
    }
}
