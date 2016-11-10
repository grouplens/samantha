package org.grouplens.samantha.server.reinforce;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.recommender.Recommender;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OffPolicyLearningExpander implements EntityExpander {
    final private String recommenderName;
    final private boolean recommend;
    final private Double sampleRate;
    final private List<String> itemAttrs;
    final private Injector injector;

    public OffPolicyLearningExpander(String recommenderName, boolean recommend, Double sampleRate,
                                     List<String> itemAttrs, Injector injector) {
        this.recommend = recommend;
        this.recommenderName = recommenderName;
        this.injector = injector;
        this.sampleRate = sampleRate;
        this.itemAttrs = itemAttrs;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        String recommendKey = expanderConfig.getString("recommendKey");
        boolean recommend = JsonHelpers.getOptionalBoolean(requestContext.getRequestBody(), recommendKey, false);
        return new OffPolicyLearningExpander(expanderConfig.getString("recommenderName"),
                recommend, expanderConfig.getDouble("sampleRate"),
                expanderConfig.getStringList("itemAttrs"), injector);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expandedResult = new ArrayList<>();
        if (recommend) {
            for (ObjectNode input : initialResult) {
                if (sampleRate == null || new Random().nextDouble() <= sampleRate) {
                    ObjectNode reqBody = Json.newObject();
                    reqBody.setAll(input);
                    RequestContext pseudoReq = new RequestContext(reqBody, requestContext.getEngineName());
                    SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
                    Recommender recommender = configService.getRecommender(recommenderName, pseudoReq);
                    RankedResult rankedResult = recommender.recommend(pseudoReq);
                    if (rankedResult.getLimit() > 0) {
                        String itemKey = FeatureExtractorUtilities.composeConcatenatedKey(input, itemAttrs);
                        for (Prediction prediction : rankedResult.getRankingList()) {
                            String recKey = FeatureExtractorUtilities.composeConcatenatedKey(
                                    prediction.getEntity(),
                                    itemAttrs
                            );
                            if (itemKey.equals(recKey)) {
                                expandedResult.add(input);
                            }
                        }
                    }
                }
            }
            return expandedResult;
        } else {
            return initialResult;
        }
    }
}
