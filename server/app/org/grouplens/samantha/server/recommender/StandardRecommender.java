package org.grouplens.samantha.server.recommender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.exception.InvalidRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.ranker.Ranker;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import org.grouplens.samantha.server.retriever.Retriever;
import play.Logger;
import play.libs.Json;

public class StandardRecommender implements Recommender {
    private final Retriever retriever;
    private final Ranker ranker;

    public StandardRecommender(Retriever retriever, Ranker ranker) {
        this.retriever = retriever;
        this.ranker = ranker;
    }

    public RankedResult recommend(RequestContext requestContext)
            throws InvalidRequestException {
        long start = System.currentTimeMillis();
        RetrievedResult retrievedResult = retriever.retrieve(requestContext);
        Logger.debug("Retriever time: {}", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        RankedResult recommendations = ranker.rank(retrievedResult, requestContext);
        Logger.debug("Ranker time: {}", System.currentTimeMillis() - start);
        return recommendations;
    }

    public JsonNode getFootprint() {
        ObjectNode print = Json.newObject();
        print.set("retriever", retriever.getFootprint());
        print.set("ranker", ranker.getFootprint());
        return print;
    }
}
