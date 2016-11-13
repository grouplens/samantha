package org.grouplens.samantha.server.recommender;

import org.grouplens.samantha.server.exception.InvalidRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.ranker.Ranker;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import org.grouplens.samantha.server.retriever.Retriever;
import play.Configuration;
import play.Logger;

import java.util.Map;

public class StandardRecommender implements Recommender {
    private final Configuration config;
    private final Retriever retriever;
    private final Ranker ranker;

    public StandardRecommender(Configuration config, Retriever retriever, Ranker ranker) {
        this.retriever = retriever;
        this.ranker = ranker;
        this.config = config;
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

    public Configuration getConfig() {
        return config;
    }
}
