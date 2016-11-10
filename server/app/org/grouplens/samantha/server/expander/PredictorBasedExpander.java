package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.predictor.Predictor;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class PredictorBasedExpander implements EntityExpander {
    private final Predictor predictor;
    private final String scoreAttr;
    private final String instanceAttr;

    public PredictorBasedExpander(Predictor predictor, String scoreAttr, String instanceAttr) {
        this.predictor = predictor;
        this.scoreAttr = scoreAttr;
        this.instanceAttr = instanceAttr;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                      Injector injector, RequestContext requestContext) {
        String predictorName = expanderConfig.getString("predictorName");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Predictor predictor = configService.getPredictor(predictorName, requestContext);
        return new PredictorBasedExpander(predictor, expanderConfig.getString("scoreAttr"),
                expanderConfig.getString("instanceAttr"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                            RequestContext requestContext){
        List<Prediction> predictions = predictor.predict(initialResult, requestContext);
        for (int i=0; i<predictions.size(); i++) {
            ObjectNode entity = initialResult.get(i);
            entity.put(scoreAttr, predictions.get(i).getScore());
            if (instanceAttr != null) {
                entity.put(instanceAttr, predictions.get(i).getInstanceString());
            }
        }
        return initialResult;
    }
}
