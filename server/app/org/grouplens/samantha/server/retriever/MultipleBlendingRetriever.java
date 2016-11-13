package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

public class MultipleBlendingRetriever extends AbstractRetriever {
    private final List<Retriever> retrievers;
    private final Integer maxHits;
    private final List<String> itemAttrs;

    public MultipleBlendingRetriever(List<Retriever> retrievers, List<String> itemAttrs, Integer maxHits,
                                     Configuration config) {
        super(config);
        this.maxHits = maxHits;
        this.retrievers = retrievers;
        this.itemAttrs = itemAttrs;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        ObjectSet<String> items = new ObjectOpenHashSet<>();
        List<ObjectNode> entities = new ArrayList<>(maxHits);
        for (Retriever retriever : retrievers) {
            long start = System.currentTimeMillis();
            RetrievedResult results = retriever.retrieve(requestContext);
            Logger.debug("{} time: {}", retriever, System.currentTimeMillis() - start);
            for (ObjectNode entity : results.getEntityList()) {
                String item = FeatureExtractorUtilities.composeConcatenatedKey(entity, itemAttrs);
                if (!items.contains(item)) {
                    items.add(item);
                    entities.add(entity);
                    if (maxHits != null && entities.size() >= maxHits) {
                        return new RetrievedResult(entities, maxHits);
                    }
                }
            }
        }
        return new RetrievedResult(entities, entities.size());
    }
}
