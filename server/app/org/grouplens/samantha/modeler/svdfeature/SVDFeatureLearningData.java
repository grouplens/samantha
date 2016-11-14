package org.grouplens.samantha.modeler.svdfeature;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

public class SVDFeatureLearningData implements LearningData {

    private final SVDFeatureModel model;
    private final EntityDAO entityDao;

    public SVDFeatureLearningData(EntityDAO entityDao,
                                  SVDFeatureModel model) {
        this.model = model;
        this.entityDao = entityDao;
    }

    public LearningInstance getLearningInstance() {
        if (entityDao.hasNextEntity()) {
            JsonNode entity = entityDao.getNextEntity();
            return model.featurize(entity, true);
        } else {
            entityDao.close();
            return null;
        }
    }

    public void startNewIteration() {
        entityDao.restart();
    }
}
