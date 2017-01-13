package org.grouplens.samantha.ephemeral;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureKey;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.AbstractRanker;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Configuration;

import java.util.List;

public class EphemeralRanker extends AbstractRanker {
    private final SVDFeature svdFeature;

    public EphemeralRanker(Configuration config, SVDFeature svdFeature) {
        super(config);
        this.svdFeature = svdFeature;
    }

    public RankedResult rank(RetrievedResult retrievedResult, RequestContext requestContext) {
        for (ObjectNode entity : retrievedResult.getEntityList()) {
            String userId = entity.get("userId").asText();
            String movieId = entity.get("movieId").asText();
            //TODO: check that this feature in the space
            int uidx = svdFeature.getIndexForKey(SVDFeatureKey.FACTORS.get(),
                    FeatureExtractorUtilities.composeKey("userId", userId));
            int midx = svdFeature.getIndexForKey(SVDFeatureKey.FACTORS.get(),
                    FeatureExtractorUtilities.composeKey("movieId", movieId));
            RealVector ufact = svdFeature.getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), uidx);
            RealVector mfact = svdFeature.getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), midx);
            double score = ufact.dotProduct(mfact);
            entity.put("score", score);
        }
        Ordering<ObjectNode> ordering = RetrieverUtilities.jsonFieldOrdering("score");
        List<ObjectNode> candidates = ordering.greatestOf(retrievedResult.getEntityList(), 200);
        //diversification
        return null;
    }
}
