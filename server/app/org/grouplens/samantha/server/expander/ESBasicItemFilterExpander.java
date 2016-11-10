package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.ESQueryBasedRetriever;
import org.grouplens.samantha.server.retriever.ESRetrieverUtilities;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class ESBasicItemFilterExpander implements EntityExpander {
    final private ESQueryBasedRetriever retriever;
    final private List<String> itemAttrs;
    final private String defaultMatch;
    final private String elasticSearchReqKey;

    public ESBasicItemFilterExpander(ESQueryBasedRetriever retriever, List<String> itemAttrs,
                                     String defaultMatch, String elasticSearchReqKey) {
        this.retriever = retriever;
        this.itemAttrs = itemAttrs;
        this.defaultMatch = defaultMatch;
        this.elasticSearchReqKey = elasticSearchReqKey;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        String retrieverName = expanderConfig.getString("retrieverName");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ESQueryBasedRetriever retriever = (ESQueryBasedRetriever) configService.getRetriever(retrieverName,
                requestContext);
        return new ESBasicItemFilterExpander(retriever, expanderConfig.getStringList("itemAttrs"),
                expanderConfig.getString("defaultMatch"), expanderConfig.getString("elasticSearchReqKey"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                            RequestContext requestContext) {
        RetrievedResult filters = ESRetrieverUtilities.requestOrDefaultMatch(requestContext, retriever,
                defaultMatch, elasticSearchReqKey);
        return ExpanderUtilities.basicItemFilter(initialResult, filters, itemAttrs, true);
    }
}
