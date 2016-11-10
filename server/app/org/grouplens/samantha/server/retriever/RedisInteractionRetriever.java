package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.svdfeature.KnnModelFeatureTrigger;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Logger;
import play.libs.Json;

import java.util.List;

public class RedisInteractionRetriever implements Retriever {
    private final RedisKeyBasedRetriever retriever;
    private final KnnModelFeatureTrigger trigger;
    private final List<EntityExpander> expanders;

    public RedisInteractionRetriever(RedisKeyBasedRetriever retriever,
                                     KnnModelFeatureTrigger trigger,
                                     List<EntityExpander> expanders) {
        this.retriever = retriever;
        this.trigger = trigger;
        this.expanders = expanders;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        long start = System.currentTimeMillis();
        RetrievedResult interactions =retriever.retrieve(requestContext);
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

    public JsonNode getFootprint() {
        return Json.newObject();
    }
}
