package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;

import java.util.List;

public class PrecomputedRetriever extends AbstractRetriever {
    final private List<ObjectNode> results;

    public PrecomputedRetriever(List<ObjectNode> results,
                                Configuration config) {
        super(config);
        this.results = results;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        return new RetrievedResult(results, results.size());
    }
}
