package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.Retriever;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class RetrieverBasedItemFilterExpander implements EntityExpander {
    final private Retriever retriever;
    final private List<String> itemAttrs;
    final private boolean exclude;

    public RetrieverBasedItemFilterExpander(Retriever retriever, List<String> itemAttrs,
                                            boolean exclude) {
        this.retriever = retriever;
        this.itemAttrs = itemAttrs;
        this.exclude = exclude;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        String retrieverName = expanderConfig.getString("retrieverName");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Retriever retriever = configService.getRetriever(retrieverName, requestContext);
        boolean exclude = true;
        if (expanderConfig.asMap().containsKey("exclude")) {
            exclude = expanderConfig.getBoolean("exclude");
        }
        return new RetrieverBasedItemFilterExpander(retriever, expanderConfig.getStringList("itemAttrs"),
                exclude);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        RetrievedResult filters = retriever.retrieve(requestContext);
        return ExpanderUtilities.basicItemFilter(initialResult, filters, itemAttrs, exclude);
    }
}
