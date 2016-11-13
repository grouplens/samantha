package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class FieldBlendingPredictor extends AbstractPredictor {
    private final List<EntityExpander> entityExpanders;
    private final Object2DoubleMap<String> defaults;

    public FieldBlendingPredictor(Configuration config, Configuration daoConfigs, Injector injector,
                                  List<EntityExpander> entityExpanders,
                                  Object2DoubleMap<String> defaults, String daoConfigKey) {
        super(config, daoConfigs, daoConfigKey, injector);
        this.entityExpanders = entityExpanders;
        this.defaults = defaults;
    }

    public List<Prediction> predict(List<ObjectNode> entityList, RequestContext requestContext) {
        for (EntityExpander expander : entityExpanders) {
            entityList = expander.expand(entityList, requestContext);
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            double score = 0.0;
            for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
                String key = entry.getKey();
                if (entity.has(key)) {
                    score += (entry.getDoubleValue() * entity.get(key).asDouble());
                }
            }
            scoredList.add(new Prediction(entity, null, score));
        }
        return scoredList;
    }
}
