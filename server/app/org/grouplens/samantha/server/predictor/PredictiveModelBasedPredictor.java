package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigRenderOptions;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class PredictiveModelBasedPredictor extends AbstractPredictor {
    private final PredictiveModel predictiveModel;
    private final Featurizer featurizer;
    private final Configuration config;
    private final List<EntityExpander> entityExpanders;

    public PredictiveModelBasedPredictor(Configuration config,
                                         PredictiveModel predictiveModel,
                                         Featurizer featurizer,
                                         Configuration daoConfigs,
                                         Injector injector,
                                         List<EntityExpander> entityExpanders,
                                         String daoConfigKey) {
        super(config, daoConfigs, daoConfigKey, injector);
        this.predictiveModel = predictiveModel;
        this.featurizer = featurizer;
        this.config = config;
        this.entityExpanders = entityExpanders;
    }


    public List<Prediction> predict(List<ObjectNode> entityList,
                                    RequestContext requestContext) {
        for (EntityExpander expander : entityExpanders) {
            entityList = expander.expand(entityList, requestContext);
        }
        List<LearningInstance> instanceList = new ArrayList<>(entityList.size());
        for (JsonNode entity : entityList) {
            instanceList.add(featurizer.featurize(entity, false));
        }
        List<Prediction> results = new ArrayList<>(entityList.size());
        for (int i=0; i<entityList.size(); i++) {
            ObjectNode entity = entityList.get(i);
            LearningInstance ins = instanceList.get(i);
            double score = predictiveModel.predict(ins);
            results.add(new Prediction(entity, ins, score));
        }
        return results;
    }

    public Configuration getConfig() {
        return config;
    }
}
