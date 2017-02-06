package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;

import java.util.ArrayList;
import java.util.List;

public class PrecomputedRetriever extends AbstractRetriever {
    final private List<EntityExpander> expanders;
    final private List<ObjectNode> results;

    public PrecomputedRetriever(List<ObjectNode> results,
                                List<EntityExpander> expanders, Configuration config) {
        super(config);
        this.expanders = expanders;
        this.results = results;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        List<ObjectNode> entities = new ArrayList<>(results.size());
        for (ObjectNode result : results) {
            entities.add(result.deepCopy());
        }
        entities = ExpanderUtilities.expand(entities, expanders, requestContext);
        return new RetrievedResult(entities, entities.size());
    }
}
