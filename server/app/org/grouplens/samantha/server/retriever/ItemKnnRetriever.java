package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.knn.KnnModelFeatureTrigger;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;

import java.util.List;

public class ItemKnnRetriever extends AbstractRetriever {
    private final Retriever retriever;
    private final KnnModelFeatureTrigger trigger;
    private final List<EntityExpander> expanders;

    public ItemKnnRetriever(Retriever retriever,
                            KnnModelFeatureTrigger trigger,
                            List<EntityExpander> expanders, Configuration config) {
        super(config);
        this.retriever = retriever;
        this.trigger = trigger;
        this.expanders = expanders;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        long start = System.currentTimeMillis();
        RetrievedResult interactions = retriever.retrieve(requestContext);
        Logger.debug("Redis retrieving time: {}", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        List<ObjectNode> results = trigger.getTriggeredFeatures(interactions.getEntityList());
        Logger.debug("Feature trigger time: {}", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        results = ExpanderUtilities.expand(results, expanders, requestContext);
        Logger.debug("Expanding time: {}", System.currentTimeMillis() - start);
        interactions.setEntityList(results);
        return interactions;
    }
}
