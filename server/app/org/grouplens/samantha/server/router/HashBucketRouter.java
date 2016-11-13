package org.grouplens.samantha.server.router;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.recommender.Recommender;
import play.Configuration;

import java.util.List;
import java.util.Map;

public class HashBucketRouter implements Router {
    private final List<String> predHashAttrs;
    private final List<String> recHashAttrs;
    private final Integer numPredBuckets;
    private final Integer numRecBuckets;
    private final Configuration predName2range;
    private final Configuration recName2range;

    public HashBucketRouter(List<String> predHashAttrs, List<String> recHashAttrs,
                            Integer numPredBuckets, Integer numRecBuckets,
                            Configuration predName2range, Configuration recName2range) {
        this.predHashAttrs = predHashAttrs;
        this.recHashAttrs = recHashAttrs;
        this.numPredBuckets = numPredBuckets;
        this.numRecBuckets = numRecBuckets;
        this.predName2range = predName2range;
        this.recName2range = recName2range;
    }

    private String route(RequestContext requestContext, List<String> hashAttrs, Integer numBuckets,
                         Configuration name2range) {
        String key = FeatureExtractorUtilities.composeConcatenatedKey(requestContext.getRequestBody(), hashAttrs);
        Integer bucket = key.hashCode() % numBuckets;
        String hit = null;
        for (String name : name2range.keys()) {
            List<Integer> ranges = name2range.getIntList(name);
            for (int i=0; i<ranges.size(); i+=2) {
                if (bucket >= ranges.get(i) && bucket <= ranges.get(i+1)) {
                    hit = name;
                }
            }
        }
        return hit;
    }

    public Recommender routeRecommender(Map<String, Recommender> recommenders,
                                        RequestContext requestContext) {
        return recommenders.get(route(requestContext, recHashAttrs, numRecBuckets, recName2range));
    }

    public Predictor routePredictor(Map<String, Predictor> predictors,
                                    RequestContext requestContext) {
        return predictors.get(route(requestContext, predHashAttrs, numPredBuckets, predName2range));
    }
}
